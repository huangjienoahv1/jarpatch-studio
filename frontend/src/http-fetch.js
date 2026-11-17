/**
 * 主进程统一 HTTP 客户端。现代 Electron 使用原生 fetch；Win7 兼容版的 Node 16
 * 没有该全局 API 时使用 node-fetch 2，保留 JSON、文本响应及 AbortController 取消行为。
 */
module.exports = typeof globalThis.fetch === 'function'
  ? globalThis.fetch.bind(globalThis)
  : require('node-fetch');
