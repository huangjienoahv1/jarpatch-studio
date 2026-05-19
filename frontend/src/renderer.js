const API_BASE = 'http://127.0.0.1:18765';
const TOAST_AUTO_CLOSE_MS = 3200;
const NOTICE_TYPE_SUCCESS = 'success';
const NOTICE_TYPE_ERROR = 'error';
const NOTICE_TYPE_INFO = 'info';
const MESSAGE_OPEN_CANCELED = '已取消打开文件';
const MESSAGE_INSPECTING = '正在解析 pom.xml 和嵌套 Jar';
const MESSAGE_INSPECT_FAILED = '预解析失败';
const MESSAGE_IMPORT_SELECTION_CANCELED = '已取消导入';
const MESSAGE_IMPORTING = '正在导入文件';
const MESSAGE_IMPORT_SUCCESS = '导入完成';
const MESSAGE_IMPORT_FAILED = '导入失败';
const MESSAGE_FILE_OPEN_FAILED = '打开文件失败';
const MESSAGE_SEARCH_KEYWORD_EMPTY = '请输入搜索关键词';
const MESSAGE_SEARCH_SUCCESS = '搜索完成';
const MESSAGE_SEARCH_FAILED = '搜索失败';
const MESSAGE_SAVE_SUCCESS = '保存完成';
const MESSAGE_SAVE_FAILED = '保存失败';
const MESSAGE_ANALYZE_SUCCESS = '分析完成';
const MESSAGE_ANALYZE_FAILED = '分析失败';
const MESSAGE_COMPILE_SUCCESS = '编译完成';
const MESSAGE_COMPILE_FAILED = '编译失败';
const MESSAGE_EXPORT_CANCELED = '已取消导出';
const MESSAGE_EXPORT_SUCCESS = '导出完成';
const MESSAGE_EXPORT_FAILED = '导出失败';
const MESSAGE_PROJECT_HISTORY_EMPTY = '暂无项目历史';
const MESSAGE_PROJECT_HISTORY_DELETE = '删除';
const MESSAGE_PROJECT_HISTORY_DELETE_CONFIRM_PREFIX = '确认从项目历史删除“';
const MESSAGE_PROJECT_HISTORY_DELETE_CONFIRM_SUFFIX = '”？这只会移除历史记录，不会删除本地工作区文件。';
const MESSAGE_PROJECT_HISTORY_DELETE_SUCCESS = '项目历史已删除';
const MESSAGE_PROJECT_HISTORY_DELETE_FAILED = '项目历史删除失败';
const MESSAGE_PROJECT_NOT_OPENED = '未打开项目';
const MESSAGE_PROJECT_OPEN_TIP = '请选择一个 Jar 或 War 文件开始';
const MESSAGE_FILE_TREE_EMPTY = '暂无文件';
const MESSAGE_ACTIVE_FILE_EMPTY = '未选择文件';
const MESSAGE_ACTIVE_FILE_TIP = '请选择左侧可编辑文件';
const MESSAGE_ANALYSIS_EMPTY = '执行分析后显示包结构、依赖和导出风险';
const MESSAGE_ANALYSIS_NOT_RUN = '未分析';
const MESSAGE_ANALYSIS_NO_RISK = '无风险';
const MESSAGE_ANALYSIS_RISK_COUNT_SUFFIX = '项风险';
const MESSAGE_FILE_TREE_LOADING = '正在加载文件树';
const BUTTON_TEXT_ANALYZE = '分析';
const BUTTON_TEXT_COMPILE = '编译';
const BUTTON_TEXT_EXPORT = '导出';
const BUTTON_TEXT_COMPILE_RUNNING = '编译中...';
const BUTTON_TEXT_EXPORT_RUNNING = '导出中...';
const STORAGE_KEY_TREE_PANEL_WIDTH = 'jarpatch.treePanelWidth';
const TREE_PANEL_DEFAULT_WIDTH = 380;
const TREE_PANEL_MIN_WIDTH = 280;
const TREE_PANEL_MAX_WIDTH = 760;
const TREE_PANEL_EDITOR_MIN_WIDTH = 560;
const TREE_PANEL_EDITOR_ANALYSIS_OPEN_MIN_WIDTH = 520;
const TREE_RESIZER_WIDTH = 8;
const ANALYSIS_TOGGLE_WIDTH = 56;
const ANALYSIS_PANEL_WIDTH = 330;
const NUMBER_PARSE_RADIX = 10;
const CHINA_TIME_ZONE = 'Asia/Shanghai';
const CHINA_LOCALE = 'zh-CN';
const PROJECT_TIME_SEPARATOR = ' · ';
const CHINA_TIME_PATTERN = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;
const FULL_TIME_FORMAT_OPTIONS = {
  timeZone: CHINA_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
};
const LOG_TIME_FORMAT_OPTIONS = {
  timeZone: CHINA_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
};
const MESSAGE_DECOMPILE_SELECTED_SUFFIX = '个 Jar 将被反编译';
const MESSAGE_DECOMPILE_NO_CANDIDATE = '未发现可选择的嵌套 Jar，将只反编译主 classes';
const MESSAGE_POM_MODULE_EMPTY = 'pom.xml 未声明 modules，已按包名前缀给出推荐项';

