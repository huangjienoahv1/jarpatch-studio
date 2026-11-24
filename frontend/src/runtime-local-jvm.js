const fs = require('fs/promises');
const http = require('http');
const https = require('https');
const net = require('net');
const os = require('os');
const path = require('path');
const { runCommand } = require('./runtime-transport');

const WINDOWS = process.platform === 'win32';
const LINUX = process.platform === 'linux';
const MACOS_LSOF = '/usr/sbin/lsof';
const TARGET_CHANGED = '目标 Java 进程已变化，请重新选择实例';
const HTTP_LIMIT = 1800000;
const DIRECTORY_PREFIX = 'jarpatch-arthas-';
const LOADED_JARS = new Set(['arthas-agent.jar', 'arthas-core.jar', 'arthas-spy.jar']);
const DEFERRED_CLEANUP = '.pending-cleanup';
const cLocale = { ...process.env, LC_ALL: 'C' };

// WMI exposes the PID creation time on both Win7 and current Windows without requiring PowerShell.
// Encode fields as ASCII for redirected output on any Windows code page.
const WINDOWS_PROCESSES = String.raw`
var pid = WScript.Arguments.length ? WScript.Arguments(0) : '';
var wmi = GetObject('winmgmts:\\\\.\\root\\cimv2');
var filter = "(Name = 'java.exe' OR Name = 'javaw.exe')";
if (pid) filter += ' AND ProcessId = ' + Number(pid);
var rows = new Enumerator(wmi.ExecQuery('SELECT * FROM Win32_Process WHERE ' + filter));
for (; !rows.atEnd(); rows.moveNext()) {
  var entry = rows.item();
  if (!entry.ExecutablePath || !entry.CreationDate) continue;
  var user = '';
  try {
    var owner = entry.ExecMethod_('GetOwner');
    if (!owner.ReturnValue) user = owner.User;
  } catch (error) { /* The JVM may belong to another account. */ }
  var command = entry.CommandLine || '';
  var jar = /(?:^|\s)-jar\s+(?:"([^"]+)"|(\S+))/i.exec(command);
  var main = /(?:^|\s)(?:-cp|-classpath|--class-path)\s+(?:"[^"]*"|\S+)\s+([A-Za-z_$][\w$.]*)(?:\s|$)/.exec(command);
  var name = jar ? (jar[1] || jar[2]).split(/[\\/]/).pop() : main ? main[1] : '';
  var fields = [entry.ProcessId, entry.CreationDate, user, entry.ExecutablePath, name];
  for (var i = 0; i < fields.length; i++) fields[i] = encodeURIComponent(fields[i]);
  WScript.Echo(fields.join('\t'));
}
`;

function windowsProgram(name) {
  return path.join(process.env.SystemRoot || 'C:\\Windows', 'System32', name);
}

async function windowsProcesses(pid, signal) {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'jarpatch-processes-'));
  const script = path.join(directory, 'processes.js');
  try {
    await fs.writeFile(script, WINDOWS_PROCESSES, { flag: 'wx' });
    const result = await runCommand(windowsProgram('cscript.exe'),
      ['//Nologo', script, ...(pid ? [pid] : [])], { signal });
    return result.split(/\r?\n/).filter(Boolean).map((line) => {
      const [pid, start, user, executable, name] = line.split('\t').map(decodeURIComponent);
      return { pid, start, user, executable, name };
    });
  } finally {
    await fs.rm(directory, { recursive: true, force: true });
  }
}

async function unixProcesses(pid, signal) {
  const args = [...(pid ? ['-p', pid] : ['-ax']), '-ww', '-o', 'pid=', '-o', 'lstart=', '-o', 'uid=', '-o', 'comm='];
  let output;
  try {
    output = await runCommand('ps', args, { signal, env: cLocale });
  } catch (error) {
    if (pid && error.message === '本机操作退出码 1') return [];
    throw error;
  }
  const found = [];
  for (const line of output.split('\n')) {
    const row = line.match(/^\s*(\d+)\s+(\S+ [A-Za-z]{3}\s+\d+ \S+ \d{4})\s+(\d+)\s+(.+)$/);
    if (!row || path.basename(row[4]) !== 'java') continue;
    let [, pid, start, user, executable] = row;
    let name;
    if (LINUX) {
      try {
        const directory = '/proc/' + pid;
        executable = await fs.readlink(directory + '/exe');
        const stat = await fs.readFile(directory + '/stat', 'utf8');
        start = stat.slice(stat.lastIndexOf(') ') + 2).trim().split(/\s+/)[19];
        const args = (await fs.readFile(directory + '/cmdline', 'utf8')).split('\0');
        const jar = args.indexOf('-jar');
        if (jar >= 0) name = path.basename(args[jar + 1] || '');
      } catch (error) {
        if (['ENOENT', 'EACCES', 'EPERM', 'ESRCH'].includes(error.code)) continue;
        throw error;
      }
    } else if (!path.isAbsolute(executable)) {
      try {
        const paths = await runCommand(MACOS_LSOF, ['-nP', '-a', '-p', pid, '-d', 'txt', '-Fn'], { signal });
        executable = paths.split('\n').find((name) => /^n\/.*\/bin\/java$/.test(name))?.slice(1);
      } catch (error) {
        if (error.message !== '本机操作退出码 1') throw error;
        continue;
      }
    }
    if (executable && path.isAbsolute(executable)) found.push({ pid, start, user, executable, name });
  }
  return found;
}

