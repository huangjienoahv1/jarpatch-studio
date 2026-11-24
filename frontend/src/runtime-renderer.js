(() => {
  const view = (name) => document.getElementById('runtime' + name);
  const root = view('Dialog');
  const tabs = [...root.querySelectorAll('[data-runtime-tab]')];
  const panes = [...root.querySelectorAll('[data-runtime-pane]')];
  const integrationsWithManagedTarget = [...root.querySelectorAll('[data-runtime-managed-only]')];
  const jvmButtons = [...root.querySelectorAll('[data-runtime-diagnose]')];
  const sourcePanel = view('SourcePanel');
  const sourcePane = sourcePanel.parentElement;
  const bridge = window.jarPatch;
  const tasks = new Map();
  const updates = new Map();
  const logCharacterLimit = 220000;
  const traceHint = '采集最多 20 秒或 3 次，可切换到请求测试触发方法。';
  const windowsTraceHint = '采集最多 20 秒或 3 次，可从目标应用的原有入口触发方法。';
  const methodAccessLabels = { package: '包可见', unknown: '访问级别未知' };
  const DEFAULT_SSH_PORT = 22;
  const AUTH_METHOD = {
    PASSWORD: 'password',
    OPENSSH: 'openssh'
  };
  const PROVIDER_TYPE = {
    DOCKER: 'docker',
    KUBERNETES: 'kubernetes',
    LOCAL: 'local',
    HOST: 'host',
    WINDOWS: 'windows',
    EXISTING: 'existing'
  };
  const LOCAL_HOST = '本机';
  const LOCAL_JVM = 'local-jvm';
  const DIAGNOSTIC_TABS = new Set(['classes', 'jvm', 'advanced']);
  const HOST_OPTION = {
    EMPTY: '',
    CUSTOM: '__custom__',
    NEW: '__new__'
  };
  const CONNECTION_COPY = {
    TOGGLE_OPEN: '连接配置',
    TOGGLE_CLOSE: '收起配置',
    HOST_PLACEHOLDER_PASSWORD: '服务器 IP 或主机名',
    HOST_PLACEHOLDER_OPENSSH: 'SSH 别名或 user@host',
    PASSWORD_SAVED_HINT: '已保存密码，留空直接连接',
    PASSWORD_EMPTY_HINT: '输入登录密码',
    SELECT_HOST_PROMPT: '请选择服务器',
    EMPTY_HOST_PROMPT: '请先配置服务器',
    ADD_HOST_OPTION: '＋ 新增 / 临时主机…',
    UNSAVED_SUFFIX: '（未保存）',
    OPENSSH_PREFIX: 'OpenSSH · ',
    LOAD_CONTAINERS: '加载容器',
    LOAD_PODS: '加载 Pod',
    CONTAINER_LABEL: '容器',
    POD_CONTAINER_LABEL: 'Pod / 容器',
    BADGE_K8S_PREFIX: 'K8s · ',
    BADGE_K8S: 'Kubernetes',
    BADGE_OPENSSH: 'OpenSSH',
    BADGE_OFFLINE_PACKAGE: '离线包',
    DEFAULT_PACKAGE_LABEL: '默认下载并校验官方完整包',
    STATUS_READY_SAVED_HOST: '已选中常用服务器，点击加载即可开始诊断',
    STATUS_NEED_HOST_CONFIG: '请在连接配置中填写服务器信息后加载目标',
    STATUS_RELOAD_OR_SELECT: '选择容器或重新加载清单',
    STATUS_HOST_SAVED: 'SSH 主机已保存，可直接点击加载',
    STATUS_HOST_REMOVED: '已移除保存的 SSH 主机',
    ERROR_HOST_REQUIRED: '请先在连接配置中填写 SSH 主机地址',
    ERROR_USER_REQUIRED: '请先在连接配置中填写 SSH 账号'
  };
  let sequence = 0;
  let host;
  let process;
  let processes = [];
  let packageName;
  let attached = false;
  let canRelease = false;
  let closing = false;
  let sourceView;
  let sourceDraft;
  let choosingSourceArchive = false;
  let methodChoices = [];
  let logBuffer = '';
  let logFrame = 0;
  let analysis;
  let integrations;
  let loadedCluster;
  let loadedLogin;
  let loadedProvider;
  let existingTarget;
  let savedHosts = [];
  let managingSshHosts = false;
  let connectionExpanded = false;
  let lastHostOptionsKey = '';

  /**
   * 展开或收起运行时工作台顶部的连接配置面板。
   * 入口：右上角配置按钮、打开工作台默认态判断、下拉框「新增主机」及 Arthas 连接成功回调；
   * 结果：更新 `#runtimeConnectionBox` 显隐与开关按钮的文案及 `aria-expanded` 状态。
   *
   * @param {boolean} expanded 是否展开配置面板
   */
  function setConnectionBoxExpanded(expanded) {
    connectionExpanded = Boolean(expanded);
    view('ConnectionBox').hidden = !connectionExpanded;
    const toggleBtn = view('ConnToggleBtn');
    toggleBtn.setAttribute('aria-expanded', String(connectionExpanded));
    view('ConnToggleLabel').textContent = connectionExpanded
      ? CONNECTION_COPY.TOGGLE_CLOSE
      : CONNECTION_COPY.TOGGLE_OPEN;
  }

  /**
   * 格式化已保存的 SSH 主机展示文案，自动为 IPv6 地址补充方括号。
   *
   * @param {{ host: string, username: string, port: number }} entry 已保存主机条目
   * @returns {string} `user@host:port` 展示字符串
   */
  function formatSavedHostLabel(entry) {
    const address = entry.host.includes(':') ? `[${entry.host}]` : entry.host;
    return `${entry.username}@${address}:${entry.port}`;
  }

  /**
   * 格式化未保存的临时主机或 OpenSSH 目标文案，用于在单行工作条服务器选择器中如实展示当前配置。
   *
   * @param {{ method: string, address: string, username?: string, port?: string|number }} login 当前登录配置
   * @returns {string} 可读目标摘要
   */
  function formatCustomHostLabel(login) {
    if (login.method === AUTH_METHOD.OPENSSH) {
      return `${CONNECTION_COPY.OPENSSH_PREFIX}${login.address}`;
    }
    const address = login.address.includes(':') && !login.address.startsWith('[')
      ? `[${login.address}]`
      : login.address;
    const userPrefix = login.username ? `${login.username}@` : '';
    const portSuffix = login.port ? `:${login.port}` : '';
    return `${userPrefix}${address}${portSuffix}${CONNECTION_COPY.UNSAVED_SUFFIX}`;
  }

  /**
   * 将一条已保存的 SSH 主机记录回填到连接配置表单中，并切换为账号密码认证模式。
   * 仅更新本地表单值，不主动触发远程 SSH 连接。
   *
   * @param {{ host: string, username: string, port: number }} entry 已保存主机条目
   */
  function applySavedHost(entry) {
    view('SshAuth').value = AUTH_METHOD.PASSWORD;
    view('Host').value = entry.host;
    view('SshUser').value = entry.username;
    view('SshPort').value = entry.port ?? DEFAULT_SSH_PORT;
    view('SshPassword').value = '';
  }

  /**
   * 重置主机配置草稿为空白账号密码状态，用于新增临时/新保存主机或移除当前主机后清理残留。
   */
  function resetNewHostDraft() {
    view('SshAuth').value = AUTH_METHOD.PASSWORD;
    view('Host').value = '';
    view('SshUser').value = '';
    view('SshPort').value = DEFAULT_SSH_PORT;
    view('SshPassword').value = '';
  }

  /**
   * 同步单行工作条中的服务器选择器（`#runtimeSshHosts`）。
   * 按当前表单状态构建选项列表：若当前配置未匹配已保存主机但已填地址，则在顶部追加只读当前目标项；
   * 底部始终附带「新增 / 临时主机」入口。仅在选项集合变化时重建 DOM，避免输入时闪烁。
   *
   * @param {{ method: string, address: string, username?: string, port?: string|number }} login 当前表单登录信息
   * @param {{ id: string } | undefined} savedHost 命中的已保存主机条目
   */
  function syncServerSelector(login, savedHost) {
    const select = view('SshHosts');
    const items = [];
    let selectedValue = HOST_OPTION.EMPTY;

    // 1. 决定顶部临时项或占位提示项，确保单行工作条始终准确反映即将连接的目标
    if (savedHost) {
      selectedValue = savedHost.id;
    } else if (login.address) {
      items.push({ value: HOST_OPTION.CUSTOM, label: formatCustomHostLabel(login) });
      selectedValue = HOST_OPTION.CUSTOM;
    } else {
      items.push({
        value: HOST_OPTION.EMPTY,
        label: savedHosts.length ? CONNECTION_COPY.SELECT_HOST_PROMPT : CONNECTION_COPY.EMPTY_HOST_PROMPT
      });
    }

    // 2. 追加全部已保存主机，方便高频一键切换
    for (const entry of savedHosts) {
      items.push({ value: entry.id, label: formatSavedHostLabel(entry) });
    }

    // 3. 底部固定追加新增/临时主机入口
    items.push({ value: HOST_OPTION.NEW, label: CONNECTION_COPY.ADD_HOST_OPTION });

    const optionsKey = items.map((item) => `${item.value}\u0000${item.label}`).join('\u0001');
    if (optionsKey !== lastHostOptionsKey) {
      lastHostOptionsKey = optionsKey;
      select.replaceChildren(...items.map((item) => new Option(item.label, item.value)));
    }
    select.value = selectedValue;
  }

  /**
   * 同步单行快速工作条的动态文案、标题栏非默认配置微徽标及 Arthas 挂载状态视觉反馈。
   *
   * @param {{ method: string }} login 当前表单登录配置
   */
  function syncConnectionBarState(login) {
    const provider = view('Provider').value;
    const isK8s = provider === PROVIDER_TYPE.KUBERNETES;
    const local = provider === PROVIDER_TYPE.LOCAL;
    const hostJvm = provider === PROVIDER_TYPE.HOST;
    const windowsJvm = provider === PROVIDER_TYPE.WINDOWS;
    const existing = provider === PROVIDER_TYPE.EXISTING;
    const needsSsh = !local && (!existing || view('ExistingTransport').value === 'ssh');
    const processOnly = local || hostJvm || windowsJvm || existing;
    const isOpenSsh = needsSsh && login.method === AUTH_METHOD.OPENSSH;
    const hasOfflinePackage = !existing && Boolean(packageName);

    // 同步单行工作条在 Docker 与 Kubernetes 模式下的按钮及字段标签
    view('Connect').textContent = processOnly ? '加载 Java 进程'
      : isK8s ? CONNECTION_COPY.LOAD_PODS : CONNECTION_COPY.LOAD_CONTAINERS;
    view('QuickBar').classList.toggle('is-local', local);
    view('QuickBar').classList.toggle('is-existing', existing);
    view('QuickBar').classList.toggle('is-existing-local', existing && !needsSsh);
    view('QuickTarget').classList.toggle('is-process-only', processOnly);
    view('Connect').hidden = existing;
    view('QuickServer').hidden = existing && !needsSsh;
    view('SshHostsField').hidden = !needsSsh;
    view('SshConfig').hidden = !needsSsh;
    view('ContainerField').hidden = processOnly;
    view('ProcessField').hidden = existing;
    view('ExistingTarget').hidden = !existing;
    view('ExistingTarget').textContent = 'HTTP ' + (view('ExistingPort').value || '—')
      + (existingTarget ? ' · PID ' + existingTarget.pid : '');
    view('ExistingConfig').hidden = !existing;
    view('PackageConfig').hidden = existing;
    view('Attach').textContent = existing ? '连接已有 Arthas' : '连接 Arthas';
    view('DetachLabel').textContent = existing ? '断开连接' : '释放 Arthas';
    for (const name of ['EditSource', 'CompareClass']) view(name).hidden = existing || windowsJvm;
    for (const name of ['TraceMethod', 'Trace', 'CancelTrace', 'TraceStatus']) {
      view(name).hidden = existing;
    }
    for (const button of tabs) {
      button.hidden = (existing || windowsJvm) && !DIAGNOSTIC_TABS.has(button.dataset.runtimeTab);
    }
    for (const integration of integrationsWithManagedTarget) integration.hidden = existing || windowsJvm;
    view('Report').hidden = existing || windowsJvm;
    view('HostLogSource').hidden = !hostJvm;
    view('LocalLogHint').hidden = !local;
    view('LogActions').hidden = local;
    view('Logs').hidden = local;
    const fileLog = view('LogSourceKind').value === 'file';
    view('LogSourceLabel').textContent = fileLog ? '日志文件' : '服务单元';
    view('LogSourceValue').placeholder = fileLog ? '/var/log/app/app.log' : 'app.service';
    view('ContainerLabel').textContent = isK8s
      ? CONNECTION_COPY.POD_CONTAINER_LABEL
      : CONNECTION_COPY.CONTAINER_LABEL;

    // 同步标题栏轻量配置徽标，收起配置面板时仍一眼可见非默认模式
    const kubeNamespace = view('KubeNamespace').value.trim();
    const providerBadge = view('BadgeProvider');
    providerBadge.hidden = !isK8s;
    providerBadge.textContent = kubeNamespace
      ? `${CONNECTION_COPY.BADGE_K8S_PREFIX}${kubeNamespace}`
      : CONNECTION_COPY.BADGE_K8S;

    const authBadge = view('BadgeAuth');
    authBadge.hidden = !isOpenSsh;
    authBadge.textContent = CONNECTION_COPY.BADGE_OPENSSH;

    const packageBadge = view('BadgePackage');
    packageBadge.hidden = !hasOfflinePackage;
    packageBadge.textContent = CONNECTION_COPY.BADGE_OFFLINE_PACKAGE;

    view('ConfigBadges').hidden = !isK8s && !isOpenSsh && !hasOfflinePackage;

    // 同步单行工作条与「释放 Arthas」按钮的在线绿灯 / 待清理警告灯样式
    const needsRetryCleanup = canRelease && !attached;
    view('QuickBar').classList.toggle('is-attached', attached);
    view('Detach').classList.toggle('is-attached', attached);
    view('Detach').classList.toggle('needs-retry', needsRetryCleanup);
  }

  function showError(error) {
    const field = view('Error');
    field.textContent = error?.message?.replace(/^Error invoking remote method '[^']+': Error:\s*/, '') || String(error);
    field.hidden = false;
    field.focus();
  }

  function clearError() {
    view('Error').hidden = true;
    view('Error').textContent = '';
  }

  function selectedTarget() {
    if (view('Provider').value === PROVIDER_TYPE.EXISTING) return existingTarget || {};
    return { host, container: view('Container').value, pid: process?.pid, start: process?.start };
  }

  function loginFields() {
    const method = view('SshAuth').value;
    const address = view('Host').value.trim();
    return method === AUTH_METHOD.PASSWORD
      ? { method, address, username: view('SshUser').value.trim(), port: view('SshPort').value }
      : { method, address };
  }

  function requireSshLogin() {
    const login = loginFields();
    if (!login.address) {
      setConnectionBoxExpanded(true);
      view('Host').focus();
      throw new Error(CONNECTION_COPY.ERROR_HOST_REQUIRED);
    }
    if (login.method === AUTH_METHOD.PASSWORD && !login.username) {
      setConnectionBoxExpanded(true);
      view('SshUser').focus();
      throw new Error(CONNECTION_COPY.ERROR_USER_REQUIRED);
    }
    return login;
  }

  function existingConnection() {
    const data = { action: 'connectExisting', connection: 'local', arthasPort: view('ExistingPort').value,
      arthasUser: view('ExistingUser').value.trim(), arthasPassword: view('ExistingPassword').value };
    if (view('ExistingTransport').value === 'ssh') {
      const login = requireSshLogin();
      Object.assign(data, { connection: login.method, host: login.address, username: login.username,
        port: login.port, password: view('SshPassword').value });
      view('SshPassword').value = '';
    }
    view('ExistingPassword').value = '';
    return data;
  }

  function savedHostFor(login) {
    if (login.method !== AUTH_METHOD.PASSWORD) return undefined;
    const address = login.address.replace(/^\[([a-f0-9:]+)\]$/i, '$1').toLowerCase();
    return savedHosts.find((entry) => entry.host === address && entry.username === login.username
      && entry.port === Number(login.port));
  }

  /**
   * 执行本地已保存 SSH 主机清单的读取、保存或移除，并在完成后刷新所有控件状态。
   *
   * @param {() => Promise<Array>} operation 返回最新已保存主机列表的异步操作
   */
  async function manageSshHosts(operation) {
    managingSshHosts = true;
    syncControls();
    try {
      savedHosts = await operation();
    } finally {
      managingSshHosts = false;
      syncControls();
    }
  }

  /**
   * 按当前会话、任务与表单状态统一刷新运行时工作台全部控件的可用性、显隐与联动文案。
   */
  function syncControls() {
    const clusterOptions = integrations?.cluster();
    const login = loginFields();
    const provider = view('Provider').value;
    const local = provider === PROVIDER_TYPE.LOCAL;
    const hostJvm = provider === PROVIDER_TYPE.HOST;
    const windowsJvm = provider === PROVIDER_TYPE.WINDOWS;
    const existing = provider === PROVIDER_TYPE.EXISTING;
    const isPassword = login.method === AUTH_METHOD.PASSWORD;
    const currentHost = Boolean(host) && loadedProvider === provider && (local || (
      JSON.stringify(loadedLogin) === JSON.stringify(login) && JSON.stringify(loadedCluster) === JSON.stringify(clusterOptions)));
    const hasContainer = currentHost && Boolean(view('Container').value);
    const hasProcess = hasContainer && Boolean(process);
    const active = tasks.size > 0 || choosingSourceArchive;
    const targeting = tasks.has('target');
    const logActive = tasks.has('logs');
    const classActive = tasks.has('classes') || tasks.get('advanced')?.type === 'sourcePatch' || choosingSourceArchive;
    const jvmActive = tasks.has('jvm');
    const requestActive = tasks.has('request');
    const lockTarget = closing || active || canRelease || managingSshHosts;
    const savedHost = savedHostFor(login);

    // 同步连接配置面板中的 SSH 表单项显隐与禁用态
    view('Host').disabled = lockTarget;
    view('Host').placeholder = isPassword
      ? CONNECTION_COPY.HOST_PLACEHOLDER_PASSWORD
      : CONNECTION_COPY.HOST_PLACEHOLDER_OPENSSH;
    view('PortField').hidden = !isPassword;
    view('PasswordFields').hidden = !isPassword;
    for (const name of ['SshAuth', 'SshUser', 'SshPort', 'SshPassword']) view(name).disabled = lockTarget;

    // 同步单行工作条的服务器选择器与主机保存/移除按钮
    syncServerSelector(login, savedHost);
    view('SshHosts').disabled = lockTarget;
    view('SshPassword').placeholder = savedHost
      ? CONNECTION_COPY.PASSWORD_SAVED_HINT
      : CONNECTION_COPY.PASSWORD_EMPTY_HINT;
    view('SaveSshHost').disabled = lockTarget || !login.address || !login.username || !login.port || !view('SshPassword').value;
    view('RemoveSshHost').disabled = lockTarget || !savedHost;

    // 同步单行工作条的目标加载与 Arthas 互斥操作按钮
    view('Connect').disabled = lockTarget;
    view('Container').disabled = lockTarget || !currentHost;
    view('Process').disabled = lockTarget || !hasContainer || processes.length === 0;
    view('Attach').hidden = canRelease;
    view('Attach').disabled = lockTarget || (!existing && !hasProcess);
    view('Detach').hidden = !canRelease;
    view('Detach').disabled = closing || active || !canRelease;
    view('Package').disabled = lockTarget;
    view('ResetPackage').disabled = lockTarget;
    for (const name of ['ExistingTransport', 'ExistingPort', 'ExistingUser', 'ExistingPassword']) view(name).disabled = lockTarget;
    view('Close').disabled = closing || managingSshHosts || choosingSourceArchive;
    const hasLogSource = !local && (!hostJvm || Boolean(view('LogSourceValue').value.trim()));
    for (const name of ['ReadLogs', 'FollowLogs']) {
      view(name).disabled = closing || targeting || !hasContainer || logActive || !hasLogSource;
    }
    for (const name of ['LogSourceKind', 'LogSourceValue']) view(name).disabled = closing || targeting || logActive;
    view('StopLogs').disabled = closing || !logActive;
    for (const name of ['Search', 'Methods', 'Source', 'Trace']) view(name).disabled = closing || !attached || classActive;
    for (const name of ['Class', 'Loader', 'Method']) view(name).disabled = closing || !attached || classActive;
    view('CancelTrace').disabled = closing || tasks.get('classes')?.type !== 'trace';
    jvmButtons.forEach((button) => { button.disabled = closing || !attached || jvmActive; });
    view('CancelJvm').disabled = closing || !jvmActive;
    view('RefreshPorts').disabled = closing || !hasProcess || windowsJvm || targeting || requestActive;
    view('Send').disabled = closing || !hasProcess || targeting || requestActive || tasks.has('suite') || !view('Ports').value;
    view('CancelRequest').disabled = closing || !requestActive;
    analysis?.sync({ closing, attached, hasProcess: hasProcess && !windowsJvm, targeting, classActive, requestActive,
      correlateEnabled: !local && !hostJvm });
    integrations?.sync({ closing, lockTarget, currentHost, hasProcess, targeting, attached, clusterOptions, classActive });
    const canPatch = !closing && attached && !classActive && !tasks.has('advanced') && !tasks.has('profile');
    view('EditSource').disabled = !canPatch || !integrations?.canApplyPatch();
    const patchHint = attached && !existing && !windowsJvm ? integrations.sourcePatchHint() : '';
    const patchAvailability = view('SourcePatchAvailability');
    patchAvailability.hidden = !patchHint;
    if (patchAvailability.textContent !== patchHint) patchAvailability.textContent = patchHint;
    view('ApplySourcePatch').disabled = !canPatch || !integrations?.canApplyPatch() || !sourceView?.isChanged();
    view('RestoreSourcePatch').hidden = !sourceDraft || !integrations?.canRestoreSource(sourceDraft);
    view('RestoreSourcePatch').disabled = !canPatch;
    syncConnectionBarState(login);
  }

  function invoke(scope, data, onUpdate) {
    if (tasks.has(scope)) throw new Error('此操作正在运行');
    const id = 'runtime-' + Date.now() + '-' + ++sequence;
    if (onUpdate) updates.set(id, onUpdate);
    const result = bridge.runRuntimeOperation(id, data).finally(() => {
      tasks.delete(scope);
      updates.delete(id);
      syncControls();
    });
    tasks.set(scope, { id, result, type: data.type });
    syncControls();
    return result;
  }

  async function cancel(scope) {
    const task = tasks.get(scope);
    if (task) await bridge.cancelRuntimeOperation(task.id);
  }

  bridge.onRuntimeUpdate(({ id, kind, value }) => {
    updates.get(id)?.(kind, value);
  });

  function bind(name, action) {
    view(name).addEventListener('click', async () => {
      clearError();
      try {
        await action();
      } catch (error) {
        if (!closing) showError(error);
      }
    });
  }

  function fillOptions(select, rows, label, value, emptyMessage, pickSingle = false) {
    select.replaceChildren(new Option(emptyMessage, ''));
    for (const row of rows) select.add(new Option(label(row), value(row)));
    if (pickSingle && rows.length === 1) select.selectedIndex = 1;
  }

  function syncSourceScroll() {
    const showSource = !sourcePane.hidden && !sourcePanel.hidden;
    const wasScrollable = root.classList.contains('runtime-source-open');
    root.classList.toggle('runtime-source-open', showSource);
    if (wasScrollable && !showSource) root.scrollTop = 0;
  }

  function setSourcePaneVisible(visible) {
    sourcePanel.hidden = !visible;
    view('ClassOutput').hidden = visible;
    syncSourceScroll();
  }

  function disposeSource() {
    sourceView?.dispose();
    sourceView = undefined;
    sourceDraft = undefined;
    view('SourceTools').hidden = true;
    view('SourcePatchStatus').textContent = '';
    setSourcePaneVisible(false);
  }

  /**
   * 离开已修改的在线反编译源码前，通过内置确认弹窗提醒用户避免误丢改动。
   *
   * @returns {Promise<boolean>} 允许继续时返回 true
   */
  async function confirmSourceDiscard() {
    if (!sourceDraft || !sourceView.isChanged() || sourceView.getValue() === sourceDraft.source) {
      return true;
    }
    return window.jarPatchDialogs.confirm({
      title: '放弃未应用的在线源码修改？',
      message: '离开当前在线反编译编辑器会丢弃本次未热替换的代码改动。',
      tone: window.jarPatchDialogs.TONE_WARN,
      actions: [
        { value: false, label: '继续编辑', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
        { value: true, label: '放弃修改并离开', variant: window.jarPatchDialogs.VARIANT_DANGER }
      ]
    });
  }

  function showSource(content, editable = false) {
    const editor = view('SourceView');
    view('ClassChoices').open = false;
    setSourcePaneVisible(true);
    try {
      sourceView = window.jarPatchEditor.createRuntimeJavaView(editor, content, editable ? syncControls : undefined);
    } catch (error) {
      disposeSource();
      throw error;
    }
    if (!sourcePane.hidden) sourcePanel.scrollIntoView({ block: 'start' });
  }

  async function editOnlineSource() {
    if (!(await confirmSourceDiscard())) return;
    disposeSource();
    const target = selectedTarget();
    const output = view('ClassOutput');
    output.textContent = '正在反编译当前 JVM 的 class…';
    try {
      const draft = await invoke('classes', {
        action: 'prepareSourcePatch', ...target, className: view('Class').value.trim(), loader: view('Loader').value.trim()
      });
      if (closing) return;
      sourceDraft = { ...draft, target };
      view('Loader').value = draft.loader;
      view('SourceTarget').textContent = draft.className + ' · ClassLoader ' + draft.loader + ' · PID ' + target.pid;
      view('SourceTools').hidden = false;
      showSource(draft.source, true);
    } catch (error) {
      output.textContent = '在线源码加载失败';
      throw error;
    } finally {
      syncControls();
    }
  }

  async function applyOnlineSource() {
    const draft = sourceDraft;
    const editor = sourceView;
    if (!draft || !editor || choosingSourceArchive) return;
    const source = editor.getValue();
    if (source === draft.source) throw new Error('请先修改源码');
    editor.setBusy(true);
    try {
      choosingSourceArchive = true;
      syncControls();
      let archive;
      try {
        archive = await bridge.openArchive();
      } finally {
        choosingSourceArchive = false;
      }
      if (!archive || closing || sourceDraft !== draft) return;
      await integrations.applySourcePatch({ ...draft.target, type: 'sourcePatch', className: draft.className,
        loader: draft.loader, expectedSha256: draft.sha256, archive, source });
    } finally {
      if (sourceView === editor) editor.setBusy(false);
      syncControls();
    }
  }

  function clearLog() {
    if (logFrame) cancelAnimationFrame(logFrame);
    logFrame = 0;
    logBuffer = '';
    view('Logs').textContent = '';
    view('LogStatus').textContent = '';
  }

  function clearOutputs() {
    disposeSource();
    view('Loader').value = '';
    clearClassChoices();
    for (const name of ['ClassOutput', 'JvmOutput', 'Response', 'RequestStatus']) view(name).textContent = '';
    view('TraceStatus').textContent = view('Provider').value === PROVIDER_TYPE.WINDOWS ? windowsTraceHint : traceHint;
  }

  async function refreshPorts() {
    const select = view('Ports');
    const previous = select.value;
    fillOptions(select, [], String, String, '正在查找端口…');
    try {
      const ports = await invoke('target', { action: 'ports', ...selectedTarget() });
      fillOptions(select, ports, String, String, ports.length ? '请选择' : '无监听端口', true);
      if (ports.includes(Number(previous))) select.value = previous;
    } catch (error) {
      fillOptions(select, [], String, String, '端口查询失败');
      throw error;
    } finally {
      syncControls();
    }
  }

  async function selectProcess() {
    process = processes.find((item) => item.pid === view('Process').value);
    clearOutputs();
    analysis.resetComparison();
    analysis.resetRequest();
    fillOptions(view('Ports'), [], String, String, '先选择 Java 进程');
    syncControls();
    if (process) {
      view('Status').textContent = ['已选择 PID ' + process.pid, process.name, process.executable, process.user].filter(Boolean).join(' · ');
      if (view('Provider').value !== PROVIDER_TYPE.WINDOWS) await refreshPorts();
    }
  }

  async function selectContainer() {
    clearLog();
    process = undefined;
    processes = [];
    fillOptions(view('Process'), [], String, String, '先选择容器');
    await selectProcess();
    if (!view('Container').value) {
      view('Status').textContent = '请选择运行中的容器';
      return;
    }
    view('Status').textContent = '正在查找 Java 进程…';
    try {
      processes = await invoke('target', { action: 'processes', ...selectedTarget() });
      fillOptions(view('Process'), processes, (p) => ['PID ' + p.pid, p.name || p.executable, p.user].filter(Boolean).join(' · '),
        (p) => p.pid, processes.length ? '请选择进程' : '未发现 Java 进程', true);
    } catch (error) {
      view('Status').textContent = 'Java 进程查询失败';
      throw error;
    }
    view('Status').textContent = processes.length ? '请选择 Java 进程' : '当前环境未发现可读的 Java 进程';
    await selectProcess();
  }

  function appendLog(text) {
    logBuffer = (logBuffer + text).slice(-logCharacterLimit);
    if (logFrame) return;
    logFrame = requestAnimationFrame(() => {
      logFrame = 0;
      const output = view('Logs');
      const followEnd = output.scrollHeight - output.scrollTop - output.clientHeight < 40;
      output.textContent = logBuffer;
      if (followEnd) output.scrollTop = output.scrollHeight;
    });
  }

  async function readLogs(follow) {
    clearLog();
    view('LogStatus').textContent = follow ? '正在追踪…' : '正在获取…';
    try {
      const result = await invoke('logs', {
        action: 'logs', ...selectedTarget(), tail: view('Tail').value, follow,
        logSource: { kind: view('LogSourceKind').value, value: view('LogSourceValue').value.trim() }
      }, (kind, chunk) => { if (kind === 'log') appendLog(chunk); });
      if (!follow) {
        appendLog(result || '当前来源没有可读的近期日志。');
        view('LogStatus').textContent = '近期日志';
      } else {
        view('LogStatus').textContent = result || '日志流已结束';
      }
    } catch (error) {
      if (error.message.includes('操作已取消')) view('LogStatus').textContent = '已停止追踪';
      else {
        view('LogStatus').textContent = '日志读取失败';
        throw error;
      }
    }
  }

  function clearClassChoices() {
    methodChoices = [];
    const matches = view('ClassMatches');
    matches.replaceChildren();
    matches.classList.remove('runtime-method-matches');
    view('ClassChoices').hidden = true;
    view('ClassChoicesTitle').textContent = '搜索结果';
    view('MethodFilters').hidden = true;
    view('MethodWarning').hidden = true;
  }

  function renderChoices(choices, select) {
    const container = view('ClassMatches');
    const buttons = document.createDocumentFragment();
    for (const choice of choices) {
      const button = document.createElement('button');
      button.className = 'tool-btn';
      button.type = 'button';
      button.textContent = choice.label;
      if (choice.title) button.title = choice.title;
      if (choice.tags) {
        button.classList.add('runtime-method-choice');
        const tags = document.createElement('span');
        tags.className = 'runtime-method-tags';
        for (const text of choice.tags) {
          const tag = document.createElement('span');
          tag.textContent = text;
          tags.appendChild(tag);
        }
        button.appendChild(tags);
      }
      button.addEventListener('click', async () => {
        if (!tasks.has('classes') && tasks.get('advanced')?.type !== 'sourcePatch' && !choosingSourceArchive
            && (await confirmSourceDiscard())) select(choice);
      });
      buttons.appendChild(button);
    }
    container.replaceChildren(buttons);
    container.scrollTop = 0;
  }

  function classMatches(rows) {
    const choices = rows.filter((row) => row.type === 'sc').flatMap((row) => row.classInfo ?? [])
      .filter((item) => item.name).map((item) => {
        const hash = item.classLoaderHash || '';
        return { name: item.name, hash, title: item.codeSource, label: item.name + (hash ? ' · ' + hash : '') };
      });
    renderChoices(choices, (choice) => {
      disposeSource();
      view('Class').value = choice.name;
      view('Loader').value = choice.hash;
      view('Method').value = '';
      view('Method').focus();
    });
    const results = view('ClassChoices');
    results.hidden = !choices.length;
    results.open = !results.hidden;
  }

  function filterMethods() {
    const access = view('MethodAccess').value;
    const kind = view('MethodKind').value;
    const choices = methodChoices.filter((choice) => (!access || choice.access === access) && (!kind || choice.kinds.includes(kind)));
    view('MethodCount').textContent = choices.length
      ? `显示 ${choices.length} / ${methodChoices.length} 个方法`
      : '没有符合筛选条件的方法';
    renderChoices(choices, (choice) => {
      disposeSource();
      view('Method').value = choice.name;
      view('Method').focus();
    });
  }

  function methodMatches({ methods, warning }) {
    const shortName = (type) => type.slice(type.lastIndexOf('.') + 1);
    methodChoices = methods.map(({ methodName, parameters, returnType, declaringClass, classLoaderHash,
      access, interfaces, httpMappings, proxy }) => {
      const kinds = [];
      const tags = [methodAccessLabels[access] ?? access];
      if (httpMappings.length) {
        kinds.push('http');
        tags.push(...httpMappings.map((item) => '@' + shortName(item)));
      }
      if (interfaces.length) {
        kinds.push('interface');
        tags.push('Java 接口');
      }
      if (proxy) {
        kinds.push('proxy');
        tags.push('代理类');
      }
      if (!kinds.length) kinds.push('ordinary');
      const signature = `${methodName}(${(parameters ?? []).map(shortName).join(', ')})`;
      const title = [`${returnType ?? ''} ${declaringClass}.${methodName}(${(parameters ?? []).join(', ')})`.trim(),
        classLoaderHash && classLoaderHash !== 'null' ? 'ClassLoader ' + classLoaderHash : '',
        interfaces.length ? 'Java 接口 ' + interfaces.join(', ') : ''].filter(Boolean).join('\n');
      const order = proxy ? 3 : httpMappings.length ? 0 : interfaces.length ? 1 : 2;
      return { name: methodName, label: signature, title, access, kinds, tags, order };
    }).sort((a, b) => a.order - b.order || a.label.localeCompare(b.label));
    if (!methodChoices.length) return;
    view('MethodAccess').value = '';
    view('MethodKind').value = '';
    view('MethodFilters').hidden = false;
    view('MethodWarning').hidden = !warning;
    view('MethodWarning').textContent = warning ? '方法分类信息不完整 · ' + warning : '';
    view('ClassMatches').classList.add('runtime-method-matches');
    view('ClassChoicesTitle').textContent = '方法列表';
    view('ClassChoices').hidden = false;
    view('ClassChoices').open = true;
    filterMethods();
  }

  function formatRows(rows) {
    return rows.map((row) => {
      const { jobId, type, ...value } = row;
      return type === 'message' ? value.message : JSON.stringify(value, null, 2);
    }).join('\n\n') || '没有匹配结果。';
  }

  async function diagnose(type, scope) {
    const isClass = scope === 'classes';
    const output = view(isClass ? 'ClassOutput' : 'JvmOutput');
    if (isClass) {
      if (!(await confirmSourceDiscard())) return;
      disposeSource();
    }
    if (type === 'classes' || type === 'methods') clearClassChoices();
    const runtimeRequest = {
      action: 'diagnose', ...selectedTarget(), type,
      className: view('Class').value.trim(), loader: view('Loader').value.trim(), method: view('Method').value.trim()
    };
    if (type === 'trace') {
      output.textContent = '';
      view('TraceStatus').textContent = '正在启动方法采集…';
    } else {
      output.textContent = '查询中…';
    }
    try {
      const result = await invoke(scope, runtimeRequest, (kind, value) => {
        if (kind === 'status') view('TraceStatus').textContent = value;
        if (kind === 'trace') {
          const block = formatRows([value]);
          output.textContent = (output.textContent + block + '\n\n').slice(-logCharacterLimit);
        }
      });
      const rows = type === 'methods' ? result.rows : result;
      if (type === 'trace') {
        view('TraceStatus').textContent = rows.matched ? '已结束，采集 ' + rows.matched + ' 次' : '已结束，未捕获目标方法';
      } else if (type === 'source' && rows.find((item) => item.type === 'jad' && item.source)) {
        const source = rows.find((item) => item.type === 'jad').source;
        showSource(source);
      } else {
        output.textContent = formatRows(rows);
        if (type === 'classes') classMatches(rows);
        if (type === 'methods') methodMatches(result);
      }
    } catch (error) {
      if (type === 'trace') {
        view('TraceStatus').textContent = error.message.includes('操作已取消') ? '已停止采集' : '采集失败';
      } else {
        output.textContent = error.message.includes('操作已取消') ? '已取消查询' : '查询失败';
      }
      if (!error.message.includes('操作已取消')) throw error;
    }
  }

  function selectTab(name) {
    for (const button of tabs) button.setAttribute('aria-pressed', String(button.dataset.runtimeTab === name));
    for (const pane of panes) pane.hidden = pane.dataset.runtimePane !== name;
    syncSourceScroll();
  }

  async function close() {
    if (closing || managingSshHosts || choosingSourceArchive || !(await confirmSourceDiscard())) return;
    closing = true;
    view('Status').textContent = '正在关闭工作台…';
    syncControls();
    try {
      const running = [...tasks.values()];
      await Promise.allSettled(running.map((task) => bridge.cancelRuntimeOperation(task.id)));
      await Promise.allSettled(running.map((task) => task.result));
      await invoke('target', { action: 'detach' });
      await invoke('target', { action: 'disconnectPassword' });
      if (loadedLogin?.method === 'password') {
        host = undefined;
        loadedLogin = undefined;
        integrations.targets([]);
        fillOptions(view('Container'), [], String, String, '请先连接 SSH');
        fillOptions(view('Process'), [], String, String, '请选择容器');
        fillOptions(view('Ports'), [], String, String, '请选择 Java 进程');
      }
      view('SshPassword').value = '';
      view('ExistingPassword').value = '';
      existingTarget = undefined;
      attached = false;
      canRelease = false;
      clearLog();
      clearOutputs();
      analysis.resetComparison();
      analysis.resetRequest();
      analysis.clearCredentials();
      integrations.setAttachment(null);
      integrations.clearCredentials();
      for (const name of ['Headers', 'Body']) view(name).value = '';
      root.close();
    } catch (error) {
      view('Status').textContent = 'Arthas 清理未确认';
      canRelease = true;
      throw error;
    } finally {
      closing = false;
      syncControls();
    }
  }

  bind('Btn', async () => {
    clearError();
    root.showModal();
    if (view('Provider').value === PROVIDER_TYPE.EXISTING) {
      setConnectionBoxExpanded(true);
      selectTab('classes');
      view('Status').textContent = '填写正在运行的 Arthas HTTP 端点';
      view('ExistingTransport').focus();
      return;
    }
    if (view('Provider').value === PROVIDER_TYPE.LOCAL) {
      setConnectionBoxExpanded(false);
      view('Status').textContent = '点击「加载 Java 进程」查看本机 JVM';
      view('Connect').focus();
      return;
    }
    await manageSshHosts(() => bridge.listSshHosts());
    // 打开工作台时按是否有已保存主机智能决定默认态：有常用主机则默认选中首台并收起配置面板
    if (!host && !view('Host').value.trim()) {
      if (savedHosts.length) {
        applySavedHost(savedHosts[0]);
        setConnectionBoxExpanded(false);
        syncControls();
        view('Status').textContent = CONNECTION_COPY.STATUS_READY_SAVED_HOST;
        view('SshHosts').focus();
      } else {
        setConnectionBoxExpanded(true);
        syncControls();
        view('Status').textContent = CONNECTION_COPY.STATUS_NEED_HOST_CONFIG;
        view('Host').focus();
      }
      return;
    }
    setConnectionBoxExpanded(false);
    syncControls();
    if (view('Provider').value === PROVIDER_TYPE.WINDOWS) selectTab('classes');
    view('Status').textContent = host ? CONNECTION_COPY.STATUS_RELOAD_OR_SELECT : CONNECTION_COPY.STATUS_READY_SAVED_HOST;
    view('SshHosts').focus();
  });
  bind('Close', close);
  view('SshHosts').addEventListener('change', () => {
    clearError();
    const selected = view('SshHosts').value;
    // 选中底部「新增 / 临时主机」时清空草稿并展开配置面板，聚焦主机输入框
    if (selected === HOST_OPTION.NEW) {
      resetNewHostDraft();
      setConnectionBoxExpanded(true);
      syncControls();
      view('Host').focus();
      return;
    }
    if (selected === HOST_OPTION.CUSTOM || selected === HOST_OPTION.EMPTY) {
      syncControls();
      return;
    }
    const entry = savedHosts.find((item) => item.id === selected);
    if (entry) applySavedHost(entry);
    syncControls();
  });
  bind('SaveSshHost', async () => {
    const { address, username, port } = loginFields();
    await manageSshHosts(() => bridge.saveSshHost({ host: address, username, port, password: view('SshPassword').value }));
    view('SshPassword').value = '';
    view('Status').textContent = CONNECTION_COPY.STATUS_HOST_SAVED;
    syncControls();
  });
  bind('RemoveSshHost', async () => {
    const entry = savedHostFor(loginFields());
    if (!entry) return;
    const confirmed = await window.jarPatchDialogs.confirm({
      title: '移除已保存的 SSH 主机',
      message: '将从本机删除该主机记录及加密保存的登录密码，后续连接需要重新输入。',
      tone: window.jarPatchDialogs.TONE_DANGER,
      details: [
        { label: '主机', value: formatSavedHostLabel(entry) }
      ],
      actions: [
        { value: false, label: '取消', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
        { value: true, label: '确认移除', variant: window.jarPatchDialogs.VARIANT_DANGER }
      ]
    });
    if (!confirmed) return;
    await manageSshHosts(() => bridge.removeSshHost(entry.id));
    resetNewHostDraft();
    view('Status').textContent = CONNECTION_COPY.STATUS_HOST_REMOVED;
    syncControls();
  });
  root.addEventListener('cancel', (event) => {
    event.preventDefault();
    close().catch(showError);
  });
  root.addEventListener('keydown', (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      event.preventDefault();
      event.stopPropagation();
    }
  });
  bind('Connect', async () => {
    const provider = view('Provider').value;
    if (provider === PROVIDER_TYPE.LOCAL) {
      host = LOCAL_HOST;
      loadedProvider = provider;
      loadedLogin = undefined;
      loadedCluster = undefined;
      integrations.targets([]);
      fillOptions(view('Container'), [{ id: LOCAL_JVM, name: LOCAL_HOST }], (item) => item.name, (item) => item.id, '', true);
      await selectContainer();
      view('Process').focus();
      return;
    }
    const login = requireSshLogin();
    const cluster = integrations.cluster();
    const hostJvm = provider === PROVIDER_TYPE.HOST;
    const windowsJvm = provider === PROVIDER_TYPE.WINDOWS;
    const sameLogin = host && JSON.stringify(login) === JSON.stringify(loadedLogin);
    const password = view('SshPassword').value;
    view('SshPassword').value = '';
    const environment = windowsJvm ? 'Windows 宿主机' : hostJvm ? 'Linux 宿主机' : cluster ? 'Kubernetes 环境' : 'Docker 环境';
    view('Status').textContent = '正在通过 SSH 连接 ' + environment + '…';
    try {
      let list;
      if (login.method === AUTH_METHOD.PASSWORD && (password || !sameLogin || savedHostFor(login))) {
        const connection = await invoke('target', { action: 'passwordContainers', host: login.address,
          username: login.username, port: login.port, password, cluster, hostJvm, windowsJvm });
        host = connection.host;
        list = connection.list;
      } else {
        const requestedHost = login.method === AUTH_METHOD.PASSWORD ? host : login.address;
        list = await invoke('target', { action: 'containers', host: requestedHost, cluster, hostJvm, windowsJvm });
        host = requestedHost;
      }
      loadedLogin = login;
      loadedCluster = cluster;
      loadedProvider = provider;
      integrations.targets(hostJvm || windowsJvm ? [] : list);
      fillOptions(view('Container'), list, (item) => [item.name, item.image, item.status].filter(Boolean).join(' · '),
        (item) => item.id, list.length ? '请选择容器' : '无运行中容器', true);
      await selectContainer();
      if (hostJvm || windowsJvm) view('Process').focus();
    } catch (error) {
      if (provider !== loadedProvider || JSON.stringify(login) !== JSON.stringify(loadedLogin)
          || JSON.stringify(cluster) !== JSON.stringify(loadedCluster)) {
        host = undefined;
        loadedLogin = undefined;
        loadedCluster = undefined;
        loadedProvider = undefined;
        integrations.targets([]);
        fillOptions(view('Container'), [], String, String, '请先连接 SSH');
        await selectContainer();
      }
      view('Status').textContent = 'SSH 或运行目标查询失败';
      throw error;
    }
  });
  view('Provider').addEventListener('change', () => {
    clearError();
    host = undefined;
    loadedLogin = undefined;
    loadedCluster = undefined;
    loadedProvider = undefined;
    existingTarget = undefined;
    view('ExistingPassword').value = '';
    integrations.targets([]);
    fillOptions(view('Container'), [], String, String, '请先加载运行目标');
    selectContainer().then(() => {
      const provider = view('Provider').value;
      if (provider === PROVIDER_TYPE.EXISTING) {
        setConnectionBoxExpanded(true);
        selectTab('classes');
        view('Status').textContent = '填写正在运行的 Arthas HTTP 端点';
      } else {
        if (provider === PROVIDER_TYPE.WINDOWS) selectTab('classes');
        view('Status').textContent = provider === PROVIDER_TYPE.LOCAL
          ? '点击「加载 Java 进程」查看本机 JVM' : '选择服务器后加载运行目标';
      }
    }).catch(showError);
  });
  view('ExistingTransport').addEventListener('change', syncControls);
  view('ExistingPort').addEventListener('input', syncControls);
  view('LogSourceKind').addEventListener('change', () => {
    view('LogSourceValue').value = '';
    clearLog();
    syncControls();
  });
  view('LogSourceValue').addEventListener('input', syncControls);
  view('Container').addEventListener('change', () => { clearError(); selectContainer().catch(showError); });
  view('Process').addEventListener('change', () => { clearError(); selectProcess().catch(showError); });
  for (const name of ['Host', 'SshUser', 'SshPort', 'SshPassword']) {
    view(name).addEventListener('keydown', (event) => {
      // 忽略中文输入法组合态中的回车按键，避免误触发远程连接
      if (!event.isComposing && event.key === 'Enter') {
        const button = view(view('Provider').value === PROVIDER_TYPE.EXISTING ? 'Attach' : 'Connect');
        if (!button.disabled && !button.hidden) button.click();
      }
    });
    view(name).addEventListener('input', syncControls);
  }
  view('SshAuth').addEventListener('change', () => {
    view('SshPassword').value = '';
    syncControls();
  });
  view('Class').addEventListener('input', () => { view('Loader').value = ''; });
  bind('Package', async () => {
    const name = await bridge.chooseArthasPackage();
    if (!name) return;
    packageName = name;
    view('PackageName').textContent = name;
    view('ResetPackage').hidden = false;
    syncControls();
  });
  bind('ResetPackage', () => {
    packageName = undefined;
    view('PackageName').textContent = CONNECTION_COPY.DEFAULT_PACKAGE_LABEL;
    view('ResetPackage').hidden = true;
    syncControls();
  });
  bind('Attach', async () => {
    const existing = view('Provider').value === PROVIDER_TYPE.EXISTING;
    const data = existing ? existingConnection()
      : { action: 'attach', ...selectedTarget(), offline: Boolean(packageName) };
    canRelease = true;
    if (existing) view('Status').textContent = '正在连接已有 Arthas…';
    try {
      const result = await invoke('target', data,
        (kind, value) => { if (kind === 'status') view('Status').textContent = value; });
      attached = true;
      existingTarget = existing ? result.target : undefined;
      integrations.setAttachment(result);
      view('Status').textContent = (existing ? '已连接已有 Arthas' : 'Arthas ' + result.version + ' 已连接') + ' · PID ' + result.pid;
      setConnectionBoxExpanded(false);
      selectTab('classes');
      view('Class').focus();
    } catch (error) {
      view('Status').textContent = 'Arthas 连接失败';
      canRelease = /Arthas 清理未确认|请先释放当前 Arthas 会话/.test(error.message);
      throw error;
    } finally {
      syncControls();
    }
  });
  bind('Detach', async () => {
    if (!(await confirmSourceDiscard())) return;
    view('Status').textContent = '正在释放 Arthas…';
    let result;
    try {
      result = await invoke('target', { action: 'detach' });
    } catch (error) {
      view('Status').textContent = 'Arthas 清理未确认';
      throw error;
    }
    attached = false;
    canRelease = false;
    existingTarget = undefined;
    integrations.setAttachment(null);
    clearOutputs();
    view('Status').textContent = result?.borrowed ? '已断开本次 Arthas 诊断连接'
      : result?.deferredCleanup
      ? 'Arthas 已释放。目标 JVM 退出后重新加载进程，可回收被占用的代理 JAR。'
      : 'Arthas 已释放';
    syncControls();
  });
  view('ConnToggleBtn').addEventListener('click', () => setConnectionBoxExpanded(!connectionExpanded));
  for (const button of tabs) button.addEventListener('click', () => selectTab(button.dataset.runtimeTab));
  bind('ReadLogs', () => readLogs(false));
  bind('FollowLogs', () => readLogs(true));
  bind('StopLogs', () => cancel('logs'));
  bind('Search', () => diagnose('classes', 'classes'));
  bind('Methods', () => diagnose('methods', 'classes'));
  view('MethodAccess').addEventListener('change', filterMethods);
  view('MethodKind').addEventListener('change', filterMethods);
  bind('Source', () => diagnose('source', 'classes'));
  bind('EditSource', editOnlineSource);
  bind('ApplySourcePatch', applyOnlineSource);
  bind('RestoreSourcePatch', () => integrations.restoreSourcePatch());
  bind('Trace', () => diagnose('trace', 'classes'));
  bind('CancelTrace', () => cancel('classes'));
  for (const button of jvmButtons) button.addEventListener('click', () => {
    clearError();
    diagnose(button.dataset.runtimeDiagnose, 'jvm').catch(showError);
  });
  bind('CancelJvm', () => cancel('jvm'));
  bind('RefreshPorts', refreshPorts);
  view('Ports').addEventListener('change', syncControls);

  function requestFields() {
    return { method: view('HttpMethod').value, protocol: view('Protocol').value, port: view('Ports').value,
      path: view('Path').value.trim(), serverName: view('ServerName').value.trim(),
      headers: view('Headers').value, body: view('Body').value };
  }

  bind('Send', async () => {
    view('Response').textContent = '';
    view('RequestStatus').textContent = '正在等待响应…';
    analysis.setRequest(null);
    try {
      const result = await invoke('request', { action: 'requestCapture', ...selectedTarget(), ...requestFields() });
      view('Response').textContent = result.text;
      analysis.setRequest(result);
      view('RequestStatus').textContent = '已收到响应';
    } catch (error) {
      const canceled = error.message.includes('操作已取消');
      view('RequestStatus').textContent = canceled ? '已取消请求' : '请求失败';
      if (!canceled) throw error;
    }
  });
  bind('CancelRequest', () => cancel('request'));

  analysis = window.createRuntimeAnalysis({ view, bridge, bind, invoke, cancel, selectedTarget, requestFields,
    selectTab, fillOptions, clearLog, appendLog, syncControls, showError, tasks });
  integrations = window.createRuntimeIntegrations({ view, bridge, bind, invoke, cancel,
    selectedTarget, requestFields, syncControls, tasks });
  fillOptions(view('Container'), [], String, String, '请先加载容器');
  fillOptions(view('Process'), [], String, String, '请选择容器');
  fillOptions(view('Ports'), [], String, String, '请选择 Java 进程');
  setConnectionBoxExpanded(false);
  syncControls();
})();