const state = {
  currentProject: null,
  currentFilePath: null,
  analysisOpen: false,
  analysisReport: null,
  currentInspection: null,
  currentTree: null,
  treePanelWidth: TREE_PANEL_DEFAULT_WIDTH,
  expandedPaths: new Set(['', 'sources', 'extracted'])
};

const elements = {
  openArchiveBtn: document.getElementById('openArchiveBtn'),
  projectList: document.getElementById('projectList'),
  currentProjectName: document.getElementById('currentProjectName'),
  currentProjectMeta: document.getElementById('currentProjectMeta'),
  analyzeBtn: document.getElementById('analyzeBtn'),
  compileBtn: document.getElementById('compileBtn'),
  exportBtn: document.getElementById('exportBtn'),
  contentGrid: document.getElementById('contentGrid'),
  treeResizeHandle: document.getElementById('treeResizeHandle'),
  fileTree: document.getElementById('fileTree'),
  searchInput: document.getElementById('searchInput'),
  searchBtn: document.getElementById('searchBtn'),
  searchResults: document.getElementById('searchResults'),
  activeFileName: document.getElementById('activeFileName'),
  activeFileKind: document.getElementById('activeFileKind'),
  saveBtn: document.getElementById('saveBtn'),
  editor: document.getElementById('editor'),
  analysisToggleBtn: document.getElementById('analysisToggleBtn'),
  analysisCloseBtn: document.getElementById('analysisCloseBtn'),
  analysisBadge: document.getElementById('analysisBadge'),
  analysisPanel: document.getElementById('analysisPanel'),
  analysisResult: document.getElementById('analysisResult'),
  logOutput: document.getElementById('logOutput'),
  notificationArea: document.getElementById('notificationArea'),
  decompileDialog: document.getElementById('decompileDialog'),
  decompileDialogMeta: document.getElementById('decompileDialogMeta'),
  decompileDialogSummary: document.getElementById('decompileDialogSummary'),
  decompileCandidateList: document.getElementById('decompileCandidateList'),
  selectRecommendedBtn: document.getElementById('selectRecommendedBtn'),
  selectAllJarsBtn: document.getElementById('selectAllJarsBtn'),
  clearJarSelectionBtn: document.getElementById('clearJarSelectionBtn'),
  cancelDecompileBtn: document.getElementById('cancelDecompileBtn'),
  confirmDecompileBtn: document.getElementById('confirmDecompileBtn')
};

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });
  const body = await response.json();
  if (!body.success) {
    throw new Error(body.message || '操作失败');
  }
  return body.data;
}