async function localProcesses(signal, pid) {
  if (pid != null && (typeof pid !== 'string' || !/^[1-9]\d{0,9}$/.test(pid))) throw new Error('Java 进程号无效');
  const rows = await (WINDOWS ? windowsProcesses(pid, signal) : unixProcesses(pid, signal));
  return rows.filter((item) => /^[1-9]\d{0,9}$/.test(item.pid) && item.start && item.executable);
}

async function confirmLocalProcess(target, signal) {
  const found = target.pid ? (await localProcesses(signal, target.pid))[0] : undefined;
  if (!found || found.pid !== target.pid || found.start !== target.start
      || (target.executable && found.executable !== target.executable)) {
    const error = new Error(TARGET_CHANGED);
    error.code = 'JARPATCH_PROCESS_CHANGED';
    throw error;
  }
  return found;
}

async function localJdk(target) {
  let executable = target.executable;
  if (WINDOWS) executable = executable.replace(/javaw\.exe$/i, 'java.exe');
  executable = executable.replace(/([\\/])jre[\\/]bin[\\/](java(?:\.exe)?)$/i, '$1bin$1$2');
  const bin = path.dirname(executable);
  const archive = path.join(bin, WINDOWS ? 'jar.exe' : 'jar');
  try {
    await fs.access(executable);
    await fs.access(archive);
  } catch (error) {
    if (!['ENOENT', 'EACCES', 'EPERM'].includes(error.code)) throw error;
    throw new Error('找不到目标进程对应的完整 JDK，请使用带 jar 工具的 JDK 启动目标应用');
  }
  return { executable, archive, tools: path.join(path.dirname(bin), 'lib', 'tools.jar') };
}

async function createLocalArthasDirectory() {
  return (await fs.mkdtemp(path.join(os.tmpdir(), DIRECTORY_PREFIX))).split(path.sep).join('/');
}

async function removeLocalArthasDirectory(directory, wasStarted) {
  if (!WINDOWS || !wasStarted) {
    await fs.rm(directory, { recursive: true, force: true });
    return false;
  }
  await fs.rm(path.join(directory, 'arthas.properties'), { force: true });
  let entries;
  try {
    entries = await fs.readdir(directory);
  } catch (error) {
    if (error.code === 'ENOENT') return false;
    throw error;
  }
  let deferred = false;
  for (const entry of entries) {
    try {
      await fs.rm(path.join(directory, entry), { recursive: true, force: true, maxRetries: 2, retryDelay: 250 });
    } catch (error) {
      if (!LOADED_JARS.has(entry) || error.code !== 'EBUSY') throw error;
      // HotSpot can retain agent and bootstrap JARs after the Arthas server stops.
      deferred = true;
    }
  }
  if (deferred) {
    await fs.writeFile(path.join(directory, DEFERRED_CLEANUP), '');
    return true;
  }
  await fs.rmdir(directory);
  return false;
}

async function sweepDeferredArthasDirectories() {
  if (!WINDOWS) return;
  const root = os.tmpdir();
  try {
    const entries = await fs.readdir(root, { withFileTypes: true });
    for (const entry of entries) {
      if (!entry.isDirectory() || !entry.name.startsWith(DIRECTORY_PREFIX)
          || !/^[a-z0-9]{6}$/i.test(entry.name.slice(DIRECTORY_PREFIX.length))) continue;
      const directory = path.join(root, entry.name);
      const files = await fs.readdir(directory, { withFileTypes: true }).catch((error) => {
        if (error.code !== 'ENOENT') throw error;
        return [];
      });
      if (!files.some((file) => file.name === DEFERRED_CLEANUP && file.isFile())
          || files.some((file) => !file.isFile() || (!LOADED_JARS.has(file.name) && file.name !== DEFERRED_CLEANUP))) continue;
      try {
        for (const jar of LOADED_JARS) await fs.rm(path.join(directory, jar), { force: true });
      } catch (error) {
        if (error.code === 'EBUSY') continue;
        throw error;
      }
      await fs.rm(path.join(directory, DEFERRED_CLEANUP), { force: true });
      await fs.rmdir(directory).catch((error) => { if (error.code !== 'ENOENT') throw error; });
    }
  } catch (error) {
    console.warn('回收本机 Arthas 缓存失败：' + (error.code || error.name));
  }
}

