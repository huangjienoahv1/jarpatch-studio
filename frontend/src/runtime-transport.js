const { spawn } = require('child_process');
const { StringDecoder } = require('string_decoder');
const { SESSION_ID, runPasswordCommand } = require('./runtime-password-ssh');

const SSH_TIMEOUT_MS = 15000;
const MAX_OUTPUT_BYTES = 2 * 1024 * 1024;
const SSH_HOST = /^(?:[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}@)?(?:[A-Za-z0-9][A-Za-z0-9_.-]{0,252}|\[[0-9A-Fa-f:]+\])$/;

function shellArgument(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function shellCommand(args) {
  return args.map(shellArgument).join(' ');
}

function remoteExitError(code, stdout, stderr, { sensitive, bootDiagnostics }) {
  const raw = Buffer.concat(stderr).toString('utf8') + (bootDiagnostics ? Buffer.concat(stdout).toString('utf8') : '');
  const lines = raw.replace(/\x1b\[[0-9;]*m/g, '').split(/\r?\n/);
  const detail = bootDiagnostics
    ? lines.filter((line) => /error|exception|failed|permission|attach/i.test(line) && !/password|authorization/i.test(line))
      .slice(-5).join(' ').replace(/\b[a-f0-9]{64}\b/gi, '[已遮盖]')
    : raw.trim().split('\n').pop();
  const message = !sensitive || bootDiagnostics || code === 255 ? detail : '';
  return new Error('远端操作退出码 ' + code + (message ? '（' + message.slice(0, 500) + '）' : ''));
}

function runSsh(host, command, options = {}) {
  if (typeof host !== 'string' || !SSH_HOST.test(host)) {
    return Promise.reject(new Error('请输入 SSH 主机别名或 user@host'));
  }
  const { input, signal, timeout = SSH_TIMEOUT_MS, onChunk, sensitive = false, bootDiagnostics = false,
    binary = false, maxOutputBytes = MAX_OUTPUT_BYTES } = options;
  if (signal?.aborted) return Promise.reject(new Error('操作已取消'));
  if (SESSION_ID.test(host)) {
    return runPasswordCommand(host, command, { ...options, timeout, maxOutputBytes }, remoteExitError);
  }
  const destination = host.replace(/\[([0-9a-f:]+)\]$/i, '$1');
  return new Promise((resolve, reject) => {
    const child = spawn('ssh', ['-T', '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=yes',
      '-o', 'ConnectTimeout=8', '-o', 'ServerAliveInterval=5', '-o', 'ServerAliveCountMax=2', destination, command],
    { windowsHide: true, stdio: [input == null ? 'ignore' : 'pipe', 'pipe', 'pipe'] });
    const stdout = [];
    const stderr = [];
    const decoder = new StringDecoder('utf8');
    let stdoutBytes = 0;
    let stderrBytes = 0;
    let failure;

    function terminate(error) {
      failure ??= error;
      child.kill();
    }

    const cancel = () => terminate(new Error('操作已取消'));
    const timer = setTimeout(() => terminate(new Error('等待远端响应超时')), timeout);
    signal?.addEventListener('abort', cancel, { once: true });
    if (signal?.aborted) cancel();

    child.stdout.on('data', (chunk) => {
      if (failure) return;
      if (onChunk) {
        const decoded = decoder.write(chunk);
        if (decoded) onChunk(decoded);
      } else {
        stdoutBytes += chunk.length;
        if (stdoutBytes > maxOutputBytes) terminate(new Error('远端输出超过大小限制，请缩小查询范围'));
        else stdout.push(chunk);
      }
    });
    child.stderr.on('data', (chunk) => {
      if (stderrBytes < 4096) {
        stderr.push(chunk.subarray(0, 4096 - stderrBytes));
        stderrBytes += chunk.length;
      }
    });
    child.on('error', (error) => {
      failure ??= error.code === 'ENOENT' ? new Error('找不到 ssh，请将兼容的 OpenSSH 客户端加入 PATH') : error;
    });
    child.stdin?.on('error', (error) => {
      if (error.code !== 'EPIPE') terminate(error);
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      signal?.removeEventListener('abort', cancel);
      if (failure) return reject(failure);
      if (code !== 0) return reject(remoteExitError(code, stdout, stderr, { sensitive, bootDiagnostics }));
      if (onChunk) {
        const remaining = decoder.end();
        if (remaining) onChunk(remaining);
      }
      const contents = Buffer.concat(stdout);
      resolve(binary ? contents : contents.toString('utf8'));
    });
    if (input != null) child.stdin.end(input);
  });
}

function dockerExec(host, container, args, options = {}) {
  const flags = [];
  if (options.input != null) flags.push('-i');
  if (options.user) flags.push('--user', options.user);
  return runSsh(host, shellCommand(['docker', 'exec', ...flags, container, ...args]), options);
}

function curlConfig(entries) {
  return entries.map(([key, value]) => {
    if (value == null) return key;
    const quoted = String(value).replace(/\\/g, '\\\\').replace(/"/g, '\\"')
      .replace(/\r/g, '\\r').replace(/\n/g, '\\n').replace(/\t/g, '\\t');
    return `${key} = "${quoted}"`;
  }).join('\n') + '\n';
}

module.exports = { runSsh, shellCommand, dockerExec, curlConfig };
