/**
 * 本地用户登录、强制改密与用户管理交互模块。
 * <p>
 * 负责启动态会话校验、登录遮罩、首次强制改密、右上角用户身份菜单、个人改密弹窗以及
 * 管理员专属用户管理弹窗，所有状态变更与 {@link window.jarPatchApiClient} 实时同步。
 * </p>
 *
 * @author 黄杰
 */
(function initializeAuthModule(global) {
  'use strict';

  const STORAGE_KEY_SESSION_PERSIST = 'jarpatch.sessionToken';
  const STORAGE_KEY_SESSION_TEMP = 'jarpatch.sessionTokenTemp';
  const ROLE_ADMIN = 'ADMIN';
  const ROLE_USER = 'USER';
  const STATUS_ENABLED = 'ENABLED';
  const STATUS_DISABLED = 'DISABLED';
  const PASSWORD_MIN_LENGTH = 8;
  const NOTICE_SUCCESS = 'success';
  const NOTICE_ERROR = 'error';
  const NOTICE_INFO = 'info';

  const LABEL_ROLE_ADMIN = '管理员';
  const LABEL_ROLE_USER = '普通用户';
  const LABEL_STATUS_ENABLED = '已启用';
  const LABEL_STATUS_DISABLED = '已禁用';
  const LABEL_NEVER_LOGGED_IN = '尚未登录';
  const MESSAGE_LOGIN_SUBMITTING = '正在登录...';
  const MESSAGE_LOGIN_BUTTON = '登录工作台';
  const MESSAGE_LOGIN_EMPTY = '请输入用户名和密码';
  const MESSAGE_PASSWORD_TOO_SHORT = '新密码长度至少需要 8 个字符';
  const MESSAGE_PASSWORD_MISMATCH = '两次输入的新密码不一致';
  const MESSAGE_PASSWORD_CHANGE_SUCCESS = '密码已修改';
  const MESSAGE_LOGOUT_SUCCESS = '已退出登录';
  const MESSAGE_USER_CREATED = '用户已创建';
  const MESSAGE_USER_UPDATED = '用户信息已更新';
  const MESSAGE_USER_DISABLED = '用户已禁用并踢出活跃会话';
  const MESSAGE_USER_ENABLED = '用户已重新启用';
  const MESSAGE_USER_RESET_PASSWORD_SUCCESS = '密码已重置，该用户下次登录需修改密码';
  const MESSAGE_USER_DELETED = '用户已删除';
  const MESSAGE_USER_LIST_EMPTY = '没有符合条件的用户';
  const MESSAGE_CONFIRM_DELETE_USER_TITLE = '确认删除用户';
  const MESSAGE_CONFIRM_DISABLE_USER_TITLE = '确认禁用用户';
  const MESSAGE_CONFIRM_LOGOUT_TITLE = '退出当前登录';
  const MESSAGE_CONFIRM_LOGOUT_BODY = '退出后将清除本机登录状态并返回登录页，请确认当前文件修改均已保存。';
  const TITLE_USER_FORM_CREATE = '新增本地用户';
  const TITLE_USER_FORM_EDIT = '编辑用户资料与权限';

  const authState = {
    currentUser: null,
    bootstrap: null,
    users: [],
    editingUserId: null,
    resetPasswordUserId: null,
    callbacks: {
      onAuthenticated: () => {},
      onLoggedOut: () => {},
      onNotify: () => {}
    }
  };

  /**
   * 集中获取登录、改密与用户管理界面 DOM 节点。
   *
   * @returns {object} DOM 引用对象
   */
  function getRefs() {
    return {
      authOverlay: document.getElementById('authOverlay'),
      loginCard: document.getElementById('authLoginCard'),
      forceChangeCard: document.getElementById('authForceChangeCard'),
      authThemeBtn: document.getElementById('authThemeToggleBtn'),
      loginForm: document.getElementById('authLoginForm'),
      loginUsername: document.getElementById('authUsernameInput'),
      loginPassword: document.getElementById('authPasswordInput'),
      toggleLoginPwdBtn: document.getElementById('authTogglePasswordBtn'),
      rememberLoginInput: document.getElementById('authRememberInput'),
      loginError: document.getElementById('authLoginError'),
      loginSubmitBtn: document.getElementById('authLoginSubmitBtn'),
      defaultAdminHint: document.getElementById('authDefaultAdminHint'),
      defaultAdminHintText: document.getElementById('authDefaultAdminHintText'),
      fillDefaultAdminBtn: document.getElementById('authFillDefaultAdminBtn'),
      forceChangeForm: document.getElementById('authForceChangeForm'),
      forceChangeSummary: document.getElementById('authForceChangeSummary'),
      forceOldPassword: document.getElementById('authForceOldPassword'),
      forceNewPassword: document.getElementById('authForceNewPassword'),
      forceConfirmPassword: document.getElementById('authForceConfirmPassword'),
      forceChangeError: document.getElementById('authForceChangeError'),
      forceChangeSubmitBtn: document.getElementById('authForceChangeSubmitBtn'),
      forceLogoutBtn: document.getElementById('authForceLogoutBtn'),
      userMenu: document.getElementById('userMenu'),
      userMenuBtn: document.getElementById('userMenuBtn'),
      userMenuAvatar: document.getElementById('userMenuAvatar'),
      userMenuName: document.getElementById('userMenuName'),
      userMenuRole: document.getElementById('userMenuRole'),
      userMenuPopover: document.getElementById('userMenuPopover'),
      userMenuHeaderName: document.getElementById('userMenuHeaderName'),
      userMenuHeaderMeta: document.getElementById('userMenuHeaderMeta'),
      openUserManagementBtn: document.getElementById('openUserManagementBtn'),
      openChangePasswordBtn: document.getElementById('openChangePasswordBtn'),
      logoutBtn: document.getElementById('logoutBtn'),
      changePasswordDialog: document.getElementById('changePasswordDialog'),
      changePasswordForm: document.getElementById('changePasswordForm'),
      currentPasswordInput: document.getElementById('currentPasswordInput'),
      newPasswordInput: document.getElementById('newPasswordInput'),
      confirmPasswordInput: document.getElementById('confirmPasswordInput'),
      changePasswordError: document.getElementById('changePasswordError'),
      closeChangePasswordBtn: document.getElementById('closeChangePasswordBtn'),
      submitChangePasswordBtn: document.getElementById('submitChangePasswordBtn'),
      userManagementDialog: document.getElementById('userManagementDialog'),
      userSearchInput: document.getElementById('userSearchInput'),
      userRoleFilter: document.getElementById('userRoleFilter'),
      userStatusFilter: document.getElementById('userStatusFilter'),
      openCreateUserBtn: document.getElementById('openCreateUserBtn'),
      userCountSummary: document.getElementById('userCountSummary'),
      userTableBody: document.getElementById('userTableBody'),
      closeUserManagementBtn: document.getElementById('closeUserManagementBtn'),
      userFormPanel: document.getElementById('userFormPanel'),
      userFormTitle: document.getElementById('userFormTitle'),
      userForm: document.getElementById('userForm'),
      userFormUsername: document.getElementById('userFormUsername'),
      userFormDisplayName: document.getElementById('userFormDisplayName'),
      userFormRole: document.getElementById('userFormRole'),
      userFormStatusField: document.getElementById('userFormStatusField'),
      userFormStatus: document.getElementById('userFormStatus'),
      userFormPasswordField: document.getElementById('userFormPasswordField'),
      userFormPassword: document.getElementById('userFormPassword'),
      userFormMustChangeField: document.getElementById('userFormMustChangeField'),
      userFormMustChange: document.getElementById('userFormMustChange'),
      userFormError: document.getElementById('userFormError'),
      cancelUserFormBtn: document.getElementById('cancelUserFormBtn'),
      saveUserFormBtn: document.getElementById('saveUserFormBtn'),
      resetPasswordPanel: document.getElementById('resetPasswordPanel'),
      resetPasswordTarget: document.getElementById('resetPasswordTarget'),
      resetPasswordForm: document.getElementById('resetPasswordForm'),
      resetPasswordInput: document.getElementById('resetPasswordInput'),
      resetPasswordConfirmInput: document.getElementById('resetPasswordConfirmInput'),
      resetPasswordError: document.getElementById('resetPasswordError'),
      cancelResetPasswordBtn: document.getElementById('cancelResetPasswordBtn'),
      confirmResetPasswordBtn: document.getElementById('confirmResetPasswordBtn')
    };
  }

  let refs = null;

  /**
   * 统一调用本地 API 客户端。
   *
   * @param {string} path    接口路径
   * @param {object} options 请求选项
   * @returns {Promise<any>} 响应数据
   */
  function api(path, options) {
    return global.jarPatchApiClient.request(path, options);
  }

  /**
   * 写入本地持久化或会话级令牌。
   *
   * @param {string} token    会话令牌
   * @param {boolean} persist 是否跨重启记住登录
   */
  function storeSessionToken(token, persist) {
    localStorage.removeItem(STORAGE_KEY_SESSION_PERSIST);
    sessionStorage.removeItem(STORAGE_KEY_SESSION_TEMP);
    if (token) {
      if (persist) {
        localStorage.setItem(STORAGE_KEY_SESSION_PERSIST, token);
      } else {
        sessionStorage.setItem(STORAGE_KEY_SESSION_TEMP, token);
      }
    }
    global.jarPatchApiClient.setSessionToken(token || '');
  }

  /**
   * 读取当前存储的会话令牌。
   *
   * @returns {string} 已保存的会话令牌
   */
  function loadStoredSessionToken() {
    return localStorage.getItem(STORAGE_KEY_SESSION_PERSIST)
      || sessionStorage.getItem(STORAGE_KEY_SESSION_TEMP)
      || '';
  }

  /**
   * 根据角色码返回中文标签。
   *
   * @param {string} role 角色码
   * @returns {string} 中文角色名
   */
  function getRoleLabel(role) {
    return role === ROLE_ADMIN ? LABEL_ROLE_ADMIN : LABEL_ROLE_USER;
  }

  /**
   * 根据状态码返回中文标签。
   *
   * @param {string} status 状态码
   * @returns {string} 中文状态名
   */
  function getStatusLabel(status) {
    return status === STATUS_ENABLED ? LABEL_STATUS_ENABLED : LABEL_STATUS_DISABLED;
  }

  /**
   * 提取用户名或显示名的首字符作为头像缩写。
   *
   * @param {object} user 用户对象
   * @returns {string} 首字符缩写
   */
  function getAvatarInitial(user) {
    const source = (user && (user.displayName || user.username)) || 'U';
    return source.trim().charAt(0).toUpperCase();
  }

  /**
   * 展示登录错误消息条。
   *
   * @param {HTMLElement} element 错误容器
   * @param {string} message      错误文案
   */
  function showFormError(element, message) {
    element.textContent = message || '';
    element.hidden = !message;
  }

  /**
   * 同步右上角用户头像按钮和下拉菜单中的身份信息。
   *
   * @param {object|null} user 当前登录用户
   */
  function syncUserMenu(user) {
    if (!user) {
      refs.userMenu.hidden = true;
      setUserMenuOpen(false);
      return;
    }
    const admin = user.role === ROLE_ADMIN;
    refs.userMenu.hidden = false;
    refs.userMenuAvatar.textContent = getAvatarInitial(user);
    refs.userMenuAvatar.classList.toggle('avatar-admin', admin);
    refs.userMenuName.textContent = user.displayName || user.username;
    refs.userMenuRole.textContent = getRoleLabel(user.role);
    refs.userMenuRole.className = `user-role-tag ${admin ? 'role-admin' : 'role-user'}`;
    refs.userMenuHeaderName.textContent = user.displayName || user.username;
    refs.userMenuHeaderMeta.textContent = `@${user.username} · ${getRoleLabel(user.role)}`;
    refs.openUserManagementBtn.hidden = !admin;
  }

  /**
   * 展开或关闭右上角用户下拉菜单。
   *
   * @param {boolean} open 是否展开
   */
  function setUserMenuOpen(open) {
    refs.userMenuPopover.hidden = !open;
    refs.userMenuBtn.setAttribute('aria-expanded', String(open));
  }

  /**
   * 显示全屏登录遮罩并切换到账号登录卡片。
   *
   * @param {string} noticeMessage 可选错误或失效提示
   */
  async function showLoginScreen(noticeMessage = '') {
    authState.currentUser = null;
    syncUserMenu(null);
    closeAllUserModals();
    refs.authOverlay.hidden = false;
    refs.loginCard.hidden = false;
    refs.forceChangeCard.hidden = true;
    refs.loginPassword.value = '';
    showFormError(refs.loginError, noticeMessage);
    await refreshBootstrapHint();
    refs.loginUsername.focus();
  }

  /**
   * 拉取并展示默认管理员首次使用提示（改密后后端自动返回 false 并隐藏）。
   */
  async function refreshBootstrapHint() {
    try {
      const bootstrap = await api('/api/auth/bootstrap', { skipUnauthorizedCallback: true });
      authState.bootstrap = bootstrap;
      if (bootstrap && bootstrap.defaultAdminHintVisible) {
        refs.defaultAdminHintText.textContent = `首次启动已预置默认管理员：账号 ${bootstrap.defaultAdminUsername}，初始密码 ${bootstrap.defaultAdminInitialPassword}（登录后须立即修改密码）`;
        refs.defaultAdminHint.hidden = false;
      } else {
        refs.defaultAdminHint.hidden = true;
      }
    } catch {
      refs.defaultAdminHint.hidden = true;
    }
  }

  /**
   * 展示首次登录或密码被重置后的强制修改密码卡片。
   *
   * @param {object} user 当前待改密用户
   */
  function showForceChangePasswordScreen(user) {
    authState.currentUser = user;
    syncUserMenu(null);
    refs.authOverlay.hidden = false;
    refs.loginCard.hidden = true;
    refs.forceChangeCard.hidden = false;
    refs.forceChangeSummary.textContent = `当前账号「${user.username}」需要先设置新密码（至少 ${PASSWORD_MIN_LENGTH} 位），完成后即可进入工作台。`;
    refs.forceOldPassword.value = '';
    refs.forceNewPassword.value = '';
    refs.forceConfirmPassword.value = '';
    showFormError(refs.forceChangeError, '');
    refs.forceOldPassword.focus();
  }

  /**
   * 完成登录并解锁主工作台。
   *
   * @param {object} user 已通过校验且无需改密的用户
   */
  function enterWorkbench(user) {
    authState.currentUser = user;
    refs.authOverlay.hidden = true;
    refs.loginCard.hidden = true;
    refs.forceChangeCard.hidden = true;
    syncUserMenu(user);
    authState.callbacks.onAuthenticated(user);
  }

  /**
   * 处理登录表单提交。
   *
   * @param {SubmitEvent} event 表单提交事件
   */
  async function handleLoginSubmit(event) {
    event.preventDefault();
    const username = refs.loginUsername.value.trim();
    const password = refs.loginPassword.value;
    if (!username || !password) {
      showFormError(refs.loginError, MESSAGE_LOGIN_EMPTY);
      return;
    }
    showFormError(refs.loginError, '');
    refs.loginSubmitBtn.disabled = true;
    refs.loginSubmitBtn.textContent = MESSAGE_LOGIN_SUBMITTING;
    try {
      const result = await api('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
        skipUnauthorizedCallback: true
      });
      storeSessionToken(result.sessionToken, refs.rememberLoginInput.checked);
      if (result.user && result.user.mustChangePassword) {
        showForceChangePasswordScreen(result.user);
        refs.forceOldPassword.value = password;
        refs.forceNewPassword.focus();
      } else {
        enterWorkbench(result.user);
      }
    } catch (error) {
      showFormError(refs.loginError, error.message);
    } finally {
      refs.loginSubmitBtn.disabled = false;
      refs.loginSubmitBtn.textContent = MESSAGE_LOGIN_BUTTON;
    }
  }

  /**
   * 处理强制修改密码提交。
   *
   * @param {SubmitEvent} event 表单提交事件
   */
  async function handleForceChangeSubmit(event) {
    event.preventDefault();
    const currentPassword = refs.forceOldPassword.value;
    const newPassword = refs.forceNewPassword.value;
    const confirmPassword = refs.forceConfirmPassword.value;
    const validationError = validateNewPasswordInputs(newPassword, confirmPassword);
    if (validationError) {
      showFormError(refs.forceChangeError, validationError);
      return;
    }
    showFormError(refs.forceChangeError, '');
    refs.forceChangeSubmitBtn.disabled = true;
    try {
      const updatedUser = await api('/api/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword })
      });
      authState.callbacks.onNotify(MESSAGE_PASSWORD_CHANGE_SUCCESS, NOTICE_SUCCESS);
      enterWorkbench(updatedUser);
    } catch (error) {
      showFormError(refs.forceChangeError, error.message);
    } finally {
      refs.forceChangeSubmitBtn.disabled = false;
    }
  }

  /**
   * 校验两次输入的新密码长度和一致性。
   *
   * @param {string} newPassword     新密码
   * @param {string} confirmPassword 确认密码
   * @returns {string} 错误提示；合法时返回空串
   */
  function validateNewPasswordInputs(newPassword, confirmPassword) {
    if (!newPassword || newPassword.length < PASSWORD_MIN_LENGTH) {
      return MESSAGE_PASSWORD_TOO_SHORT;
    }
    if (newPassword !== confirmPassword) {
      return MESSAGE_PASSWORD_MISMATCH;
    }
    return '';
  }

  /**
   * 打开个人修改密码弹窗。
   */
  function openChangePasswordDialog() {
    setUserMenuOpen(false);
    refs.currentPasswordInput.value = '';
    refs.newPasswordInput.value = '';
    refs.confirmPasswordInput.value = '';
    showFormError(refs.changePasswordError, '');
    refs.changePasswordDialog.classList.add('open');
    refs.changePasswordDialog.setAttribute('aria-hidden', 'false');
    refs.currentPasswordInput.focus();
  }

  /**
   * 关闭个人修改密码弹窗。
   */
  function closeChangePasswordDialog() {
    refs.changePasswordDialog.classList.remove('open');
    refs.changePasswordDialog.setAttribute('aria-hidden', 'true');
  }

  /**
   * 提交个人修改密码表单。
   *
   * @param {SubmitEvent} event 表单提交事件
   */
  async function handleChangePasswordSubmit(event) {
    event.preventDefault();
    const currentPassword = refs.currentPasswordInput.value;
    const newPassword = refs.newPasswordInput.value;
    const confirmPassword = refs.confirmPasswordInput.value;
    const validationError = validateNewPasswordInputs(newPassword, confirmPassword);
    if (validationError) {
      showFormError(refs.changePasswordError, validationError);
      return;
    }
    showFormError(refs.changePasswordError, '');
    refs.submitChangePasswordBtn.disabled = true;
    try {
      const updated = await api('/api/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword })
      });
      authState.currentUser = updated;
      closeChangePasswordDialog();
      authState.callbacks.onNotify(MESSAGE_PASSWORD_CHANGE_SUCCESS, NOTICE_SUCCESS);
    } catch (error) {
      showFormError(refs.changePasswordError, error.message);
    } finally {
      refs.submitChangePasswordBtn.disabled = false;
    }
  }

  /**
   * 退出当前登录会话并返回登录页。
   *
   * @param {boolean} requireConfirm 是否弹出确认框
   */
  async function logout(requireConfirm = true) {
    setUserMenuOpen(false);
    if (requireConfirm) {
      const confirmed = await global.jarPatchDialogs.confirm({
        title: MESSAGE_CONFIRM_LOGOUT_TITLE,
        message: MESSAGE_CONFIRM_LOGOUT_BODY,
        tone: global.jarPatchDialogs.TONE_WARN,
        actions: [
          { value: false, label: '继续留在这里', variant: global.jarPatchDialogs.VARIANT_SECONDARY },
          { value: true, label: '确认退出登录', variant: global.jarPatchDialogs.VARIANT_PRIMARY }
        ]
      });
      if (!confirmed) {
        return;
      }
    }
    try {
      if (global.jarPatchApiClient.getSessionToken()) {
        await api('/api/auth/logout', { method: 'POST', skipUnauthorizedCallback: true });
      }
    } catch {
      // 会话已过期或后端已吊销时仍清理本地状态，保证用户总能安全退出。
    }
    storeSessionToken('', false);
    authState.callbacks.onLoggedOut();
    await showLoginScreen();
    if (requireConfirm) {
      authState.callbacks.onNotify(MESSAGE_LOGOUT_SUCCESS, NOTICE_INFO);
    }
  }

  /**
   * 打开管理员专属的用户管理弹窗并加载最新用户列表。
   */
  async function openUserManagement() {
    setUserMenuOpen(false);
    if (!isAdmin()) {
      return;
    }
    hideUserSubPanels();
    refs.userManagementDialog.classList.add('open');
    refs.userManagementDialog.setAttribute('aria-hidden', 'false');
    await loadUserList();
  }

  /**
   * 关闭用户管理弹窗及其子面板。
   */
  function closeUserManagement() {
    hideUserSubPanels();
    refs.userManagementDialog.classList.remove('open');
    refs.userManagementDialog.setAttribute('aria-hidden', 'true');
  }

  /**
   * 关闭全部用户管理与个人改密弹窗。
   */
  function closeAllUserModals() {
    setUserMenuOpen(false);
    closeChangePasswordDialog();
    closeUserManagement();
  }

  /**
   * 隐藏新增/编辑用户表单与重置密码子面板。
   */
  function hideUserSubPanels() {
    authState.editingUserId = null;
    authState.resetPasswordUserId = null;
    refs.userFormPanel.hidden = true;
    refs.resetPasswordPanel.hidden = true;
    showFormError(refs.userFormError, '');
    showFormError(refs.resetPasswordError, '');
  }

  /**
   * 按筛选条件加载用户管理列表。
   */
  async function loadUserList() {
    const params = new URLSearchParams();
    const keyword = refs.userSearchInput.value.trim();
    const role = refs.userRoleFilter.value;
    const status = refs.userStatusFilter.value;
    if (keyword) {
      params.set('keyword', keyword);
    }
    if (role) {
      params.set('role', role);
    }
    if (status) {
      params.set('status', status);
    }
    const query = params.toString();
    try {
      const users = await api(`/api/users${query ? `?${query}` : ''}`);
      authState.users = Array.isArray(users) ? users : [];
      renderUserTable();
    } catch (error) {
      authState.callbacks.onNotify(`读取用户列表失败：${error.message}`, NOTICE_ERROR);
    }
  }

  /**
   * 渲染用户管理表格行与行内操作按钮。
   */
  function renderUserTable() {
    refs.userCountSummary.textContent = `共 ${authState.users.length} 个账号`;
    refs.userTableBody.replaceChildren();
    if (!authState.users.length) {
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 5;
      cell.className = 'user-empty-cell';
      cell.textContent = MESSAGE_USER_LIST_EMPTY;
      row.appendChild(cell);
      refs.userTableBody.appendChild(row);
      return;
    }

    const selfId = authState.currentUser ? authState.currentUser.id : '';
    for (const user of authState.users) {
      const isSelf = user.id === selfId;
      const isUserAdmin = user.role === ROLE_ADMIN;
      const isEnabled = user.status === STATUS_ENABLED;
      const tr = document.createElement('tr');

      const accountTd = document.createElement('td');
      accountTd.innerHTML = `
        <div class="user-cell-account">
          <span class="user-mini-avatar ${isUserAdmin ? 'avatar-admin' : ''}">${escapeText(getAvatarInitial(user))}</span>
          <div>
            <strong>${escapeText(user.displayName)}${isSelf ? ' <em class="self-tag">当前</em>' : ''}</strong>
            <span>@${escapeText(user.username)}${user.mustChangePassword ? ' · 待改密' : ''}</span>
          </div>
        </div>`;

      const roleTd = document.createElement('td');
      roleTd.innerHTML = `<span class="user-role-tag ${isUserAdmin ? 'role-admin' : 'role-user'}">${escapeText(getRoleLabel(user.role))}</span>`;

      const statusTd = document.createElement('td');
      statusTd.innerHTML = `<span class="user-status-pill ${isEnabled ? 'status-enabled' : 'status-disabled'}"><i aria-hidden="true"></i>${escapeText(getStatusLabel(user.status))}</span>`;

      const loginTd = document.createElement('td');
      loginTd.className = 'user-time-cell';
      loginTd.textContent = user.lastLoginAt || LABEL_NEVER_LOGGED_IN;

      const actionsTd = document.createElement('td');
      actionsTd.className = 'user-actions-cell';
      actionsTd.appendChild(createRowButton('编辑', 'mini-text-btn', () => openEditUserForm(user)));
      actionsTd.appendChild(createRowButton('重置密码', 'mini-text-btn', () => openResetPasswordForm(user)));
      if (!isSelf) {
        actionsTd.appendChild(createRowButton(
          isEnabled ? '禁用' : '启用',
          isEnabled ? 'mini-text-btn user-btn-warn' : 'mini-text-btn user-btn-success',
          () => toggleUserStatus(user)
        ));
        actionsTd.appendChild(createRowButton('删除', 'mini-text-btn user-btn-danger', () => deleteUserAccount(user)));
      }

      tr.appendChild(accountTd);
      tr.appendChild(roleTd);
      tr.appendChild(statusTd);
      tr.appendChild(loginTd);
      tr.appendChild(actionsTd);
      refs.userTableBody.appendChild(tr);
    }
  }

  /**
   * 创建用户表格行内动作按钮。
   *
   * @param {string} label     按钮文本
   * @param {string} className 样式类
   * @param {Function} onClick 点击回调
   * @returns {HTMLButtonElement} 按钮元素
   */
  function createRowButton(label, className, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    button.textContent = label;
    button.addEventListener('click', onClick);
    return button;
  }

  /**
   * 打开新增用户子面板。
   */
  function openCreateUserForm() {
    hideUserSubPanels();
    authState.editingUserId = null;
    refs.userFormTitle.textContent = TITLE_USER_FORM_CREATE;
    refs.userFormUsername.value = '';
    refs.userFormUsername.disabled = false;
    refs.userFormDisplayName.value = '';
    refs.userFormRole.value = ROLE_USER;
    refs.userFormRole.disabled = false;
    refs.userFormStatus.value = STATUS_ENABLED;
    refs.userFormStatusField.hidden = true;
    refs.userFormPasswordField.hidden = false;
    refs.userFormPassword.value = '';
    refs.userFormMustChangeField.hidden = false;
    refs.userFormMustChange.checked = true;
    refs.userFormPanel.hidden = false;
    refs.userFormUsername.focus();
  }

  /**
   * 打开编辑用户资料与角色子面板。
   *
   * @param {object} user 待编辑用户
   */
  function openEditUserForm(user) {
    hideUserSubPanels();
    const isSelf = Boolean(authState.currentUser && user.id === authState.currentUser.id);
    authState.editingUserId = user.id;
    refs.userFormTitle.textContent = `${TITLE_USER_FORM_EDIT}（@${user.username}）`;
    refs.userFormUsername.value = user.username;
    refs.userFormUsername.disabled = true;
    refs.userFormDisplayName.value = user.displayName;
    refs.userFormRole.value = user.role;
    refs.userFormRole.disabled = isSelf;
    refs.userFormStatus.value = user.status;
    refs.userFormStatus.disabled = isSelf;
    refs.userFormStatusField.hidden = false;
    refs.userFormPasswordField.hidden = true;
    refs.userFormMustChangeField.hidden = true;
    refs.userFormPanel.hidden = false;
    refs.userFormDisplayName.focus();
  }

  /**
   * 保存新增或编辑的用户信息。
   *
   * @param {SubmitEvent} event 提交事件
   */
  async function handleUserFormSubmit(event) {
    event.preventDefault();
    showFormError(refs.userFormError, '');
    refs.saveUserFormBtn.disabled = true;
    try {
      if (!authState.editingUserId) {
        const created = await api('/api/users', {
          method: 'POST',
          body: JSON.stringify({
            username: refs.userFormUsername.value.trim(),
            displayName: refs.userFormDisplayName.value.trim(),
            role: refs.userFormRole.value,
            password: refs.userFormPassword.value,
            mustChangePassword: refs.userFormMustChange.checked
          })
        });
        hideUserSubPanels();
        await loadUserList();
        authState.callbacks.onNotify(`${MESSAGE_USER_CREATED}：${created.username}`, NOTICE_SUCCESS);
      } else {
        const updated = await api(`/api/users/${encodeURIComponent(authState.editingUserId)}`, {
          method: 'PUT',
          body: JSON.stringify({
            displayName: refs.userFormDisplayName.value.trim(),
            role: refs.userFormRole.value,
            status: refs.userFormStatus.value
          })
        });
        if (authState.currentUser && updated.id === authState.currentUser.id) {
          authState.currentUser = updated;
          syncUserMenu(updated);
          authState.callbacks.onAuthenticated(updated);
        }
        hideUserSubPanels();
        await loadUserList();
        authState.callbacks.onNotify(`${MESSAGE_USER_UPDATED}：${updated.username}`, NOTICE_SUCCESS);
      }
    } catch (error) {
      showFormError(refs.userFormError, error.message);
    } finally {
      refs.saveUserFormBtn.disabled = false;
    }
  }

  /**
   * 打开管理员重置密码子面板。
   *
   * @param {object} user 目标用户
   */
  function openResetPasswordForm(user) {
    hideUserSubPanels();
    authState.resetPasswordUserId = user.id;
    refs.resetPasswordTarget.textContent = `正在重置账号「${user.displayName} (@${user.username})」的密码；重置后该用户所有活跃登录将立即失效。`;
    refs.resetPasswordInput.value = '';
    refs.resetPasswordConfirmInput.value = '';
    refs.resetPasswordPanel.hidden = false;
    refs.resetPasswordInput.focus();
  }

  /**
   * 提交管理员重置密码请求。
   *
   * @param {SubmitEvent} event 提交事件
   */
  async function handleResetPasswordSubmit(event) {
    event.preventDefault();
    const newPassword = refs.resetPasswordInput.value;
    const confirmPassword = refs.resetPasswordConfirmInput.value;
    const validationError = validateNewPasswordInputs(newPassword, confirmPassword);
    if (validationError) {
      showFormError(refs.resetPasswordError, validationError);
      return;
    }
    showFormError(refs.resetPasswordError, '');
    refs.confirmResetPasswordBtn.disabled = true;
    try {
      await api(`/api/users/${encodeURIComponent(authState.resetPasswordUserId)}/reset-password`, {
        method: 'POST',
        body: JSON.stringify({ newPassword })
      });
      hideUserSubPanels();
      await loadUserList();
      authState.callbacks.onNotify(MESSAGE_USER_RESET_PASSWORD_SUCCESS, NOTICE_SUCCESS);
    } catch (error) {
      showFormError(refs.resetPasswordError, error.message);
    } finally {
      refs.confirmResetPasswordBtn.disabled = false;
    }
  }

  /**
   * 快速切换目标用户的启用/禁用状态。
   *
   * @param {object} user 目标用户
   */
  async function toggleUserStatus(user) {
    const willDisable = user.status === STATUS_ENABLED;
    if (willDisable) {
      const confirmed = await global.jarPatchDialogs.confirm({
        title: MESSAGE_CONFIRM_DISABLE_USER_TITLE,
        message: `禁用后，账号「${user.displayName} (@${user.username})」将立即被强制下线且无法继续登录。`,
        tone: global.jarPatchDialogs.TONE_WARN,
        actions: [
          { value: false, label: '取消', variant: global.jarPatchDialogs.VARIANT_SECONDARY },
          { value: true, label: '确认禁用', variant: global.jarPatchDialogs.VARIANT_DANGER }
        ]
      });
      if (!confirmed) {
        return;
      }
    }
    try {
      await api(`/api/users/${encodeURIComponent(user.id)}`, {
        method: 'PUT',
        body: JSON.stringify({
          displayName: user.displayName,
          role: user.role,
          status: willDisable ? STATUS_DISABLED : STATUS_ENABLED
        })
      });
      await loadUserList();
      authState.callbacks.onNotify(willDisable ? MESSAGE_USER_DISABLED : MESSAGE_USER_ENABLED, NOTICE_SUCCESS);
    } catch (error) {
      authState.callbacks.onNotify(`更新用户状态失败：${error.message}`, NOTICE_ERROR);
    }
  }

  /**
   * 删除目标用户账号。
   *
   * @param {object} user 目标用户
   */
  async function deleteUserAccount(user) {
    const confirmed = await global.jarPatchDialogs.confirm({
      title: MESSAGE_CONFIRM_DELETE_USER_TITLE,
      message: '删除后该账号及其登录会话将被永久移除，既有项目历史中的操作人审计名仍会保留。',
      tone: global.jarPatchDialogs.TONE_DANGER,
      details: [
        { label: '用户名', value: user.username },
        { label: '显示名称', value: user.displayName },
        { label: '角色', value: getRoleLabel(user.role) }
      ],
      actions: [
        { value: false, label: '取消', variant: global.jarPatchDialogs.VARIANT_SECONDARY },
        { value: true, label: '确认删除用户', variant: global.jarPatchDialogs.VARIANT_DANGER }
      ]
    });
    if (!confirmed) {
      return;
    }
    try {
      await api(`/api/users/${encodeURIComponent(user.id)}`, { method: 'DELETE' });
      await loadUserList();
      authState.callbacks.onNotify(`${MESSAGE_USER_DELETED}：${user.username}`, NOTICE_SUCCESS);
    } catch (error) {
      authState.callbacks.onNotify(`删除用户失败：${error.message}`, NOTICE_ERROR);
    }
  }

  /**
   * 转义 HTML 特殊字符，防止用户名或显示名注入标签。
   *
   * @param {string} value 原始文本
   * @returns {string} 安全文本
   */
  function escapeText(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  /**
   * 绑定全部登录、用户菜单和用户管理 DOM 事件。
   */
  function bindEvents() {
    refs.loginForm.addEventListener('submit', handleLoginSubmit);
    refs.forceChangeForm.addEventListener('submit', handleForceChangeSubmit);
    refs.forceLogoutBtn.addEventListener('click', () => logout(false));
    refs.toggleLoginPwdBtn.addEventListener('click', () => {
      const showing = refs.loginPassword.type === 'text';
      refs.loginPassword.type = showing ? 'password' : 'text';
      refs.toggleLoginPwdBtn.setAttribute('aria-pressed', String(!showing));
    });
    refs.fillDefaultAdminBtn.addEventListener('click', () => {
      if (!authState.bootstrap || !authState.bootstrap.defaultAdminHintVisible) {
        return;
      }
      refs.loginUsername.value = authState.bootstrap.defaultAdminUsername || '';
      refs.loginPassword.value = authState.bootstrap.defaultAdminInitialPassword || '';
      refs.loginSubmitBtn.focus();
    });
    refs.authThemeBtn.addEventListener('click', () => {
      const mainThemeBtn = document.getElementById('themeToggleBtn');
      if (mainThemeBtn) {
        mainThemeBtn.click();
      }
    });

    refs.userMenuBtn.addEventListener('click', (event) => {
      event.stopPropagation();
      setUserMenuOpen(refs.userMenuPopover.hidden);
    });
    document.addEventListener('click', (event) => {
      if (!refs.userMenuPopover.hidden && !refs.userMenu.contains(event.target)) {
        setUserMenuOpen(false);
      }
    });
    refs.openUserManagementBtn.addEventListener('click', openUserManagement);
    refs.openChangePasswordBtn.addEventListener('click', openChangePasswordDialog);
    refs.logoutBtn.addEventListener('click', () => logout(true));

    refs.changePasswordForm.addEventListener('submit', handleChangePasswordSubmit);
    refs.closeChangePasswordBtn.addEventListener('click', closeChangePasswordDialog);
    refs.changePasswordDialog.addEventListener('click', (event) => {
      if (event.target === refs.changePasswordDialog) {
        closeChangePasswordDialog();
      }
    });

    refs.closeUserManagementBtn.addEventListener('click', closeUserManagement);
    refs.userManagementDialog.addEventListener('click', (event) => {
      if (event.target === refs.userManagementDialog) {
        closeUserManagement();
      }
    });
    refs.userSearchInput.addEventListener('input', () => {
      void loadUserList();
    });
    refs.userRoleFilter.addEventListener('change', () => {
      void loadUserList();
    });
    refs.userStatusFilter.addEventListener('change', () => {
      void loadUserList();
    });
    refs.openCreateUserBtn.addEventListener('click', openCreateUserForm);
    refs.cancelUserFormBtn.addEventListener('click', hideUserSubPanels);
    refs.userForm.addEventListener('submit', handleUserFormSubmit);
    refs.cancelResetPasswordBtn.addEventListener('click', hideUserSubPanels);
    refs.resetPasswordForm.addEventListener('submit', handleResetPasswordSubmit);
  }

  /**
   * 初始化认证模块：绑定事件、注册全局 401 失效处理并尝试恢复既有会话。
   *
   * @param {object} callbacks 回调集合（onAuthenticated, onLoggedOut, onNotify）
   * @returns {Promise<object|null>} 已认证用户或 null
   */
  async function initialize(callbacks = {}) {
    refs = getRefs();
    authState.callbacks = {
      onAuthenticated: callbacks.onAuthenticated || (() => {}),
      onLoggedOut: callbacks.onLoggedOut || (() => {}),
      onNotify: callbacks.onNotify || (() => {})
    };
    bindEvents();

    // 任意业务请求返回 HTTP 401 时自动踢回登录页，避免工作台停留在失效态。
    global.jarPatchApiClient.setUnauthorizedHandler((message) => {
      storeSessionToken('', false);
      authState.callbacks.onLoggedOut();
      void showLoginScreen(message);
    });

    const savedToken = loadStoredSessionToken();
    if (savedToken) {
      global.jarPatchApiClient.setSessionToken(savedToken);
      try {
        const user = await api('/api/auth/me', { skipUnauthorizedCallback: true });
        if (user && user.mustChangePassword) {
          showForceChangePasswordScreen(user);
          return null;
        }
        enterWorkbench(user);
        return user;
      } catch {
        storeSessionToken('', false);
      }
    }

    await showLoginScreen();
    return null;
  }

  /**
   * 响应 Esc 快捷键关闭用户菜单或用户相关弹窗。
   *
   * @returns {boolean} 消费事件返回 true
   */
  function closeTopmostAuthOverlay() {
    if (!refs) {
      return false;
    }
    if (!refs.userMenuPopover.hidden) {
      setUserMenuOpen(false);
      refs.userMenuBtn.focus();
      return true;
    }
    if (refs.changePasswordDialog.classList.contains('open')) {
      closeChangePasswordDialog();
      return true;
    }
    if (refs.userManagementDialog.classList.contains('open')) {
      if (!refs.userFormPanel.hidden || !refs.resetPasswordPanel.hidden) {
        hideUserSubPanels();
        return true;
      }
      closeUserManagement();
      return true;
    }
    return false;
  }

  /**
   * 判断当前用户是否为系统管理员。
   *
   * @returns {boolean} 管理员返回 true
   */
  function isAdmin() {
    return Boolean(authState.currentUser && authState.currentUser.role === ROLE_ADMIN);
  }

  /**
   * 返回当前已认证用户对象。
   *
   * @returns {object|null} 当前用户
   */
  function getCurrentUser() {
    return authState.currentUser;
  }

  global.jarPatchAuth = Object.freeze({
    initialize,
    getCurrentUser,
    isAdmin,
    openUserManagement,
    openChangePasswordDialog,
    closeChangePasswordDialog,
    closeUserManagement,
    logout,
    closeTopmostAuthOverlay
  });
}(window));
