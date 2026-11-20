const fetch = require('./http-fetch');

const LOKI_MAX_BYTES = 2 * 1024 * 1024;
const ACTUATOR_ENDPOINTS = new Set(['health', 'info', 'metrics', 'mappings', 'httpexchanges']);

function requireWebUrl(value, message) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error(message);
  }
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error(message);
  }
  return url;
}

function tunnelUrl(data) {
  if (typeof data.agentId !== 'string' || !/^[A-Za-z0-9_.-]{1,120}$/.test(data.agentId)) throw new Error('Arthas Agent ID 格式无效');
  const url = requireWebUrl(data.url, '请输入 Tunnel 控制台的 HTTP 或 HTTPS 地址');
  if (url.protocol === 'http:' && !['127.0.0.1', '[::1]', 'localhost'].includes(url.hostname)) {
    throw new Error('远程 Tunnel 控制台请使用 HTTPS');
  }
  if (data.port) {
    const port = Number(data.port);
    if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Tunnel WebSocket 端口无效');
    url.searchParams.set('port', String(port));
  }
  url.searchParams.set('agentId', data.agentId);
  return url.toString();
}

function actuatorPath(data) {
  const path = data.basePath || '/actuator';
  if (typeof path !== 'string' || path.length > 160 || !/^\/[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)*\/?$/.test(path)) {
    throw new Error('Actuator 基础路径无效');
  }
  if (!ACTUATOR_ENDPOINTS.has(data.endpoint)) throw new Error('请选择可读取的 Actuator 端点');
  let endpoint = data.endpoint;
  if (endpoint === 'metrics' && data.metric) {
    if (typeof data.metric !== 'string' || !/^[A-Za-z0-9_.-]{1,120}$/.test(data.metric)) throw new Error('指标名格式无效');
    endpoint += '/' + data.metric;
  }
  return path.replace(/\/$/, '') + '/' + endpoint;
}

async function queryLoki(data, signal) {
  const url = requireWebUrl(data.url, '请输入 Loki HTTP 或 HTTPS 基础地址');
  const minutes = Number(data.minutes);
  if (!Number.isInteger(minutes) || minutes < 1 || minutes > 60) throw new Error('日志查询时段须为最近 1 至 60 分钟');
  if (typeof data.query !== 'string' || !data.query.trim() || data.query.length > 1000 || /[\u0000-\u0008\u000b-\u001f]/.test(data.query)) {
    throw new Error('请输入有效的 LogQL 查询表达式');
  }
  const headers = { Accept: 'application/json' };
  if (data.tenant) {
    if (typeof data.tenant !== 'string' || !/^[A-Za-z0-9_.-]{1,150}$/.test(data.tenant)) throw new Error('Loki 租户格式无效');
    headers['X-Scope-OrgID'] = data.tenant;
  }
  if (data.token) {
    if (typeof data.token !== 'string' || !/^[A-Za-z0-9_.~+\/=-]{1,4096}$/.test(data.token)) throw new Error('Loki Bearer 凭据格式无效');
    headers.Authorization = 'Bearer ' + data.token;
  }
  const end = Date.now();
  url.pathname = url.pathname.replace(/\/$/, '') + '/loki/api/v1/query_range';
  url.searchParams.set('query', data.query);
  url.searchParams.set('start', String(BigInt(end - minutes * 60000) * 1000000n));
  url.searchParams.set('end', String(BigInt(end) * 1000000n));
  url.searchParams.set('limit', '1000');
  url.searchParams.set('direction', 'backward');
  const controller = new AbortController();
  const abort = () => controller.abort();
  const timer = setTimeout(abort, 12000);
  signal.addEventListener('abort', abort, { once: true });
  if (signal.aborted) abort();
  try {
    const response = await fetch(url, { method: 'GET', headers, signal: controller.signal, redirect: 'error' });
    if (!response.ok) throw new Error('Loki 返回 HTTP ' + response.status);
    const chunks = [];
    let size = 0;
    for await (const chunk of response.body) {
      size += chunk.length;
      if (size > LOKI_MAX_BYTES) throw new Error('Loki 结果超过 2 MiB，请缩小查询范围');
      chunks.push(chunk);
    }
    const body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
    if (body.status !== 'success' || body.data?.resultType !== 'streams') throw new Error('请使用返回日志流的 LogQL 查询');
    const lines = body.data.result.flatMap((entry) => entry.values.map(([timestamp, text]) => ({ timestamp, text, labels: entry.stream })))
      .filter((line) => typeof line.text === 'string' && /^\d{1,20}$/.test(line.timestamp))
      .sort((a, b) => (a.timestamp < b.timestamp ? 1 : a.timestamp > b.timestamp ? -1 : 0));
    return { count: lines.length, text: lines.slice(0, 1000).map(({ timestamp, text, labels }) =>
      new Date(Number(BigInt(timestamp) / 1000000n)).toISOString() + ' ' + JSON.stringify(labels) + ' ' + text).join('\n') };
  } catch (error) {
    if (error.name === 'AbortError') throw new Error(signal.aborted ? '操作已取消' : 'Loki 查询超时');
    if (error instanceof TypeError) throw new Error('无法连接 Loki，请核对网络、证书或代理地址');
    if (error instanceof SyntaxError) throw new Error('Loki 未返回有效 JSON');
    throw error;
  } finally {
    abort();
    clearTimeout(timer);
    signal.removeEventListener('abort', abort);
  }
}

module.exports = { queryLoki, actuatorPath, tunnelUrl };