async function availablePort() {
  const listener = net.createServer();
  return new Promise((resolve, reject) => {
    listener.once('error', reject);
    listener.listen(0, '127.0.0.1', () => {
      const { port } = listener.address();
      listener.close((error) => error ? reject(error) : resolve(port));
    });
  });
}

async function startLocalArthas(session, jdk, properties) {
  await fs.writeFile(path.join(session.directory, 'arthas.properties'), properties, { mode: 0o600 });
  const javaOptions = [];
  try {
    await fs.access(jdk.tools);
    javaOptions.push('-Xbootclasspath/a:' + jdk.tools);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  const core = path.join(session.directory, 'arthas-core.jar');
  const agent = path.join(session.directory, 'arthas-agent.jar');
  await confirmLocalProcess(session);
  await runCommand(jdk.executable, [...javaOptions, '-jar', core, '-pid', session.pid, '-target-ip', '127.0.0.1',
    '-telnet-port', '-1', '-http-port', String(session.port), '-session-timeout', '120', '-core', core, '-agent', agent],
  { cwd: session.directory, timeout: 30000, sensitive: true, bootDiagnostics: true });
}

async function linuxPorts(pid) {
  const directory = '/proc/' + pid;
  const sockets = new Set();
  for (const fd of await fs.readdir(directory + '/fd')) {
    try {
      const link = await fs.readlink(directory + '/fd/' + fd);
      const socket = /^socket:\[(\d+)\]$/.exec(link);
      if (socket) sockets.add(socket[1]);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }
  const ports = [];
  for (const table of ['tcp', 'tcp6']) {
    const output = await fs.readFile(directory + '/net/' + table, 'utf8').catch((error) => {
      if (table === 'tcp6' && error.code === 'ENOENT') return '';
      throw error;
    });
    for (const line of output.split('\n')) {
      const fields = line.trim().split(/\s+/);
      if (fields[3] === '0A' && sockets.has(fields[9])) ports.push(parseInt(fields[1].split(':')[1], 16));
    }
  }
  return ports;
}

async function localPorts(target, signal) {
  await confirmLocalProcess(target, signal);
  if (WINDOWS) {
    const output = await runCommand(windowsProgram('netstat.exe'), ['-ano'], { signal });
    return output.split(/\r?\n/).flatMap((line) => {
      const [protocol, address, remote, , pid] = line.trim().split(/\s+/);
      // The unspecified remote endpoint identifies listening TCP sockets on localized Windows.
      return protocol === 'TCP' && pid === target.pid && /^(?:0\.0\.0\.0|\[::\]):0$/.test(remote)
        ? [Number(address.slice(address.lastIndexOf(':') + 1))] : [];
    });
  }
  if (LINUX) return linuxPorts(target.pid);
  try {
    const output = await runCommand(MACOS_LSOF, ['-nP', '-a', '-p', target.pid, '-iTCP', '-sTCP:LISTEN', '-Fn'],
      { signal, env: cLocale });
    return [...output.matchAll(/^n.*:(\d+)$/gm)].map((row) => Number(row[1]));
  } catch (error) {
    if (error.code === 'ENOENT') throw new Error('本机未安装 lsof，可从应用日志确认其监听端口');
    if (error.message === '本机操作退出码 1') return [];
    throw error;
  }
}

function loopbackHttp({ port, serverName, protocol = 'http', method = 'POST', address, headers, body, signal,
  timeout = 12000, maxBytes = HTTP_LIMIT, createConnection }) {
  const client = protocol === 'https' ? https : http;
  return new Promise((resolve, reject) => {
    const request = client.request({
      hostname: '127.0.0.1', port, servername: serverName, method, path: address, headers, signal,
      agent: createConnection ? undefined : false, createConnection
    }, (response) => {
      const chunks = [];
      let size = 0;
      response.on('error', reject);
      response.on('data', (chunk) => {
        size += chunk.length;
        if (size > maxBytes) request.destroy(new Error('响应超过大小限制'));
        else chunks.push(chunk);
      });
      response.on('end', () => resolve({
        status: response.statusCode, reason: response.statusMessage, headers: response.rawHeaders, body: Buffer.concat(chunks)
      }));
    });
    const timer = setTimeout(() => request.destroy(new Error('等待本机 HTTP 响应超时')), timeout);
    request.once('error', reject);
    request.once('close', () => clearTimeout(timer));
    request.end(body);
  });
}

module.exports = {
  availablePort, confirmLocalProcess, createLocalArthasDirectory, loopbackHttp, localJdk, localPorts, localProcesses,
  removeLocalArthasDirectory, startLocalArthas, sweepDeferredArthasDirectories
};
