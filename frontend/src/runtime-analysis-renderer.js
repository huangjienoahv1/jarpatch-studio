window.createRuntimeAnalysis = function createRuntimeAnalysis(context) {
  const { view, bridge, bind, invoke, cancel, selectedTarget, requestFields, selectTab, fillOptions,
    clearLog, appendLog, syncControls, showError, tasks } = context;
  const suiteEditor = view('SuiteJson');
  const profiles = {};
  let baseline;
  let candidate;
  let lastRequest;
  let logLink;
  let comparison;
  let dump;
  let archive;
  let comparing = false;
  let compareGeneration = 0;
  let comparisonView;

  suiteEditor.value = JSON.stringify({ version: 1, cases: [] }, null, 2);

  function parseSuite() {
    try {
      const value = JSON.parse(suiteEditor.value);
      if (value?.version === 1 && Array.isArray(value.cases)) return value;
    } catch {
      // 输入正在编辑时保留草稿，并在用户执行操作时说明格式。
    }
    throw new Error('请输入版本为 1 的请求集合 JSON');
  }

  function resetComparison() {
    compareGeneration++;
    comparisonView?.dispose();
    comparisonView = undefined;
    comparison = undefined;
    dump = undefined;
    archive = undefined;
    view('ComparisonMeta').textContent = '';
    view('ComparisonTarget').textContent = '从「类与方法」选择已加载类并对照本地包。';
    view('ComparisonView').hidden = true;
    fillOptions(view('ComparisonEntry'), [], String, String, '暂无包内候选');
  }

  function resetRequest() {
    lastRequest = undefined;
    logLink = undefined;
  }

  function setRequest(result) {
    lastRequest = result ? { target: result.target, startedAt: result.startedAt,
      status: result.status, elapsedMs: result.elapsedMs, size: result.size } : undefined;
    logLink = undefined;
    syncControls();
  }

  function sync({ closing, attached, hasProcess, targeting, classActive, requestActive }) {
    const suiteRun = tasks.has('suite');
    const profileRun = tasks.has('profile');
    const available = !closing && hasProcess && !targeting;
    const diagnosticsReady = available && attached;
    view('CompareClass').disabled = !diagnosticsReady || classActive || comparing || profileRun;
    view('ComparisonEntry').disabled = closing || comparing || !comparison?.entries?.length;
    view('Correlate').disabled = !available || !lastRequest || requestActive || tasks.has('logs');
    view('AddCase').disabled = closing || suiteRun;
    for (const control of ['RunBaseline', 'RunCandidate']) {
      view(control).disabled = !available || !view('Ports').value || suiteRun || requestActive || profileRun;
    }
    view('CancelSuite').disabled = closing || !suiteRun;
    view('OpenSuite').disabled = closing || suiteRun;
    view('SaveSuite').disabled = closing || suiteRun;
    suiteEditor.readOnly = suiteRun;
    view('Profile').disabled = !diagnosticsReady || profileRun || classActive || comparing || suiteRun;
    for (const control of ['ProfileType', 'ProfileSeconds', 'ProfileEvent']) view(control).disabled = closing || profileRun;
    view('CancelProfile').disabled = closing || !profileRun;
    view('SaveJfr').disabled = closing || !profiles.jfr;
    view('SaveFlame').disabled = closing || !profiles.flame;
    view('Report').disabled = closing || !(lastRequest || comparison || baseline || candidate || Object.keys(profiles).length);
  }

  function showComparison(value) {
    comparisonView?.dispose();
    comparisonView = undefined;
    comparison = value;
    const entries = value.entries;
    const select = view('ComparisonEntry');
    fillOptions(select, entries, (item) => item.location, (item) => String(item.index), '请选择候选');
    if (value.selected != null) select.value = String(value.selected);
    const chosen = entries[value.selected];
    const lines = ['运行中 SHA-256  ' + value.runtimeSha256];
    if (!entries.length) lines.push('包内未找到该类。');
    else if (!chosen) lines.push('找到 ' + entries.length + ' 个候选，请选择需要对照的条目。');
    else {
      lines.push('包内 SHA-256    ' + chosen.sha256, value.identical ? '字节码一致' : '字节码有差异');
      if (value.runtimeSource == null || value.localSource == null) lines.push('保留字节码结果，当前 class 未生成可显示的两侧源码。');
    }
    view('ComparisonMeta').textContent = lines.join('\n');
    const container = view('ComparisonView');
    container.hidden = value.runtimeSource == null || value.localSource == null;
    if (!container.hidden) {
      comparisonView = window.jarPatchEditor.createDiffView(container, {
        path: dump.className + '.java', originalContent: value.localSource, currentContent: value.runtimeSource,
        originalLabel: '本地部署包', currentLabel: '运行中 class'
      });
    }
  }

  async function compareWithArchive(index) {
    const generation = compareGeneration;
    const previous = index == null ? undefined : comparison.entries.find((entry) => entry.index === index);
    view('ComparisonMeta').textContent = '正在读取部署包并计算 class 摘要…';
    const api = window.jarPatchApiClient;
    const result = await api.request('/api/runtime/class-comparison', {
      method: 'POST',
      body: JSON.stringify({ archive, className: dump.className, contents: dump.contents,
        entryIndex: index, expectedSha256: previous?.sha256 })
    });
    if (generation === compareGeneration) showComparison(result);
  }

  bind('CompareClass', async () => {
    const name = view('Class').value.trim();
    if (!name) throw new Error('先在「类与方法」输入准确的类名');
    const file = await bridge.openArchive();
    if (!file) return;
    resetComparison();
    comparing = true;
    const target = selectedTarget();
    archive = file;
    selectTab('comparison');
    view('ComparisonTarget').textContent = name + ' · PID ' + target.pid + ' · ' + file.split(/[\\/]/).pop();
    view('ComparisonMeta').textContent = '正在采集运行中的 class…';
    try {
      dump = await invoke('classes', { action: 'captureClass', ...target, className: name, loader: view('Loader').value.trim() });
      await compareWithArchive();
    } catch (error) {
      view('ComparisonMeta').textContent = '对照失败';
      throw error;
    } finally {
      comparing = false;
      syncControls();
    }
  });
  view('ComparisonEntry').addEventListener('change', async () => {
    if (!dump || !view('ComparisonEntry').value) return;
    comparing = true;
    syncControls();
    try {
      await compareWithArchive(Number(view('ComparisonEntry').value));
    } catch (error) {
      view('ComparisonMeta').textContent = '候选对照失败';
      showError(error);
    } finally {
      comparing = false;
      syncControls();
    }
  });

  bind('Correlate', async () => {
    const result = await invoke('logs', { action: 'correlate', ...selectedTarget() });
    logLink = { mode: result.mode, lines: result.lines };
    selectTab('logs');
    clearLog();
    view('LogStatus').textContent = result.mode === 'id' ? '按 Request ID 或 Trace ID 匹配'
      : '时间窗口内的候选日志，关联未确认';
    appendLog(result.text || '请求发生时段没有可读日志。');
  });

  bind('AddCase', async () => {
    const suite = parseSuite();
    const fields = requestFields();
    delete fields.port;
    let suffix = suite.cases.length + 1;
    let name = fields.method + ' 用例 ' + suffix;
    while (suite.cases.some((test) => test.name === name)) name = fields.method + ' 用例 ' + ++suffix;
    const next = { ...suite, cases: [...suite.cases, { name, ...fields, assertions: [{ type: 'status', value: 200 }] }] };
    const normalized = await invoke('suite', { action: 'normalizeSuite', suite: next });
    suiteEditor.value = JSON.stringify(normalized, null, 2);
    selectTab('suite');
    view('SuiteStatus').textContent = '已添加 ' + name;
  });

  bind('OpenSuite', async () => {
    const loaded = await bridge.openRuntimeSuite();
    if (!loaded) return;
    suiteEditor.value = JSON.stringify(loaded, null, 2);
    baseline = undefined;
    candidate = undefined;
    updateSuiteOutput();
    view('SuiteStatus').textContent = '已打开 ' + loaded.cases.length + ' 个用例';
    syncControls();
  });
  bind('SaveSuite', async () => {
    const file = await bridge.saveRuntimeSuite(parseSuite());
    if (file) view('SuiteStatus').textContent = '请求集合已保存';
  });

  function successful(result) {
    return !result.error && result.assertions.every((assertion) => assertion.passed);
  }

  function summarizeRun(title, run) {
    const lines = [title + ' · ' + (run.target.hostLabel ?? run.target.host) + ' · ' + run.target.container.slice(0, 12) + ' · PID ' + run.target.pid];
    for (const result of run.results) {
      const info = result.error ? '请求失败（' + result.error + '）'
        : 'HTTP ' + result.status + ' · ' + result.elapsedMs + ' ms · ' + result.size + ' 字节';
      lines.push((successful(result) ? '通过  ' : '失败  ') + result.name + ' · ' + info);
      if (result.assertions?.length) lines.push('      ' + result.assertions.map((item) => item.type + (item.passed ? ' 通过' : ' 失败')).join(' / '));
    }
    return lines;
  }

  function differences() {
    if (!baseline || !candidate || baseline.suiteSha256 !== candidate.suiteSha256) return undefined;
    return candidate.results.map((after, index) => {
      const before = baseline.results[index];
      const comparable = !before.error && !after.error;
      return {
        name: after.name, comparable, beforePass: successful(before), afterPass: successful(after),
        statusChanged: comparable ? before.status !== after.status : null,
        responseChanged: comparable ? before.digest !== after.digest : null,
        elapsedDeltaMs: comparable ? after.elapsedMs - before.elapsedMs : null
      };
    });
  }

  function updateSuiteOutput() {
    const lines = [];
    if (baseline) lines.push(...summarizeRun('补丁前', baseline), '');
    if (candidate) lines.push(...summarizeRun('补丁后', candidate), '');
    if (baseline && candidate) {
      const changes = differences();
      if (!changes) lines.push('两次集合内容不同，请使用同一版本集合重新运行。');
      else for (const row of changes) {
        const pass = (row.beforePass ? '通过' : '失败') + ' → ' + (row.afterPass ? '通过' : '失败');
        if (!row.comparable) lines.push(row.name + ' · ' + pass + ' · 响应不可比较');
        else lines.push(row.name + ' · ' + pass + ' · 状态码' + (row.statusChanged ? '变化' : '相同')
          + ' · 响应体' + (row.responseChanged ? '变化' : '相同')
          + ' · 耗时 ' + (row.elapsedDeltaMs > 0 ? '+' : '') + row.elapsedDeltaMs + ' ms');
      }
    }
    view('SuiteOutput').textContent = lines.join('\n');
  }

  async function runSuite(side) {
    view('SuiteStatus').textContent = '正在校验用例…';
    try {
      const result = await invoke('suite', { action: 'suite', ...selectedTarget(), port: view('Ports').value,
        suite: parseSuite(), credentials: view('SuiteCredentials').value },
      (kind, value) => { if (kind === 'status') view('SuiteStatus').textContent = value; });
      if (side === 'before') baseline = result;
      else candidate = result;
      updateSuiteOutput();
      view('SuiteStatus').textContent = result.results.filter(successful).length + ' / ' + result.results.length + ' 用例通过';
    } catch (error) {
      const canceled = error.message.includes('操作已取消');
      view('SuiteStatus').textContent = canceled ? '已停止运行' : '集合运行失败';
      if (!canceled) throw error;
    } finally {
      syncControls();
    }
  }

  bind('RunBaseline', () => runSuite('before'));
  bind('RunCandidate', () => runSuite('after'));
  bind('CancelSuite', () => cancel('suite'));

  view('ProfileType').addEventListener('change', () => { view('ProfileEventField').hidden = view('ProfileType').value !== 'flame'; });
  bind('Profile', async () => {
    view('ProfileStatus').textContent = '正在启动采集…';
    try {
      const result = await invoke('profile', {
        action: 'profile', ...selectedTarget(), type: view('ProfileType').value,
        event: view('ProfileEvent').value, seconds: view('ProfileSeconds').value
      }, (kind, value) => { if (kind === 'status') view('ProfileStatus').textContent = value; });
      profiles[result.type] = { ...result, capturedAt: new Date().toISOString() };
      view('ProfileStatus').textContent = '采集完成';
      view('ProfileOutput').textContent = Object.values(profiles)
        .map((item) => item.label + ' · ' + item.duration + ' 秒 · ' + item.size + ' 字节\n' + item.filename).join('\n\n');
    } catch (error) {
      const canceled = error.message.includes('操作已取消');
      view('ProfileStatus').textContent = canceled ? '采集已取消' : '采集失败';
      if (!canceled) throw error;
    } finally {
      syncControls();
    }
  });
  bind('CancelProfile', async () => {
    await cancel('profile');
    view('ProfileStatus').textContent = '等待 JVM 内的限时采集结束…';
  });
  bind('SaveJfr', () => bridge.saveRuntimeProfile('jfr'));
  bind('SaveFlame', () => bridge.saveRuntimeProfile('flame'));

  function reportRun(run) {
    if (!run) return undefined;
    return {
      target: run.target, capturedAt: run.capturedAt, suiteSha256: run.suiteSha256,
      results: run.results.map(({ name, method, status, elapsedMs, size, assertions, error }) =>
        ({ name, method, status, elapsedMs, size, assertions, transportFailed: Boolean(error) }))
    };
  }

  bind('Report', async () => {
    const entry = comparison?.entries[comparison?.selected];
    const result = {
      format: 'jarpatch-runtime-report', version: 1, createdAt: new Date().toISOString(),
      request: lastRequest, logCorrelation: logLink,
      classComparison: comparison ? { target: dump.target, className: dump.className, loader: dump.loader, archive: comparison.archive,
        runtimeSha256: comparison.runtimeSha256, selected: entry, identical: comparison.identical } : undefined,
      baseline: reportRun(baseline), candidate: reportRun(candidate), differences: differences(),
      profiles: Object.values(profiles)
    };
    const file = await bridge.saveDiagnostic('jarpatch-runtime-report.json', JSON.stringify(result, null, 2));
    if (file) view('Status').textContent = '诊断报告已导出';
  });

  fillOptions(view('ComparisonEntry'), [], String, String, '暂无包内候选');
  return { sync, resetComparison, resetRequest, setRequest, clearCredentials: () => { view('SuiteCredentials').value = ''; } };
};
