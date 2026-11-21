(function initializeApiClient(global) {
  'use strict';

  const TOKEN_HEADER = 'X-JarPatch-Token';
  const SESSION_HEADER = 'X-JarPatch-Session';
  const CONTENT_TYPE_HEADER = 'Content-Type';
  const JSON_CONTENT_TYPE = 'application/json';
  const HTTP_STATUS_UNAUTHORIZED = 401;
  const AUTH_LOGIN_PATH = '/api/auth/login';
  const AUTH_BOOTSTRAP_PATH = '/api/auth/bootstrap';
  const MESSAGE_CONNECTION_NOT_READY = '后端安全连接尚未就绪';
  const MESSAGE_OPERATION_FAILED = '操作失败';

  let apiBase = '';
  let webSocketBase = '';
  let token = '';
  let sessionToken = '';
  let unauthorizedHandler = null;

  /**
   * 保存 Electron 主进程提供的本地后端安全连接信息。
   *
   * @param {object} connection 后端地址和一次性实例令牌
   */
  function configure(connection) {
    apiBase = connection.apiBase;
    webSocketBase = apiBase.replace(/^http/, 'ws');
    token = connection.token;
  }

  /**
   * 更新当前登录用户的会话令牌，同时在存在主进程桥接时同步给主进程。
   *
   * @param {string} nextSessionToken 用户会话令牌，传空串表示已退出
   */
  function setSessionToken(nextSessionToken) {
    sessionToken = typeof nextSessionToken === 'string' ? nextSessionToken.trim() : '';
    if (global.jarPatch && typeof global.jarPatch.setSessionToken === 'function') {
      global.jarPatch.setSessionToken(sessionToken).catch(() => {});
    }
  }

  /**
   * 读取当前内存中的用户会话令牌。
   *
   * @returns {string} 会话令牌
   */
  function getSessionToken() {
    return sessionToken;
  }

  /**
   * 注册会话失效（HTTP 401）全局回调，供认证模块立即切回登录页。
   *
   * @param {Function|null} handler 回调处理器
   */
  function setUnauthorizedHandler(handler) {
    unauthorizedHandler = typeof handler === 'function' ? handler : null;
  }

  /**
   * 发起带本地实例令牌与用户会话令牌的 JSON API 请求。
   *
   * @param {string} path    接口相对路径
   * @param {object} options fetch 选项
   * @returns {Promise<any>} 接口 data 字段
   */
  async function request(path, options = {}) {
    if (!apiBase || !token) {
      throw new Error(MESSAGE_CONNECTION_NOT_READY);
    }
    const { headers = {}, skipUnauthorizedCallback = false, ...requestOptions } = options;
    const mergedHeaders = {
      [CONTENT_TYPE_HEADER]: JSON_CONTENT_TYPE,
      [TOKEN_HEADER]: token,
      ...headers
    };
    if (sessionToken) {
      mergedHeaders[SESSION_HEADER] = sessionToken;
    }
    const response = await fetch(`${apiBase}${path}`, {
      ...requestOptions,
      headers: mergedHeaders
    });
    const body = await response.json();
    if (response.status === HTTP_STATUS_UNAUTHORIZED
        && !skipUnauthorizedCallback
        && path !== AUTH_LOGIN_PATH
        && path !== AUTH_BOOTSTRAP_PATH
        && unauthorizedHandler) {
      unauthorizedHandler(body && body.message ? body.message : MESSAGE_OPERATION_FAILED);
    }
    if (!body || !body.success) {
      const error = new Error(body && body.message ? body.message : MESSAGE_OPERATION_FAILED);
      error.httpStatus = response.status;
      throw error;
    }
    return body.data;
  }

  /**
   * 生成带实例令牌与用户会话令牌的任务 WebSocket 地址。
   *
   * @param {string} taskId 任务 ID
   * @returns {string} WebSocket 完整地址
   */
  function taskWebSocketUrl(taskId) {
    if (!webSocketBase || !token) {
      throw new Error(MESSAGE_CONNECTION_NOT_READY);
    }
    const sessionQuery = sessionToken ? `&session=${encodeURIComponent(sessionToken)}` : '';
    return `${webSocketBase}/ws/tasks/${taskId}?token=${encodeURIComponent(token)}${sessionQuery}`;
  }

  global.jarPatchApiClient = Object.freeze({
    configure,
    request,
    taskWebSocketUrl,
    setSessionToken,
    getSessionToken,
    setUnauthorizedHandler
  });
}(window));
