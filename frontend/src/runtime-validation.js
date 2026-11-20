const { createHash, randomBytes } = require('crypto');

const HTTP_METHODS = new Set(['GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS']);
const MAX_CASES = 30;
const MAX_JSON_BYTES = 512 * 1024;
const PRIVATE_HEADERS = /(?:^|[-_])(auth|authorization|authentication|cookie|token|api[-_]?key|secret|password|credential|csrf|session[-_]?id)(?:$|[-_])/i;
const CORRELATION_HEADERS = /^(x-request-id|request-id|x-trace-id|trace-id|traceparent|x-b3-traceid)$/i;
const SAFE_CORRELATION_ID = /^[A-Za-z0-9_.:-]{8,160}$/;

function sha256(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

function normalizeSuite(input) {
  if (!input || input.version !== 1 || !Array.isArray(input.cases) || input.cases.length > MAX_CASES) {
    throw new Error('请求集合需使用版本 1，最多 30 条用例');
  }
  const names = new Set();
  const cases = input.cases.map((item, index) => {
    const name = String(item?.name ?? '').trim();
    if (!name || name.length > 100 || names.has(name)) throw new Error('用例名称须唯一，长度为 1 至 100 字符');
    names.add(name);
    if (!HTTP_METHODS.has(item.method) || !['http', 'https'].includes(item.protocol)
        || typeof item.path !== 'string' || !/^\/[^\u0000-\u0020\u007f#]*$/.test(item.path) || item.path.length > 4096) {
      throw new Error('第 ' + (index + 1) + ' 条用例的请求方式、协议或路径无效');
    }
    const serverName = String(item.serverName ?? '').trim();
    const headers = String(item.headers ?? '').split(/\r?\n/).filter((line) => {
      const delimiter = line.indexOf(':');
      return delimiter < 0 || !PRIVATE_HEADERS.test(line.slice(0, delimiter).trim());
    }).join('\n');
    const body = String(item.body ?? '');
    if (serverName.length > 253 || Buffer.byteLength(headers) > 8192 || Buffer.byteLength(body) > 65536) {
      throw new Error('用例的 TLS 主机名、请求头或请求体超过大小限制');
    }
    const rules = item.assertions ?? [];
    if (!Array.isArray(rules) || rules.length > 12) throw new Error('每条用例最多 12 个断言');
    return { name, method: item.method, protocol: item.protocol, path: item.path, serverName, headers, body,
      assertions: rules.map(normalizeRule) };
  });
  const suite = { version: 1, cases };
  if (Buffer.byteLength(JSON.stringify(suite, null, 2)) > MAX_JSON_BYTES) throw new Error('请求集合超过 512 KiB');
  return suite;
}

function normalizeRule(rule) {
  const type = rule?.type;
  const value = rule?.value;
  if (type === 'status' && Number.isInteger(value) && value >= 100 && value <= 599) return { type, value };
  if (type === 'maxMs' && Number.isInteger(value) && value >= 1 && value <= 12000) return { type, value };
  if (type === 'contains' && typeof value === 'string' && value.length > 0 && value.length <= 2048) return { type, value };
  if (type === 'jsonEquals' && typeof rule.pointer === 'string' && rule.pointer.length <= 300
      && /^(?:\/(?:[^~/]|~[01])*)*$/.test(rule.pointer) && Object.hasOwn(rule, 'value')
      && JSON.stringify(value)?.length <= 4096) {
    canonicalJson(value);
    return { type, pointer: rule.pointer, value };
  }
  throw new Error('断言不完整，支持 status、maxMs、contains、jsonEquals');
}

function readPointer(value, pointer) {
  if (!pointer) return { found: true, value };
  for (const segment of pointer.slice(1).split('/')) {
    const field = segment.replace(/~1/g, '/').replace(/~0/g, '~');
    if (value === null || typeof value !== 'object' || !Object.hasOwn(value, field)) return { found: false };
    value = value[field];
  }
  return { found: true, value };
}

function canonicalJson(value) {
  if (typeof value === 'number' && (!Number.isFinite(value) || (Number.isInteger(value) && !Number.isSafeInteger(value)))) {
    throw new Error('JSON 数值超出精度范围，可用字符串作为断言值');
  }
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function assertionResults(rules, response) {
  return rules.map((rule) => {
    let passed;
    if (rule.type === 'status') passed = response.status === rule.value;
    else if (rule.type === 'maxMs') passed = response.elapsedMs <= rule.value;
    else if (rule.type === 'contains') passed = response.textBody.includes(rule.value);
    else {
      try {
        const item = readPointer(JSON.parse(response.textBody), rule.pointer);
        passed = item.found && canonicalJson(item.value) === canonicalJson(rule.value);
      } catch {
        passed = false;
      }
    }
    return { type: rule.type, passed: Boolean(passed) };
  });
}

function captureMarker() {
  return '\nJARPATCH_HTTP_' + randomBytes(16).toString('hex') + ' ';
}

function correlationValues(headerText) {
  const values = [];
  for (const line of String(headerText || '').split(/\r?\n/)) {
    const index = line.indexOf(':');
    const header = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim();
    if (index < 1 || !CORRELATION_HEADERS.test(header) || !SAFE_CORRELATION_ID.test(value)) continue;
    values.push(value);
    if (header.toLowerCase() === 'traceparent') {
      const match = value.match(/^[\da-f]{2}-([\da-f]{32})-[\da-f]{16}-[\da-f]{2}$/i);
      if (match) values.push(match[1]);
    }
  }
  return values;
}

function parseHttpCapture(output, marker, outgoingHeaders) {
  const split = output.lastIndexOf(marker);
  const match = split >= 0 && /^(\d{3}) (\d+(?:\.\d+)?) (\d+)\n$/.exec(output.subarray(split + marker.length).toString('ascii'));
  if (!match) throw new Error('无法识别远端 HTTP 状态和响应大小');
  const status = Number(match[1]);
  const elapsedMs = Math.round(Number(match[2]) * 1000);
  const size = Number(match[3]);
  if (status < 100 || status > 599 || !Number.isSafeInteger(size) || size > split) throw new Error('远端 HTTP 响应元数据无效');
  const body = output.subarray(split - size, split);
  const headers = output.subarray(0, split - size).toString('utf8');
  const textBody = body.toString('utf8');
  let digest = sha256(body);
  try {
    if (/\bcontent-type:[^\r\n]*\bjson\b/i.test(headers)) digest = sha256(canonicalJson(JSON.parse(textBody)));
  } catch {
    // 非法或过深的 JSON 仍可按原始字节比较。
  }
  const correlationIds = [...new Set([...correlationValues(headers), ...correlationValues(outgoingHeaders)])].slice(0, 8);
  return {
    status, elapsedMs, size, digest, correlationIds, textBody,
    text: output.subarray(0, split).toString('utf8') + '\n\nHTTP ' + status + '  |  ' + match[2] + 's  |  ' + size + ' bytes\n'
  };
}

function selectCorrelatedLogs(logs, ids) {
  const lines = logs.split(/\r?\n/);
  const tokens = ids.map((id) => new RegExp('(^|[^A-Za-z0-9_.-])' + id.replace(/[.*+?^$(){}|[\]\\]/g, '\\$&') + '(?=$|[^A-Za-z0-9_.-])'));
  const matches = new Set();
  lines.forEach((line, index) => {
    if (tokens.some((token) => token.test(line))) {
      for (let context = Math.max(index - 1, 0); context <= Math.min(index + 2, lines.length - 1); context++) matches.add(context);
    }
  });
  return matches.size
    ? { mode: 'id', text: [...matches].sort((a, b) => a - b).map((index) => lines[index]).join('\n') }
    : { mode: 'time', text: logs };
}

module.exports = { HTTP_METHODS, MAX_JSON_BYTES, normalizeSuite, sha256, assertionResults, captureMarker,
  parseHttpCapture, selectCorrelatedLogs };
