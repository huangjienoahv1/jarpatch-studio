const API_BASE = 'http://127.0.0.1:18765';
const WS_BASE = API_BASE.replace(/^http/, 'ws');
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
const MESSAGE_FILE_BINARY_READONLY = '二进制文件只支持查看，不支持编辑';
const MESSAGE_FILE_SIGNATURE_READONLY = '签名文件只支持查看，不支持编辑';
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
const MESSAGE_TASK_IDLE = '未启动任务';
const MESSAGE_TASK_IDLE_DETAIL = '先创建任务再连接实时日志';
const MESSAGE_TASK_CONNECTING = '正在连接任务日志';
const MESSAGE_TASK_CONNECTED = '任务日志已连接';
const MESSAGE_TASK_CANCELING = '正在取消任务';
const MESSAGE_TASK_CANCELED = '任务已取消';
const MESSAGE_TASK_COMPLETED = '任务已完成';
const MESSAGE_TASK_FAILED = '任务已失败';
const MESSAGE_TASK_CANCEL_FAILED = '取消任务失败';
const MESSAGE_TASK_START_FAILED = '任务启动失败';
const MESSAGE_TASK_LOG_UNAVAILABLE = '任务日志连接失败';
const MESSAGE_TASK_LABEL_IMPORT = '导入';
const MESSAGE_TASK_LABEL_ANALYZE = '分析';
const MESSAGE_TASK_LABEL_COMPILE = '编译';
const MESSAGE_TASK_LABEL_EXPORT = '导出';
const TASK_TYPE_IMPORT = 'IMPORT';
const TASK_TYPE_ANALYZE = 'ANALYZE';
const TASK_TYPE_COMPILE = 'COMPILE';
const TASK_TYPE_EXPORT = 'EXPORT';
const MESSAGE_TASK_CANCEL_BUTTON = '取消任务';
const MESSAGE_TASK_PANEL_LABEL = '任务状态';
const MESSAGE_TASK_PANEL_READY = '等待开始';
const MESSAGE_TASK_PANEL_RUNNING = '执行中';
const MESSAGE_TASK_PANEL_DONE = '执行完成';
const MESSAGE_TASK_PANEL_FAILED = '执行失败';
const MESSAGE_TASK_PANEL_CANCELED = '已取消';
const MESSAGE_TASK_PROGRESS_PATTERN = /^\[(\d+)%\]\s*(.*)$/;
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
const TASK_LABEL_MAP = {
  IMPORT: MESSAGE_TASK_LABEL_IMPORT,
  ANALYZE: MESSAGE_TASK_LABEL_ANALYZE,
  COMPILE: MESSAGE_TASK_LABEL_COMPILE,
  EXPORT: MESSAGE_TASK_LABEL_EXPORT
};

