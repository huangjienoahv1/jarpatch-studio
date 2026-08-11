let API_BASE = '';
let WS_BASE = '';
let API_TOKEN = '';
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
const MESSAGE_SAVE_UNCHANGED = '文件内容未变化，未写入磁盘';
const MESSAGE_SAVE_FAILED = '保存失败';
const MESSAGE_UNICODE_PREVIEW_EMPTY = '当前文件没有可预览的中文 Unicode 转义';
const MESSAGE_ANALYZE_SUCCESS = '分析完成';
const MESSAGE_ANALYZE_FAILED = '分析失败';
const MESSAGE_DIFF_LOAD_FAILED = '读取差异失败';
const MESSAGE_DIFF_EMPTY = '当前工作区与导入基线一致，且没有已提交编译产物';
const MESSAGE_DIFF_EXPORT_CONFIRM = '请确认已查看源码、资源和 class 差异。是否继续选择导出路径？';
const MESSAGE_SIGNATURE_POLICY_CONFIRM = '是否移除原包中会因修改而失效的签名文件？\n\n选择“确定”：移除失效签名并导出未签名包。\n选择“取消”：保留签名；若包已有修改，后端会阻止导出。';
const MESSAGE_COMPILE_SUCCESS = '编译完成';
const MESSAGE_COMPILE_FAILED = '编译失败';
const MESSAGE_EXPORT_CANCELED = '已取消导出';
const MESSAGE_EXPORT_SUCCESS = '导出完成';
const MESSAGE_EXPORT_FAILED = '导出失败';
const MESSAGE_SETTINGS_LOADING = '正在读取 JDK 配置';
const MESSAGE_SETTINGS_LOAD_FAILED = '读取 JDK 配置失败';
const MESSAGE_SETTINGS_SAVE_FAILED = '保存 JDK 配置失败';
const MESSAGE_SETTINGS_SAVE_SUCCESS = 'JDK 配置已保存';
const MESSAGE_PROJECT_SETTINGS_LOAD_FAILED = '读取项目设置失败';
const MESSAGE_PROJECT_SETTINGS_SAVE_FAILED = '保存项目设置失败';
const MESSAGE_PROJECT_SETTINGS_SAVE_SUCCESS = '项目设置已保存';
const MESSAGE_WORKSPACE_CLEANUP_FAILED = '工作区清理失败';
const MESSAGE_WORKSPACE_CLEANUP_SUCCESS = '工作区已清理，项目历史仍然保留';
const MESSAGE_WORKSPACE_CLEANUP_CONFIRM = '确认清理以下工作区吗？此操作会删除工作区文件，但保留项目历史。';
const MESSAGE_ERROR_GUIDE_LOAD_FAILED = '读取错误向导失败';
const MESSAGE_UNSAVED_CHANGES_CONFIRM = '当前文件有未保存修改，确认放弃这些修改吗？';
const MESSAGE_UNSAVED_CHANGES = '未保存';
const MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION = '当前文件有未保存修改，请先保存再执行该操作';
const MESSAGE_WORKSPACE_CLEANED_MARKER = '工作区已清理';
const MESSAGE_ERROR_GUIDE_LOADING = '正在读取排错向导...';
const EMPTY_JSON_OBJECT = '{}';
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
const MESSAGE_SETTINGS_SUMMARY = '保存后编译服务会优先使用这里配置的 JDK 路径';
const MESSAGE_SETTINGS_TIP = '请选择 JDK 安装目录，保存时会校验 bin 目录下的 javac。';
const MESSAGE_SETTINGS_PLACEHOLDER = '例如：C:\\Program Files\\Java\\jdk-17';
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
const BUTTON_TEXT_UNICODE_PREVIEW = '中文预览';
const BUTTON_TEXT_UNICODE_SOURCE = '返回原文';
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
const UNICODE_HEX_RADIX = 16;
const UNICODE_ESCAPE_HEX_LENGTH = 4;
const BACKSLASH_ESCAPE_PAIR_LENGTH = 2;
const UNICODE_ESCAPE_HEX_PATTERN = new RegExp(`^[0-9a-fA-F]{${UNICODE_ESCAPE_HEX_LENGTH}}$`);
const CHINESE_READABLE_RANGES = [
  [0x3400, 0x4DBF],
  [0x4E00, 0x9FFF],
  [0xF900, 0xFAFF],
  [0x3000, 0x303F],
  [0xFF00, 0xFFEF]
];
const BYTES_PER_MEGABYTE = 1024 * 1024;
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
  currentFileHash: null,
  currentFileOriginalContent: null,
  unicodePreviewOpen: false,
  analysisOpen: false,
  analysisReport: null,
  diffReport: null,
  currentInspection: null,
  currentTree: null,
  currentTask: null,
  currentTaskSocket: null,
  currentTaskAbortController: null,
  currentTaskCancelRequested: false,
  jdkSettings: null,
  projectSettings: null,
  treePanelWidth: TREE_PANEL_DEFAULT_WIDTH,
  expandedPaths: new Set(['', 'sources', 'extracted'])
};