async function loadProjects() {
  try {
    const projects = await api('/api/projects');
    renderProjects(projects);
  } catch (error) {
    appendLog(`读取项目历史失败：${error.message}`);
  }
}

function renderProjects(projects) {
  elements.projectList.innerHTML = '';
  if (!projects.length) {
    elements.projectList.innerHTML = `<div class="empty">${MESSAGE_PROJECT_HISTORY_EMPTY}</div>`;
    return;
  }
  projects.forEach((project) => {
    const item = document.createElement('div');
    item.className = `project-item ${state.currentProject && state.currentProject.id === project.id ? 'active' : ''}`;

    const selectButton = document.createElement('button');
    selectButton.className = 'project-main';
    selectButton.innerHTML = `<strong>${escapeHtml(project.name)}</strong><span>${project.packageType}${PROJECT_TIME_SEPARATOR}${escapeHtml(formatProjectTime(project.updatedAt))}</span>`;
    selectButton.addEventListener('click', () => selectProject(project));

    const deleteButton = document.createElement('button');
    deleteButton.className = 'project-delete';
    deleteButton.type = 'button';
    deleteButton.title = MESSAGE_PROJECT_HISTORY_DELETE;
    deleteButton.setAttribute('aria-label', `${MESSAGE_PROJECT_HISTORY_DELETE}：${project.name}`);
    deleteButton.textContent = MESSAGE_PROJECT_HISTORY_DELETE;
    deleteButton.addEventListener('click', () => deleteProjectHistory(project));

    item.appendChild(selectButton);
    item.appendChild(deleteButton);
    elements.projectList.appendChild(item);
  });
}

async function selectProject(project) {
  state.currentProject = project;
  state.currentFilePath = null;
  state.currentTree = null;
  elements.currentProjectName.textContent = project.name;
  elements.currentProjectMeta.textContent = `${project.packageType} · ${project.workspacePath}`;
  restoreProjectActionButtons();
  elements.searchBtn.disabled = false;
  elements.saveBtn.disabled = true;
  elements.editor.value = '';
  elements.editor.disabled = true;
  elements.activeFileName.textContent = '未选择文件';
  elements.activeFileKind.textContent = '请选择左侧可编辑文件';
  resetAnalysisPanel();
  await loadTree(project.id);
  await loadProjects();
}