const state = {
  currentProject: null,
  currentFilePath: null,
  analysisOpen: false,
  analysisReport: null,
  currentInspection: null,
  currentTree: null,
  currentTask: null,
  currentTaskSocket: null,
  currentTaskAbortController: null,
  currentTaskCancelRequested: false,
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
  taskStatusText: document.getElementById('taskStatusText'),
  taskStatusDetail: document.getElementById('taskStatusDetail'),
  cancelTaskBtn: document.getElementById('cancelTaskBtn'),
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

/**
 * 获取任务展示名称。
 *
 * @param taskType 任务类型码
 * @return 中文任务名称
 */
function getTaskLabel(taskType) {
  return TASK_LABEL_MAP[taskType] || taskType || MESSAGE_TASK_PANEL_LABEL;
}

/**
 * 判断是否为任务取消类错误。
 *
 * @param error 异常对象
 * @return 是取消错误时返回 true
 */
function isTaskCanceledError(error) {
  return Boolean(error) && (error.name === 'AbortError' || error.message === MESSAGE_TASK_CANCELED);
}

/**
 * 解析任务日志中的进度信息。
 *
 * @param message 任务日志文本
 * @return 进度和正文
 */
function parseTaskProgress(message) {
  const match = MESSAGE_TASK_PROGRESS_PATTERN.exec(message);
  if (!match) {
    return { progress: null, detail: message };
  }
  return {
    progress: Number.parseInt(match[1], NUMBER_PARSE_RADIX),
    detail: match[2] || message
  };
}

/**
 * 更新任务状态栏。
 *
 * @param title  标题文案
 * @param detail 详情文案
 * @param active 是否处于执行中
 */
function updateTaskPanel(title, detail, active) {
  elements.taskStatusText.textContent = title;
  elements.taskStatusDetail.textContent = detail;
  elements.cancelTaskBtn.disabled = !active || state.currentTaskCancelRequested;
}

/**
 * 恢复工作台可操作状态。
 */
function restoreWorkspaceControls() {
  elements.openArchiveBtn.disabled = false;
  elements.projectList.style.pointerEvents = '';
  elements.fileTree.style.pointerEvents = '';
  elements.searchResults.style.pointerEvents = '';
  elements.analysisToggleBtn.disabled = false;
  elements.analysisCloseBtn.disabled = false;
  elements.searchInput.disabled = !state.currentProject;
  elements.searchBtn.disabled = !state.currentProject;
  elements.saveBtn.disabled = !state.currentProject || !state.currentFilePath;
  elements.editor.disabled = !state.currentProject || !state.currentFilePath;
  elements.analyzeBtn.disabled = !state.currentProject;
  elements.compileBtn.disabled = !state.currentProject;
  elements.exportBtn.disabled = !state.currentProject;
  elements.compileBtn.classList.remove('busy');
  elements.exportBtn.classList.remove('busy');
  elements.compileBtn.textContent = BUTTON_TEXT_COMPILE;
  elements.exportBtn.textContent = BUTTON_TEXT_EXPORT;
  elements.analyzeBtn.textContent = BUTTON_TEXT_ANALYZE;
  elements.cancelTaskBtn.disabled = true;
}

/**
 * 锁定工作台控件，避免任务执行期间并发修改。
 */
function lockWorkspaceControls() {
  elements.openArchiveBtn.disabled = true;
  elements.projectList.style.pointerEvents = 'none';
  elements.fileTree.style.pointerEvents = 'none';
  elements.searchResults.style.pointerEvents = 'none';
  elements.analysisToggleBtn.disabled = true;
  elements.analysisCloseBtn.disabled = true;
  elements.searchInput.disabled = true;
  elements.searchBtn.disabled = true;
  elements.saveBtn.disabled = true;
  elements.editor.disabled = true;
  elements.analyzeBtn.disabled = true;
  elements.compileBtn.disabled = true;
  elements.exportBtn.disabled = true;
}

/**
 * 关闭当前任务 WebSocket。
 */
function closeTaskSocket() {
  if (state.currentTaskSocket) {
    state.currentTaskSocket.onopen = null;
    state.currentTaskSocket.onmessage = null;
    state.currentTaskSocket.onerror = null;
    state.currentTaskSocket.onclose = null;
    state.currentTaskSocket.close();
    state.currentTaskSocket = null;
  }
}

/**
 * 打开当前任务的实时日志 WebSocket。
 *
 * @param taskId 任务 ID
 * @return 连接完成后的 Promise
 */
function openTaskSocket(taskId) {
  closeTaskSocket();
  const socket = new WebSocket(`${WS_BASE}/ws/tasks/${taskId}`);
  state.currentTaskSocket = socket;
  return new Promise((resolve) => {
    let settled = false;
    const settle = (value) => {
      if (!settled) {
        settled = true;
        resolve(value);
      }
    };
    const taskLabel = state.currentTask ? getTaskLabel(state.currentTask.taskType) : MESSAGE_TASK_PANEL_LABEL;
    socket.onopen = () => {
      updateTaskPanel(`${taskLabel} · 0%`, MESSAGE_TASK_CONNECTED, true);
      settle(socket);
    };
    socket.onmessage = (event) => {
      const message = String(event.data || '');
      appendLog(message);
      const parsed = parseTaskProgress(message);
      if (parsed.progress == null) {
        elements.taskStatusDetail.textContent = parsed.detail;
        return;
      }
      elements.taskStatusText.textContent = `${taskLabel} · ${parsed.progress}%`;
      elements.taskStatusDetail.textContent = parsed.detail;
    };
    socket.onerror = () => {
      appendLog(MESSAGE_TASK_LOG_UNAVAILABLE);
      settle(null);
    };
    socket.onclose = () => settle(null);
    setTimeout(() => settle(null), 1500);
  });
}

/**
 * 开始一条可实时追踪的任务会话。
 *
 * @param task 任务记录
 */
function beginTaskSession(task) {
  state.currentTask = task;
  state.currentTaskAbortController = new AbortController();
  state.currentTaskCancelRequested = false;
  lockWorkspaceControls();
  updateTaskPanel(`${getTaskLabel(task.taskType)} · 0%`, task.message || MESSAGE_TASK_PANEL_READY, true);
}

/**
 * 结束当前任务会话。
 *
 * @param task    任务记录
 * @param status  最终状态文案
 * @param detail  最终详情文案
 */
function endTaskSession(task, status, detail) {
  const label = getTaskLabel(task.taskType);
  updateTaskPanel(`${label} · ${status}`, detail, false);
  closeTaskSocket();
  state.currentTaskAbortController = null;
  state.currentTaskCancelRequested = false;
  state.currentTask = null;
  restoreWorkspaceControls();
}

/**
 * 创建任务、连接实时日志并执行后续请求。
 *
 * @param taskType     任务类型
 * @param projectId    项目 ID，可为空
 * @param message      初始消息
 * @param requestRunner 具体业务请求
 * @return 业务请求结果
 */
async function executeTaskOperation(taskType, projectId, message, requestRunner) {
  const task = await api('/api/tasks', {
    method: 'POST',
    body: JSON.stringify({
      taskType,
      projectId,
      message
    })
  });
  beginTaskSession(task);
  await openTaskSocket(task.id);
  try {
    const result = await requestRunner(task, state.currentTaskAbortController.signal);
    endTaskSession(task, MESSAGE_TASK_PANEL_DONE, result && result.message ? result.message : MESSAGE_TASK_COMPLETED);
    return result;
  } catch (error) {
    if (isTaskCanceledError(error)) {
      endTaskSession(task, MESSAGE_TASK_PANEL_CANCELED, MESSAGE_TASK_CANCELED);
      notify(MESSAGE_TASK_CANCELED, NOTICE_TYPE_INFO);
      return null;
    }
    endTaskSession(task, MESSAGE_TASK_PANEL_FAILED, error.message || MESSAGE_TASK_FAILED);
    throw error;
  }
}

/**
 * 取消当前执行中的任务。
 */
async function cancelCurrentTask() {
  if (!state.currentTask) {
    return;
  }
  const task = state.currentTask;
  state.currentTaskCancelRequested = true;
  updateTaskPanel(`${getTaskLabel(task.taskType)} · ${MESSAGE_TASK_PANEL_CANCELED}`, MESSAGE_TASK_CANCELING, true);
  elements.cancelTaskBtn.disabled = true;
  if (state.currentTaskAbortController) {
    state.currentTaskAbortController.abort();
  }
  try {
    await api(`/api/tasks/${task.id}/cancel`, { method: 'POST' });
  } catch (error) {
    state.currentTaskCancelRequested = false;
    updateTaskPanel(`${getTaskLabel(task.taskType)} · ${MESSAGE_TASK_PANEL_RUNNING}`, `${MESSAGE_TASK_CANCEL_FAILED}：${error.message}`, true);
    notify(`${MESSAGE_TASK_CANCEL_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
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
  restoreWorkspaceControls();
  elements.searchInput.disabled = false;
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
  restoreWorkspaceControls();
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
    return;
  }
  if (node.kind === 'SIGNATURE') {
    notify(MESSAGE_FILE_SIGNATURE_READONLY, NOTICE_TYPE_INFO);
    return;
  }
  if (node.kind === 'BINARY') {
    notify(MESSAGE_FILE_BINARY_READONLY, NOTICE_TYPE_INFO);
    return;
  }
  notify(MESSAGE_FILE_OPEN_FAILED, NOTICE_TYPE_INFO);
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
    const project = await executeTaskOperation(TASK_TYPE_IMPORT, null, `${MESSAGE_IMPORTING}：${filePath}`, async (task, signal) => api('/api/projects/import', {
      method: 'POST',
      signal,
      body: JSON.stringify({ filePath, selectedNestedJars, taskId: task.id })
    }));
    if (project) {
      notify(`${MESSAGE_IMPORT_SUCCESS}：${project.name}`, NOTICE_TYPE_SUCCESS);
      await selectProject(project);
    }
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
    const report = await executeTaskOperation(TASK_TYPE_ANALYZE, state.currentProject.id, '开始分析包结构', (task, signal) => api(`/api/projects/${state.currentProject.id}/analyze`, {
      method: 'POST',
      signal,
      headers: {
        'X-Task-Id': task.id
      }
    }));
    if (report) {
      renderAnalysis(report);
      setAnalysisPanelOpen(true);
      notify(MESSAGE_ANALYZE_SUCCESS, NOTICE_TYPE_SUCCESS);
    }
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
    const result = await executeTaskOperation(TASK_TYPE_COMPILE, state.currentProject.id, '开始编译修改过的 Java 文件', (task, signal) => api(`/api/projects/${state.currentProject.id}/compile`, {
      method: 'POST',
      signal,
      headers: {
        'X-Task-Id': task.id
      }
    }));
    if (result) {
      notify(`${MESSAGE_COMPILE_SUCCESS}：${result.changedFiles.length} 个文件`, NOTICE_TYPE_SUCCESS);
    }
  } catch (error) {
    notify(`${MESSAGE_COMPILE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
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
    const result = await executeTaskOperation(TASK_TYPE_EXPORT, state.currentProject.id, `开始导出修改后的包: ${outputPath}`, (task, signal) => api(`/api/projects/${state.currentProject.id}/export`, {
      method: 'POST',
      signal,
      body: JSON.stringify({ outputPath, taskId: task.id })
    }));
    if (result) {
      notify(`${MESSAGE_EXPORT_SUCCESS}：${result.outputPath}`, NOTICE_TYPE_SUCCESS);
    }
  } catch (error) {
    notify(`${MESSAGE_EXPORT_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
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
  restoreWorkspaceControls();
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
elements.cancelTaskBtn.addEventListener('click', cancelCurrentTask);
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
restoreWorkspaceControls();
loadProjects();
