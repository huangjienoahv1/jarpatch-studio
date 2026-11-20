const crypto = require('crypto');
const fs = require('fs/promises');
const { setTimeout: delay } = require('timers/promises');
const fetch = require('./http-fetch');
const { runSsh, shellCommand, curlConfig } = require('./runtime-transport');
const { RuntimeTargets } = require('./runtime-targets');
const { openPasswordSession, closePasswordSession, passwordSessionLabel } = require('./runtime-password-ssh');
const { HTTP_METHODS, normalizeSuite, sha256, assertionResults, captureMarker, parseHttpCapture,
  selectCorrelatedLogs } = require('./runtime-validation');
const { collectClass, collectProfile } = require('./runtime-artifacts');
const { queryLoki, actuatorPath } = require('./runtime-integrations');
const { readPatch, validatePatch, patchDirectory, patchResult } = require('./runtime-isolated-patch');
const { JAVA_CLASS, describeRuntimeMethods } = require('./runtime-methods');

const ARTHAS_VERSION = '4.3.5';
const ARTHAS_SIZE = 17150150;
const ARTHAS_SHA256 = 'f4ea8e4ab4bb20a9925c21780bfb91331e73968eae881fd89b855cccaad21f1c';
const ARTHAS_URL = 'https://github.com/alibaba/arthas/releases/download/arthas-all-' + ARTHAS_VERSION + '/arthas-bin.zip';
const ARTHAS_USER = 'jarpatch';
const LOG_SECONDS = 300;
const OBSERVE_SECONDS = 20;
const MAX_PATCH_SOURCE_BYTES = 2 * 1024 * 1024;
const CONTAINER_ID = /^[0-9a-f]{64}$/;
const CLASS_SEARCH = /^[A-Za-z_$*][\w$.*]*$/;
const JAVA_METHOD = /^[A-Za-z_$][\w$]*$/;
const DNS_NAME = /^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)*[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/i;

const PROCESS_START = 'awk \'{sub(/^.*\\) /,""); print $20}\'';
const SCAN_JAVA = [
  'for proc in /proc/[0-9]*; do',
  '  exe=$(readlink "$proc/exe" 2>/dev/null) || exe=',
  '  case "$exe" in */bin/java) ;; "") [ "$(cat "$proc/comm" 2>/dev/null)" = java ] || continue ;; *) continue ;; esac',
  '  start=$(' + PROCESS_START + ' "$proc/stat" 2>/dev/null) || continue',
  '  ids=$(awk \'/^Uid:/{uid=$3} /^Gid:/{gid=$3} END{print uid ":" gid}\' "$proc/status" 2>/dev/null) || continue',
  '  printf "%s\\t%s\\t%s\\t%s\\n" "$(basename "$proc")" "$start" "$ids" "$exe"',
  'done'
].join('\n');

// 同一容器内 PID 也可能被复用，用进程启动计数重新核对目标。
const CHECK_TARGET = [
  'pid=$1; expected=$2; shift 2',
  'actual=$(' + PROCESS_START + ' "/proc/$pid/stat" 2>/dev/null) || exit 191',
  '[ "$actual" = "$expected" ] || exit 191'
].join('\n') + '\n';
const FIND_PORTS = [
  'fds=$(ls -l "/proc/$pid/fd" 2>/dev/null) || exit 192',
  'set -- /proc/net/tcp; if [ -r /proc/net/tcp6 ]; then set -- "$@" /proc/net/tcp6; fi',
  'printf "%s\\n" "$fds" | awk \'',
  '  /socket:\\[[0-9]+\\]$/ { sub(/^.*socket:\\[/,""); sub(/\\]$/,""); sockets[$0]=1 }',
  '  END { for (inode in sockets) print inode }\' |',
  'awk \'FILENAME=="-" {sockets[$1]=1; next}',
  '  $4=="0A" && ($10 in sockets) {n=split($2,a,":"); print a[n]}\' - "$@"',
].join('\n');

// 新建目录由所选 JVM 的系统用户拥有，上传、解压和 Attach 都用该身份。
const PREPARE = CHECK_TARGET + [
  'set -eu',
  '[ -x "$2" ] || exit 194',
  'missing=""',
  'for tool in curl timeout; do',
  '  command -v "$tool" >/dev/null 2>&1 || missing="${missing:+$missing、}$tool"',
  'done',
  'if command -v unzip >/dev/null 2>&1; then extractor=unzip',
  'elif [ -x "${2%/*}/jar" ]; then extractor=jdk-jar',
  'elif command -v jar >/dev/null 2>&1; then extractor=jar',
  'else extractor=""; missing="${missing:+$missing、}unzip 或 JDK jar"; fi',
  'if [ -n "$missing" ]; then printf \'缺少容器工具：%s\\n\' "$missing" >&2; exit 193; fi',
  'umask 077; mkdir "$1"',
  'printf "%s\\n" "$extractor"'
].join('\n');
const UNPACK_ARTHAS = [
  'case "$4" in',
  '  unzip) unzip -oq package.zip ;;',
  '  jdk-jar) "${2%/*}/jar" xf package.zip ;;',
  '  jar) jar xf package.zip ;;',
  '  *) echo "Unknown Arthas extractor" >&2; exit 200 ;;',
  'esac',
  'rm -f package.zip'
].join('\n');
const START_ARTHAS = CHECK_TARGET + [
  'set -eu; umask 077; cd "$1"',
  'porthex=$(printf "%04X" "$3")',
  'if awk -v port=":$porthex" \'$4=="0A" && $2 ~ port"$" {found=1} END{exit !found}\' /proc/net/tcp*; then exit 195; fi',
  UNPACK_ARTHAS,
  'cat > arthas.properties',
  'home=$1; java=$2; port=$3; tools=$(dirname "$(dirname "$java")")/lib/tools.jar',
  'set --; if [ -r "$tools" ]; then set -- "-Xbootclasspath/a:$tools"; fi',
  'exec timeout 28 "$java" "$@" -jar arthas-core.jar -pid "$pid" -target-ip 127.0.0.1 -telnet-port -1 \\',
  '  -http-port "$port" -session-timeout 120 -core "$home/arthas-core.jar" -agent "$home/arthas-agent.jar"'
].join('\n');
const START_ARTHAS_CROSS = CHECK_TARGET + [
  'set -eu; umask 077; cd "$1"',
  'porthex=$(printf "%04X" "$3")',
  'if awk -v port=":$porthex" \'$4=="0A" && $2 ~ port"$" {found=1} END{exit !found}\' /proc/net/tcp*; then exit 195; fi',
  UNPACK_ARTHAS,
  'cat > arthas.properties',
  // Bootstrap staggers the install through the target JVM mount namespace. The properties keep Telnet disabled.
  'exec timeout 33 "$2" -jar arthas-boot.jar --arthas-home "$1" --attach-only \\',
  '  --target-ip 127.0.0.1 --telnet-port 0 --http-port "$3" --session-timeout 120 "$pid"'
].join('\n');
const READ_ARTIFACT = [
  'case "$1" in "$2"/*) ;; *) exit 198 ;; esac',
  'rest=${1#"$2"/}; current="$mount$2"',
  '[ -L "$current" ] && exit 198',
  // Canonicalizing /proc/PID/root would lose its mount namespace. Check each path component in place.
  'while :; do',
  '  piece=${rest%%/*}',
  '  case "$piece" in ""|.|..) exit 198 ;; esac',
  '  current="$current/$piece"; [ -L "$current" ] && exit 198',
  '  [ "$rest" = "$piece" ] && break',
  '  rest=${rest#*/}',
  'done',
  '[ -s "$current" ] && [ -f "$current" ] || exit 197',
  'exec cat -- "$current"'
].join('\n');

function requireContainer(id) {
  if (typeof id !== 'string' || !CONTAINER_ID.test(id)) throw new Error('请选择运行中的容器');
  return id;
}

function readLines(output) {
  return output.split(/\r?\n/).filter(Boolean);
}

function assertMatch(value, pattern, message, maxLength = 240) {
  if (typeof value !== 'string' || value.length > maxLength || !pattern.test(value)) throw new Error(message);
  return value;
}

function throwableMessage(error) {
  return error instanceof Error ? error.message : String(error);
}

function targetIdentity({ host, container, pid, start }) {
  return { host, container, pid, start, hostLabel: passwordSessionLabel(host) };
}

async function getDistribution(file, signal) {
  let contents;
  if (file) {
    const handle = await fs.open(file, 'r');
    try {
      if ((await handle.stat()).size !== ARTHAS_SIZE) throw new Error('离线包大小与官方 Arthas ' + ARTHAS_VERSION + ' 不符');
      contents = await handle.readFile({ signal });
    } finally {
      await handle.close();
    }
  } else {
    const controller = new AbortController();
    const abort = () => controller.abort();
    const timer = setTimeout(abort, 45000);
    signal.addEventListener('abort', abort, { once: true });
    if (signal.aborted) abort();
    try {
      const response = await fetch(ARTHAS_URL, { signal: controller.signal });
      if (!response.ok) throw new Error('下载 Arthas 失败（HTTP ' + response.status + '），也可选择官方离线包');
      const chunks = [];
      let bytes = 0;
      for await (const chunk of response.body) {
        bytes += chunk.length;
        if (bytes > ARTHAS_SIZE) {
          abort();
          throw new Error('下载包大小与官方 Arthas 版本不符');
        }
        chunks.push(chunk);
      }
      contents = Buffer.concat(chunks);
    } catch (error) {
      if (controller.signal.aborted && !signal.aborted) throw new Error('下载 Arthas 超时，可选择已下载的官方离线包');
      throw error;
    } finally {
      clearTimeout(timer);
      signal.removeEventListener('abort', abort);
    }
  }
  if (crypto.createHash('sha256').update(contents).digest('hex') !== ARTHAS_SHA256) {
    throw new Error('Arthas ' + ARTHAS_VERSION + ' SHA-256 校验失败，请重新获取官方完整包');
  }
  return contents;
}

function checkArthasResult(response, expected = 'SUCCEEDED') {
  if (response.state !== expected) throw new Error(response.message || 'Arthas 请求未完成（' + response.state + '）');
  const results = response.body?.results || [];
  const failed = results.find((item) => item.type === 'status' && Number(item.statusCode) !== 0);
  if (failed) {
    const explanation = results.filter((item) => item.type === 'message').map((item) => item.message).join('\n');
    throw new Error(failed.message || explanation || 'Arthas 命令执行失败');
  }
  return results;
}

function arthasCommand(request) {
  const type = request.type;
  switch (type) {
    case 'jvm': return 'jvm';
    case 'memory': return 'memory';
    case 'threads': return 'thread -n 10';
    case 'classes': {
      const name = assertMatch(request.className, CLASS_SEARCH, '请输入简单类名、类全名或类名通配模式');
      const pattern = name.includes('.') || name.includes('*') ? name : '*' + name + '*';
      return 'sc -d -n 30 ' + pattern;
    }
    case 'methods':
    case 'source':
    case 'trace': {
      const name = assertMatch(request.className, JAVA_CLASS, '请输入准确的 Java 类全名');
      const loader = request.loader ? ' -c ' + assertMatch(request.loader, /^[a-f0-9]{1,8}$/i, 'ClassLoader 哈希格式无效') : '';
      if (type === 'methods') return 'sm -d' + loader + ' ' + name;
      if (type === 'source') return 'jad --source-only' + loader + ' ' + name;
      const method = assertMatch(request.method, JAVA_METHOD, '请输入准确的方法名');
      return 'trace -n 3' + loader + ' ' + name + ' ' + method;
    }
    default: throw new Error('未知 JVM 诊断操作');
  }
}

class RuntimeWorkbench {
  constructor({ confirmSshHost, requestRuntimeSource } = {}) {
    this.operations = new Map();
    this.session = null;
    this.attaching = false;
    this.closing = false;
    this.profiles = new Map();
    this.targets = new RuntimeTargets();
    this.confirmSshHost = confirmSshHost;
    this.requestRuntimeSource = requestRuntimeSource;
  }

  run(id, data, emit = () => {}) {
    if (typeof id !== 'string' || !/^[a-z0-9-]{1,64}$/i.test(id) || this.operations.has(id)
        || this.operations.size >= 8) throw new Error('运行时任务状态无效或已达并发上限');
    const controller = new AbortController();
    const promise = this.execute(data, controller.signal, emit).catch((error) => {
      if (controller.signal.aborted && error.name === 'AbortError') throw new Error('操作已取消');
      if (error.message?.startsWith('远端操作退出码 191')) throw new Error('目标 Java 进程已变化，请重新选择实例');
      if (error.message?.startsWith('远端操作退出码 192')) throw new Error('目标 JVM 的监听端口不可读');
      if (/^远端操作退出码 193\b/.test(error.message)) {
        const missing = error.message.match(/缺少容器工具：([^）\r\n]+)/)?.[1];
        throw new Error(missing ? '当前执行容器缺少工具：' + missing
          : '当前执行容器的依赖检查失败，需要 curl、timeout，以及 unzip 或 JDK jar');
      }
      if (error.message?.startsWith('远端操作退出码 194')) throw new Error('未找到目标进程对应的完整 JDK');
      if (error.message?.startsWith('远端操作退出码 195')) throw new Error('Arthas 临时端口已占用，请重新连接');
      if (error.message?.startsWith('远端操作退出码 196')) throw new Error('该端口已关闭或不再由所选 JVM 监听，请刷新端口');
      if (error.message?.startsWith('远端操作退出码 197')) throw new Error('JVM 未写出可读取的采集文件');
      if (error.message?.startsWith('远端操作退出码 198')) throw new Error('采集文件超出工作台临时目录');
      if (error.message?.startsWith('远端操作退出码 199')) throw new Error('执行容器与目标 JVM 的 UID/GID 不同，请选择匹配的调试容器');
      throw error;
    }).finally(() => this.operations.delete(id));
    this.operations.set(id, { controller, promise });
    return promise;
  }

  cancel(id) {
    this.operations.get(id)?.controller.abort();
  }

  async shutdown() {
    if (!this.shutdownPromise) {
      const pending = [...this.operations.values()];
      pending.forEach((operation) => operation.controller.abort());
      this.shutdownPromise = Promise.allSettled(pending.map((operation) => operation.promise))
        .then(() => this.detach()).finally(() => {
          closePasswordSession(this.passwordHost);
          this.passwordHost = undefined;
          this.shutdownPromise = null;
        });
    }
    return this.shutdownPromise;
  }

  async execute(data, signal, emit) {
    switch (data?.action) {
      case 'containers': return this.containers(data.host, signal, data.cluster);
      case 'passwordContainers': return this.passwordContainers(data, signal);
      case 'disconnectPassword':
        if (this.session || this.attaching || this.passwordConnecting) throw new Error('请先结束当前连接操作');
        closePasswordSession(this.passwordHost);
        this.passwordHost = undefined;
        return;
      case 'processes': return this.processes(data.host, requireContainer(data.container), signal);
      case 'ports': return this.ports(await this.target(data, signal), signal);
      case 'logs': return this.logs(data, signal, emit);
      case 'attach': return this.attach(data, signal, emit);
      case 'detach': return this.detach();
      case 'diagnose': return this.diagnose(data, signal, emit);
      case 'request': return this.request(data, signal);
      case 'requestCapture': return this.requestCapture(data, signal);
      case 'correlate': return this.correlate(data, signal);
      case 'normalizeSuite': return normalizeSuite(data.suite);
      case 'suite': return this.runSuite(data, signal, emit);
      case 'captureClass': return this.captureArtifact(data, signal, emit);
      case 'profile': return this.captureArtifact(data, signal, emit);
      case 'inventory': return this.inventory(data, signal, emit);
      case 'actuator': return this.request({ ...data, method: 'GET', path: actuatorPath(data) }, signal);
      case 'loki': return queryLoki(data, signal);
      case 'prepareSourcePatch': return this.prepareSourcePatch(data, signal);
      case 'applySourcePatch':
      case 'applyPatch': return this.applyPatch(data, signal, emit);
      case 'restorePatch': return this.restorePatch(data);
      default: throw new Error('未知运行时工作台操作');
    }
  }

  async passwordContainers(data, signal) {
    if (!this.confirmSshHost) throw new Error('密码认证需要桌面工作台确认服务器指纹');
    if (this.session || this.attaching || this.passwordConnecting) throw new Error('请先结束当前连接操作');
    this.passwordConnecting = true;
    let host;
    try {
      host = await openPasswordSession(data, this.confirmSshHost, signal);
      const list = await this.containers(host, signal, data.cluster);
      if (signal.aborted) throw new Error('操作已取消');
      closePasswordSession(this.passwordHost);
      this.passwordHost = host;
      return { host, list };
    } catch (error) {
      closePasswordSession(host);
      throw error;
    } finally {
      this.passwordConnecting = false;
    }
  }

  async containers(host, signal, cluster) {
    if (cluster) return this.targets.listKubernetes(host, cluster, signal);
    const output = await runSsh(host, shellCommand(['docker', 'ps', '--no-trunc', '--format', '{{json .}}']), { signal });
    return readLines(output).map(JSON.parse).filter((item) => CONTAINER_ID.test(item.ID)).map((item) => ({
      id: item.ID, name: item.Names, image: item.Image, status: item.Status, ports: item.Ports
    }));
  }

  async inventory(data, signal, emit) {
    const selected = data.selected;
    if (!Array.isArray(selected) || !selected.length || selected.length > 20) throw new Error('请选择 1 至 20 个目标实例');
    const visible = await this.containers(data.host, signal, data.cluster);
    const targets = selected.map((id) => visible.find((item) => item.id === requireContainer(id)));
    if (targets.some((item) => !item)) throw new Error('有目标实例已变化，请重新加载列表');
    const output = [];
    for (const item of targets) {
      if (signal.aborted) throw new Error('操作已取消');
      emit('status', '检查 ' + item.name);
      try {
        const processes = await this.processes(data.host, item.id, signal);
        const java = [];
        for (const p of processes) {
          java.push({ pid: p.pid, start: p.start, executable: p.executable,
            ports: await this.ports({ ...p, host: data.host, container: item.id }, signal) });
        }
        output.push({ id: item.id, name: item.name, image: item.image, status: item.status, java });
      } catch (error) {
        if (signal.aborted) throw error;
        output.push({ id: item.id, name: item.name, status: item.status, error: throwableMessage(error) });
      }
    }
    return output;
  }

  async processes(host, container, signal) {
    const output = await this.targets.exec(host, container, ['sh', '-c', SCAN_JAVA], { signal });
    const found = readLines(output).map((line) => {
      const [pid, start, user, executable] = line.split('\t');
      return { pid, start, user, executable };
    }).filter((p) => /^\d+$/.test(p.pid) && /^\d+$/.test(p.start) && /^\d+:\d+$/.test(p.user));
    // 入口用户和 JVM 用户不同时，按 JVM UID/GID 读取被 /proc 权限保护的可执行文件。
    for (const process of found.filter((item) => !item.executable)) {
      const output = await this.targetExec({ ...process, host, container },
        CHECK_TARGET + 'readlink "/proc/$pid/exe" 2>/dev/null || :', [], { signal });
      process.executable = output.trimEnd();
    }
    return found.filter((p) => /^\/[^\r\n\0]+\/bin\/java$/.test(p.executable));
  }

  async target(data, signal) {
    const container = requireContainer(data.container);
    const processes = await this.processes(data.host, container, signal);
    const process = processes.find((candidate) => candidate.pid === data.pid && candidate.start === data.start);
    if (!process) throw new Error('目标 Java 进程已变化，请重新选择实例');
    const pod = this.targets.selected(data.host, container);
    return { ...process, host: data.host, container, crossMount: Boolean(pod && pod.app !== pod.executor) };
  }

  targetExec(target, script, args = [], options = {}) {
    return this.targets.exec(target.host, target.container, ['sh', '-c', script, 'sh', target.pid, target.start, ...args],
      { ...options, user: target.user });
  }

  async ports(target, signal) {
    const output = await this.targetExec(target, CHECK_TARGET + FIND_PORTS, [], { signal });
    const active = this.session;
    const diagnosticPort = active?.host === target.host && active.container === target.container && active.pid === target.pid
      ? active.port : undefined;
    return [...new Set(readLines(output).filter((line) => /^[0-9A-F]{4}$/i.test(line)).map((hex) => parseInt(hex, 16)))]
      .filter((port) => port !== diagnosticPort).sort((a, b) => a - b);
  }

  logs(data, signal, emit) {
    const container = requireContainer(data.container);
    const tail = Number(data.tail);
    if (!Number.isInteger(tail) || tail < 1 || tail > 1000) throw new Error('日志行数应为 1 至 1000');
    if (!data.follow) return this.targets.logs(data.host, container, { tail }, { signal })
      .catch((error) => { throw new Error('读取容器标准输出日志失败：' + throwableMessage(error)); });
    return this.targets.logs(data.host, container, { tail, follow: true, seconds: LOG_SECONDS },
      { signal, timeout: (LOG_SECONDS + 10) * 1000, onChunk: (value) => emit('log', value) })
      .catch((error) => {
        if (!error.message.startsWith('远端操作退出码 124')) throw error;
        return '单次追踪已达 5 分钟，可继续追踪';
      });
  }

  async attach(data, signal, emit) {
    if (this.session || this.attaching || this.closing || this.passwordConnecting) throw new Error('请先结束当前连接操作');
    this.attaching = true;
    let session;
    let created = false;
    let starting = false;
    try {
      const target = await this.target(data, signal);
      const executable = target.crossMount
        ? (await this.targetExec(target, CHECK_TARGET + 'command -v java', [], { signal })).trim()
        : target.executable.replace(/\/jre\/bin\/java$/, '/bin/java');
      const directory = '/tmp/jarpatch-arthas-' + crypto.randomBytes(12).toString('hex');
      session = { ...target, directory, port: crypto.randomInt(20000, 40000), secret: crypto.randomBytes(32).toString('hex') };
      session.patchEnabled = !this.targets.selected(target.host, target.container);
      emit('status', '检查容器工具和目标 JDK');
      const extractor = (await this.targetExec(session, PREPARE, [directory, executable], { signal })).trim();
      created = true;
      emit('status', '校验 Arthas ' + ARTHAS_VERSION + ' 完整包' + (extractor === 'unzip' ? '' : '（使用 JDK jar 解包）'));
      const distribution = await getDistribution(data.file, signal);
      emit('status', '上传离线包');
      await this.targets.exec(target.host, target.container, ['sh', '-c', 'umask 077; cat > "$1/package.zip"', 'sh', directory],
        { signal, user: target.user, input: distribution, timeout: 45000 });
      const properties = [
        'arthas.config.overrideAll=true',
        'arthas.ip=127.0.0.1',
        'arthas.telnetPort=-1',
        'arthas.httpPort=' + session.port,
        'arthas.sessionTimeout=120',
        'arthas.username=' + ARTHAS_USER,
        'arthas.password=' + session.secret,
        'arthas.localConnectionNonAuth=false',
        'arthas.disabledCommands=ognl,tt,vmtool,mc,retransform,getstatic,sysenv,sysprop,options,vmoption,logger,heapdump,cat,tee'
          + (session.patchEnabled ? '' : ',redefine'),
        ''
      ].join('\n');
      emit('status', '连接 Java 进程 ' + target.pid);
      starting = true;
      await this.targetExec(session, target.crossMount ? START_ARTHAS_CROSS : START_ARTHAS,
        [directory, executable, String(session.port), extractor],
        { input: properties, timeout: 42000, sensitive: true, bootDiagnostics: true });
      let proof;
      for (let attempt = 0; attempt < 5; attempt++) {
        if (signal.aborted) throw new Error('操作已取消');
        try {
          proof = await this.arthas(session, { action: 'exec', command: 'session', execTimeout: 3000 }, { signal, timeout: 6500 });
          break;
        } catch (error) {
          if (attempt === 4 || signal.aborted) throw error;
          await delay(450, undefined, { signal });
        }
      }
      const rows = checkArthasResult(proof);
      this.checkIdentity(session, rows);
      await this.checkAuthentication(session, signal);
      if (session.crossMount) {
        await this.targetExec(session, CHECK_TARGET + 'umask 077; mkdir "/proc/$pid/root$1"', [directory], { signal });
        session.targetArtifactsCreated = true;
      }
      this.session = session;
      return { pid: session.pid, version: ARTHAS_VERSION, patchEnabled: session.patchEnabled };
    } catch (error) {
      if (created) {
        try {
          if (starting && !error.message?.startsWith('远端操作退出码 195')) await this.stopOwnedSession(session);
          await this.removeDirectory(session);
        } catch (cleanupError) {
          this.session = session;
          throw new Error(throwableMessage(error) + '。远端清理未确认，可重新释放会话：' + throwableMessage(cleanupError));
        }
      }
      throw error;
    } finally {
      this.attaching = false;
    }
  }

  async arthas(session, data, options = {}) {
    const auth = Buffer.from(ARTHAS_USER + ':' + session.secret).toString('base64');
    const entries = [
      ['url', 'http://127.0.0.1:' + session.port + '/api'],
      ['header', 'Authorization: Basic ' + auth], ['header', 'Content-Type: application/json'],
      ['data-binary', JSON.stringify(data)], ['max-time', String(Math.floor((options.timeout || 12000) / 1000) - 2)],
    ];
    const output = await this.targetExec(session,
      CHECK_TARGET + 'exec curl -q --noproxy "*" --silent --show-error --fail --config -', [],
      { ...options, input: curlConfig(entries), sensitive: true });
    try {
      return JSON.parse(output);
    } catch {
      throw new Error('Arthas 未返回有效的 JSON 结果');
    }
  }

  checkIdentity(session, rows) {
    const identity = rows.find((row) => row.type === 'session');
    if (String(identity?.javaPid) !== session.pid) throw new Error('Arthas 未返回匹配的 Java 进程');
  }

  async checkAuthentication(session, signal) {
    const entries = [
      ['url', 'http://127.0.0.1:' + session.port + '/api'],
      ['header', 'Content-Type: application/json'],
      ['data-binary', '{"action":"exec","command":"version"}'],
      ['output', '/dev/null'], ['write-out', '%{http_code}'], ['max-time', '4']
    ];
    const status = await this.targetExec(session,
      CHECK_TARGET + 'exec curl -q --noproxy "*" --silent --show-error --config -', [],
      { signal, input: curlConfig(entries), timeout: 6500, sensitive: true });
    if (status !== '401') throw new Error('Arthas 本地身份验证未生效，无法确认代理归属');
  }

  async stopOwnedSession(session) {
    try {
      const status = await this.arthas(session, { action: 'exec', command: 'session', execTimeout: 2000 }, { timeout: 6500 });
      this.checkIdentity(session, checkArthasResult(status));
      await this.checkAuthentication(session);
    } catch (error) {
      if (/^远端操作退出码 (7|191)\b/.test(error.message)) return;
      throw error;
    }
    let stopError;
    try {
      await this.arthas(session, { action: 'exec', command: 'stop', execTimeout: 3000 }, { timeout: 7000 });
    } catch (error) {
      // 关闭 HTTP 服务时响应可能先断开，以下探测确认服务已退出。
      stopError = error;
    }
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        await this.arthas(session, { action: 'exec', command: 'version', execTimeout: 2000 }, { timeout: 6500 });
      } catch (error) {
        if (/^远端操作退出码 (7|191)\b/.test(error.message)) return;
        stopError ??= error;
      }
      await delay(350);
    }
    throw new Error('未能确认 Arthas 已停止' + (stopError ? '（' + stopError.message + '）' : ''));
  }

  async removeDirectory(session) {
    if (!/^\/tmp\/jarpatch-arthas-[a-f0-9]{24}$/.test(session.directory)) throw new Error('Arthas 临时目录无法确认');
    if (session.targetArtifactsCreated) {
      try {
        await this.targetExec(session, CHECK_TARGET + 'exec rm -rf -- "/proc/$pid/root$1"', [session.directory], { timeout: 10000 });
      } catch (error) {
        if (!/^远端操作退出码 191\b/.test(error.message)) throw error;
      }
      session.targetArtifactsCreated = false;
    }
    return this.targets.exec(session.host, session.container, ['rm', '-rf', '--', session.directory],
      { user: session.user, timeout: 10000 });
  }

  async detach() {
    if (this.attaching || this.closing) throw new Error('Arthas 会话正在变更，请稍后重试');
    const session = this.session;
    if (!session) return;
    if (session.observing || session.artifact) throw new Error('请先结束当前采集');
    this.closing = true;
    try {
      if (session.hotPatch) await this.restoreHotPatch(session);
      await this.stopOwnedSession(session);
      await this.removeDirectory(session);
      this.session = null;
    } finally {
      this.closing = false;
    }
  }

  requireSession(data) {
    const session = this.session;
    if (!session || this.closing || session.container !== data.container || session.host !== data.host
        || session.pid !== data.pid || session.start !== data.start) throw new Error('请先连接所选 Java 进程的 Arthas 会话');
    return session;
  }

  async diagnose(data, signal, emit) {
    const session = this.requireSession(data);
    const command = arthasCommand(data);
    if ((data.type === 'source' || data.type === 'trace') && session.hotPatch?.className === data.className) {
      throw new Error('请先恢复该类的原 class，再执行反编译或追踪');
    }
    if (data.type === 'trace') {
      if (session.artifact) throw new Error('请先结束当前 class 或性能采集');
      return this.observe(session, command, data, signal, emit);
    }
    const read = async (command) => checkArthasResult(await this.arthas(session,
      { action: 'exec', command, execTimeout: 6500 }, { signal, timeout: 11000 })).filter((row) => row.type !== 'status');
    const rows = await read(command);
    return data.type === 'methods' ? describeRuntimeMethods(rows, read, signal) : rows;
  }

  async observe(session, command, data, signal, emit) {
    if (session.observing) throw new Error('当前会话已有方法诊断在运行');
    session.observing = true;
    let ids;
    let submitted = false;
    const controller = new AbortController();
    const abort = () => controller.abort();
    signal.addEventListener('abort', abort, { once: true });
    if (signal.aborted) abort();
    const options = { signal: controller.signal, timeout: 13000 };
    let timer;
    let completed = false;
    try {
      const started = await this.arthas(session, { action: 'init_session' }, { timeout: 6500 });
      checkArthasResult(started);
      ids = { sessionId: started.sessionId, consumerId: started.consumerId };
      if (!ids.sessionId || !ids.consumerId) throw new Error('Arthas 未返回诊断会话标识');
      if (signal.aborted) throw new Error('操作已取消');
      submitted = true;
      const submission = await this.arthas(session, { action: 'async_exec', ...ids, command }, options);
      checkArthasResult(submission, 'SCHEDULED');
      const jobId = submission.body?.jobId;
      if (!jobId) throw new Error('Arthas 未返回方法采集任务标识');
      timer = setTimeout(abort, OBSERVE_SECONDS * 1000);
      let matched = 0;
      let ready = false;
      while (!controller.signal.aborted) {
        let result;
        try {
          result = await this.arthas(session, { action: 'pull_results', ...ids }, options);
        } catch (error) {
          if (controller.signal.aborted) break;
          throw error;
        }
        const rows = checkArthasResult(result).filter((item) => item.jobId === jobId);
        const enhancement = rows.find((item) => item.type === 'enhancer');
        if (enhancement && !ready) {
          if (enhancement.success === false || enhancement.effect?.methodCount === 0) throw new Error('未找到可采集的目标方法');
          ready = true;
          emit('status', '方法采集已启动。请在 ' + OBSERVE_SECONDS + ' 秒内触发请求');
        }
        for (const row of rows.filter((item) => item.type === 'trace')) {
          matched++;
          emit('trace', row);
        }
        if (rows.some((item) => item.type === 'status')) {
          completed = true;
          break;
        }
      }
      if (signal.aborted) throw new Error('操作已取消');
      if (completed && !ready && !matched) throw new Error('未找到可采集的目标方法');
      return { matched };
    } finally {
      clearTimeout(timer);
      signal.removeEventListener('abort', abort);
      try {
        await this.closeObservation(session, data.className, ids, submitted, completed);
      } finally {
        session.observing = false;
      }
    }
  }

  async closeObservation(session, name, ids, submitted, completed) {
    if (!ids?.sessionId) return;
    let interruptionError;
    let sessionError;
    if (submitted && !completed) {
      try {
        await this.arthas(session, { action: 'interrupt_job', sessionId: ids.sessionId }, { timeout: 6500 });
      } catch (error) {
        // 请求中断失败时，关闭会话并重置目标类仍可完成清理。
        interruptionError = error;
      }
    }
    try {
      checkArthasResult(await this.arthas(session, { action: 'close_session', sessionId: ids.sessionId }, { timeout: 6500 }));
    } catch (error) {
      sessionError = error;
    }
    if (submitted) {
      try {
        checkArthasResult(await this.arthas(session, {
          action: 'exec', command: 'reset ' + name, execTimeout: 4000
        }, { timeout: 7500 }));
      } catch (error) {
        sessionError ??= error;
      }
    }
    if (sessionError) {
      const detail = [interruptionError, sessionError].filter(Boolean).map(throwableMessage).join('；');
      throw new Error('未能确认方法诊断已清理，请释放 Arthas 会话：' + detail);
    }
  }

  artifactAccess(session) {
    const mount = session.crossMount ? 'mount="/proc/$pid/root"\n' : 'mount=\n';
    return {
      exec: async (command, commandSignal) => checkArthasResult(await this.arthas(session, {
        action: 'exec', command, execTimeout: 6500
      }, { signal: commandSignal, timeout: 11000 })),
      create: (directory, commandSignal) => this.targetExec(session, CHECK_TARGET + mount + 'umask 077; mkdir -- "$mount$1"',
        [directory], { signal: commandSignal }),
      remove: (directory) => this.targetExec(session, CHECK_TARGET + mount + 'exec rm -rf -- "$mount$1"', [directory], { timeout: 10000 }),
      read: (location, maxOutputBytes, commandSignal) => this.targetExec(session, CHECK_TARGET + mount + READ_ARTIFACT,
      [location, session.directory], { signal: commandSignal, binary: true, maxOutputBytes, timeout: 30000 })
    };
  }

  async captureArtifact(data, signal, emit) {
    const session = this.requireSession(data);
    if (session.observing || session.artifact) throw new Error('当前 JVM 已有诊断采集，请先结束该任务');
    session.artifact = true;
    const access = this.artifactAccess(session);
    try {
      if (data.action === 'captureClass') {
        const name = assertMatch(data.className, JAVA_CLASS, '请输入准确的 Java 类全名');
        const loader = data.loader ? assertMatch(data.loader, /^[a-f0-9]{1,8}$/i, 'ClassLoader 哈希格式无效') : '';
        return { ...await collectClass(access, session, name, loader, signal), target: targetIdentity(session) };
      }
      const artifact = await collectProfile(access, session, data.type, Number(data.seconds), data.event, signal, emit);
      const { contents, ...metadata } = artifact;
      metadata.size = contents.length;
      metadata.target = targetIdentity(session);
      this.profiles.set(metadata.type, { ...metadata, contents });
      return metadata;
    } finally {
      session.artifact = false;
    }
  }

  requirePatchSession(data) {
    const session = this.requireSession(data);
    if (!session.patchEnabled) throw new Error('在线编辑和热替换支持 Docker 连接');
    if (session.hotPatch || session.observing || session.artifact) throw new Error('请先还原已替换的 class 或结束诊断采集');
    return session;
  }

  async prepareSourcePatch(data, signal) {
    const session = this.requirePatchSession(data);
    session.artifact = true;
    session.sourcePatch = undefined;
    try {
      const className = assertMatch(data.className, JAVA_CLASS, '请输入准确的类全名');
      let loader = data.loader;
      const access = this.artifactAccess(session);
      if (!loader) {
        const rows = await access.exec('sc -d ' + className, signal);
        const matches = rows.filter((row) => row.type === 'sc').flatMap((row) => row.classInfo ?? [])
          .filter((item) => item.name === className);
        if (matches.length !== 1) throw new Error('请从搜索结果选择已加载的类及其 ClassLoader');
        loader = matches[0].classLoaderHash;
      }
      loader = assertMatch(loader, /^[a-f0-9]{1,8}$/i, '请选择带有效 ClassLoader 哈希的应用类');
      const snapshot = await collectClass(access, session, className, loader, signal);
      const source = await this.requestRuntimeSource('class-source', { className, contents: snapshot.contents }, signal);
      session.sourcePatch = { ...snapshot, source };
      return { className, loader, sha256: snapshot.sha256, source };
    } finally {
      session.artifact = false;
    }
  }

  async compilePatch(data, session, name, loader, signal, emit) {
    const snapshot = session.sourcePatch;
    if (!snapshot || snapshot.className !== name || snapshot.loader !== loader || snapshot.sha256 !== data.expectedSha256) {
      throw new Error('编辑源码对应的目标已变化，请重新打开在线源码');
    }
    if (typeof data.source !== 'string' || !data.source.trim() || Buffer.byteLength(data.source) > MAX_PATCH_SOURCE_BYTES) {
      throw new Error('请输入 Java 源码，内容须小于或等于 2 MiB');
    }
    if (data.source === snapshot.source) throw new Error('请先修改源码');
    emit('status', '正在本地编译 · ' + name);
    const encoded = await this.requestRuntimeSource('class-compile', {
      archive: data.archive, className: name, contents: snapshot.contents, source: data.source
    }, signal);
    const bytes = validatePatch(Buffer.from(encoded, 'base64'), name);
    const original = Buffer.from(snapshot.contents, 'base64');
    if (!bytes.subarray(4, 8).equals(original.subarray(4, 8))) throw new Error('编译产物的 class 版本与原 class 不一致');
    emit('status', '编译完成，正在核对并备份原 class…');
    return bytes;
  }

  async applyPatch(data, signal, emit) {
    const session = this.requirePatchSession(data);
    session.artifact = true;
    try {
      const name = assertMatch(data.className, JAVA_CLASS, '请输入准确的类全名');
      const loader = data.loader ? assertMatch(data.loader, /^[a-f0-9]{1,8}$/i, 'ClassLoader 哈希格式无效') : '';
      const fromSource = data.action === 'applySourcePatch';
      const candidate = fromSource ? await this.compilePatch(data, session, name, loader, signal, emit)
        : await readPatch(data.file, name, signal);
      const access = this.artifactAccess(session);
      const snapshot = await collectClass(access, session, name, loader, signal);
      if (fromSource && snapshot.sha256 !== data.expectedSha256) throw new Error('运行中的 class 已变化，请重新打开在线源码');
      const directory = patchDirectory(session);
      await access.create(directory, signal);
      const original = Buffer.from(snapshot.contents, 'base64');
      const args = [directory];
      const loaderOption = loader ? ' -c ' + loader : '';
      try {
        await this.targetExec(session, CHECK_TARGET + 'umask 077; cat > "$1/original.class"', args, { input: original, signal });
        await this.targetExec(session, CHECK_TARGET + 'umask 077; cat > "$1/replacement.class"', args, { input: candidate, signal });
        if (signal.aborted) throw new Error('操作已取消');
      } catch (error) {
        await access.remove(directory);
        throw error;
      }
      session.hotPatch = { directory, className: name, loaderOption, original };
      try {
        await access.exec('redefine' + loaderOption + ' ' + directory + '/replacement.class');
      } catch (error) {
        try {
          await this.restoreHotPatch(session);
        } catch (restoreError) {
          throw new Error(throwableMessage(error) + '；原 class 的恢复待确认：' + throwableMessage(restoreError));
        }
        throw error;
      }
      return patchResult('applied', name, candidate);
    } finally {
      session.artifact = false;
    }
  }

  async restoreHotPatch(session) {
    const patch = session.hotPatch;
    if (!patch) throw new Error('当前会话没有待恢复的 class');
    const access = this.artifactAccess(session);
    if (!patch.restored) {
      await access.exec('redefine' + patch.loaderOption + ' ' + patch.directory + '/original.class');
      patch.restored = true;
    }
    await access.remove(patch.directory);
    session.hotPatch = null;
    return patchResult('restored', patch.className, patch.original);
  }

  async restorePatch(data) {
    const session = this.requireSession(data);
    if (session.observing || session.artifact) throw new Error('当前 JVM 已有诊断采集，请先结束该任务');
    session.artifact = true;
    try {
      return await this.restoreHotPatch(session);
    } finally {
      session.artifact = false;
    }
  }

  async requestCapture(data, signal) {
    const started = Date.now();
    const response = await this.request(data, signal, true);
    const target = targetIdentity(data);
    this.lastRequest = { ...target, started, ended: Date.now(), ids: response.correlationIds };
    return { target: { ...target, port: Number(data.port) }, startedAt: new Date(started).toISOString(), text: response.text,
      status: response.status, elapsedMs: response.elapsedMs, size: response.size };
  }

  async correlate(data, signal) {
    const record = this.lastRequest;
    if (!record || ['host', 'container', 'pid', 'start'].some((key) => record[key] !== data[key])) {
      throw new Error('请在当前目标执行一次请求后查询关联日志');
    }
    const padMs = 2500;
    const windowEnd = record.ended + padMs;
    const remaining = windowEnd - Date.now();
    if (remaining > 0) await delay(remaining, undefined, { signal });
    const result = await this.targets.logs(record.host, record.container, { tail: 1000,
      since: new Date(record.started - padMs).toISOString(), until: new Date(windowEnd).toISOString() }, { signal });
    const matched = selectCorrelatedLogs(result, record.ids);
    return { ...matched, lines: readLines(matched.text).length };
  }

  async runSuite(data, signal, emit) {
    const suite = normalizeSuite(data.suite);
    if (!suite.cases.length) throw new Error('请先添加请求用例');
    const target = await this.target(data, signal);
    const results = [];
    for (const [index, test] of suite.cases.entries()) {
      if (signal.aborted) throw new Error('操作已取消');
      emit('status', '执行用例 ' + (index + 1) + ' / ' + suite.cases.length + ' · ' + test.name);
      try {
        const response = await this.request({ ...test, ...target, port: data.port,
          headers: [test.headers, data.credentials].filter(Boolean).join('\n') }, signal, true, target);
        results.push({ name: test.name, method: test.method, status: response.status, elapsedMs: response.elapsedMs,
          size: response.size, digest: response.digest, assertions: assertionResults(test.assertions, response) });
      } catch (error) {
        if (signal.aborted) throw error;
        if (/^远端操作退出码 (191|196|255)\b/.test(error.message)) throw error;
        results.push({ name: test.name, method: test.method, error: throwableMessage(error) });
      }
    }
    return { target: { ...targetIdentity(target), port: Number(data.port) },
      capturedAt: new Date().toISOString(), suiteSha256: sha256(JSON.stringify(suite)), results };
  }

  async request(data, signal, structured = false, resolvedTarget) {
    const method = data.method;
    if (!HTTP_METHODS.has(method)) throw new Error('不支持的 HTTP 方法');
    const protocol = data.protocol;
    if (protocol !== 'http' && protocol !== 'https') throw new Error('请选择 HTTP 或 HTTPS');
    const port = Number(data.port);
    if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('请选择 JVM 监听端口');
    const address = assertMatch(data.path, /^\/[^\u0000-\u0020\u007f#]*$/, '路径须以 / 开头，空格请先编码', 4096);
    const host = data.serverName
      ? assertMatch(data.serverName, DNS_NAME, 'TLS 主机名格式无效', 253) : '127.0.0.1';
    const target = resolvedTarget || await this.target(data, signal);
    if (this.session?.host === target.host && this.session.container === target.container && this.session.port === port) {
      throw new Error('请选择业务端口');
    }
    const marker = captureMarker();
    const entries = [
      ['url', protocol + '://' + host + ':' + port + address],
      ['request', method],
      ['write-out', marker + '%{http_code} %{time_total} %{size_download}\n'],
      ['max-time', '12'], ['max-filesize', '1800000']
    ];
    if (host !== '127.0.0.1') entries.push(['resolve', host + ':' + port + ':127.0.0.1']);
    entries.push(method === 'HEAD' ? ['head'] : ['dump-header', '-']);
    if (data.headers) {
      if (typeof data.headers !== 'string' || data.headers.length > 8192) throw new Error('请求头超过大小限制');
      for (const header of readLines(data.headers)) {
        if (!/^[!#$%&'*+.^_\x60|~\w-]+:[^\u0000-\u0008\u000a-\u001f\u007f]*$/.test(header)) {
          throw new Error('请求头每行应为“名称: 值”格式');
        }
        entries.push(['header', header]);
      }
    }
    if (data.body && !['HEAD', 'GET'].includes(method)) {
      if (typeof data.body !== 'string' || Buffer.byteLength(data.body) > 65536 || data.body.includes('\0')) {
        throw new Error('请求体须为 64 KiB 以内的文本');
      }
      entries.push(['data-raw', data.body]);
    }
    const send = CHECK_TARGET + 'ports=$(\n' + FIND_PORTS + '\n) || exit 192\n'
      + 'printf "%s\\n" "$ports" | grep -Fxi -- "$1" >/dev/null || exit 196\n'
      + 'exec curl -q --noproxy "*" --globoff --silent --show-error --config -';
    try {
      const output = await this.targetExec(target, send, [port.toString(16).padStart(4, '0')],
        { signal, input: curlConfig(entries), timeout: 16000, sensitive: true, binary: true });
      const response = parseHttpCapture(output, marker, data.headers);
      return structured ? response : response.text;
    } catch (error) {
      if (/^远端操作退出码 (35|51|60)\b/.test(error.message)) throw new Error('TLS 握手或证书验证失败，请核对协议、端口、证书信任和 TLS 主机名');
      if (/^远端操作退出码 7\b/.test(error.message)) throw new Error('无法通过容器内回环地址连接该端口');
      if (/^远端操作退出码 28\b/.test(error.message)) throw new Error('业务请求超时');
      if (/^远端操作退出码 63\b/.test(error.message)) throw new Error('响应超过大小限制');
      throw error;
    }
  }
}

module.exports = { RuntimeWorkbench, ARTHAS_VERSION };
