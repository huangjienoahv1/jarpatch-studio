(function initializeApiClient(global) {
  'use strict';

  const TOKEN_HEADER = 'X-JarPatch-Token';
  const CONTENT_TYPE_HEADER = 'Content-Type';
  const JSON_CONTENT_TYPE = 'application/json';
  const MESSAGE_CONNECTION_NOT_READY = '后端安全连接尚未就绪';
  const MESSAGE_OPERATION_FAILED = '操作失败';

  let apiBase = '';
  let webSocketBase = '';
  let token = '';

  /**
   * 保存 Electron 主进程提供的本地后端安全连接信息。
   *
   * @param connection 后端地址和一次性令牌
   */
  function configure(connection) {
    apiBase = connection.apiBase;
    webSocketBase = apiBase.replace(/^http/, 'ws');
    token = connection.token;
  }

  /**
   * 发起带本地实例令牌的 JSON API 请求。
   *
   * @param path 接口相对路径
   * @param options fetch 选项
   * @return 接口 data 字段
   */
  async function request(path, options = {}) {
    if (!apiBase || !token) {
      throw new Error(MESSAGE_CONNECTION_NOT_READY);
    }
    const { headers = {}, ...requestOptions } = options;
    const response = await fetch(`${apiBase}${path}`, {
      ...requestOptions,
      headers: {
        [CONTENT_TYPE_HEADER]: JSON_CONTENT_TYPE,
        [TOKEN_HEADER]: token,
        ...headers
      }
    });
    const body = await response.json();
    if (!body.success) {
      throw new Error(body.message || MESSAGE_OPERATION_FAILED);
    }
    return body.data;
  }

  /**
   * 生成带令牌的任务 WebSocket 地址。
   *
   * @param taskId 任务 ID
   * @return WebSocket 完整地址
   */
  function taskWebSocketUrl(taskId) {
    if (!webSocketBase || !token) {
      throw new Error(MESSAGE_CONNECTION_NOT_READY);
    }
    return `${webSocketBase}/ws/tasks/${taskId}?token=${encodeURIComponent(token)}`;
  }

  global.jarPatchApiClient = Object.freeze({ configure, request, taskWebSocketUrl });
}(window));