const elements = {
  openArchiveBtn: document.getElementById('openArchiveBtn'),
  projectList: document.getElementById('projectList'),
  currentProjectName: document.getElementById('currentProjectName'),
  currentProjectMeta: document.getElementById('currentProjectMeta'),
  analyzeBtn: document.getElementById('analyzeBtn'),
  diffBtn: document.getElementById('diffBtn'),
  compileBtn: document.getElementById('compileBtn'),
  exportBtn: document.getElementById('exportBtn'),
  settingsBtn: document.getElementById('settingsBtn'),
  projectSettingsBtn: document.getElementById('projectSettingsBtn'),
  cleanupWorkspaceBtn: document.getElementById('cleanupWorkspaceBtn'),
  errorGuideBtn: document.getElementById('errorGuideBtn'),
  settingsDialog: document.getElementById('settingsDialog'),
  settingsDialogSummary: document.getElementById('settingsDialogSummary'),
  jdkHomeInput: document.getElementById('jdkHomeInput'),
  browseJdkBtn: document.getElementById('browseJdkBtn'),
  jdkSettingsTip: document.getElementById('jdkSettingsTip'),
  jdkSettingsStatus: document.getElementById('jdkSettingsStatus'),
  closeSettingsBtn: document.getElementById('closeSettingsBtn'),
  saveJdkSettingsBtn: document.getElementById('saveJdkSettingsBtn'),
  projectSettingsDialog: document.getElementById('projectSettingsDialog'),
  projectJavaVersionInput: document.getElementById('projectJavaVersionInput'),
  defaultExportDirectoryInput: document.getElementById('defaultExportDirectoryInput'),
  browseExportDirectoryBtn: document.getElementById('browseExportDirectoryBtn'),
  maxEditableFileMbInput: document.getElementById('maxEditableFileMbInput'),
  selectedNestedJarsInput: document.getElementById('selectedNestedJarsInput'),
  uiPreferencesInput: document.getElementById('uiPreferencesInput'),
  closeProjectSettingsBtn: document.getElementById('closeProjectSettingsBtn'),
  saveProjectSettingsBtn: document.getElementById('saveProjectSettingsBtn'),
  errorGuideDialog: document.getElementById('errorGuideDialog'),
  errorGuideContent: document.getElementById('errorGuideContent'),
  closeErrorGuideBtn: document.getElementById('closeErrorGuideBtn'),
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
  unicodePreviewBtn: document.getElementById('unicodePreviewBtn'),
  saveBtn: document.getElementById('saveBtn'),
  editor: document.getElementById('editor'),
  unicodePreview: document.getElementById('unicodePreview'),
  analysisToggleBtn: document.getElementById('analysisToggleBtn'),
  analysisCloseBtn: document.getElementById('analysisCloseBtn'),
  analysisBadge: document.getElementById('analysisBadge'),
  analysisPanel: document.getElementById('analysisPanel'),
  analysisResult: document.getElementById('analysisResult'),
  logOutput: document.getElementById('logOutput'),
  notificationArea: document.getElementById('notificationArea'),
  decompileDialog: document.getElementById('decompileDialog'),
  diffDialog: document.getElementById('diffDialog'),
  diffDialogSummary: document.getElementById('diffDialogSummary'),
  diffContent: document.getElementById('diffContent'),
  closeDiffBtn: document.getElementById('closeDiffBtn'),
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
  if (!API_BASE || !API_TOKEN) {
    throw new Error('后端安全连接尚未就绪');
  }
  const { headers = {}, ...requestOptions } = options;
  const response = await fetch(`${API_BASE}${path}`, {
    ...requestOptions,
    headers: {
      'Content-Type': 'application/json',
      'X-JarPatch-Token': API_TOKEN,
      ...headers
    }
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
 * 判断当前编辑器内容是否尚未保存。
 *
 * @return 存在未保存内容时返回 true
 */
function isEditorDirty() {
  return Boolean(state.currentFilePath)
    && state.currentFileOriginalContent !== null
    && elements.editor.value !== state.currentFileOriginalContent;
}

/**
 * 在切换文件、项目或清理工作区前确认是否放弃未保存内容。
 *
 * @return 可以继续操作时返回 true
 */
function confirmDiscardUnsavedChanges() {
  return !isEditorDirty() || window.confirm(MESSAGE_UNSAVED_CHANGES_CONFIRM);
}

/**
 * 根据编辑器脏状态更新文件提示和保存按钮。
 */
function updateDirtyIndicator() {
  if (!state.currentFilePath) {
    return;
  }
  const dirty = isEditorDirty();
  const kind = elements.activeFileKind.dataset.kind || '';
  elements.activeFileKind.textContent = dirty ? `${kind} · ${MESSAGE_UNSAVED_CHANGES}` : kind;
  elements.saveBtn.classList.toggle('busy', dirty);
  updateUnicodePreviewButton();
}

/**
 * 判断字符是否属于常用中文、中文标点或全角字符范围。
 *
 * @param value 待判断字符
 * @return 可以转换为中文可读内容时返回 true
 */
function isChineseReadableCharacter(value) {
  const codePoint = value.codePointAt(0);
  return CHINESE_READABLE_RANGES.some(([start, end]) => codePoint >= start && codePoint <= end);
}

/**
 * 将文本中符合 Java 反斜杠规则的中文 Unicode 转义转换为只读预览内容。
 * 连续偶数个反斜杠表示转义后的字面量，不参与转换，避免改变原代码含义。
 *
 * @param content 编辑器原始文本
 * @return 预览内容和实际转换数量
 */
function decodeChineseUnicodeEscapes(content) {
  let index = 0;
  let convertedCount = 0;
  const previewParts = [];
  while (index < content.length) {
    if (content[index] !== '\\') {
      previewParts.push(content[index]);
      index++;
      continue;
    }

    let slashEnd = index;
    while (slashEnd < content.length && content[slashEnd] === '\\') {
      slashEnd++;
    }
    const slashCount = slashEnd - index;
    if (slashCount % BACKSLASH_ESCAPE_PAIR_LENGTH === 0) {
      previewParts.push(content.slice(index, slashEnd));
      index = slashEnd;
      continue;
    }

    previewParts.push(content.slice(index, slashEnd - 1));
    let unicodeEnd = slashEnd;
    while (unicodeEnd < content.length && content[unicodeEnd] === 'u') {
      unicodeEnd++;
    }
    const hexadecimal = content.slice(unicodeEnd, unicodeEnd + UNICODE_ESCAPE_HEX_LENGTH);
    if (unicodeEnd > slashEnd && UNICODE_ESCAPE_HEX_PATTERN.test(hexadecimal)) {
      const decoded = String.fromCharCode(Number.parseInt(hexadecimal, UNICODE_HEX_RADIX));
      if (isChineseReadableCharacter(decoded)) {
        previewParts.push(decoded);
        convertedCount++;
        index = unicodeEnd + UNICODE_ESCAPE_HEX_LENGTH;
        continue;
      }
    }
    previewParts.push('\\');
    index = slashEnd;
  }
  return { content: previewParts.join(''), convertedCount };
}

/**
 * 根据当前文件内容更新中文预览按钮状态。
 */
function updateUnicodePreviewButton() {
  elements.unicodePreviewBtn.disabled = elements.editor.disabled
    || !state.currentFilePath
    || !elements.editor.value.includes('\\u');
}

/**
 * 关闭只读预览并恢复原始编辑器；原始文本始终保存在 editor 中，不参与替换。
 */
function closeUnicodePreview() {
  state.unicodePreviewOpen = false;
  elements.editor.hidden = false;
  elements.unicodePreview.hidden = true;
  elements.unicodePreview.value = '';
  elements.unicodePreviewBtn.textContent = BUTTON_TEXT_UNICODE_PREVIEW;
  elements.unicodePreviewBtn.setAttribute('aria-pressed', 'false');
}

/**
 * 在原始编辑器和中文只读预览之间切换。
 */
function toggleUnicodePreview() {
  if (state.unicodePreviewOpen) {
    closeUnicodePreview();
    elements.editor.focus();
    return;
  }
  const decoded = decodeChineseUnicodeEscapes(elements.editor.value);
  if (!decoded.convertedCount) {
    notify(MESSAGE_UNICODE_PREVIEW_EMPTY, NOTICE_TYPE_INFO);
    return;
  }
  elements.unicodePreview.value = decoded.content;
  elements.unicodePreview.scrollTop = elements.editor.scrollTop;
  elements.editor.hidden = true;
  elements.unicodePreview.hidden = false;
  elements.unicodePreviewBtn.textContent = BUTTON_TEXT_UNICODE_SOURCE;
  elements.unicodePreviewBtn.setAttribute('aria-pressed', 'true');
  state.unicodePreviewOpen = true;
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
  elements.settingsBtn.disabled = false;
  elements.errorGuideBtn.disabled = false;
  elements.projectSettingsBtn.disabled = !state.currentProject;
  elements.cleanupWorkspaceBtn.disabled = !state.currentProject;
  elements.projectList.style.pointerEvents = '';
  elements.fileTree.style.pointerEvents = '';
  elements.searchResults.style.pointerEvents = '';
  elements.analysisToggleBtn.disabled = false;
  elements.analysisCloseBtn.disabled = false;
  elements.searchInput.disabled = !state.currentProject;
  elements.searchBtn.disabled = !state.currentProject;
  elements.saveBtn.disabled = !state.currentProject || !state.currentFilePath;
  elements.editor.disabled = !state.currentProject || !state.currentFilePath;
  updateUnicodePreviewButton();
  elements.analyzeBtn.disabled = !state.currentProject;
  elements.diffBtn.disabled = !state.currentProject;
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
  elements.settingsBtn.disabled = true;
  elements.errorGuideBtn.disabled = true;
  elements.projectSettingsBtn.disabled = true;
  elements.cleanupWorkspaceBtn.disabled = true;
  elements.projectList.style.pointerEvents = 'none';
  elements.fileTree.style.pointerEvents = 'none';
  elements.searchResults.style.pointerEvents = 'none';
  elements.analysisToggleBtn.disabled = true;
  elements.analysisCloseBtn.disabled = true;
  elements.searchInput.disabled = true;
  elements.searchBtn.disabled = true;
  elements.saveBtn.disabled = true;
  elements.unicodePreviewBtn.disabled = true;
  elements.editor.disabled = true;
  elements.analyzeBtn.disabled = true;
  elements.diffBtn.disabled = true;
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
  const socket = new WebSocket(`${WS_BASE}/ws/tasks/${taskId}?token=${encodeURIComponent(API_TOKEN)}`);
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

/**
 * 打开 JDK 配置弹窗。
 */
async function openSettingsDialog() {
  elements.settingsDialog.classList.add('open');
  elements.settingsDialog.setAttribute('aria-hidden', 'false');
  renderJdkSettings(null, MESSAGE_SETTINGS_LOADING);
  try {
    const settings = await api('/api/settings/jdk');
    state.jdkSettings = settings;
    renderJdkSettings(settings, MESSAGE_SETTINGS_SUMMARY);
  } catch (error) {
    renderJdkSettings(null, MESSAGE_SETTINGS_LOADING);
    notify(`${MESSAGE_SETTINGS_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 关闭 JDK 配置弹窗。
 */
function closeSettingsDialog() {
  elements.settingsDialog.classList.remove('open');
  elements.settingsDialog.setAttribute('aria-hidden', 'true');
}

/**
 * 渲染 JDK 配置状态。
 *
 * @param settings JDK 配置视图
 * @param summary  弹窗说明文案
 */
function renderJdkSettings(settings, summary) {
  const view = settings || {
    configuredJavaHome: '',
    configuredJavacPath: '',
    configuredValid: false,
    effectiveJavaHome: '',
    effectiveJavacPath: '',
    effectiveValid: false,
    message: MESSAGE_SETTINGS_LOADING
  };
  elements.settingsDialogSummary.textContent = summary || MESSAGE_SETTINGS_SUMMARY;
  elements.jdkHomeInput.value = view.configuredJavaHome || '';
  elements.jdkSettingsTip.textContent = MESSAGE_SETTINGS_TIP;
  elements.jdkSettingsStatus.innerHTML = `
    <div class="settings-status-item">
      <div class="settings-status-label">已保存路径</div>
      <div class="settings-status-value">${escapeHtml(view.configuredJavaHome || '未保存')}</div>
    </div>
    <div class="settings-status-item">
      <div class="settings-status-label">已保存 javac</div>
      <div class="settings-status-value">${escapeHtml(view.configuredJavacPath || '未校验')}</div>
    </div>
    <div class="settings-status-item">
      <div class="settings-status-label">生效路径</div>
      <div class="settings-status-value">${escapeHtml(view.effectiveJavaHome || '未检测到')}</div>
    </div>
    <div class="settings-status-item">
      <div class="settings-status-label">生效 javac</div>
      <div class="settings-status-value">${escapeHtml(view.effectiveJavacPath || '未检测到')}</div>
    </div>
    <div class="settings-status-item">
      <div class="settings-status-label">状态说明</div>
      <div class="settings-status-value">${escapeHtml(view.message || MESSAGE_SETTINGS_SUMMARY)}</div>
    </div>
  `;
}

/**
 * 设置 JDK 配置弹窗中的按钮状态。
 *
 * @param busy 是否忙碌
 */
function setSettingsDialogBusy(busy) {
  elements.jdkHomeInput.disabled = busy;
  elements.browseJdkBtn.disabled = busy;
  elements.saveJdkSettingsBtn.disabled = busy;
  elements.closeSettingsBtn.disabled = busy;
}

/**
 * 浏览并填充 JDK 安装目录。
 */
async function pickJdkHomeDirectory() {
  const initialPath = elements.jdkHomeInput.value.trim()
    || (state.jdkSettings && state.jdkSettings.configuredJavaHome)
    || (state.jdkSettings && state.jdkSettings.effectiveJavaHome)
    || '';
  const selectedPath = await window.jarPatch.pickDirectory(initialPath, '选择 JDK 安装目录');
  if (!selectedPath) {
    return;
  }
  elements.jdkHomeInput.value = selectedPath;
}

/**
 * 保存 JDK 配置。
 */
async function saveJdkSettings() {
  const javaHome = elements.jdkHomeInput.value.trim();
  if (!javaHome) {
    notify('请输入 JDK 安装目录', NOTICE_TYPE_INFO);
    return;
  }
  setSettingsDialogBusy(true);
  try {
    const settings = await api('/api/settings/jdk', {
      method: 'PUT',
      body: JSON.stringify({ javaHome })
    });
    state.jdkSettings = settings;
    renderJdkSettings(settings, MESSAGE_SETTINGS_SUMMARY);
    notify(MESSAGE_SETTINGS_SAVE_SUCCESS, NOTICE_TYPE_SUCCESS);
    closeSettingsDialog();
  } catch (error) {
    notify(`${MESSAGE_SETTINGS_SAVE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  } finally {
    setSettingsDialogBusy(false);
  }
}

/**
 * 打开并加载当前项目设置。
 */
async function openProjectSettingsDialog() {
  if (!state.currentProject) {
    return;
  }
  elements.projectSettingsDialog.classList.add('open');
  elements.projectSettingsDialog.setAttribute('aria-hidden', 'false');
  try {
    const settings = await api(`/api/projects/${state.currentProject.id}/settings`);
    state.projectSettings = settings;
    elements.projectJavaVersionInput.value = `Java ${settings.targetJavaVersion}`;
    elements.defaultExportDirectoryInput.value = settings.defaultExportDirectory || '';
    elements.maxEditableFileMbInput.value = settings.maxEditableFileBytes / BYTES_PER_MEGABYTE;
    elements.selectedNestedJarsInput.value = (settings.selectedNestedJars || []).join('\n');
    elements.uiPreferencesInput.value = settings.uiPreferencesJson || EMPTY_JSON_OBJECT;
  } catch (error) {
    notify(`${MESSAGE_PROJECT_SETTINGS_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
    closeProjectSettingsDialog();
  }
}

/**
 * 关闭项目设置弹窗。
 */
function closeProjectSettingsDialog() {
  elements.projectSettingsDialog.classList.remove('open');
  elements.projectSettingsDialog.setAttribute('aria-hidden', 'true');
}

/**
 * 选择当前项目的默认导出目录。
 */
async function pickExportDirectory() {
  const selectedPath = await window.jarPatch.pickDirectory(
    elements.defaultExportDirectoryInput.value.trim(),
    '选择默认导出目录'
  );
  if (selectedPath) {
    elements.defaultExportDirectoryInput.value = selectedPath;
  }
}

/**
 * 保存当前项目设置。
 */
async function saveProjectSettings() {
  if (!state.currentProject || !state.projectSettings) {
    return;
  }
  const maxEditableFileMb = Number(elements.maxEditableFileMbInput.value);
  const selectedNestedJars = elements.selectedNestedJarsInput.value
    .split(/\r?\n/)
    .map((value) => value.trim())
    .filter(Boolean);
  try {
    const settings = await api(`/api/projects/${state.currentProject.id}/settings`, {
      method: 'PUT',
      body: JSON.stringify({
        targetJavaVersion: state.projectSettings.targetJavaVersion,
        defaultExportDirectory: elements.defaultExportDirectoryInput.value.trim() || null,
        selectedNestedJars,
        maxEditableFileBytes: maxEditableFileMb * BYTES_PER_MEGABYTE,
        uiPreferencesJson: elements.uiPreferencesInput.value.trim()
      })
    });
    state.projectSettings = settings;
    notify(MESSAGE_PROJECT_SETTINGS_SAVE_SUCCESS, NOTICE_TYPE_SUCCESS);
    closeProjectSettingsDialog();
  } catch (error) {
    notify(`${MESSAGE_PROJECT_SETTINGS_SAVE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 预览并确认清理当前项目工作区。
 */
async function cleanupCurrentWorkspace() {
  if (!state.currentProject || !confirmDiscardUnsavedChanges()) {
    return;
  }
  try {
    const preview = await api(`/api/projects/${state.currentProject.id}/workspace/cleanup-preview`);
    const details = [
      MESSAGE_WORKSPACE_CLEANUP_CONFIRM,
      `项目：${preview.projectName}`,
      `路径：${preview.workspacePath}`,
      `文件：${preview.fileCount} 个`,
      `大小：${formatBytes(preview.totalBytes)}`,
      `最后使用：${preview.lastUsedAt}`
    ].join('\n');
    if (!window.confirm(details)) {
      return;
    }
    await api(`/api/projects/${state.currentProject.id}/workspace?confirmationId=${encodeURIComponent(preview.confirmationId)}`, {
      method: 'DELETE'
    });
    resetCurrentProject();
    await loadProjects();
    notify(MESSAGE_WORKSPACE_CLEANUP_SUCCESS, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_WORKSPACE_CLEANUP_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 打开错误排查向导。
 */
async function openErrorGuideDialog() {
  elements.errorGuideDialog.classList.add('open');
  elements.errorGuideDialog.setAttribute('aria-hidden', 'false');
  elements.errorGuideContent.innerHTML = `<div class="empty">${MESSAGE_ERROR_GUIDE_LOADING}</div>`;
  try {
    const items = await api('/api/system/error-guide');
    elements.errorGuideContent.innerHTML = items.map((item) => `
      <section class="error-guide-item">
        <h3>${escapeHtml(item.title)}</h3>
        <p><strong>现象：</strong>${escapeHtml(item.symptom)}</p>
        <p><strong>检查：</strong></p>
        <ul>${item.checks.map((value) => `<li>${escapeHtml(value)}</li>`).join('')}</ul>
        <p><strong>处理：</strong></p>
        <ul>${item.actions.map((value) => `<li>${escapeHtml(value)}</li>`).join('')}</ul>
      </section>
    `).join('');
  } catch (error) {
    elements.errorGuideContent.innerHTML = `<div class="empty">${escapeHtml(error.message)}</div>`;
    notify(`${MESSAGE_ERROR_GUIDE_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 关闭错误排查向导。
 */
function closeErrorGuideDialog() {
  elements.errorGuideDialog.classList.remove('open');
  elements.errorGuideDialog.setAttribute('aria-hidden', 'true');
}

/**
 * 把字节数格式化为便于确认的容量文本。
 *
 * @param bytes 字节数
 * @return 容量文本
 */
function formatBytes(bytes) {
  return `${(bytes / BYTES_PER_MEGABYTE).toFixed(2)} MB`;
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
    const workspaceState = project.workspaceCleanedAt
      ? `${PROJECT_TIME_SEPARATOR}${MESSAGE_WORKSPACE_CLEANED_MARKER}`
      : '';
    selectButton.innerHTML = `<strong>${escapeHtml(project.name)}</strong><span>${project.packageType}${PROJECT_TIME_SEPARATOR}${escapeHtml(formatProjectTime(project.updatedAt))}${workspaceState}</span>`;
    selectButton.disabled = Boolean(project.workspaceCleanedAt);
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
  if (project.workspaceCleanedAt || !confirmDiscardUnsavedChanges()) {
    return;
  }
  state.currentProject = project;
  state.projectSettings = null;
  state.diffReport = null;
  state.currentFilePath = null;
  state.currentFileHash = null;
  state.currentFileOriginalContent = null;
  state.currentTree = null;
  elements.currentProjectName.textContent = project.name;
  elements.currentProjectMeta.textContent = `${project.packageType} · ${project.workspacePath}`;
  restoreWorkspaceControls();
  elements.searchInput.disabled = false;
  elements.searchBtn.disabled = false;
  elements.saveBtn.disabled = true;
  closeUnicodePreview();
  elements.editor.value = '';
  elements.editor.disabled = true;
  elements.activeFileName.textContent = '未选择文件';
  elements.activeFileKind.textContent = '请选择左侧可编辑文件';
  resetAnalysisPanel();
  await loadTree(project.id);
  await loadProjects();
}

async function deleteProjectHistory(project) {
  if (state.currentProject && state.currentProject.id === project.id && !confirmDiscardUnsavedChanges()) {
    return;
  }
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
  state.projectSettings = null;
  state.diffReport = null;
  state.currentFilePath = null;
  state.currentFileHash = null;
  state.currentFileOriginalContent = null;
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
  closeUnicodePreview();
  elements.editor.value = '';
  elements.editor.disabled = true;
  elements.activeFileName.textContent = MESSAGE_ACTIVE_FILE_EMPTY;
  elements.activeFileKind.textContent = MESSAGE_ACTIVE_FILE_TIP;
  elements.activeFileKind.dataset.kind = '';
  elements.saveBtn.classList.remove('busy');
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
  if (node.path !== state.currentFilePath && !confirmDiscardUnsavedChanges()) {
    return;
  }
  try {
    const contentView = await api(`/api/projects/${state.currentProject.id}/files/content?path=${encodeURIComponent(node.path)}`);
    closeUnicodePreview();
    state.currentFilePath = node.path;
    elements.editor.disabled = false;
    elements.editor.value = contentView.content;
    state.currentFileOriginalContent = elements.editor.value;
    state.currentFileHash = contentView.contentHash;
    updateDirtyIndicator();
    elements.saveBtn.disabled = false;
    elements.activeFileName.textContent = node.path;
    elements.activeFileKind.textContent = node.kind;
    elements.activeFileKind.dataset.kind = node.kind;
    updateDirtyIndicator();
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
  if (elements.editor.value === state.currentFileOriginalContent) {
    notify(MESSAGE_SAVE_UNCHANGED, NOTICE_TYPE_INFO);
    return;
  }
  try {
    const contentView = await api(`/api/projects/${state.currentProject.id}/files/content`, {
      method: 'PUT',
      body: JSON.stringify({
        path: state.currentFilePath,
        content: elements.editor.value,
        expectedHash: state.currentFileHash
      })
    });
    elements.editor.value = contentView.content;
    if (state.unicodePreviewOpen) {
      elements.unicodePreview.value = decodeChineseUnicodeEscapes(elements.editor.value).content;
    }
    state.currentFileOriginalContent = elements.editor.value;
    state.currentFileHash = contentView.contentHash;
    updateDirtyIndicator();
    notify(`${MESSAGE_SAVE_SUCCESS}：${state.currentFilePath}`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_SAVE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

async function importArchive() {
  if (!confirmDiscardUnsavedChanges()) {
    return;
  }
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
  if (isEditorDirty()) {
    notify(MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION, NOTICE_TYPE_INFO);
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
  if (isEditorDirty()) {
    notify(MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION, NOTICE_TYPE_INFO);
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

async function loadDiffReport() {
  if (!state.currentProject) {
    return null;
  }
  const report = await api(`/api/projects/${state.currentProject.id}/diff`);
  state.diffReport = report;
  return report;
}

function renderDiffReport(report) {
  const sourceDiffs = report.sourceDiffs || [];
  const resourceDiffs = report.resourceDiffs || [];
  const artifacts = report.compiledArtifacts || [];
  elements.diffDialogSummary.textContent = `源码 ${sourceDiffs.length} 项 · 资源 ${resourceDiffs.length} 项 · class ${artifacts.length} 项`;
  elements.diffContent.replaceChildren();

  const fileDiffs = [...sourceDiffs, ...resourceDiffs];
  for (const diff of fileDiffs) {
    const details = document.createElement('details');
    details.className = 'diff-item';
    const summary = document.createElement('summary');
    summary.textContent = `${diff.status} · ${diff.path}`;
    details.appendChild(summary);

    const columns = document.createElement('div');
    columns.className = 'diff-columns';
    const original = document.createElement('pre');
    original.textContent = `导入基线\n${diff.originalContent == null ? '（不存在）' : diff.originalContent}`;
    const current = document.createElement('pre');
    current.textContent = `当前内容\n${diff.currentContent == null ? '（不存在）' : diff.currentContent}`;
    columns.appendChild(original);
    columns.appendChild(current);
    details.appendChild(columns);
    elements.diffContent.appendChild(details);
  }

  if (artifacts.length > 0) {
    const artifactSection = document.createElement('section');
    artifactSection.className = 'diff-artifacts';
    const title = document.createElement('strong');
    title.textContent = '已提交 class 清单';
    const content = document.createElement('pre');
    content.textContent = artifacts.join('\n');
    artifactSection.appendChild(title);
    artifactSection.appendChild(content);
    elements.diffContent.appendChild(artifactSection);
  }

  if (fileDiffs.length === 0 && artifacts.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'diff-empty';
    empty.textContent = MESSAGE_DIFF_EMPTY;
    elements.diffContent.appendChild(empty);
  }
}

async function openDiffDialog() {
  try {
    const report = await loadDiffReport();
    if (!report) {
      return;
    }
    renderDiffReport(report);
    elements.diffDialog.classList.add('open');
    elements.diffDialog.setAttribute('aria-hidden', 'false');
  } catch (error) {
    notify(`${MESSAGE_DIFF_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

function closeDiffDialog() {
  elements.diffDialog.classList.remove('open');
  elements.diffDialog.setAttribute('aria-hidden', 'true');
}

async function exportProject() {
  if (!state.currentProject) {
    return;
  }
  if (isEditorDirty()) {
    notify(MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  let diffReport;
  try {
    diffReport = await loadDiffReport();
  } catch (error) {
    notify(`${MESSAGE_DIFF_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
    return;
  }
  renderDiffReport(diffReport);
  elements.diffDialog.classList.add('open');
  elements.diffDialog.setAttribute('aria-hidden', 'false');
  if (!window.confirm(MESSAGE_DIFF_EXPORT_CONFIRM)) {
    closeDiffDialog();
    return;
  }
  closeDiffDialog();
  const removeSignatures = window.confirm(MESSAGE_SIGNATURE_POLICY_CONFIRM);
  const settings = state.projectSettings || await api(`/api/projects/${state.currentProject.id}/settings`);
  state.projectSettings = settings;
  const defaultName = state.currentProject.name.replace(/(\.jar|\.war)$/i, '-patched$1');
  const defaultPath = settings.defaultExportDirectory
    ? `${settings.defaultExportDirectory.replace(/[\\/]$/, '')}/${defaultName}`
    : defaultName;
  const outputPath = await window.jarPatch.saveArchive(defaultPath);
  if (!outputPath) {
    notify(MESSAGE_EXPORT_CANCELED, NOTICE_TYPE_INFO);
    return;
  }
  setActionButtonRunning(elements.exportBtn, BUTTON_TEXT_EXPORT_RUNNING);
  try {
    const result = await executeTaskOperation(TASK_TYPE_EXPORT, state.currentProject.id, `开始导出修改后的包: ${outputPath}`, (task, signal) => api(`/api/projects/${state.currentProject.id}/export`, {
      method: 'POST',
      signal,
      body: JSON.stringify({
        outputPath,
        taskId: task.id,
        signaturePolicy: removeSignatures ? 'REMOVE_INVALID_SIGNATURES' : 'PRESERVE_ONLY_UNMODIFIED'
      })
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
elements.unicodePreviewBtn.addEventListener('click', toggleUnicodePreview);
elements.saveBtn.addEventListener('click', saveCurrentFile);
elements.analyzeBtn.addEventListener('click', analyzeProject);
elements.diffBtn.addEventListener('click', openDiffDialog);
elements.compileBtn.addEventListener('click', compileProject);
elements.exportBtn.addEventListener('click', exportProject);
elements.settingsBtn.addEventListener('click', openSettingsDialog);
elements.projectSettingsBtn.addEventListener('click', openProjectSettingsDialog);
elements.cleanupWorkspaceBtn.addEventListener('click', cleanupCurrentWorkspace);
elements.errorGuideBtn.addEventListener('click', openErrorGuideDialog);
elements.searchBtn.addEventListener('click', searchProject);
elements.cancelTaskBtn.addEventListener('click', cancelCurrentTask);
elements.browseJdkBtn.addEventListener('click', pickJdkHomeDirectory);
elements.closeSettingsBtn.addEventListener('click', closeSettingsDialog);
elements.saveJdkSettingsBtn.addEventListener('click', saveJdkSettings);
elements.browseExportDirectoryBtn.addEventListener('click', pickExportDirectory);
elements.closeProjectSettingsBtn.addEventListener('click', closeProjectSettingsDialog);
elements.saveProjectSettingsBtn.addEventListener('click', saveProjectSettings);
elements.closeErrorGuideBtn.addEventListener('click', closeErrorGuideDialog);
elements.closeDiffBtn.addEventListener('click', closeDiffDialog);
elements.treeResizeHandle.addEventListener('pointerdown', beginTreeResize);
elements.analysisToggleBtn.addEventListener('click', () => setAnalysisPanelOpen(!state.analysisOpen));
elements.analysisCloseBtn.addEventListener('click', () => setAnalysisPanelOpen(false));
elements.selectRecommendedBtn.addEventListener('click', selectRecommendedCandidates);
elements.selectAllJarsBtn.addEventListener('click', () => setAllCandidatesSelected(true));
elements.clearJarSelectionBtn.addEventListener('click', () => setAllCandidatesSelected(false));
elements.settingsDialog.addEventListener('click', (event) => {
  if (event.target === elements.settingsDialog) {
    closeSettingsDialog();
  }
});
elements.searchInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    searchProject();
  }
});

async function initializeApplication() {
  const connection = await window.jarPatch.getBackendConnection();
  API_BASE = connection.apiBase;
  WS_BASE = API_BASE.replace(/^http/, 'ws');
  API_TOKEN = connection.token;
  initializeTreePanelWidth();
  restoreWorkspaceControls();
  await loadProjects();
}

initializeApplication().catch((error) => {
  notify(`连接本地后端失败：${error.message}`, NOTICE_TYPE_ERROR);
});
elements.projectSettingsDialog.addEventListener('click', (event) => {
  if (event.target === elements.projectSettingsDialog) {
    closeProjectSettingsDialog();
  }
});
elements.errorGuideDialog.addEventListener('click', (event) => {
  if (event.target === elements.errorGuideDialog) {
    closeErrorGuideDialog();
  }
});
elements.editor.addEventListener('input', updateDirtyIndicator);
document.addEventListener('keydown', (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault();
    saveCurrentFile();
  }
});
window.addEventListener('beforeunload', (event) => {
  if (isEditorDirty()) {
    event.preventDefault();
    event.returnValue = '';
  }
});
elements.diffDialog.addEventListener('click', (event) => {
  if (event.target === elements.diffDialog) {
    closeDiffDialog();
  }
});
