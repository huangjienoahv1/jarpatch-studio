(() => {
  const view = (name) => document.getElementById('runtime' + name);
  const root = view('Dialog');
  const tabs = [...root.querySelectorAll('[data-runtime-tab]')];
  const panes = [...root.querySelectorAll('[data-runtime-pane]')];
  const jvmButtons = [...root.querySelectorAll('[data-runtime-diagnose]')];
  const sourcePanel = view('SourcePanel');
  const sourcePane = sourcePanel.parentElement;
  const bridge = window.jarPatch;
  const tasks = new Map();
  const updates = new Map();
  const logCharacterLimit = 220000;
  const traceHint = '采集最多 20 秒或 3 次，可切换到请求测试触发方法。';
  const methodAccessLabels = { package: '包可见', unknown: '访问级别未知' };
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
  let savedHosts = [];
  let managingSshHosts = false;

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
    return { host, container: view('Container').value, pid: process?.pid, start: process?.start };
  }

  function loginFields() {
    const method = view('SshAuth').value;
    const address = view('Host').value.trim();
    return method === 'password'
      ? { method, address, username: view('SshUser').value.trim(), port: view('SshPort').value }
      : { method, address };
  }

  function savedHostFor(login) {
    if (login.method !== 'password') return undefined;
    const address = login.address.replace(/^\[([a-f0-9:]+)\]$/i, '$1').toLowerCase();
    return savedHosts.find((entry) => entry.host === address && entry.username === login.username
      && entry.port === Number(login.port));
  }

  async function manageSshHosts(operation) {
    managingSshHosts = true;
    syncControls();
    try {
      savedHosts = await operation();
      fillOptions(view('SshHosts'), savedHosts, (entry) => {
        const address = entry.host.includes(':') ? '[' + entry.host + ']' : entry.host;
        return `${entry.username}@${address}:${entry.port}`;
      }, (entry) => entry.id, savedHosts.length ? '选择已保存的主机' : '暂无保存的主机');
    } finally {
      managingSshHosts = false;
      syncControls();
    }
  }

  function syncControls() {
    const clusterOptions = integrations?.cluster();
    const login = loginFields();
    const currentHost = Boolean(host) && JSON.stringify(loadedLogin) === JSON.stringify(login)
      && JSON.stringify(loadedCluster) === JSON.stringify(clusterOptions);
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
    view('Host').disabled = lockTarget;
    view('Host').placeholder = login.method === 'password' ? '服务器 IP 或主机名' : 'SSH 别名或 user@host';
    view('PasswordFields').hidden = login.method !== 'password';
    for (const name of ['SshAuth', 'SshUser', 'SshPort', 'SshPassword']) view(name).disabled = lockTarget;
    view('SshHosts').value = savedHost?.id || '';
    view('SshHosts').disabled = lockTarget || !savedHosts.length;
    view('SshPassword').placeholder = savedHost ? '已保存密码，留空直接连接' : '';
    view('SaveSshHost').disabled = lockTarget || !login.address || !login.username || !login.port || !view('SshPassword').value;
    view('RemoveSshHost').disabled = lockTarget || !savedHost;
    view('Connect').disabled = lockTarget;
    view('Container').disabled = lockTarget || !currentHost;
    view('Process').disabled = lockTarget || !hasContainer || processes.length === 0;
    view('Attach').disabled = closing || active || !hasProcess || canRelease;
    view('Detach').disabled = closing || active || !canRelease;
    view('Package').disabled = lockTarget;
    view('ResetPackage').disabled = lockTarget;
    view('Close').disabled = closing || managingSshHosts || choosingSourceArchive;
    for (const name of ['ReadLogs', 'FollowLogs']) view(name).disabled = closing || targeting || !hasContainer || logActive;
    view('StopLogs').disabled = closing || !logActive;
    for (const name of ['Search', 'Methods', 'Source', 'Trace']) view(name).disabled = closing || !attached || classActive;
    for (const name of ['Class', 'Loader', 'Method']) view(name).disabled = closing || !attached || classActive;
    view('CancelTrace').disabled = closing || tasks.get('classes')?.type !== 'trace';
    jvmButtons.forEach((button) => { button.disabled = closing || !attached || jvmActive; });
    view('CancelJvm').disabled = closing || !jvmActive;
    view('RefreshPorts').disabled = closing || !hasProcess || targeting || requestActive;
    view('Send').disabled = closing || !hasProcess || targeting || requestActive || tasks.has('suite') || !view('Ports').value;
    view('CancelRequest').disabled = closing || !requestActive;
    analysis?.sync({ closing, attached, hasProcess, targeting, classActive, requestActive });
    integrations?.sync({ closing, lockTarget, currentHost, hasProcess, targeting, attached, clusterOptions, classActive });
    const canPatch = !closing && attached && !classActive && !tasks.has('advanced') && !tasks.has('profile');
    view('EditSource').disabled = !canPatch || !integrations?.canApplyPatch();
    const patchHint = attached ? integrations.sourcePatchHint() : '';
    const patchAvailability = view('SourcePatchAvailability');
    patchAvailability.hidden = !patchHint;
    if (patchAvailability.textContent !== patchHint) patchAvailability.textContent = patchHint;
    view('ApplySourcePatch').disabled = !canPatch || !integrations?.canApplyPatch() || !sourceView?.isChanged();
    view('RestoreSourcePatch').hidden = !sourceDraft || !integrations?.canRestoreSource(sourceDraft);
    view('RestoreSourcePatch').disabled = !canPatch;
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

  function confirmSourceDiscard() {
    return !sourceDraft || !sourceView.isChanged() || sourceView.getValue() === sourceDraft.source
      || window.confirm('离开当前源码编辑器会丢弃本次修改，继续吗？');
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
    if (!confirmSourceDiscard()) return;
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
    view('TraceStatus').textContent = traceHint;
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
      view('Status').textContent = '已选择 PID ' + process.pid + ' · ' + process.executable;
      await refreshPorts();
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
    view('Status').textContent = '正在查找容器中的 Java 进程…';
    try {
      processes = await invoke('target', { action: 'processes', ...selectedTarget() });
      fillOptions(view('Process'), processes, (p) => 'PID ' + p.pid + ' · ' + p.executable,
        (p) => p.pid, processes.length ? '请选择进程' : '未发现 Java 进程', true);
      view('Status').textContent = processes.length ? '请选择 Java 进程' : '当前容器未发现可读的 Java 进程';
      await selectProcess();
    } catch (error) {
      view('Status').textContent = '容器已选中';
      throw error;
    }
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
        action: 'logs', ...selectedTarget(), tail: view('Tail').value, follow
      }, (kind, chunk) => { if (kind === 'log') appendLog(chunk); });
      if (!follow) {
        appendLog(result || '该容器没有可读的近期标准输出日志。');
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
      button.addEventListener('click', () => {
        if (!tasks.has('classes') && tasks.get('advanced')?.type !== 'sourcePatch' && !choosingSourceArchive
            && confirmSourceDiscard()) select(choice);
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
      if (!confirmSourceDiscard()) return;
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
    if (closing || managingSshHosts || choosingSourceArchive || !confirmSourceDiscard()) return;
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
      view('Status').textContent = '远端清理未确认';
      canRelease = true;
      throw error;
    } finally {
      closing = false;
      syncControls();
    }
  }

  bind('Btn', async () => {
    view('Status').textContent = host ? '选择容器或重新加载清单' : '选择 SSH 认证方式后加载容器';
    root.showModal();
    await manageSshHosts(() => bridge.listSshHosts());
    view('Host').focus();
  });
  bind('Close', close);
  view('SshHosts').addEventListener('change', () => {
    clearError();
    const entry = savedHosts.find((item) => item.id === view('SshHosts').value);
    if (entry) view('SshAuth').value = 'password';
    view('Host').value = entry?.host || '';
    view('SshUser').value = entry?.username || '';
    view('SshPort').value = entry?.port ?? 22;
    view('SshPassword').value = '';
    syncControls();
  });
  bind('SaveSshHost', async () => {
    const { address, username, port } = loginFields();
    await manageSshHosts(() => bridge.saveSshHost({ host: address, username, port, password: view('SshPassword').value }));
    view('SshPassword').value = '';
    view('Status').textContent = 'SSH 主机已保存，可以直接加载目标';
    syncControls();
  });
  bind('RemoveSshHost', async () => {
    const entry = savedHostFor(loginFields());
    if (!entry || !window.confirm('移除此主机及已保存的密码？')) return;
    await manageSshHosts(() => bridge.removeSshHost(entry.id));
    view('Status').textContent = '已移除保存的 SSH 主机';
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
    const login = loginFields();
    const cluster = integrations.cluster();
    const sameLogin = host && JSON.stringify(login) === JSON.stringify(loadedLogin);
    const password = view('SshPassword').value;
    view('SshPassword').value = '';
    view('Status').textContent = '正在通过 SSH 读取' + (cluster ? ' Kubernetes Pod…' : ' Docker 容器…');
    try {
      let list;
      if (login.method === 'password' && (password || !sameLogin || savedHostFor(login))) {
        const connection = await invoke('target', { action: 'passwordContainers', host: login.address,
          username: login.username, port: login.port, password, cluster });
        host = connection.host;
        list = connection.list;
      } else {
        const requestedHost = login.method === 'password' ? host : login.address;
        list = await invoke('target', { action: 'containers', host: requestedHost, cluster });
        host = requestedHost;
      }
      loadedLogin = login;
      loadedCluster = cluster;
      integrations.targets(list);
      fillOptions(view('Container'), list, (item) => item.name + ' · ' + item.image + ' · ' + item.status,
        (item) => item.id, list.length ? '请选择容器' : '无运行中容器', true);
      await selectContainer();
    } catch (error) {
      if (JSON.stringify(login) !== JSON.stringify(loadedLogin) || JSON.stringify(cluster) !== JSON.stringify(loadedCluster)) {
        host = undefined;
        loadedLogin = undefined;
        loadedCluster = undefined;
        integrations.targets([]);
        fillOptions(view('Container'), [], String, String, '请先连接 SSH');
        await selectContainer();
      }
      view('Status').textContent = 'SSH 或容器查询失败';
      throw error;
    }
  });
  view('Container').addEventListener('change', () => { clearError(); selectContainer().catch(showError); });
  view('Process').addEventListener('change', () => { clearError(); selectProcess().catch(showError); });
  for (const name of ['Host', 'SshUser', 'SshPort', 'SshPassword']) {
    view(name).addEventListener('keydown', (event) => {
      if (event.key === 'Enter' && !view('Connect').disabled) view('Connect').click();
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
  });
  bind('ResetPackage', () => {
    packageName = undefined;
    view('PackageName').textContent = '默认下载并校验官方完整包';
    view('ResetPackage').hidden = true;
  });
  bind('Attach', async () => {
    canRelease = true;
    try {
      const result = await invoke('target', { action: 'attach', ...selectedTarget(), offline: Boolean(packageName) },
        (kind, value) => { if (kind === 'status') view('Status').textContent = value; });
      attached = true;
      integrations.setAttachment(result);
      view('Status').textContent = 'Arthas ' + result.version + ' 已连接 · PID ' + result.pid;
      selectTab('classes');
      view('Class').focus();
    } catch (error) {
      view('Status').textContent = 'Arthas 连接失败';
      canRelease = /远端清理未确认|请先释放当前 Arthas 会话/.test(error.message);
      throw error;
    } finally {
      syncControls();
    }
  });
  bind('Detach', async () => {
    if (!confirmSourceDiscard()) return;
    view('Status').textContent = '正在释放 Arthas…';
    try {
      await invoke('target', { action: 'detach' });
    } catch (error) {
      view('Status').textContent = '远端清理未确认';
      throw error;
    }
    attached = false;
    canRelease = false;
    integrations.setAttachment(null);
    clearOutputs();
    view('Status').textContent = 'Arthas 已释放';
    syncControls();
  });
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
  syncControls();
})();