async function deleteProjectHistory(project) {
  const confirmed = window.confirm(`${MESSAGE_PROJECT_HISTORY_DELETE_CONFIRM_PREFIX}${project.name}${MESSAGE_PROJECT_HISTORY_DELETE_CONFIRM_SUFFIX}`);
  if (!confirmed) {
    return;
  }
  try {
    await api(`/api/projects/${project.id}`, { method: 'DELETE' });
    if (state.currentProject && state.currentProject.id === project.id) {
      resetCurrentProject();
    }
    await loadProjects();
    notify(`${MESSAGE_PROJECT_HISTORY_DELETE_SUCCESS}：${project.name}`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_PROJECT_HISTORY_DELETE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

function resetCurrentProject() {
  state.currentProject = null;
  state.currentFilePath = null;
  state.currentTree = null;
  elements.currentProjectName.textContent = MESSAGE_PROJECT_NOT_OPENED;
  elements.currentProjectMeta.textContent = MESSAGE_PROJECT_OPEN_TIP;
  restoreProjectActionButtons();
  elements.searchBtn.disabled = true;
  elements.saveBtn.disabled = true;
  elements.searchInput.value = '';
  elements.searchResults.innerHTML = '';
  elements.fileTree.classList.add('empty');
  elements.fileTree.textContent = MESSAGE_FILE_TREE_EMPTY;
  elements.editor.value = '';
  elements.editor.disabled = true;
  elements.activeFileName.textContent = MESSAGE_ACTIVE_FILE_EMPTY;
  elements.activeFileKind.textContent = MESSAGE_ACTIVE_FILE_TIP;
  resetAnalysisPanel();
}

async function loadTree(projectId) {
  try {
    elements.fileTree.classList.add('empty');
    elements.fileTree.textContent = MESSAGE_FILE_TREE_LOADING;
    const tree = await api(`/api/projects/${projectId}/tree`);
    state.currentTree = tree;
    renderCurrentTree(false);
  } catch (error) {
    elements.fileTree.innerHTML = `<div class="empty">文件树加载失败：${escapeHtml(error.message)}</div>`;
  }
}

function renderCurrentTree(keepScroll) {
  if (!state.currentTree) {
    return;
  }
  const scrollTop = keepScroll ? elements.fileTree.scrollTop : 0;
  const scrollLeft = keepScroll ? elements.fileTree.scrollLeft : 0;
  const fragment = document.createDocumentFragment();
  fragment.appendChild(renderTreeNode(state.currentTree));
  elements.fileTree.classList.remove('empty');
  elements.fileTree.innerHTML = '';
  elements.fileTree.appendChild(fragment);
  elements.fileTree.scrollTop = scrollTop;
  elements.fileTree.scrollLeft = scrollLeft;
}

function renderTreeNode(node) {
  const wrapper = document.createElement('div');
  const hasChildren = node.children && node.children.length;
  const expanded = state.expandedPaths.has(node.path);
  wrapper.className = `tree-node ${hasChildren && !expanded ? 'collapsed' : ''}`;

  const label = document.createElement('button');
  label.className = `tree-label ${node.editable ? '' : 'disabled'}`;
  const toggle = hasChildren ? (expanded ? '▾' : '▸') : '';
  label.innerHTML = `<span class="tree-toggle">${toggle}</span>${escapeHtml(node.name)}`;
  label.addEventListener('click', () => handleTreeClick(node));
  wrapper.appendChild(label);

  if (node.children && node.children.length && expanded) {
    const children = document.createElement('div');
    children.className = 'tree-children';
    node.children.forEach((child) => children.appendChild(renderTreeNode(child)));
    wrapper.appendChild(children);
  }
  return wrapper;
}

function handleTreeClick(node) {
  if (node.children && node.children.length) {
    if (state.expandedPaths.has(node.path)) {
      state.expandedPaths.delete(node.path);
    } else {
      state.expandedPaths.add(node.path);
    }
    renderCurrentTree(true);
    return;
  }
  if (node.editable) {
    openFile(node);
  }
}

async function openFile(node) {
  if (!state.currentProject) {
    return;
  }
  try {
    const content = await api(`/api/projects/${state.currentProject.id}/files/content?path=${encodeURIComponent(node.path)}`);
    state.currentFilePath = node.path;
    elements.editor.disabled = false;
    elements.editor.value = content;
    elements.saveBtn.disabled = false;
    elements.activeFileName.textContent = node.path;
    elements.activeFileKind.textContent = node.kind;
  } catch (error) {
    notify(`${MESSAGE_FILE_OPEN_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

async function searchProject() {
  if (!state.currentProject) {
    return;
  }
  const keyword = elements.searchInput.value.trim();
  if (!keyword) {
    elements.searchResults.innerHTML = '';
    notify(MESSAGE_SEARCH_KEYWORD_EMPTY, NOTICE_TYPE_INFO);
    return;
  }
  try {
    const results = await api(`/api/projects/${state.currentProject.id}/search?keyword=${encodeURIComponent(keyword)}`);
    renderSearchResults(results);
    notify(`${MESSAGE_SEARCH_SUCCESS}：${keyword}，${results.length} 条结果`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_SEARCH_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

function renderSearchResults(results) {
  elements.searchResults.innerHTML = '';
  if (!results.length) {
    elements.searchResults.innerHTML = '<div class="empty">没有搜索结果</div>';
    return;
  }
  results.forEach((result) => {
    const button = document.createElement('button');
    button.className = 'search-result';
    button.innerHTML = `<strong>${escapeHtml(result.path)}:${result.lineNumber}</strong><span>${escapeHtml(result.preview)}</span>`;
    button.addEventListener('click', () => openSearchResult(result));
    elements.searchResults.appendChild(button);
  });
}

async function openSearchResult(result) {
  expandPathParents(result.path);
  if (state.currentTree) {
    renderCurrentTree(true);
  } else {
    await loadTree(state.currentProject.id);
  }
  await openFile({
    path: result.path,
    kind: 'SEARCH_RESULT',
    editable: true
  });
  focusEditorLine(result.lineNumber);
}

function expandPathParents(path) {
  const parts = path.split('/');
  let current = '';
  for (let index = 0; index < parts.length - 1; index++) {
    current = current ? `${current}/${parts[index]}` : parts[index];
    state.expandedPaths.add(current);
  }
}

function focusEditorLine(lineNumber) {
  const lines = elements.editor.value.split('\n');
  let position = 0;
  for (let index = 0; index < Math.max(lineNumber - 1, 0) && index < lines.length; index++) {
    position += lines[index].length + 1;
  }
  elements.editor.focus();
  elements.editor.setSelectionRange(position, position);
}

async function saveCurrentFile() {
  if (!state.currentProject || !state.currentFilePath) {
    return;
  }
  try {
    await api(`/api/projects/${state.currentProject.id}/files/content`, {
      method: 'PUT',
      body: JSON.stringify({
        path: state.currentFilePath,
        content: elements.editor.value
      })
    });
    notify(`${MESSAGE_SAVE_SUCCESS}：${state.currentFilePath}`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_SAVE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

async function importArchive() {
  const filePath = await window.jarPatch.openArchive();
  if (!filePath) {
    notify(MESSAGE_OPEN_CANCELED, NOTICE_TYPE_INFO);
    return;
  }
  notify(`${MESSAGE_INSPECTING}：${filePath}`, NOTICE_TYPE_INFO);
  try {
    const inspection = await api('/api/projects/inspect', {
      method: 'POST',
      body: JSON.stringify({ filePath })
    });
    const selectedNestedJars = await openDecompileDialog(inspection);
    if (!selectedNestedJars) {
      notify(MESSAGE_IMPORT_SELECTION_CANCELED, NOTICE_TYPE_INFO);
      return;
    }
    notify(`${MESSAGE_IMPORTING}：${filePath}`, NOTICE_TYPE_INFO);
    const project = await api('/api/projects/import', {
      method: 'POST',
      body: JSON.stringify({ filePath, selectedNestedJars })
    });
    notify(`${MESSAGE_IMPORT_SUCCESS}：${project.name}`, NOTICE_TYPE_SUCCESS);
    await selectProject(project);
  } catch (error) {
    notify(`${MESSAGE_IMPORT_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

function openDecompileDialog(inspection) {
  state.currentInspection = cloneInspection(inspection);
  renderDecompileDialog();
  elements.decompileDialog.classList.add('open');
  elements.decompileDialog.setAttribute('aria-hidden', 'false');
  return new Promise((resolve) => {
    const cleanup = () => {
      elements.confirmDecompileBtn.removeEventListener('click', confirmHandler);
      elements.cancelDecompileBtn.removeEventListener('click', cancelHandler);
      elements.decompileDialog.classList.remove('open');
      elements.decompileDialog.setAttribute('aria-hidden', 'true');
    };
    const confirmHandler = () => {
      const selected = state.currentInspection.candidates
        .filter((candidate) => candidate.selected)
        .map((candidate) => candidate.path);
      cleanup();
      resolve(selected);
    };
    const cancelHandler = () => {
      cleanup();
      resolve(null);
    };
    elements.confirmDecompileBtn.addEventListener('click', confirmHandler);
    elements.cancelDecompileBtn.addEventListener('click', cancelHandler);
  });
}

function cloneInspection(inspection) {
  return {
    ...inspection,
    candidates: (inspection.candidates || []).map((candidate) => ({ ...candidate }))
  };
}

function renderDecompileDialog() {
  const inspection = state.currentInspection;
  const candidates = inspection.candidates || [];
  const selectedCount = candidates.filter((candidate) => candidate.selected).length;
  elements.decompileDialogMeta.textContent = `${inspection.packageType} · ${inspection.filePath}`;
  elements.decompileDialogSummary.textContent = candidates.length
    ? `pom.xml 模块：${renderPomModuleSummary(inspection.pomModules)}；当前已选择 ${selectedCount} ${MESSAGE_DECOMPILE_SELECTED_SUFFIX}`
    : MESSAGE_DECOMPILE_NO_CANDIDATE;
  elements.decompileCandidateList.innerHTML = '';
  if (!candidates.length) {
    elements.decompileCandidateList.innerHTML = `<div class="empty">${MESSAGE_DECOMPILE_NO_CANDIDATE}</div>`;
    return;
  }
  candidates.forEach((candidate, index) => {
    elements.decompileCandidateList.appendChild(renderCandidateItem(candidate, index));
  });
}

function renderPomModuleSummary(modules) {
  if (!modules || !modules.length) {
    return MESSAGE_POM_MODULE_EMPTY;
  }
  return modules.slice(0, 8).join('、') + (modules.length > 8 ? ` 等 ${modules.length} 个` : '');
}

function renderCandidateItem(candidate, index) {
  const item = document.createElement('label');
  item.className = 'candidate-item';
  item.innerHTML = `
    <input type="checkbox" ${candidate.selected ? 'checked' : ''}>
    <div>
      <div class="candidate-name">${escapeHtml(candidate.name)}</div>
      <div class="candidate-path">${escapeHtml(candidate.path)}</div>
      <div class="candidate-reason">${escapeHtml(candidate.reason)}</div>
    </div>
    <div class="candidate-count">${candidate.classCount} class</div>
  `;
  const checkbox = item.querySelector('input');
  checkbox.addEventListener('change', () => {
    state.currentInspection.candidates[index].selected = checkbox.checked;
    renderDecompileDialog();
  });
  return item;
}

function selectRecommendedCandidates() {
  if (!state.currentInspection) {
    return;
  }
  state.currentInspection.candidates.forEach((candidate) => {
    candidate.selected = candidate.reason !== '等待手动选择';
  });
  renderDecompileDialog();
}

function setAllCandidatesSelected(selected) {
  if (!state.currentInspection) {
    return;
  }
  state.currentInspection.candidates.forEach((candidate) => {
    candidate.selected = selected;
  });
  renderDecompileDialog();
}

async function analyzeProject() {
  if (!state.currentProject) {
    return;
  }
  try {
    const report = await api(`/api/projects/${state.currentProject.id}/analyze`, { method: 'POST' });
    renderAnalysis(report);
    setAnalysisPanelOpen(true);
    notify(MESSAGE_ANALYZE_SUCCESS, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_ANALYZE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

function renderAnalysis(report) {
  state.analysisReport = report;
  elements.analysisResult.classList.remove('empty');
  const risks = report.risks.length
    ? report.risks.map((risk) => `<div class="risk ${risk.level}"><strong>${escapeHtml(risk.title)}</strong><br>${escapeHtml(risk.detail)}</div>`).join('')
    : '<div class="empty">未发现导出风险</div>';
  updateAnalysisBadge(report);
  elements.analysisResult.innerHTML = `
    <div class="metric"><span>包类型</span><strong>${escapeHtml(report.packageType)}</strong></div>
    <div class="metric"><span>入口类</span><strong>${escapeHtml(report.entryClass || '未识别')}</strong></div>
    <div class="metric"><span>Manifest</span><strong>${report.manifestExists ? '存在' : '不存在'}</strong></div>
    <div class="metric"><span>Class数量</span><strong>${report.classCount}</strong></div>
    <div class="metric"><span>依赖数量</span><strong>${report.dependencyCount}</strong></div>
    <div class="metric"><span>修改文件</span><strong>${report.modifiedFiles.length}</strong></div>
    ${risks}
  `;
}

function setAnalysisPanelOpen(open) {
  state.analysisOpen = open;
  elements.contentGrid.classList.toggle('analysis-open', open);
  elements.analysisPanel.setAttribute('aria-hidden', String(!open));
  elements.analysisToggleBtn.setAttribute('aria-expanded', String(open));
  setTreePanelWidth(state.treePanelWidth, true);
}

function resetAnalysisPanel() {
  state.analysisReport = null;
  elements.analysisResult.classList.add('empty');
  elements.analysisResult.textContent = MESSAGE_ANALYSIS_EMPTY;
  elements.analysisBadge.textContent = MESSAGE_ANALYSIS_NOT_RUN;
  setAnalysisPanelOpen(false);
}

function updateAnalysisBadge(report) {
  const riskCount = report.risks.length;
  elements.analysisBadge.textContent = riskCount ? `${riskCount} ${MESSAGE_ANALYSIS_RISK_COUNT_SUFFIX}` : MESSAGE_ANALYSIS_NO_RISK;
}

async function compileProject() {
  if (!state.currentProject) {
    return;
  }
  setActionButtonRunning(elements.compileBtn, BUTTON_TEXT_COMPILE_RUNNING);
  try {
    const result = await api(`/api/projects/${state.currentProject.id}/compile`, { method: 'POST' });
    notify(`${MESSAGE_COMPILE_SUCCESS}：${result.changedFiles.length} 个文件`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_COMPILE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  } finally {
    restoreProjectActionButtons();
  }
}

async function exportProject() {
  if (!state.currentProject) {
    return;
  }
  const outputPath = await window.jarPatch.saveArchive(state.currentProject.name.replace(/(\.jar|\.war)$/i, '-patched$1'));
  if (!outputPath) {
    notify(MESSAGE_EXPORT_CANCELED, NOTICE_TYPE_INFO);
    return;
  }
  setActionButtonRunning(elements.exportBtn, BUTTON_TEXT_EXPORT_RUNNING);
  try {
    const result = await api(`/api/projects/${state.currentProject.id}/export`, {
      method: 'POST',
      body: JSON.stringify({ outputPath })
    });
    notify(`${MESSAGE_EXPORT_SUCCESS}：${result.outputPath}`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_EXPORT_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  } finally {
    restoreProjectActionButtons();
  }
}

function setActionButtonRunning(button, runningText) {
  button.textContent = runningText;
  button.classList.add('busy');
  elements.analyzeBtn.disabled = true;
  elements.compileBtn.disabled = true;
  elements.exportBtn.disabled = true;
}

function restoreProjectActionButtons() {
  const projectOpened = Boolean(state.currentProject);
  elements.analyzeBtn.textContent = BUTTON_TEXT_ANALYZE;
  elements.compileBtn.textContent = BUTTON_TEXT_COMPILE;
  elements.exportBtn.textContent = BUTTON_TEXT_EXPORT;
  elements.analyzeBtn.disabled = !projectOpened;
  elements.compileBtn.disabled = !projectOpened;
  elements.exportBtn.disabled = !projectOpened;
  elements.compileBtn.classList.remove('busy');
  elements.exportBtn.classList.remove('busy');
}

function initializeTreePanelWidth() {
  const savedWidth = Number.parseInt(localStorage.getItem(STORAGE_KEY_TREE_PANEL_WIDTH), NUMBER_PARSE_RADIX);
  const initialWidth = Number.isNaN(savedWidth) ? TREE_PANEL_DEFAULT_WIDTH : savedWidth;
  setTreePanelWidth(initialWidth, false);
}

function setTreePanelWidth(width, persist) {
  const nextWidth = clampTreePanelWidth(width);
  state.treePanelWidth = nextWidth;
  document.documentElement.style.setProperty('--tree-panel-width', `${nextWidth}px`);
  if (persist) {
    localStorage.setItem(STORAGE_KEY_TREE_PANEL_WIDTH, String(nextWidth));
  }
}

function clampTreePanelWidth(width) {
  const gridWidth = elements.contentGrid.getBoundingClientRect().width;
  const editorMinWidth = state.analysisOpen ? TREE_PANEL_EDITOR_ANALYSIS_OPEN_MIN_WIDTH : TREE_PANEL_EDITOR_MIN_WIDTH;
  const rightPanelWidth = state.analysisOpen ? ANALYSIS_PANEL_WIDTH : ANALYSIS_TOGGLE_WIDTH;
  const dynamicMaxWidth = gridWidth - editorMinWidth - rightPanelWidth - TREE_RESIZER_WIDTH;
  const maxWidth = Math.max(TREE_PANEL_MIN_WIDTH, Math.min(TREE_PANEL_MAX_WIDTH, dynamicMaxWidth));
  return Math.min(Math.max(width, TREE_PANEL_MIN_WIDTH), maxWidth);
}

function beginTreeResize(event) {
  event.preventDefault();
  elements.treeResizeHandle.classList.add('active');
  document.body.classList.add('resizing-tree');
  document.addEventListener('pointermove', resizeTreePanel);
  document.addEventListener('pointerup', endTreeResize);
}

function resizeTreePanel(event) {
  const gridRect = elements.contentGrid.getBoundingClientRect();
  setTreePanelWidth(event.clientX - gridRect.left, false);
}

function endTreeResize() {
  elements.treeResizeHandle.classList.remove('active');
  document.body.classList.remove('resizing-tree');
  document.removeEventListener('pointermove', resizeTreePanel);
  document.removeEventListener('pointerup', endTreeResize);
  setTreePanelWidth(state.treePanelWidth, true);
}

function notify(message, type = NOTICE_TYPE_INFO) {
  appendLog(message);
  const item = document.createElement('div');
  item.className = `notification ${type}`;
  item.textContent = message;
  elements.notificationArea.appendChild(item);
  setTimeout(() => {
    item.classList.add('leaving');
    item.addEventListener('transitionend', () => item.remove(), { once: true });
  }, TOAST_AUTO_CLOSE_MS);
}

function appendLog(message) {
  const time = new Date().toLocaleTimeString(CHINA_LOCALE, LOG_TIME_FORMAT_OPTIONS);
  elements.logOutput.textContent += `[${time}] ${message}\n`;
  elements.logOutput.scrollTop = elements.logOutput.scrollHeight;
}

function formatProjectTime(value) {
  if (!value || CHINA_TIME_PATTERN.test(value)) {
    return value || '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat(CHINA_LOCALE, FULL_TIME_FORMAT_OPTIONS)
    .format(date)
    .replace(/\//g, '-');
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

elements.openArchiveBtn.addEventListener('click', importArchive);
elements.saveBtn.addEventListener('click', saveCurrentFile);
elements.analyzeBtn.addEventListener('click', analyzeProject);
elements.compileBtn.addEventListener('click', compileProject);
elements.exportBtn.addEventListener('click', exportProject);
elements.searchBtn.addEventListener('click', searchProject);
elements.treeResizeHandle.addEventListener('pointerdown', beginTreeResize);
elements.analysisToggleBtn.addEventListener('click', () => setAnalysisPanelOpen(!state.analysisOpen));
elements.analysisCloseBtn.addEventListener('click', () => setAnalysisPanelOpen(false));
elements.selectRecommendedBtn.addEventListener('click', selectRecommendedCandidates);
elements.selectAllJarsBtn.addEventListener('click', () => setAllCandidatesSelected(true));
elements.clearJarSelectionBtn.addEventListener('click', () => setAllCandidatesSelected(false));
elements.searchInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    searchProject();
  }
});

initializeTreePanelWidth();
loadProjects();
