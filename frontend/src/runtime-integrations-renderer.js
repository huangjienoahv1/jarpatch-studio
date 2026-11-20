window.createRuntimeIntegrations = function createRuntimeIntegrations(context) {
  const { view, bridge, bind, invoke, cancel, selectedTarget, requestFields, syncControls, tasks } = context;
  const connectionFields = ['Provider', 'KubeContext', 'KubeNamespace', 'KubeSelector', 'KubeDebug', 'KubeConfig'];
  let patchEnabled = false;
  let patchActive;

  function cluster() {
    if (view('Provider').value !== 'kubernetes') return undefined;
    return { context: view('KubeContext').value.trim(), namespace: view('KubeNamespace').value.trim(),
      selector: view('KubeSelector').value.trim(), debug: view('KubeDebug').value.trim(), kubeconfig: view('KubeConfig').value.trim() };
  }

  function targets(rows) {
    const list = view('InventoryTargets');
    list.replaceChildren(...rows.map((row) => new Option(row.name, row.id)));
  }

  function setAttachment(result) {
    patchEnabled = Boolean(result?.patchEnabled);
    patchActive = undefined;
  }

  function sourcePatchHint() {
    if (patchEnabled) return patchActive ? '请先恢复原 class，再编辑下一次补丁。' : '';
    return '在线编辑和热替换支持 Docker 连接。';
  }

  function sync({ closing, lockTarget, currentHost, hasProcess, targeting, attached, clusterOptions, classActive }) {
    const busy = tasks.has('advanced');
    const busyJvm = classActive || tasks.has('profile');
    view('Kubernetes').hidden = view('Provider').value !== 'kubernetes';
    for (const name of connectionFields) view(name).disabled = lockTarget;
    view('Inventory').disabled = closing || !currentHost || targeting || busy || !view('InventoryTargets').selectedOptions.length;
    view('Actuator').disabled = closing || !hasProcess || targeting || busy || !view('Ports').value;
    view('ActuatorMetric').disabled = view('ActuatorEndpoint').value !== 'metrics';
    view('Loki').disabled = closing || busy;
    view('Tunnel').disabled = closing;
    view('Patch').disabled = closing || busy || busyJvm || !attached || !patchEnabled || patchActive || Boolean(clusterOptions);
    view('Rollback').disabled = closing || busy || busyJvm || !attached || !patchActive;
    view('CancelAdvanced').disabled = closing || !busy || tasks.get('advanced')?.type !== 'query';
  }

  async function perform(data, summarize, inline = false) {
    const status = view(inline ? 'SourcePatchStatus' : 'AdvancedStatus');
    const output = inline ? null : view('AdvancedOutput');
    if (output) output.textContent = '';
    status.textContent = '处理中…';
    try {
      const result = await invoke('advanced', data, (kind, value) => {
        if (kind === 'status') status.textContent = value;
      });
      const summary = summarize(result);
      if (output) output.textContent = summary;
      status.textContent = output ? '已完成' : summary;
      return result;
    } catch (error) {
      const canceled = error.message.includes('操作已取消');
      status.textContent = canceled ? '已取消操作' : '操作失败';
      if (!canceled) throw error;
    } finally {
      syncControls();
    }
  }

  bind('Inventory', () => perform({ action: 'inventory', type: 'query', host: selectedTarget().host, cluster: cluster(),
    selected: [...view('InventoryTargets').selectedOptions].map((option) => option.value) }, (rows) => rows.map((item) => {
    const title = item.name + ' · ' + item.status + (item.image ? ' · ' + item.image : '');
    if (item.error) return title + '\n  ' + item.error;
    const lines = item.java.length ? item.java.map((jvm) => '  PID ' + jvm.pid + ' · ' + jvm.executable + ' · '
      + (jvm.ports.length ? jvm.ports.join(', ') : '无监听端口')) : ['  没有可见的 JVM'];
    return [title, ...lines].join('\n');
  }).join('\n\n')));

  bind('Actuator', () => {
    const fields = requestFields();
    return perform({
      action: 'actuator', type: 'query', ...selectedTarget(), protocol: fields.protocol, port: fields.port,
      serverName: fields.serverName, headers: fields.headers, basePath: view('ActuatorBase').value.trim(),
      endpoint: view('ActuatorEndpoint').value, metric: view('ActuatorMetric').value.trim()
    }, (result) => result);
  });

  bind('Loki', () => perform({ action: 'loki', type: 'query', url: view('LokiUrl').value.trim(),
    tenant: view('LokiTenant').value.trim(), token: view('LokiToken').value.trim(), minutes: view('LokiMinutes').value,
    query: view('LokiQuery').value.trim() }, (result) => result.count + ' 条日志\n\n' + result.text));

  bind('Tunnel', () => bridge.openRuntimeTunnel({ url: view('TunnelUrl').value.trim(), agentId: view('TunnelAgent').value.trim(),
    port: view('TunnelPort').value }));
  bind('CancelAdvanced', () => cancel('advanced'));

  async function applyPatch(data, inline = false) {
    try {
      const result = await perform(data, (value) => 'JVM 已接受热替换 · ' + value.className + '\nSHA-256 ' + value.sha256, inline);
      if (result) patchActive = { className: result.className, loader: data.loader };
    } catch (error) {
      if (error.message.includes('恢复待确认')) patchActive = { className: data.className, loader: data.loader };
      throw error;
    } finally {
      syncControls();
    }
  }

  async function restorePatch(inline = false) {
    const result = await perform({ action: 'restorePatch', ...selectedTarget(), type: inline ? 'sourcePatch' : undefined },
      (value) => '已恢复原 class · ' + value.className + '\nSHA-256 ' + value.sha256, inline);
    if (result) patchActive = undefined;
    syncControls();
  }

  bind('Patch', async () => {
    const className = view('Class').value.trim();
    if (!className) throw new Error('先在「类与方法」选择准确的类名');
    const file = await bridge.chooseRuntimePatch();
    if (!file) return;
    await applyPatch({ action: 'applyPatch', ...selectedTarget(), className, loader: view('Loader').value.trim() });
  });

  bind('Rollback', () => restorePatch());

  for (const name of connectionFields) view(name).addEventListener('input', syncControls);
  for (const name of ['Provider', 'InventoryTargets', 'ActuatorEndpoint']) view(name).addEventListener('change', syncControls);
  return { cluster, targets, setAttachment, sync,
    sourcePatchHint,
    canApplyPatch: () => patchEnabled && !patchActive,
    canRestoreSource: (draft) => patchActive?.className === draft.className && patchActive.loader === draft.loader,
    applySourcePatch: (data) => applyPatch({ ...data, action: 'applySourcePatch' }, true),
    restoreSourcePatch: () => restorePatch(true),
    clearCredentials: () => { view('LokiToken').value = ''; } };
};
