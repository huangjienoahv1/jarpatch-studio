const { createHash, randomBytes } = require('crypto');
const { isIP } = require('net');
const { StringDecoder } = require('string_decoder');
const { Client } = require('ssh2');

const SESSION_ID = /^password-ssh-[a-f0-9]{32}$/;
const HOSTNAME = /^[a-z0-9](?:[a-z0-9_.-]{0,251}[a-z0-9])?$/i;
const USERNAME = /^[a-z0-9_][a-z0-9_.-]{0,63}$/i;
const sessions = new Map();

function sshPasswordTarget(data) {
  const inputHost = typeof data.host === 'string' ? data.host.trim().replace(/^\[([a-f0-9:]+)\]$/i, '$1') : '';
  const username = typeof data.username === 'string' ? data.username.trim() : '';
  const port = Number(data.port);
  if (!HOSTNAME.test(inputHost) && !isIP(inputHost)) throw new Error('请输入服务器的 IP 地址或主机名');
  if (!USERNAME.test(username)) throw new Error('请输入有效的 SSH 账号');
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('SSH 端口须为 1 至 65535');
  return { host: inputHost.toLowerCase(), username, port };
}

function assertSshPassword(password) {
  if (typeof password !== 'string' || !password || Buffer.byteLength(password) > 4096 || password.includes('\0')) {
    throw new Error('请输入有效的 SSH 密码');
  }
}

function openPasswordSession(data, confirmHost, signal) {
  const { host: inputHost, username, port } = sshPasswordTarget(data);
  let password = data.password;
  assertSshPassword(password);
  if (signal?.aborted) return Promise.reject(new Error('操作已取消'));
  const client = new Client();
  const id = 'password-ssh-' + randomBytes(16).toString('hex');
  return new Promise((resolve, reject) => {
    let finished = false;
    let ready = false;
    let verificationError;

    function forgetPassword() {
      password = '';
      if (client.config) client.config.password = undefined;
    }
    function fail(message) {
      if (finished) return;
      finished = true;
      signal?.removeEventListener('abort', abort);
      forgetPassword();
      client.destroy();
      reject(new Error(message));
    }
    const abort = () => fail('操作已取消');
    signal?.addEventListener('abort', abort, { once: true });
    client.on('error', (error) => {
      if (ready) sessions.delete(id);
      else fail(verificationError || (error.level === 'client-authentication'
        ? 'SSH 登录失败，请检查账号、密码和服务器的密码认证配置'
        : '无法建立 SSH 连接，请检查主机、端口和网络'));
    });
    client.on('close', () => {
      sessions.delete(id);
      fail(verificationError || 'SSH 连接在登录完成前关闭');
    });
    client.on('keyboard-interactive', (_, __, ___, prompts, answer) => {
      const prompt = prompts[0];
      answer(prompts.length === 1 && !prompt.echo && /password|密码/i.test(prompt.prompt) ? [password] : []);
    });
    client.once('ready', () => {
      if (finished) return;
      finished = true;
      ready = true;
      signal?.removeEventListener('abort', abort);
      forgetPassword();
      sessions.set(id, { client, label: username + '@' + inputHost + ':' + port });
      resolve(id);
    });
    try {
      client.connect({
        host: inputHost, port, username, password, tryKeyboard: true,
        readyTimeout: 60000, keepaliveInterval: 5000, keepaliveCountMax: 2,
        hostVerifier(key, done) {
          const fingerprint = 'SHA256:' + createHash('sha256').update(key).digest('base64').replace(/=+$/, '');
          Promise.resolve().then(() => confirmHost({ host: inputHost, port, fingerprint })).then((confirmed) => {
            if (finished) return;
            if (!confirmed) verificationError = '未确认 SSH 服务器指纹，连接已取消';
            done(Boolean(confirmed));
          }, (error) => {
            if (finished) return;
            verificationError = error.message;
            done(false);
          });
        }
      });
    } catch {
      fail('无法初始化 SSH 连接');
    }
  });
}

function closePasswordSession(id) {
  const session = sessions.get(id);
  sessions.delete(id);
  session?.client.end();
}

function passwordSessionLabel(id) {
  return sessions.get(id)?.label;
}

function openPasswordLoopback(id, port, signal, callback) {
  const session = sessions.get(id);
  if (!session) return callback(new Error('密码 SSH 会话已断开，请重新连接'));
  if (signal?.aborted) return callback(new Error('操作已取消'));
  try {
    session.client.forwardOut('127.0.0.1', 0, '127.0.0.1', port, (error, stream) => {
      if (signal?.aborted) {
        stream?.close();
        return callback(new Error('操作已取消'));
      }
      if (error) return callback(new Error('SSH 转发失败，请检查服务端转发权限和 Arthas HTTP 端口'));
      callback(null, stream);
    });
  } catch {
    callback(new Error('密码 SSH 会话已断开，请重新连接'));
  }
}

function runPasswordCommand(id, command, options, exitError) {
  const session = sessions.get(id);
  if (!session) return Promise.reject(new Error('密码 SSH 会话已断开，请重新输入密码连接'));
  const { signal, input, timeout, maxOutputBytes, binary, onChunk } = options;
  if (signal?.aborted) return Promise.reject(new Error('操作已取消'));
  return new Promise((resolve, reject) => {
    const stdout = [];
    const stderr = [];
    const decoder = new StringDecoder('utf8');
    let bytes = 0;
    let errorBytes = 0;
    let finished = false;
    let channel;
    function finish(error, value) {
      if (finished) return;
      finished = true;
      clearTimeout(timer);
      signal?.removeEventListener('abort', cancel);
      session.client.removeListener('close', disconnected);
      if (error) reject(error);
      else resolve(value);
    }
    function stop(message) {
      finish(new Error(message));
      channel?.close();
    }
    const cancel = () => stop('操作已取消');
    const disconnected = () => stop('密码 SSH 会话已断开，请重新输入密码连接');
    const timer = setTimeout(() => stop('等待远端响应超时'), timeout);
    signal?.addEventListener('abort', cancel, { once: true });
    session.client.once('close', disconnected);
    try {
      session.client.exec(command, (error, stream) => {
        channel = stream;
        if (finished) {
          stream?.close();
          return;
        }
        if (error) return finish(new Error('无法在 SSH 会话中启动远程操作'));
        stream.on('data', (chunk) => {
          if (finished) return;
          if (onChunk) {
            const decoded = decoder.write(chunk);
            if (decoded) onChunk(decoded);
          } else {
            bytes += chunk.length;
            if (bytes > maxOutputBytes) stop('远端输出超过大小限制，请缩小查询范围');
            else stdout.push(chunk);
          }
        });
        stream.stderr.on('data', (chunk) => {
          if (errorBytes >= 4096) return;
          stderr.push(chunk.subarray(0, 4096 - errorBytes));
          errorBytes += chunk.length;
        });
        stream.once('error', () => finish(new Error('SSH 远程操作意外中断')));
        stream.once('close', (code) => {
          if (finished) return;
          if (code == null) return finish(new Error('SSH 远程操作未返回退出状态'));
          if (code !== 0) return finish(exitError(code, stdout, stderr, options));
          const remaining = onChunk && decoder.end();
          if (remaining) onChunk(remaining);
          const contents = Buffer.concat(stdout);
          finish(null, binary ? contents : contents.toString('utf8'));
        });
        stream.end(input);
      });
    } catch {
      disconnected();
    }
  });
}

module.exports = { SESSION_ID, openPasswordSession, closePasswordSession, passwordSessionLabel, runPasswordCommand,
  openPasswordLoopback, sshPasswordTarget, assertSshPassword };
