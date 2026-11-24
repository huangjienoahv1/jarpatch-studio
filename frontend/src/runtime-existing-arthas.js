const { loopbackHttp } = require('./runtime-local-jvm');
const { openSshLoopback } = require('./runtime-transport');

const EXISTING_ARTHAS = 'existing-arthas';
const CONNECTIONS = new Set(['local', 'openssh', 'password']);

function existingArthasOptions(data) {
  if (!CONNECTIONS.has(data.connection)) throw new Error('请选择本机或 SSH 连接方式');
  const port = Number(data.arthasPort);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Arthas HTTP 端口须为 1 至 65535');
  const username = data.arthasUser ?? '';
  const password = data.arthasPassword ?? '';
  if (typeof username !== 'string' || typeof password !== 'string' || Boolean(username) !== Boolean(password)
      || Buffer.byteLength(username) > 128 || Buffer.byteLength(password) > 4096
      || /[:\u0000-\u001f\u007f]/.test(username) || /[\u0000-\u001f\u007f]/.test(password)) {
    throw new Error('Arthas 账号和密码须同时填写，或同时留空');
  }
  return { port, authorization: username ? 'Basic ' + Buffer.from(username + ':' + password).toString('base64') : undefined };
}

async function arthasHttp(port, data, { authorization, sshHost, signal, timeout } = {}) {
  const body = JSON.stringify(data);
  const headers = { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body), Connection: 'close' };
  if (authorization) headers.Authorization = authorization;
  const createConnection = sshHost ? (_, callback) => openSshLoopback(sshHost, port, signal, callback) : undefined;
  return loopbackHttp({ port, address: '/api', headers, body, signal, timeout, createConnection });
}

function parseArthasHttp(response) {
  if (response.status === 401 || response.status === 403) throw new Error('Arthas 身份验证失败，请检查账号和密码');
  if (response.status !== 200) throw new Error('Arthas HTTP 返回状态 ' + response.status);
  try {
    const result = JSON.parse(response.body.toString('utf8'));
    if (!result || typeof result.state !== 'string'
        || (result.body?.results != null && !Array.isArray(result.body.results))) throw new Error('Invalid Arthas response');
    return result;
  } catch {
    throw new Error('目标端口未返回有效的 Arthas HTTP API 响应');
  }
}

module.exports = { EXISTING_ARTHAS, existingArthasOptions, arthasHttp, parseArthasHttp };
