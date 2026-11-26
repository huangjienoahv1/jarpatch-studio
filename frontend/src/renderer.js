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
const MESSAGE_DIFF_METADATA_ONLY = '文本内容一致，文件字节有变化（例如 BOM 或换行符）。';
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
const MESSAGE_DEVELOPMENT_SETTINGS_SAVE_SUCCESS = '开发能力配置已保存';
const MESSAGE_DEVELOPMENT_SETTINGS_LOAD_FAILED = '读取开发能力配置失败';
const MESSAGE_DEVELOPMENT_SETTINGS_SUMMARY = '分别配置项目编译 JDK、Java 语言服务器和 AI 代码助手';
const MESSAGE_DEVELOPMENT_SETTINGS_PARTIAL = '配置未全部保存';
const MESSAGE_COMPILE_JDK_LABEL = '编译 JDK';
const MESSAGE_DEVELOPMENT_SETTINGS_LABEL = '开发能力';
const MESSAGE_AI_KEY_SAVED = 'AI API Key 已由系统安全存储加密保存。';
const MESSAGE_AI_KEY_EMPTY = '尚未保存 AI API Key。本机或内网 IPv4 端点无鉴权时可留空。';
const MESSAGE_LANGUAGE_SERVER_NOT_CONFIGURED = 'Java 智能未启用；Monaco 基础编辑功能可用';
const MESSAGE_LANGUAGE_SERVER_UNAVAILABLE = 'Java 智能启动失败';
const MESSAGE_ORGANIZE_IMPORTS_EMPTY = '没有可整理的 import';
const MESSAGE_ORGANIZE_IMPORTS_SUCCESS = 'import 已整理，请检查后保存';
const MESSAGE_AI_NOT_CONFIGURED = 'AI 代码助手尚未启用，请先打开开发能力配置';
const MESSAGE_AI_RUNNING = 'AI 正在处理代码';
const MESSAGE_AI_FAILED = 'AI 代码助手执行失败';
const MESSAGE_AI_STALE_RESULT = 'AI 处理期间代码已变化，结果未应用';
const MESSAGE_AI_APPLIED = 'AI 修改已应用到编辑器，请检查后保存';
const MESSAGE_AI_INLINE_REQUESTED = '正在请求 AI 行内补全，返回后按 Tab 接受';
const MESSAGE_AI_PREVIEW_DEFAULT = 'AI 结果会先在这里预览，不会自动覆盖源码。';
const MESSAGE_AI_CONTEXT_SELECTION = '将发送当前选区';
const MESSAGE_AI_CONTEXT_FILE = '未选择代码，将发送当前文件';
const MESSAGE_AI_CONTEXT_SUFFIX = '个字符';
const MESSAGE_AI_RESULT_EXPLANATION = 'AI 已返回解释，不会修改代码。';
const MESSAGE_AI_RESULT_REPLACEMENT = 'AI 已返回修改建议；确认预览内容后可应用到编辑器。';
const MESSAGE_AI_NO_FILE = '请先打开需要处理的文件';
const BUTTON_TEXT_AI_RUN = '运行 AI';
const BUTTON_TEXT_AI_RUNNING = 'AI 处理中...';
const MESSAGE_PROJECT_SETTINGS_LOAD_FAILED = '读取项目设置失败';
const MESSAGE_PROJECT_SETTINGS_SAVE_FAILED = '保存项目设置失败';
const MESSAGE_PROJECT_SETTINGS_SAVE_SUCCESS = '项目设置已保存';
const MESSAGE_PROJECT_HISTORY_LOAD_FAILED = '读取项目历史失败';
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
const MESSAGE_PROJECT_HISTORY_DELETE_WITH_WORKSPACE = '是否先预览并清理该项目工作区，再删除历史？\n\n选择“确定”：预览确认清理后删除历史。\n选择“取消”：下一步可选择仅删除历史或放弃。';
const MESSAGE_PROJECT_HISTORY_DELETE_ONLY = '确认仅删除项目历史并保留工作区吗？保留的目录稍后可在“孤立工作区”中预览清理。';
const MESSAGE_ORPHAN_WORKSPACE_EMPTY = '没有发现孤立工作区';
const MESSAGE_ORPHAN_WORKSPACE_CLEAN_SUCCESS = '孤立工作区已清理';
const MESSAGE_ORPHAN_WORKSPACE_CLEAN_FAILED = '孤立工作区清理失败';
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
const MESSAGE_TASK_IN_PROGRESS = '已有任务正在执行，请等待当前任务结束';
const MESSAGE_TASK_LOG_UNAVAILABLE = '任务日志连接失败';
const MESSAGE_DIAGNOSTIC_EXPORT_SUCCESS = '脱敏诊断信息已导出';
const MESSAGE_DIAGNOSTIC_EXPORT_FAILED = '导出诊断信息失败';
const MESSAGE_TASK_LABEL_IMPORT = '导入';
const MESSAGE_TASK_LABEL_ANALYZE = '分析';
const MESSAGE_TASK_LABEL_COMPILE = '编译';
const MESSAGE_TASK_LABEL_EXPORT = '导出';
const MESSAGE_SETTINGS_SUMMARY = '保存后编译服务会优先使用这里配置的 JDK 路径';
const MESSAGE_SETTINGS_TIP = '请选择 JDK 安装目录，保存时会校验 bin 目录下的 javac。';
const MESSAGE_BUNDLED_JDK_NOT_INCLUDED = '未内置编译 JDK';
const MESSAGE_BUNDLED_JDK_LOAD_FAILED = '无法读取内置 JDK 版本';
const MESSAGE_SETTINGS_PLACEHOLDER = '例如：C:\\Program Files\\Java\\jdk-17';
const TASK_TYPE_IMPORT = 'IMPORT';
const TASK_TYPE_ANALYZE = 'ANALYZE';
const TASK_TYPE_COMPILE = 'COMPILE';
const TASK_TYPE_PATCH_DIRECTORY = 'PATCH_DIRECTORY';
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
const AI_ACTION_EXPLAIN = 'EXPLAIN';
const AI_ACTION_FIX = 'FIX';
const AI_ACTION_REFACTOR = 'REFACTOR';
const AI_ACTION_COMPLETE = 'COMPLETE';
const AI_RESULT_TYPE_REPLACEMENT = 'replacement';
const AI_CONTEXT_MAX_SOURCE_CHARACTERS = 60000;
const DIAGNOSTIC_DEFAULT_FILE_NAME = 'jarpatch-studio-diagnostics.json';
const JSON_INDENT_SPACES = 2;
const STORAGE_KEY_TREE_PANEL_WIDTH = 'jarpatch.treePanelWidth';
const STORAGE_KEY_THEME = 'jarpatch.uiTheme';
const STORAGE_KEY_LOG_HEIGHT = 'jarpatch.logPanelHeight';
const STORAGE_KEY_LOG_COLLAPSED = 'jarpatch.logPanelCollapsed';
const THEME_DARK = 'dark';
const THEME_LIGHT = 'light';
const LABEL_SWITCH_TO_LIGHT = '切换浅色主题';
const LABEL_SWITCH_TO_DARK = '切换深色主题';
const TREE_PANEL_DEFAULT_WIDTH = 340;
const TREE_PANEL_MIN_WIDTH = 240;
const TREE_PANEL_MAX_WIDTH = 720;
const TREE_PANEL_EDITOR_MIN_WIDTH = 480;
const TREE_PANEL_EDITOR_ANALYSIS_OPEN_MIN_WIDTH = 440;
const TREE_RESIZER_WIDTH = 6;
const ANALYSIS_TOGGLE_WIDTH = 44;
const ANALYSIS_PANEL_WIDTH = 324;
const LOG_PANEL_DEFAULT_HEIGHT = 128;
const LOG_PANEL_MIN_HEIGHT = 76;
const LOG_PANEL_MAX_HEIGHT = 360;
const BUTTON_TEXT_LOG_COLLAPSE = '收起';
const BUTTON_TEXT_LOG_EXPAND = '展开';
const MESSAGE_LOG_EMPTY_TO_COPY = '当前没有可复制的执行日志';
const MESSAGE_LOG_COPIED = '执行日志已复制到剪贴板';
const MESSAGE_LOG_COPY_FAILED = '复制日志失败';
const MESSAGE_EMPTY_NO_PROJECT_TITLE = '欢迎使用 JarPatch Studio';
const MESSAGE_EMPTY_NO_PROJECT_DESC = '导入本地 Jar、Spring Boot Jar 或 War 包，浏览反编译源码、修改配置或代码、校验差异并安全导出新包。';
const MESSAGE_EMPTY_NO_FILE_TITLE = '从左侧文件树选择文件';
const MESSAGE_EMPTY_NO_FILE_DESC = '展开 sources 查看反编译后的 Java 源码，或展开 extracted 修改配置与文本资源。';
const TASK_STATE_IDLE = 'idle';
const TASK_STATE_RUNNING = 'running';
const TASK_STATE_DONE = 'done';
const TASK_STATE_FAILED = 'failed';
const TASK_STATE_CANCELED = 'canceled';
const TASK_PROGRESS_MIN = 0;
const TASK_PROGRESS_MAX = 100;
const PACKAGE_TYPE_BADGE_MAP = {
  SPRING_BOOT_JAR: { label: 'Spring Boot', className: 'pkg-boot' },
  WAR: { label: 'WAR', className: 'pkg-war' },
  PLAIN_JAR: { label: 'JAR', className: 'pkg-jar' }
};
const TREE_ICON_SVG_MAP = {
  dirClosed: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>',
  dirOpen: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.5-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.54 6a2 2 0 0 1-1.95 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>',
  java: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m8 6-6 6 6 6M16 6l6 6-6 6"/></svg>',
  config: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/></svg>',
  markup: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M9 15h6"/></svg>',
  archive: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8v13H3V8M1 3h22v5H1zM10 12h4"/></svg>',
  signature: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  binary: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/></svg>',
  file: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/></svg>'
};
const DELETE_ICON_SVG = '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/></svg>';
const STORAGE_KEY_COMPACT_FOLDERS = 'jarpatch.compactFolders';
const MAX_OPEN_EDITOR_TABS = 12;
const TAB_CLOSE_ICON_SVG = '<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>';
const MESSAGE_PATH_COPIED = '文件路径已复制';
const MESSAGE_PATH_COPY_FAILED = '复制文件路径失败';
const MESSAGE_ADMIN_ONLY_ACTION = '当前操作仅限管理员执行';
const MESSAGE_PROJECT_FILTER_EMPTY = '没有匹配的项目';
const MESSAGE_CANDIDATE_FILTER_EMPTY = '没有匹配的嵌套 Jar';
const HELP_SCROLL_ACTIVE_OFFSET_PX = 48;
const HELP_SCROLL_BOTTOM_THRESHOLD_PX = 4;
const HELP_HASH_PREFIX_LENGTH = 1;
const HELP_ACTIVE_CLASS = 'is-active';
const HELP_ARIA_CURRENT_VALUE = 'location';
const HELP_SEARCH_RESULT_PREFIX = '已匹配 ';
const HELP_SEARCH_RESULT_SUFFIX = ' 个章节';
const HELP_SEARCH_EMPTY_TEXT = '未找到匹配章节';
const HISTORY_TAB_OPERATIONS = 'operations';
const HISTORY_TAB_ANALYSES = 'analyses';
const HISTORY_TAB_VALIDATIONS = 'validations';
const SETTINGS_TAB_JDK = 'jdk';
const SETTINGS_TAB_JDTLS = 'jdtls';
const SETTINGS_TAB_AI = 'ai';
const LANGUAGE_LABEL_MAP = {
  java: 'Java',
  xml: 'XML',
  yaml: 'YAML',
  ini: 'Properties',
  json: 'JSON',
  javascript: 'JavaScript',
  css: 'CSS',
  html: 'HTML',
  plaintext: 'Plain Text'
};
const DIFF_STATUS_LABEL_MAP = {
  MODIFIED: { label: '已修改', className: 'diff-modified' },
  ADDED: { label: '新增', className: 'diff-added' },
  DELETED: { label: '已删除', className: 'diff-deleted' }
};
const RISK_LEVEL_LABEL_MAP = {
  HIGH: '高风险',
  WARN: '警告',
  INFO: '提示'
};
const HISTORY_STATUS_META_MAP = {
  SUCCESS: { label: '成功', className: 'status-success' },
  FAILED: { label: '失败', className: 'status-failed' },
  CANCELED: { label: '已取消', className: 'status-canceled' },
  RUNNING: { label: '进行中', className: 'status-running' }
};
const COMPACT_FOLDER_MAX_DEPTH = 12;
const DEFAULT_OPERATOR_NAME = 'admin';
const SIGNATURE_POLICY_REMOVE = 'REMOVE_INVALID_SIGNATURES';
const SIGNATURE_POLICY_PRESERVE = 'PRESERVE_ONLY_UNMODIFIED';
const DELETE_ACTION_HISTORY_ONLY = 'history';
const DELETE_ACTION_WITH_WORKSPACE = 'both';
const DELETE_ACTION_CANCEL = 'cancel';
const UNICODE_PREVIEW_REFRESH_DELAY_MS = 400;
const UNICODE_PREVIEW_FIRST_LINE = 1;
const UNICODE_PREVIEW_FIRST_COLUMN = 1;
const UNICODE_PREVIEW_FIRST_INDEX = 0;
const UNICODE_PREVIEW_NO_ACTIVE_INDEX = -1;
const UNICODE_PREVIEW_STEP_PREV = -1;
const UNICODE_PREVIEW_STEP_NEXT = 1;
const UNICODE_PREVIEW_MIN_NAVIGABLE_COUNT = 2;
const CRLF_SEQUENCE_LENGTH = 2;
const MESSAGE_UNICODE_PREVIEW_SUMMARY_PREFIX = '已解码 ';
const MESSAGE_UNICODE_PREVIEW_SUMMARY_SPAN_UNIT = ' 处（';
const MESSAGE_UNICODE_PREVIEW_SUMMARY_CHAR_UNIT = ' 字，分布 ';
const MESSAGE_UNICODE_PREVIEW_SUMMARY_LINE_UNIT = ' 行）· 原文件未修改';
const MESSAGE_UNICODE_PREVIEW_MATCH_SEPARATOR = ' / ';
const MESSAGE_UNICODE_PREVIEW_LINE_PREFIX = ' · 第 ';
const MESSAGE_UNICODE_PREVIEW_LINE_SUFFIX = ' 行';
const LOG_OUTPUT_MAX_CHARS = 128 * 1024;
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
  [TASK_TYPE_PATCH_DIRECTORY]: '应用补丁',
  EXPORT: MESSAGE_TASK_LABEL_EXPORT
};

const TASK_STATUS_CANCELED = 'CANCELED';
const MESSAGE_SAVE_IN_PROGRESS = '文件正在保存，请稍后再保存后续修改';

const state = {
  projectSessionId: 0,
  patchDirectoryOpening: false,
  fileOpenRequestId: 0,
  documentSessionId: 0,
  editorRevision: 0,
  savedEditorVersionId: 0,
  unicodePreviewRefreshTimer: null,
  currentSavePromise: null,
  currentProject: null,
  currentFilePath: null,
  currentFileHash: null,
  currentFileEncoding: null,
  currentFileOriginalContent: null,
  unicodePreviewOpen: false,
  unicodePreviewView: null,
  unicodePreviewRanges: [],
  unicodePreviewActiveIndex: UNICODE_PREVIEW_NO_ACTIVE_INDEX,
  analysisOpen: false,
  analysisReport: null,
  diffRequestId: 0,
  currentInspection: null,
  currentTree: null,
  taskStarting: false,
  currentTask: null,
  failedCompileProjectId: null,
  currentTaskSocket: null,
  currentTaskAbortController: null,
  currentTaskCancelRequested: false,
  jdkSettings: null,
  projectSettings: null,
  developmentSettings: null,
  languageServerReady: false,
  aiRequestContext: null,
  aiResult: null,
  allProjects: [],
  openTabs: [],
  activeSearchPath: null,
  lastSearchKeyword: '',
  currentProjectHistory: null,
  activeHistoryTab: HISTORY_TAB_OPERATIONS,
  activeSettingsTab: SETTINGS_TAB_JDK,
  compactFolders: true,
  treePanelWidth: TREE_PANEL_DEFAULT_WIDTH,
  logPanelHeight: LOG_PANEL_DEFAULT_HEIGHT,
  logPanelCollapsed: false,
  uiTheme: THEME_DARK,
  helpScrollSyncFrame: 0,
  helpSuppressNextScrollSync: false,
  expandedPaths: new Set([''])
};

const elements = {
  appShell: document.querySelector('.app-shell'),
  workspace: document.querySelector('.workspace'),
  sidebarToggleBtn: document.getElementById('sidebarToggleBtn'),
  sidebarQuickOpenBtn: document.getElementById('sidebarQuickOpenBtn'),
  sidebarContent: document.getElementById('sidebarContent'),
  openArchiveBtn: document.getElementById('openArchiveBtn'),
  projectCountBadge: document.getElementById('projectCountBadge'),
  projectSearchInput: document.getElementById('projectSearchInput'),
  projectList: document.getElementById('projectList'),
  currentProjectBadge: document.getElementById('currentProjectBadge'),
  currentProjectName: document.getElementById('currentProjectName'),
  currentProjectMeta: document.getElementById('currentProjectMeta'),
  analyzeBtn: document.getElementById('analyzeBtn'),
  diffBtn: document.getElementById('diffBtn'),
  compileBtn: document.getElementById('compileBtn'),
  patchDirectoryBtn: document.getElementById('patchDirectoryBtn'),
  patchDirectoryDialog: document.getElementById('patchDirectoryDialog'),
  patchDirectoryPath: document.getElementById('patchDirectoryPath'),
  patchDirectoryStatus: document.getElementById('patchDirectoryStatus'),
  patchDirectoryEntries: document.getElementById('patchDirectoryEntries'),
  applyPatchDirectoryBtn: document.getElementById('applyPatchDirectoryBtn'),
  exportBtn: document.getElementById('exportBtn'),
  settingsBtn: document.getElementById('settingsBtn'),
  runtimeBtn: document.getElementById('runtimeBtn'),
  moreToolsMenu: document.getElementById('moreToolsMenu'),
  moreToolsBtn: document.getElementById('moreToolsBtn'),
  moreToolsPopover: document.getElementById('moreToolsPopover'),
  themeToggleBtn: document.getElementById('themeToggleBtn'),
  projectSettingsBtn: document.getElementById('projectSettingsBtn'),
  projectHistoryBtn: document.getElementById('projectHistoryBtn'),
  cleanupWorkspaceBtn: document.getElementById('cleanupWorkspaceBtn'),
  orphanWorkspacesBtn: document.getElementById('orphanWorkspacesBtn'),
  errorGuideBtn: document.getElementById('errorGuideBtn'),
  diagnosticBtn: document.getElementById('diagnosticBtn'),
  helpBtn: document.getElementById('helpBtn'),
  helpDialog: document.getElementById('helpDialog'),
  helpNavigation: document.getElementById('helpNavigation'),
  helpSearchInput: document.getElementById('helpSearchInput'),
  helpSearchClearBtn: document.getElementById('helpSearchClearBtn'),
  helpSearchStatus: document.getElementById('helpSearchStatus'),
  helpContent: document.getElementById('helpContent'),
  helpSearchEmpty: document.getElementById('helpSearchEmpty'),
  helpSearchResetBtn: document.getElementById('helpSearchResetBtn'),
  settingsDialog: document.getElementById('settingsDialog'),
  settingsTabs: document.getElementById('settingsTabs'),
  settingsDotJdk: document.getElementById('settingsDotJdk'),
  settingsDotJdtls: document.getElementById('settingsDotJdtls'),
  settingsDotAi: document.getElementById('settingsDotAi'),
  settingsDialogSummary: document.getElementById('settingsDialogSummary'),
  jdkHomeInput: document.getElementById('jdkHomeInput'),
  browseJdkBtn: document.getElementById('browseJdkBtn'),
  jdkSettingsTip: document.getElementById('jdkSettingsTip'),
  bundledJdkVersion: document.getElementById('bundledJdkVersion'),
  jdkSettingsStatus: document.getElementById('jdkSettingsStatus'),
  closeSettingsBtn: document.getElementById('closeSettingsBtn'),
  saveJdkSettingsBtn: document.getElementById('saveJdkSettingsBtn'),
  languageServerEnabledInput: document.getElementById('languageServerEnabledInput'),
  jdtLsHomeInput: document.getElementById('jdtLsHomeInput'),
  browseJdtLsBtn: document.getElementById('browseJdtLsBtn'),
  jdtLsJavaHomeInput: document.getElementById('jdtLsJavaHomeInput'),
  browseJdtLsJavaBtn: document.getElementById('browseJdtLsJavaBtn'),
  aiEnabledInput: document.getElementById('aiEnabledInput'),
  aiProtocolSelect: document.getElementById('aiProtocolSelect'),
  aiMessagesAuthField: document.getElementById('aiMessagesAuthField'),
  aiMessagesAuthSelect: document.getElementById('aiMessagesAuthSelect'),
  aiEndpointInput: document.getElementById('aiEndpointInput'),
  aiModelInput: document.getElementById('aiModelInput'),
  aiApiKeyInput: document.getElementById('aiApiKeyInput'),
  clearAiApiKeyInput: document.getElementById('clearAiApiKeyInput'),
  developmentSettingsStatus: document.getElementById('developmentSettingsStatus'),
  projectSettingsDialog: document.getElementById('projectSettingsDialog'),
  projectJavaVersionInput: document.getElementById('projectJavaVersionInput'),
  defaultExportDirectoryInput: document.getElementById('defaultExportDirectoryInput'),
  browseExportDirectoryBtn: document.getElementById('browseExportDirectoryBtn'),
  maxEditableFileMbInput: document.getElementById('maxEditableFileMbInput'),
  projectDefaultEncodingSelect: document.getElementById('projectDefaultEncodingSelect'),
  selectedNestedJarsInput: document.getElementById('selectedNestedJarsInput'),
  uiPreferencesInput: document.getElementById('uiPreferencesInput'),
  closeProjectSettingsBtn: document.getElementById('closeProjectSettingsBtn'),
  saveProjectSettingsBtn: document.getElementById('saveProjectSettingsBtn'),
  projectHistoryDialog: document.getElementById('projectHistoryDialog'),
  projectHistoryTabs: document.getElementById('projectHistoryTabs'),
  projectHistoryContent: document.getElementById('projectHistoryContent'),
  closeProjectHistoryBtn: document.getElementById('closeProjectHistoryBtn'),
  errorGuideDialog: document.getElementById('errorGuideDialog'),
  errorGuideContent: document.getElementById('errorGuideContent'),
  closeErrorGuideBtn: document.getElementById('closeErrorGuideBtn'),
  taskBar: document.getElementById('taskBar'),
  taskStatusText: document.getElementById('taskStatusText'),
  taskStatusDetail: document.getElementById('taskStatusDetail'),
  taskProgressFill: document.getElementById('taskProgressFill'),
  cancelTaskBtn: document.getElementById('cancelTaskBtn'),
  retryCompileBtn: document.getElementById('retryCompileBtn'),
  dismissTaskBtn: document.getElementById('dismissTaskBtn'),
  contentGrid: document.getElementById('contentGrid'),
  treeResizeHandle: document.getElementById('treeResizeHandle'),
  compactFoldersBtn: document.getElementById('compactFoldersBtn'),
  collapseTreeBtn: document.getElementById('collapseTreeBtn'),
  fileTree: document.getElementById('fileTree'),
  searchInput: document.getElementById('searchInput'),
  clearSearchBtn: document.getElementById('clearSearchBtn'),
  searchBtn: document.getElementById('searchBtn'),
  searchResults: document.getElementById('searchResults'),
  editorTabs: document.getElementById('editorTabs'),
  activeFileDirtyDot: document.getElementById('activeFileDirtyDot'),
  editorBreadcrumb: document.getElementById('editorBreadcrumb'),
  activeFileName: document.getElementById('activeFileName'),
  copyFilePathBtn: document.getElementById('copyFilePathBtn'),
  activeFileKind: document.getElementById('activeFileKind'),
  findInEditorBtn: document.getElementById('findInEditorBtn'),
  unicodePreviewBtn: document.getElementById('unicodePreviewBtn'),
  fileEncodingSelect: document.getElementById('fileEncodingSelect'),
  saveBtn: document.getElementById('saveBtn'),
  quickFixBtn: document.getElementById('quickFixBtn'),
  organizeImportsBtn: document.getElementById('organizeImportsBtn'),
  aiInlineBtn: document.getElementById('aiInlineBtn'),
  aiAssistantBtn: document.getElementById('aiAssistantBtn'),
  editorIntelligenceStatus: document.getElementById('editorIntelligenceStatus'),
  editorIntelligenceText: document.getElementById('editorIntelligenceText'),
  statusCursorPos: document.getElementById('statusCursorPos'),
  statusLanguage: document.getElementById('statusLanguage'),
  statusCurrentUser: document.getElementById('statusCurrentUser'),
  editorEmptyState: document.getElementById('editorEmptyState'),
  emptyStateTitle: document.getElementById('emptyStateTitle'),
  emptyStateDesc: document.getElementById('emptyStateDesc'),
  emptyStateActions: document.getElementById('emptyStateActions'),
  emptyOpenArchiveBtn: document.getElementById('emptyOpenArchiveBtn'),
  emptyOpenRuntimeBtn: document.getElementById('emptyOpenRuntimeBtn'),
  editor: window.jarPatchEditor.create(document.getElementById('editor')),
  unicodePreviewPanel: document.getElementById('unicodePreviewPanel'),
  unicodePreviewSummary: document.getElementById('unicodePreviewSummary'),
  unicodePreviewMatchStatus: document.getElementById('unicodePreviewMatchStatus'),
  unicodePreviewPrevBtn: document.getElementById('unicodePreviewPrevBtn'),
  unicodePreviewNextBtn: document.getElementById('unicodePreviewNextBtn'),
  unicodePreview: document.getElementById('unicodePreview'),
  aiAssistantDialog: document.getElementById('aiAssistantDialog'),
  aiContextSummary: document.getElementById('aiContextSummary'),
  aiActionSelect: document.getElementById('aiActionSelect'),
  aiInstructionInput: document.getElementById('aiInstructionInput'),
  aiResultSummary: document.getElementById('aiResultSummary'),
  aiResultOutput: document.getElementById('aiResultOutput'),
  closeAiAssistantBtn: document.getElementById('closeAiAssistantBtn'),
  applyAiResultBtn: document.getElementById('applyAiResultBtn'),
  runAiAssistantBtn: document.getElementById('runAiAssistantBtn'),
  analysisToggleBtn: document.getElementById('analysisToggleBtn'),
  analysisCloseBtn: document.getElementById('analysisCloseBtn'),
  analysisBadge: document.getElementById('analysisBadge'),
  analysisPanel: document.getElementById('analysisPanel'),
  analysisResult: document.getElementById('analysisResult'),
  logResizeHandle: document.getElementById('logResizeHandle'),
  logPanel: document.getElementById('logPanel'),
  copyLogBtn: document.getElementById('copyLogBtn'),
  clearLogBtn: document.getElementById('clearLogBtn'),
  toggleLogBtn: document.getElementById('toggleLogBtn'),
  logOutput: document.getElementById('logOutput'),
  notificationArea: document.getElementById('notificationArea'),
  decompileDialog: document.getElementById('decompileDialog'),
  decompileFilterInput: document.getElementById('decompileFilterInput'),
  decompileSelectedBadge: document.getElementById('decompileSelectedBadge'),
  diffDialog: document.getElementById('diffDialog'),
  diffExpandAllBtn: document.getElementById('diffExpandAllBtn'),
  diffCollapseAllBtn: document.getElementById('diffCollapseAllBtn'),
  diffDialogSummary: document.getElementById('diffDialogSummary'),
  diffContent: document.getElementById('diffContent'),
  continueExportBtn: document.getElementById('continueExportBtn'),
  decompileDialogMeta: document.getElementById('decompileDialogMeta'),
  decompileDialogSummary: document.getElementById('decompileDialogSummary'),
  decompileCandidateList: document.getElementById('decompileCandidateList'),
  selectRecommendedBtn: document.getElementById('selectRecommendedBtn'),
  selectAllJarsBtn: document.getElementById('selectAllJarsBtn'),
  clearJarSelectionBtn: document.getElementById('clearJarSelectionBtn'),
  cancelDecompileBtn: document.getElementById('cancelDecompileBtn'),
  confirmDecompileBtn: document.getElementById('confirmDecompileBtn')
};

const api = window.jarPatchApiClient.request;

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
 * <p>
 * 用 Monaco 撤销栈版本号与保存基线比较，是 O(1) 判断；value 取值会整文拷贝，
 * 不能放在每次按键都会执行的路径上。撤销/重做回到保存基线版本时数值相等，
 * 与逐字比较的语义一致。
 * </p>
 *
 * @return 存在未保存内容时返回 true
 */
function isEditorDirty() {
  return Boolean(state.currentFilePath)
    && state.currentFileOriginalContent !== null
    && elements.editor.alternativeVersionId !== state.savedEditorVersionId;
}

/**
 * 在切换文件、项目或清理工作区前，通过内置确认弹窗提醒用户是否放弃未保存内容。
 *
 * @returns {Promise<boolean>} 可以继续操作时返回 true
 */
async function confirmDiscardUnsavedChanges() {
  if (!isEditorDirty()) {
    return true;
  }
  return window.jarPatchDialogs.confirm({
    title: '放弃未保存的修改？',
    message: MESSAGE_UNSAVED_CHANGES_CONFIRM,
    tone: window.jarPatchDialogs.TONE_WARN,
    details: state.currentFilePath ? [{ label: '当前文件', value: state.currentFilePath }] : [],
    actions: [
      { value: false, label: '继续编辑', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
      { value: true, label: '放弃修改并继续', variant: window.jarPatchDialogs.VARIANT_DANGER }
    ]
  });
}

/**
 * 渲染编辑器路径面包屑导航并控制复制路径按钮。
 *
 * @param {string|null} filePath 当前文件相对路径
 */
function renderEditorBreadcrumb(filePath) {
  if (!filePath) {
    elements.editorBreadcrumb.replaceChildren();
    const emptyNode = document.createElement('span');
    emptyNode.id = 'activeFileName';
    emptyNode.className = 'active-file';
    emptyNode.textContent = MESSAGE_ACTIVE_FILE_EMPTY;
    elements.editorBreadcrumb.appendChild(emptyNode);
    elements.activeFileName = emptyNode;
    elements.copyFilePathBtn.hidden = true;
    return;
  }
  const parts = filePath.split('/').filter(Boolean);
  const fragment = document.createDocumentFragment();
  parts.forEach((segment, index) => {
    if (index > 0) {
      const sep = document.createElement('span');
      sep.className = 'breadcrumb-sep';
      sep.textContent = '›';
      sep.setAttribute('aria-hidden', 'true');
      fragment.appendChild(sep);
    }
    const span = document.createElement('span');
    if (index === parts.length - 1) {
      span.id = 'activeFileName';
      span.className = 'active-file';
      elements.activeFileName = span;
    } else {
      span.className = 'breadcrumb-seg';
    }
    span.textContent = segment;
    fragment.appendChild(span);
  });
  elements.editorBreadcrumb.replaceChildren(fragment);
  elements.editorBreadcrumb.title = filePath;
  elements.copyFilePathBtn.hidden = false;
}

/**
 * 将当前打开文件加入多文件标签栏（去重且保持上限），并重绘标签栏。
 *
 * @param {string} path 文件相对路径
 * @param {string} kind 文件类型
 */
function upsertEditorTab(path, kind) {
  const existing = state.openTabs.find((tab) => tab.path === path);
  if (existing) {
    existing.kind = kind || existing.kind;
  } else {
    state.openTabs.push({ path, kind: kind || 'SOURCE' });
    if (state.openTabs.length > MAX_OPEN_EDITOR_TABS) {
      state.openTabs.shift();
    }
  }
  renderEditorTabs();
}

/**
 * 重绘编辑器顶部多文件标签栏。
 */
function renderEditorTabs() {
  if (!state.openTabs.length) {
    elements.editorTabs.hidden = true;
    elements.editorTabs.replaceChildren();
    return;
  }
  elements.editorTabs.hidden = false;
  const dirty = isEditorDirty();
  const fragment = document.createDocumentFragment();
  for (const tab of state.openTabs) {
    const active = tab.path === state.currentFilePath;
    const fileName = tab.path.split('/').pop() || tab.path;
    const tabBtn = document.createElement('div');
    tabBtn.className = `editor-tab ${active ? 'active' : ''}`;
    tabBtn.setAttribute('role', 'tab');
    tabBtn.setAttribute('aria-selected', String(active));
    tabBtn.title = tab.path;
    tabBtn.tabIndex = 0;

    if (active && dirty) {
      const dot = document.createElement('span');
      dot.className = 'editor-tab-dot';
      dot.title = MESSAGE_UNSAVED_CHANGES;
      tabBtn.appendChild(dot);
    }

    const nameSpan = document.createElement('span');
    nameSpan.className = 'editor-tab-name';
    nameSpan.textContent = fileName;
    tabBtn.appendChild(nameSpan);

    const closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'editor-tab-close';
    closeBtn.setAttribute('aria-label', `关闭 ${fileName}`);
    closeBtn.title = '关闭标签';
    closeBtn.innerHTML = TAB_CLOSE_ICON_SVG;
    closeBtn.addEventListener('click', (event) => {
      event.stopPropagation();
      void closeEditorTab(tab.path);
    });
    tabBtn.appendChild(closeBtn);

    tabBtn.addEventListener('click', () => {
      if (tab.path !== state.currentFilePath) {
        void openFile({ path: tab.path, kind: tab.kind, editable: true });
      }
    });
    tabBtn.addEventListener('keydown', (event) => {
      if ((event.key === 'Enter' || event.key === ' ') && tab.path !== state.currentFilePath) {
        event.preventDefault();
        void openFile({ path: tab.path, kind: tab.kind, editable: true });
      }
    });
    fragment.appendChild(tabBtn);
  }
  elements.editorTabs.replaceChildren(fragment);
}

/**
 * 关闭指定文件标签；如果关闭的是当前激活文件，先确认未保存改动再切换到相邻标签或恢复空态。
 *
 * @param {string} targetPath 待关闭文件路径
 */
async function closeEditorTab(targetPath) {
  const index = state.openTabs.findIndex((tab) => tab.path === targetPath);
  if (index < 0) {
    return;
  }
  const closingActive = targetPath === state.currentFilePath;
  if (closingActive && !(await confirmDiscardUnsavedChanges())) {
    return;
  }
  state.openTabs.splice(index, 1);
  if (!closingActive) {
    renderEditorTabs();
    return;
  }
  const nextTab = state.openTabs[index] || state.openTabs[index - 1] || null;
  if (nextTab) {
    state.currentFileOriginalContent = null;
    await openFile({ path: nextTab.path, kind: nextTab.kind, editable: true });
    return;
  }
  clearActiveFileState();
}

/**
 * 清空当前打开的文件状态并回到编辑器空态，同时保持项目已打开。
 */
function clearActiveFileState() {
  state.documentSessionId++;
  state.currentFilePath = null;
  state.currentFileHash = null;
  state.currentFileEncoding = null;
  state.currentFileOriginalContent = null;
  closeUnicodePreview();
  elements.editor.closeDocument();
  elements.editor.disabled = true;
  elements.saveBtn.disabled = true;
  elements.saveBtn.classList.remove('busy');
  elements.fileEncodingSelect.disabled = true;
  elements.activeFileDirtyDot.hidden = true;
  renderEditorBreadcrumb(null);
  elements.activeFileKind.textContent = MESSAGE_ACTIVE_FILE_TIP;
  elements.activeFileKind.dataset.kind = '';
  updateLanguageStatus(null);
  updateCursorStatus(null);
  updateEditorEmptyState();
  updateEditorActionButtons();
  renderEditorTabs();
  renderCurrentTree(true);
}

/**
 * 复制当前打开文件的相对路径到系统剪贴板。
 */
async function copyCurrentFilePath() {
  if (!state.currentFilePath) {
    return;
  }
  try {
    await navigator.clipboard.writeText(state.currentFilePath);
    notify(`${MESSAGE_PATH_COPIED}：${state.currentFilePath}`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_PATH_COPY_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 更新底部 IDE 状态栏中的光标行列号显示。
 *
 * @param {{lineNumber: number, column: number}|null} position 光标位置
 */
function updateCursorStatus(position) {
  if (!position || !state.currentFilePath) {
    elements.statusCursorPos.hidden = true;
    return;
  }
  elements.statusCursorPos.textContent = `行 ${position.lineNumber}, 列 ${position.column}`;
  elements.statusCursorPos.hidden = false;
}

/**
 * 更新底部 IDE 状态栏中的语言标识。
 *
 * @param {string|null} filePath 当前文件路径
 */
function updateLanguageStatus(filePath) {
  if (!filePath) {
    elements.statusLanguage.hidden = true;
    return;
  }
  const languageId = window.jarPatchEditor.detectLanguage(filePath);
  elements.statusLanguage.textContent = LANGUAGE_LABEL_MAP[languageId] || languageId;
  elements.statusLanguage.hidden = false;
}

/**
 * 根据当前登录用户角色同步界面管理员权限入口及底部状态栏身份。
 *
 * @param {object|null} user 当前已认证用户
 */
function applyUserRoleUi(user) {
  const admin = Boolean(user && user.role === 'ADMIN');
  elements.settingsBtn.hidden = !admin;
  elements.cleanupWorkspaceBtn.hidden = !admin;
  elements.orphanWorkspacesBtn.hidden = !admin;
  if (!user) {
    elements.statusCurrentUser.textContent = '未登录';
    return;
  }
  const roleLabel = admin ? '管理员' : '普通用户';
  elements.statusCurrentUser.textContent = `${user.displayName || user.username} · ${roleLabel}`;
}

/**
 * 更新带图标按钮的文案节点，避免直接改写 textContent 误删内置 SVG 图标。
 *
 * @param {HTMLButtonElement} button 目标按钮
 * @param {string} text 新文案
 */
function setButtonLabel(button, text) {
  const label = button.querySelector('.btn-label');
  if (label) {
    label.textContent = text;
    return;
  }
  button.textContent = text;
}

/**
 * 解析包类型对应的中文短标签和样式类。
 *
 * @param {string} packageType 后端包类型枚举值
 * @returns {{label: string, className: string}} 展示徽标信息
 */
function getPackageTypeBadge(packageType) {
  return PACKAGE_TYPE_BADGE_MAP[packageType] || { label: packageType || 'JAR', className: 'pkg-jar' };
}

/**
 * 同步顶部工具栏的包类型徽标。
 *
 * @param {object|null} project 当前打开的项目
 */
function updateCurrentProjectBadge(project) {
  if (!project) {
    elements.currentProjectBadge.hidden = true;
    elements.currentProjectBadge.textContent = '';
    elements.currentProjectBadge.className = 'pkg-badge';
    return;
  }
  const badge = getPackageTypeBadge(project.packageType);
  elements.currentProjectBadge.hidden = false;
  elements.currentProjectBadge.textContent = badge.label;
  elements.currentProjectBadge.className = `pkg-badge ${badge.className}`;
}

/**
 * 按当前是否已打开项目和文件更新编辑器欢迎空状态覆盖层。
 */
function updateEditorEmptyState() {
  const hasFile = Boolean(state.currentFilePath);
  elements.editorEmptyState.hidden = hasFile;
  if (hasFile) {
    return;
  }
  const hasProject = Boolean(state.currentProject);
  elements.emptyStateTitle.textContent = hasProject ? MESSAGE_EMPTY_NO_FILE_TITLE : MESSAGE_EMPTY_NO_PROJECT_TITLE;
  elements.emptyStateDesc.textContent = hasProject ? MESSAGE_EMPTY_NO_FILE_DESC : MESSAGE_EMPTY_NO_PROJECT_DESC;
  elements.emptyStateActions.hidden = hasProject;
}

/**
 * 根据编辑器脏状态更新文件提示、未保存圆点、保存按钮以及多文件标签栏。
 */
function updateDirtyIndicator() {
  if (!state.currentFilePath) {
    elements.activeFileDirtyDot.hidden = true;
    return;
  }
  const dirty = isEditorDirty();
  const kind = elements.activeFileKind.dataset.kind || '';
  elements.activeFileDirtyDot.hidden = !dirty;
  elements.activeFileKind.textContent = dirty ? `${kind} · ${MESSAGE_UNSAVED_CHANGES}` : kind;
  elements.saveBtn.classList.toggle('busy', dirty);
  renderEditorTabs();
  scheduleUnicodePreviewButtonRefresh();
}

/**
 * 根据文件类型、当前预览状态和已启用能力更新编辑器工具栏按钮。
 */
function updateEditorActionButtons() {
  const hasFile = Boolean(state.currentProject && state.currentFilePath);
  // 只读中文预览态禁用源码级修复与 AI 改写入口，避免操作隐藏中的源编辑器。
  const editableView = hasFile && !state.unicodePreviewOpen;
  const javaFile = editableView && state.currentFilePath.toLowerCase().endsWith('.java');
  const aiEnabled = Boolean(state.developmentSettings && state.developmentSettings.aiEnabled);
  elements.findInEditorBtn.disabled = !hasFile;
  elements.quickFixBtn.disabled = !javaFile || !state.languageServerReady;
  elements.organizeImportsBtn.disabled = !javaFile || !state.languageServerReady;
  elements.aiInlineBtn.disabled = !editableView || !aiEnabled;
  elements.aiAssistantBtn.disabled = !editableView || !aiEnabled;
}

/**
 * 展示 Java 语言服务器状态和当前文件诊断数量。
 *
 * @param status Monaco 编辑器返回的语言智能状态
 */
function handleLanguageStatus(status) {
  state.languageServerReady = status.state === 'ready';
  elements.editorIntelligenceStatus.dataset.state = status.state || 'idle';
  elements.editorIntelligenceText.textContent = status.message || MESSAGE_LANGUAGE_SERVER_NOT_CONFIGURED;
  updateEditorActionButtons();
}

/**
 * 按当前开发配置启动项目专属 Java 语言服务器。
 *
 * @param project 当前项目
 */
async function startLanguageIntelligence(project) {
  state.languageServerReady = false;
  updateEditorActionButtons();
  if (!state.developmentSettings || !state.developmentSettings.languageServerEnabled) {
    await elements.editor.stopLanguageServer();
    handleLanguageStatus({ state: 'idle', message: MESSAGE_LANGUAGE_SERVER_NOT_CONFIGURED });
    return;
  }
  try {
    await elements.editor.startLanguageServer(project);
  } catch (error) {
    handleLanguageStatus({ state: 'error', message: `${MESSAGE_LANGUAGE_SERVER_UNAVAILABLE}：${error.message}` });
  }
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
 * 将文本中符合 Java 反斜杠规则的中文 Unicode 转义转换为只读预览内容，
 * 并同步记录每段连续解码中文在预览文本中的 1-based 行列区间，供 Monaco 高亮与上下处跳转。
 * 连续偶数个反斜杠表示转义后的字面量，不参与转换，避免改变原代码含义。
 *
 * @param {string} content 编辑器原始文本
 * @returns {{content: string, convertedCount: number, ranges: Array<object>, lineCount: number}} 预览文本、转换字数、连续区间与涉及行数
 */
function decodeChineseUnicodeEscapes(content) {
  let index = 0;
  let convertedCount = 0;
  let lineNumber = UNICODE_PREVIEW_FIRST_LINE;
  let column = UNICODE_PREVIEW_FIRST_COLUMN;
  const previewParts = [];
  const ranges = [];
  const decodedLines = new Set();
  let activeSpan = null;

  /**
   * 结束当前连续中文解码区间并写入结果数组。
   */
  const flushActiveSpan = () => {
    if (activeSpan) {
      ranges.push(activeSpan);
      activeSpan = null;
    }
  };

  while (index < content.length) {
    const ch = content[index];
    if (ch !== '\\') {
      flushActiveSpan();
      if (ch === '\r') {
        if (content[index + 1] === '\n') {
          previewParts.push('\r\n');
          index += CRLF_SEQUENCE_LENGTH;
        } else {
          previewParts.push('\r');
          index++;
        }
        lineNumber++;
        column = UNICODE_PREVIEW_FIRST_COLUMN;
        continue;
      }
      if (ch === '\n') {
        previewParts.push('\n');
        lineNumber++;
        column = UNICODE_PREVIEW_FIRST_COLUMN;
        index++;
        continue;
      }
      previewParts.push(ch);
      column++;
      index++;
      continue;
    }

    let slashEnd = index;
    while (slashEnd < content.length && content[slashEnd] === '\\') {
      slashEnd++;
    }
    const slashCount = slashEnd - index;
    if (slashCount % BACKSLASH_ESCAPE_PAIR_LENGTH === 0) {
      flushActiveSpan();
      previewParts.push(content.slice(index, slashEnd));
      column += slashCount;
      index = slashEnd;
      continue;
    }

    const prefixSlashCount = slashCount - 1;
    if (prefixSlashCount > 0) {
      flushActiveSpan();
      previewParts.push(content.slice(index, slashEnd - 1));
      column += prefixSlashCount;
    }

    let unicodeEnd = slashEnd;
    while (unicodeEnd < content.length && content[unicodeEnd] === 'u') {
      unicodeEnd++;
    }
    const hexadecimal = content.slice(unicodeEnd, unicodeEnd + UNICODE_ESCAPE_HEX_LENGTH);
    if (unicodeEnd > slashEnd && UNICODE_ESCAPE_HEX_PATTERN.test(hexadecimal)) {
      const decoded = String.fromCharCode(Number.parseInt(hexadecimal, UNICODE_HEX_RADIX));
      if (isChineseReadableCharacter(decoded)) {
        previewParts.push(decoded);
        // 连续的中文转义合并为同一高亮区间，便于整词高亮与“上一处/下一处”按片段跳转。
        if (activeSpan && activeSpan.endLineNumber === lineNumber && activeSpan.endColumn === column) {
          activeSpan.endColumn += decoded.length;
        } else {
          flushActiveSpan();
          activeSpan = {
            startLineNumber: lineNumber,
            startColumn: column,
            endLineNumber: lineNumber,
            endColumn: column + decoded.length
          };
        }
        decodedLines.add(lineNumber);
        column += decoded.length;
        convertedCount++;
        index = unicodeEnd + UNICODE_ESCAPE_HEX_LENGTH;
        continue;
      }
    }
    flushActiveSpan();
    previewParts.push('\\');
    column++;
    index = slashEnd;
  }

  flushActiveSpan();
  return {
    content: previewParts.join(''),
    convertedCount,
    ranges,
    lineCount: decodedLines.size
  };
}

/**
 * 懒加载创建独立的只读 Monaco 中文预览实例，并接入底部状态栏光标同步。
 *
 * @returns {object} 只读预览视图控制器
 */
function ensureUnicodePreviewView() {
  if (!state.unicodePreviewView) {
    state.unicodePreviewView = window.jarPatchEditor.createUnicodePreviewView(elements.unicodePreview);
    state.unicodePreviewView.onCursorChange((position) => {
      if (state.unicodePreviewOpen) {
        updateCursorStatus(position);
      }
    });
  }
  return state.unicodePreviewView;
}

/**
 * 更新中文预览栏中的当前定位计数与上下处按钮可用态。
 */
function updateUnicodePreviewMatchStatus() {
  const total = state.unicodePreviewRanges.length;
  const activeRange = total > 0 && state.unicodePreviewActiveIndex >= UNICODE_PREVIEW_FIRST_INDEX
    ? state.unicodePreviewRanges[state.unicodePreviewActiveIndex]
    : null;
  if (activeRange) {
    elements.unicodePreviewMatchStatus.textContent = `${state.unicodePreviewActiveIndex + 1}${MESSAGE_UNICODE_PREVIEW_MATCH_SEPARATOR}${total}${MESSAGE_UNICODE_PREVIEW_LINE_PREFIX}${activeRange.startLineNumber}${MESSAGE_UNICODE_PREVIEW_LINE_SUFFIX}`;
  } else {
    elements.unicodePreviewMatchStatus.textContent = '';
  }
  const canNavigate = total >= UNICODE_PREVIEW_MIN_NAVIGABLE_COUNT;
  elements.unicodePreviewPrevBtn.disabled = !canNavigate;
  elements.unicodePreviewNextBtn.disabled = !canNavigate;
}

/**
 * 刷新只读中文预览顶部提示条摘要信息。
 *
 * @param {{convertedCount: number, ranges: Array<object>, lineCount: number}} decoded 解码统计结果
 */
function renderUnicodePreviewSummary(decoded) {
  elements.unicodePreviewSummary.textContent = `${MESSAGE_UNICODE_PREVIEW_SUMMARY_PREFIX}${decoded.ranges.length}${MESSAGE_UNICODE_PREVIEW_SUMMARY_SPAN_UNIT}${decoded.convertedCount}${MESSAGE_UNICODE_PREVIEW_SUMMARY_CHAR_UNIT}${decoded.lineCount}${MESSAGE_UNICODE_PREVIEW_SUMMARY_LINE_UNIT}`;
  updateUnicodePreviewMatchStatus();
}

/**
 * 在只读中文预览中跳转到上一处或下一处解码中文位置。
 *
 * @param {number} step 步进方向（-1 表示上一处，1 表示下一处）
 */
function navigateUnicodePreviewMatch(step) {
  if (!state.unicodePreviewOpen || !state.unicodePreviewView || !state.unicodePreviewRanges.length) {
    return;
  }
  state.unicodePreviewActiveIndex = state.unicodePreviewView.revealRange(state.unicodePreviewActiveIndex + step);
  updateUnicodePreviewMatchStatus();
  state.unicodePreviewView.focus();
}

/**
 * 保存后刷新当前打开的只读中文预览内容，保持滚动位置不变。
 */
function refreshUnicodePreviewContent() {
  if (!state.unicodePreviewOpen || !state.unicodePreviewView) {
    return;
  }
  const decoded = decodeChineseUnicodeEscapes(elements.editor.value);
  if (!decoded.convertedCount) {
    closeUnicodePreview({ syncScrollToEditor: true });
    return;
  }
  state.unicodePreviewRanges = decoded.ranges;
  state.unicodePreviewActiveIndex = state.unicodePreviewView.updateContent({
    content: decoded.content,
    language: elements.editor.getLanguageId(),
    ranges: decoded.ranges
  });
  renderUnicodePreviewSummary(decoded);
}

/**
 * 输入路径上的 Unicode 预览按钮刷新按固定间隔合帧；全文转义检查随文件大小线性，
 * 逐键执行会让大文件输入持续卡顿。打开文件、保存等一次性路径仍直接调用
 * updateUnicodePreviewButton。
 */
function scheduleUnicodePreviewButtonRefresh() {
  if (state.unicodePreviewRefreshTimer) {
    return;
  }
  state.unicodePreviewRefreshTimer = setTimeout(() => {
    state.unicodePreviewRefreshTimer = null;
    updateUnicodePreviewButton();
  }, UNICODE_PREVIEW_REFRESH_DELAY_MS);
}

/**
 * 根据当前文件内容与预览状态更新中文预览按钮可用性。
 */
function updateUnicodePreviewButton() {
  elements.unicodePreviewBtn.disabled = elements.editor.disabled
    || !state.currentFilePath
    || (!state.unicodePreviewOpen && !elements.editor.value.includes('\\u'));
}

/**
 * 关闭只读中文预览并恢复原始编辑器；原始文本始终保存在 editor 中，不参与替换。
 *
 * @param {{syncScrollToEditor?: boolean}} [options] 关闭选项；用户主动返回原文时同步预览滚动位置回主编辑器
 */
function closeUnicodePreview(options = {}) {
  const { syncScrollToEditor = false } = options;
  const previewScrollTop = state.unicodePreviewOpen && syncScrollToEditor && state.unicodePreviewView
    ? state.unicodePreviewView.getScrollTop()
    : null;
  state.unicodePreviewOpen = false;
  state.unicodePreviewRanges = [];
  state.unicodePreviewActiveIndex = UNICODE_PREVIEW_NO_ACTIVE_INDEX;
  if (state.unicodePreviewView) {
    state.unicodePreviewView.clear();
  }
  elements.unicodePreviewPanel.hidden = true;
  elements.unicodePreviewSummary.textContent = '';
  elements.unicodePreviewMatchStatus.textContent = '';
  elements.unicodePreviewPrevBtn.disabled = true;
  elements.unicodePreviewNextBtn.disabled = true;
  elements.editor.hidden = false;
  if (previewScrollTop !== null) {
    // 预览与原文行号严格 1:1 对齐，返回原文时同步纵向位置便于直接定位修改。
    elements.editor.scrollTop = previewScrollTop;
  }
  if (state.currentFilePath) {
    updateCursorStatus(elements.editor.getCursorPosition());
  }
  setButtonLabel(elements.unicodePreviewBtn, BUTTON_TEXT_UNICODE_PREVIEW);
  elements.unicodePreviewBtn.setAttribute('aria-pressed', 'false');
  updateEditorActionButtons();
  updateUnicodePreviewButton();
}

/**
 * 在原始 Monaco 编辑器和只读 Monaco 中文预览之间切换。
 * 入口为顶部“中文预览 / 返回原文”按钮；渲染结果仅落在独立只读预览模型，不改写源文件。
 */
function toggleUnicodePreview() {
  if (state.unicodePreviewOpen) {
    closeUnicodePreview({ syncScrollToEditor: true });
    elements.editor.focus();
    return;
  }
  const decoded = decodeChineseUnicodeEscapes(elements.editor.value);
  if (!decoded.convertedCount) {
    notify(MESSAGE_UNICODE_PREVIEW_EMPTY, NOTICE_TYPE_INFO);
    return;
  }
  const sourceScrollTop = elements.editor.scrollTop;
  // 先切换面板可见性，再初始化并打开 Monaco 只读预览，确保视口高度与滚动定位计算准确。
  elements.editor.hidden = true;
  elements.unicodePreviewPanel.hidden = false;
  const previewView = ensureUnicodePreviewView();
  const openResult = previewView.open({
    content: decoded.content,
    language: elements.editor.getLanguageId(),
    ranges: decoded.ranges,
    initialScrollTop: sourceScrollTop
  });
  state.unicodePreviewRanges = decoded.ranges;
  state.unicodePreviewActiveIndex = openResult.activeIndex;
  state.unicodePreviewOpen = true;
  renderUnicodePreviewSummary(decoded);
  setButtonLabel(elements.unicodePreviewBtn, BUTTON_TEXT_UNICODE_SOURCE);
  elements.unicodePreviewBtn.setAttribute('aria-pressed', 'true');
  updateEditorActionButtons();
  updateUnicodePreviewButton();
  previewView.focus();
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
 * 更新任务状态栏文案、可视化状态点与进度条百分比。
 *
 * @param title    标题文案
 * @param detail   详情文案
 * @param active   是否处于执行中
 * @param barState 任务栏视觉状态（idle / running / done / failed / canceled）
 * @param progress 进度百分比（0-100），传入 null 时保持当前进度条
 */
function updateTaskPanel(title, detail, active, barState = null, progress = null) {
  elements.taskStatusText.textContent = title;
  updateTaskDetail(detail);
  elements.cancelTaskBtn.hidden = !active;
  elements.cancelTaskBtn.disabled = !active || state.currentTaskCancelRequested;
  elements.taskBar.dataset.state = barState || (active ? TASK_STATE_RUNNING : TASK_STATE_IDLE);
  elements.dismissTaskBtn.hidden = barState !== TASK_STATE_FAILED;
  updateCompileRetryButton();
  if (typeof progress === 'number' && !Number.isNaN(progress)) {
    const clamped = Math.min(Math.max(progress, TASK_PROGRESS_MIN), TASK_PROGRESS_MAX);
    elements.taskProgressFill.style.width = `${clamped}%`;
  }
}

function updateTaskDetail(detail) {
  elements.taskStatusDetail.textContent = detail;
  elements.taskStatusDetail.title = detail;
}

function updateCompileRetryButton() {
  elements.retryCompileBtn.hidden = elements.taskBar.dataset.state !== TASK_STATE_FAILED
    || !state.currentProject || state.failedCompileProjectId !== state.currentProject.id;
}

function dismissTaskFailure() {
  state.failedCompileProjectId = null;
  updateTaskPanel(MESSAGE_TASK_IDLE, MESSAGE_TASK_IDLE_DETAIL, false, TASK_STATE_IDLE, TASK_PROGRESS_MIN);
}

/**
 * 恢复工作台可操作状态。
 */
function restoreWorkspaceControls() {
  if (state.taskStarting || state.currentTask) {
    return;
  }
  elements.openArchiveBtn.disabled = false;
  elements.sidebarQuickOpenBtn.disabled = false;
  elements.settingsBtn.disabled = false;
  elements.errorGuideBtn.disabled = false;
  elements.projectSettingsBtn.disabled = !state.currentProject;
  elements.projectHistoryBtn.disabled = !state.currentProject;
  elements.cleanupWorkspaceBtn.disabled = !state.currentProject;
  elements.orphanWorkspacesBtn.disabled = false;
  elements.projectList.style.pointerEvents = '';
  elements.fileTree.style.pointerEvents = '';
  elements.searchResults.style.pointerEvents = '';
  elements.analysisToggleBtn.disabled = false;
  elements.analysisCloseBtn.disabled = false;
  elements.compactFoldersBtn.disabled = !state.currentProject;
  elements.collapseTreeBtn.disabled = !state.currentProject;
  elements.searchInput.disabled = !state.currentProject;
  elements.searchBtn.disabled = !state.currentProject;
  elements.saveBtn.disabled = !state.currentProject || !state.currentFilePath;
  elements.fileEncodingSelect.disabled = !state.currentProject || !state.currentFilePath;
  elements.editor.disabled = !state.currentProject || !state.currentFilePath;
  updateUnicodePreviewButton();
  updateEditorActionButtons();
  elements.analyzeBtn.disabled = !state.currentProject;
  elements.diffBtn.disabled = !state.currentProject;
  elements.compileBtn.disabled = !state.currentProject;
  elements.patchDirectoryBtn.disabled = !state.currentProject || state.patchDirectoryOpening;
  elements.exportBtn.disabled = !state.currentProject;
  elements.compileBtn.classList.remove('busy');
  elements.exportBtn.classList.remove('busy');
  setButtonLabel(elements.compileBtn, BUTTON_TEXT_COMPILE);
  setButtonLabel(elements.exportBtn, BUTTON_TEXT_EXPORT);
  setButtonLabel(elements.analyzeBtn, BUTTON_TEXT_ANALYZE);
  elements.cancelTaskBtn.disabled = true;
  updateCompileRetryButton();
}

/**
 * 锁定工作台控件，避免任务执行期间并发修改。
 */
function lockWorkspaceControls() {
  elements.openArchiveBtn.disabled = true;
  elements.sidebarQuickOpenBtn.disabled = true;
  elements.settingsBtn.disabled = true;
  elements.errorGuideBtn.disabled = true;
  elements.projectSettingsBtn.disabled = true;
  elements.projectHistoryBtn.disabled = true;
  elements.cleanupWorkspaceBtn.disabled = true;
  elements.orphanWorkspacesBtn.disabled = true;
  elements.projectList.style.pointerEvents = 'none';
  elements.fileTree.style.pointerEvents = 'none';
  elements.searchResults.style.pointerEvents = 'none';
  elements.analysisToggleBtn.disabled = true;
  elements.analysisCloseBtn.disabled = true;
  elements.compactFoldersBtn.disabled = true;
  elements.collapseTreeBtn.disabled = true;
  elements.searchInput.disabled = true;
  elements.searchBtn.disabled = true;
  elements.saveBtn.disabled = true;
  elements.fileEncodingSelect.disabled = true;
  elements.unicodePreviewBtn.disabled = true;
  elements.editor.disabled = true;
  elements.quickFixBtn.disabled = true;
  elements.organizeImportsBtn.disabled = true;
  elements.aiInlineBtn.disabled = true;
  elements.aiAssistantBtn.disabled = true;
  elements.analyzeBtn.disabled = true;
  elements.diffBtn.disabled = true;
  elements.compileBtn.disabled = true;
  elements.patchDirectoryBtn.disabled = true;
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
async function openTaskSocket(taskId) {
  closeTaskSocket();
  let socket;
  try {
    socket = new WebSocket(window.jarPatchApiClient.taskWebSocketUrl(taskId));
  } catch (error) {
    appendLog(`${MESSAGE_TASK_LOG_UNAVAILABLE}：${error.message}`);
    return null;
  }
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
      updateTaskPanel(`${taskLabel} · 0%`, MESSAGE_TASK_CONNECTED, true, TASK_STATE_RUNNING, TASK_PROGRESS_MIN);
      settle(socket);
    };
    socket.onmessage = (event) => {
      const message = String(event.data || '');
      appendLog(message);
      const parsed = parseTaskProgress(message);
      if (parsed.progress == null) {
        updateTaskDetail(parsed.detail);
        return;
      }
      updateTaskPanel(`${taskLabel} · ${parsed.progress}%`, parsed.detail, true, TASK_STATE_RUNNING, parsed.progress);
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
  state.taskStarting = false;
  state.currentTaskAbortController = new AbortController();
  state.currentTaskCancelRequested = false;
  updateTaskPanel(`${getTaskLabel(task.taskType)} · 0%`, task.message || MESSAGE_TASK_PANEL_READY, true, TASK_STATE_RUNNING, TASK_PROGRESS_MIN);
}

/**
 * 结束当前任务会话。
 *
 * @param taskType 任务类型
 * @param status   最终状态文案
 * @param detail   最终详情文案
 * @param barState 任务栏结束态标识
 */
function endTaskSession(taskType, status, detail, barState) {
  const label = getTaskLabel(taskType);
  const finalProgress = barState === TASK_STATE_DONE ? TASK_PROGRESS_MAX : null;
  updateTaskPanel(`${label} · ${status}`, detail, false, barState, finalProgress);
  closeTaskSocket();
  state.taskStarting = false;
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
  if (state.taskStarting || state.currentTask) {
    throw new Error(MESSAGE_TASK_IN_PROGRESS);
  }
  state.taskStarting = true;
  state.failedCompileProjectId = null;
  lockWorkspaceControls();
  if (taskType === TASK_TYPE_COMPILE) {
    setActionButtonRunning(elements.compileBtn, BUTTON_TEXT_COMPILE_RUNNING);
  } else if (taskType === TASK_TYPE_EXPORT) {
    setActionButtonRunning(elements.exportBtn, BUTTON_TEXT_EXPORT_RUNNING);
  }
  updateTaskPanel(`${getTaskLabel(taskType)} · 0%`, message, false, TASK_STATE_RUNNING, TASK_PROGRESS_MIN);
  try {
    const task = await api('/api/tasks', {
      method: 'POST',
      body: JSON.stringify({ taskType, projectId, message })
    });
    beginTaskSession(task);
    await openTaskSocket(task.id);
    const result = await requestRunner(task, state.currentTaskAbortController.signal);
    endTaskSession(taskType, MESSAGE_TASK_PANEL_DONE, result && result.message ? result.message : MESSAGE_TASK_COMPLETED, TASK_STATE_DONE);
    return result;
  } catch (error) {
    if (state.currentTask && isTaskCanceledError(error)) {
      endTaskSession(taskType, MESSAGE_TASK_PANEL_CANCELED, MESSAGE_TASK_CANCELED, TASK_STATE_CANCELED);
      notify(MESSAGE_TASK_CANCELED, NOTICE_TYPE_INFO);
      return null;
    }
    if (taskType === TASK_TYPE_COMPILE) {
      state.failedCompileProjectId = projectId;
    }
    const failureMessage = state.taskStarting ? MESSAGE_TASK_START_FAILED : MESSAGE_TASK_FAILED;
    endTaskSession(taskType, MESSAGE_TASK_PANEL_FAILED, error.message || failureMessage, TASK_STATE_FAILED);
    throw error;
  }
}

/**
 * 取消当前执行中的任务。
 */
async function cancelCurrentTask() {
  if (!state.currentTask || state.currentTaskCancelRequested) {
    return;
  }
  const task = state.currentTask;
  const controller = state.currentTaskAbortController;
  state.currentTaskCancelRequested = true;
  updateTaskPanel(`${getTaskLabel(task.taskType)} · ${MESSAGE_TASK_PANEL_RUNNING}`, MESSAGE_TASK_CANCELING, true);
  try {
    const latest = await api(`/api/tasks/${task.id}/cancel`, { method: 'POST' });
    if (!state.currentTask || state.currentTask.id !== task.id) {
      return;
    }
    if (latest.status === TASK_STATUS_CANCELED) {
      if (controller) {
        controller.abort();
      }
    } else {
      // 最终提交先于取消完成时保留业务响应，由实际结果结束会话。
      state.currentTaskCancelRequested = false;
      updateTaskPanel(`${getTaskLabel(task.taskType)} · ${MESSAGE_TASK_PANEL_RUNNING}`, latest.message, true);
    }
  } catch (error) {
    if (state.currentTask && state.currentTask.id === task.id) {
      state.currentTaskCancelRequested = false;
      updateTaskPanel(`${getTaskLabel(task.taskType)} · ${MESSAGE_TASK_PANEL_RUNNING}`, `${MESSAGE_TASK_CANCEL_FAILED}：${error.message}`, true);
      notify(`${MESSAGE_TASK_CANCEL_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
    }
  }
}

/**
 * 切换开发能力配置弹窗的分段标签页。
 *
 * @param {string} tabName 目标标签名（jdk / jdtls / ai）
 */
function selectSettingsTab(tabName) {
  const normalized = tabName === SETTINGS_TAB_JDTLS || tabName === SETTINGS_TAB_AI
    ? tabName
    : SETTINGS_TAB_JDK;
  state.activeSettingsTab = normalized;
  elements.settingsTabs.querySelectorAll('[data-settings-tab]').forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.settingsTab === normalized));
  });
  elements.settingsDialog.querySelectorAll('[data-settings-pane]').forEach((pane) => {
    pane.hidden = pane.dataset.settingsPane !== normalized;
  });
}

/**
 * 同步开发能力配置标签页的实时状态指示圆点。
 */
function updateSettingsTabDots() {
  const jdkReady = Boolean(state.jdkSettings && state.jdkSettings.effectiveValid);
  elements.settingsDotJdk.className = `tab-status-dot ${jdkReady ? 'ready' : 'warn'}`;

  const jdtlsEnabled = elements.languageServerEnabledInput.checked;
  elements.settingsDotJdtls.className = `tab-status-dot ${jdtlsEnabled ? 'ready' : ''}`.trim();

  const aiEnabled = elements.aiEnabledInput.checked;
  elements.settingsDotAi.className = `tab-status-dot ${aiEnabled ? 'ready' : ''}`.trim();
}

/**
 * 打开开发能力配置弹窗，同时读取编译 JDK、JDT LS 与 AI 配置。
 */
async function openSettingsDialog() {
  if (!window.jarPatchAuth.isAdmin()) {
    notify(MESSAGE_ADMIN_ONLY_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  selectSettingsTab(SETTINGS_TAB_JDK);
  elements.settingsDialog.classList.add('open');
  elements.settingsDialog.setAttribute('aria-hidden', 'false');
  renderJdkSettings(null, MESSAGE_SETTINGS_LOADING);
  void loadBundledJdkVersion();
  try {
    const [jdkSettings, developmentSettings] = await Promise.all([
      api('/api/settings/jdk'),
      window.jarPatch.getDevelopmentSettings()
    ]);
    state.jdkSettings = jdkSettings;
    state.developmentSettings = developmentSettings;
    renderJdkSettings(jdkSettings, MESSAGE_DEVELOPMENT_SETTINGS_SUMMARY);
    renderDevelopmentSettings(developmentSettings);
    updateSettingsTabDots();
    updateEditorActionButtons();
  } catch (error) {
    renderJdkSettings(null, MESSAGE_SETTINGS_LOADING);
    updateSettingsTabDots();
    notify(`${MESSAGE_DEVELOPMENT_SETTINGS_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 读取并显示随发布包分发的编译 JDK 版本。
 */
async function loadBundledJdkVersion() {
  elements.bundledJdkVersion.textContent = MESSAGE_SETTINGS_LOADING;
  try {
    const version = await window.jarPatch.getBundledJdkVersion();
    elements.bundledJdkVersion.textContent = version || MESSAGE_BUNDLED_JDK_NOT_INCLUDED;
  } catch (error) {
    console.error(MESSAGE_BUNDLED_JDK_LOAD_FAILED, error);
    elements.bundledJdkVersion.textContent = MESSAGE_BUNDLED_JDK_LOAD_FAILED;
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
 * 渲染脱敏后的 JDT LS 与 AI 配置。
 *
 * @param settings Electron 主进程返回的开发能力配置
 */
function renderDevelopmentSettings(settings) {
  const view = settings || {
    languageServerEnabled: false,
    jdtLsHome: '',
    jdtLsJavaHome: '',
    aiEnabled: false,
    aiProtocol: 'responses',
    aiMessagesAuth: 'x-api-key',
    aiEndpoint: '',
    aiModel: '',
    hasAiApiKey: false
  };
  elements.languageServerEnabledInput.checked = view.languageServerEnabled;
  elements.jdtLsHomeInput.value = view.jdtLsHome || '';
  elements.jdtLsJavaHomeInput.value = view.jdtLsJavaHome || '';
  elements.jdtLsHomeInput.placeholder = view.bundledLanguageServerAvailable
    ? '留空使用内置 Eclipse JDT LS'
    : '包含 plugins 和 config_win 等目录';
  elements.jdtLsJavaHomeInput.placeholder = view.bundledLanguageServerAvailable
    ? '留空使用内置 JDK 21'
    : 'Java 21 或更高版本的 JDK 目录';
  elements.aiEnabledInput.checked = view.aiEnabled;
  elements.aiProtocolSelect.value = view.aiProtocol || 'responses';
  elements.aiMessagesAuthSelect.value = view.aiMessagesAuth || 'x-api-key';
  updateAiProtocolFields();
  elements.aiEndpointInput.value = view.aiEndpoint || '';
  elements.aiModelInput.value = view.aiModel || '';
  elements.aiApiKeyInput.value = '';
  elements.clearAiApiKeyInput.checked = false;
  elements.developmentSettingsStatus.textContent = view.hasAiApiKey
    ? MESSAGE_AI_KEY_SAVED
    : MESSAGE_AI_KEY_EMPTY;
}

/**
 * 根据所选 AI 协议展示地址示例和适用的鉴权选项。
 */
function updateAiProtocolFields() {
  const option = elements.aiProtocolSelect.selectedOptions[0];
  const example = option?.dataset.endpointExample;
  elements.aiEndpointInput.placeholder = example ? `例如：${example}` : '';
  elements.aiMessagesAuthField.hidden = option?.value !== 'anthropic-messages';
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
  elements.languageServerEnabledInput.disabled = busy;
  elements.jdtLsHomeInput.disabled = busy;
  elements.browseJdtLsBtn.disabled = busy;
  elements.jdtLsJavaHomeInput.disabled = busy;
  elements.browseJdtLsJavaBtn.disabled = busy;
  elements.aiEnabledInput.disabled = busy;
  elements.aiProtocolSelect.disabled = busy;
  elements.aiMessagesAuthSelect.disabled = busy;
  elements.aiEndpointInput.disabled = busy;
  elements.aiModelInput.disabled = busy;
  elements.aiApiKeyInput.disabled = busy;
  elements.clearAiApiKeyInput.disabled = busy;
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
 * 浏览并填充 Eclipse JDT LS 解压目录。
 */
async function pickJdtLsHomeDirectory() {
  const selectedPath = await window.jarPatch.pickDirectory(
    elements.jdtLsHomeInput.value.trim(),
    '选择 Eclipse JDT LS 解压目录'
  );
  if (selectedPath) {
    elements.jdtLsHomeInput.value = selectedPath;
  }
}

/**
 * 浏览并填充 JDT LS 专用 Java 21+ 运行 JDK。
 */
async function pickJdtLsJavaHomeDirectory() {
  const selectedPath = await window.jarPatch.pickDirectory(
    elements.jdtLsJavaHomeInput.value.trim(),
    '选择 JDT LS 运行 JDK（Java 21+）'
  );
  if (selectedPath) {
    elements.jdtLsJavaHomeInput.value = selectedPath;
  }
}

/**
 * 保存编译 JDK、Java 语言服务器与 AI 配置，并分别报告两个独立配置域的结果。
 */
async function saveJdkSettings() {
  const javaHome = elements.jdkHomeInput.value.trim();
  setSettingsDialogBusy(true);
  try {
    const developmentRequest = {
      languageServerEnabled: elements.languageServerEnabledInput.checked,
      jdtLsHome: elements.jdtLsHomeInput.value.trim(),
      jdtLsJavaHome: elements.jdtLsJavaHomeInput.value.trim(),
      aiEnabled: elements.aiEnabledInput.checked,
      aiProtocol: elements.aiProtocolSelect.value,
      aiMessagesAuth: elements.aiMessagesAuthSelect.value,
      aiEndpoint: elements.aiEndpointInput.value.trim(),
      aiModel: elements.aiModelInput.value.trim(),
      aiApiKey: elements.aiApiKeyInput.value.trim(),
      clearAiApiKey: elements.clearAiApiKeyInput.checked
    };
    const jdkPromise = javaHome
      ? api('/api/settings/jdk', {
        method: 'PUT',
        body: JSON.stringify({ javaHome })
      })
      : Promise.resolve(state.jdkSettings);
    const [jdkResult, developmentResult] = await Promise.allSettled([
      jdkPromise,
      window.jarPatch.saveDevelopmentSettings(developmentRequest)
    ]);
    const failures = [];
    if (jdkResult.status === 'fulfilled') {
      state.jdkSettings = jdkResult.value;
      renderJdkSettings(jdkResult.value, MESSAGE_DEVELOPMENT_SETTINGS_SUMMARY);
    } else {
      failures.push(`${MESSAGE_COMPILE_JDK_LABEL}：${jdkResult.reason.message}`);
    }
    if (developmentResult.status === 'fulfilled') {
      state.developmentSettings = developmentResult.value;
      renderDevelopmentSettings(developmentResult.value);
      if (state.currentProject) {
        await startLanguageIntelligence(state.currentProject);
      }
    } else {
      failures.push(`${MESSAGE_DEVELOPMENT_SETTINGS_LABEL}：${developmentResult.reason.message}`);
    }
    updateSettingsTabDots();
    if (failures.length) {
      notify(`${MESSAGE_DEVELOPMENT_SETTINGS_PARTIAL}：${failures.join('；')}`, NOTICE_TYPE_ERROR);
      return;
    }
    notify(`${MESSAGE_SETTINGS_SAVE_SUCCESS}；${MESSAGE_DEVELOPMENT_SETTINGS_SAVE_SUCCESS}`, NOTICE_TYPE_SUCCESS);
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
    elements.projectDefaultEncodingSelect.value = settings.defaultEncoding || 'UTF-8';
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
 * 切换项目历史弹窗的分段标签页并渲染对应列表。
 *
 * @param {string} tabName 目标标签名（operations / analyses / validations）
 */
function selectHistoryTab(tabName) {
  const normalized = tabName === HISTORY_TAB_ANALYSES || tabName === HISTORY_TAB_VALIDATIONS
    ? tabName
    : HISTORY_TAB_OPERATIONS;
  state.activeHistoryTab = normalized;
  elements.projectHistoryTabs.querySelectorAll('[data-history-tab]').forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.historyTab === normalized));
  });
  if (state.currentProjectHistory) {
    renderActiveProjectHistoryTab();
  }
}

/**
 * 加载并展示当前项目最近的业务操作、分析和导出校验历史。
 */
async function openProjectHistoryDialog() {
  if (!state.currentProject) {
    return;
  }
  state.currentProjectHistory = null;
  selectHistoryTab(HISTORY_TAB_OPERATIONS);
  elements.projectHistoryDialog.classList.add('open');
  elements.projectHistoryDialog.setAttribute('aria-hidden', 'false');
  elements.projectHistoryContent.innerHTML = '<div class="empty">正在读取项目历史...</div>';
  try {
    const history = await api(`/api/projects/${state.currentProject.id}/history`);
    renderProjectHistory(history);
  } catch (error) {
    elements.projectHistoryContent.innerHTML = '<div class="empty">项目历史读取失败</div>';
    notify(`${MESSAGE_PROJECT_HISTORY_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 关闭项目历史弹窗。
 */
function closeProjectHistoryDialog() {
  elements.projectHistoryDialog.classList.remove('open');
  elements.projectHistoryDialog.setAttribute('aria-hidden', 'true');
}

/**
 * 解析历史操作状态对应的中文文案与徽标样式类。
 *
 * @param {string} status 后端操作状态值
 * @returns {{label: string, className: string}} 展示元数据
 */
function getHistoryStatusMeta(status) {
  const key = String(status || '').toUpperCase();
  return HISTORY_STATUS_META_MAP[key] || { label: status || '未知', className: 'status-running' };
}

/**
 * 保存并渲染项目历史聚合视图。
 *
 * @param {object} history 后端历史聚合数据
 */
function renderProjectHistory(history) {
  state.currentProjectHistory = history || { operations: [], analyses: [], exportValidations: [] };
  renderActiveProjectHistoryTab();
}

/**
 * 根据当前选中的项目历史分段渲染对应内容，展示操作人与状态徽标。
 */
function renderActiveProjectHistoryTab() {
  const history = state.currentProjectHistory || { operations: [], analyses: [], exportValidations: [] };
  if (state.activeHistoryTab === HISTORY_TAB_ANALYSES) {
    const analyses = (history.analyses || []).map((item) => {
      const risks = item.report && Array.isArray(item.report.risks) ? item.report.risks.length : 0;
      const badgeClass = risks > 0 ? 'status-canceled' : 'status-success';
      const badgeText = risks > 0 ? `${risks} 项风险` : '无风险';
      return `
        <div class="history-item">
          <div class="history-item-head">
            <strong>${escapeHtml(item.createdAt)}</strong>
            <span class="history-status-badge ${badgeClass}">${escapeHtml(badgeText)}</span>
          </div>
          <span>包类型：${escapeHtml(item.report && item.report.packageType ? item.report.packageType : '-')} · 修改文件：${item.report && Array.isArray(item.report.modifiedFiles) ? item.report.modifiedFiles.length : 0}</span>
        </div>`;
    }).join('') || '<div class="empty">暂无结构分析记录</div>';
    elements.projectHistoryContent.innerHTML = `<section class="history-section"><h3>结构分析历史</h3>${analyses}</section>`;
    return;
  }

  if (state.activeHistoryTab === HISTORY_TAB_VALIDATIONS) {
    const validations = (history.exportValidations || []).map((item) => `
      <div class="history-item">
        <div class="history-item-head">
          <strong>${escapeHtml(item.createdAt)}</strong>
          <span class="history-status-badge ${item.valid ? 'status-success' : 'status-failed'}">${item.valid ? '校验通过' : '未通过'}</span>
        </div>
        <span>${escapeHtml(item.outputPath || '-')}</span>
        <span>检查 ${item.checks ? item.checks.length : 0} 项，错误 ${item.errors ? item.errors.length : 0} 项</span>
      </div>`).join('') || '<div class="empty">暂无导出校验记录</div>';
    elements.projectHistoryContent.innerHTML = `<section class="history-section"><h3>导出校验历史</h3>${validations}</section>`;
    return;
  }

  const operations = (history.operations || []).map((item) => {
    const statusMeta = getHistoryStatusMeta(item.status);
    const operator = item.operator || DEFAULT_OPERATOR_NAME;
    return `
      <div class="history-item">
        <div class="history-item-head">
          <strong>${escapeHtml(item.createdAt)} · ${escapeHtml(item.operationType)}</strong>
          <span class="history-status-badge ${statusMeta.className}">${escapeHtml(statusMeta.label)}</span>
          <span class="history-operator-tag">@${escapeHtml(operator)}</span>
        </div>
        <span>操作 ID：${escapeHtml(item.operationId || '-')}</span>
        <span>${escapeHtml(item.details || '')}</span>
      </div>`;
  }).join('') || '<div class="empty">暂无操作记录</div>';
  elements.projectHistoryContent.innerHTML = `<section class="history-section"><h3>操作时间线</h3>${operations}</section>`;
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
        defaultEncoding: elements.projectDefaultEncodingSelect.value,
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
  if (!window.jarPatchAuth.isAdmin()) {
    notify(MESSAGE_ADMIN_ONLY_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  if (!state.currentProject || !(await confirmDiscardUnsavedChanges())) {
    return;
  }
  try {
    const preview = await api(`/api/projects/${state.currentProject.id}/workspace/cleanup-preview`);
    const confirmed = await window.jarPatchDialogs.confirm({
      title: '确认清理当前工作区',
      message: MESSAGE_WORKSPACE_CLEANUP_CONFIRM,
      tone: window.jarPatchDialogs.TONE_DANGER,
      details: [
        { label: '项目名称', value: preview.projectName },
        { label: '工作区路径', value: preview.workspacePath },
        { label: '文件数量', value: `${preview.fileCount} 个` },
        { label: '占用空间', value: formatBytes(preview.totalBytes) },
        { label: '最后使用', value: preview.lastUsedAt }
      ],
      actions: [
        { value: false, label: '取消', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
        { value: true, label: '确认清理工作区', variant: window.jarPatchDialogs.VARIANT_DANGER }
      ]
    });
    if (!confirmed) {
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
 * 请求后端生成脱敏诊断快照，并通过系统保存对话框导出 JSON。
 */
async function exportDiagnostics() {
  elements.diagnosticBtn.disabled = true;
  try {
    const snapshot = await api('/api/system/diagnostics');
    const content = `${JSON.stringify(snapshot, null, JSON_INDENT_SPACES)}\n`;
    const savedPath = await window.jarPatch.saveDiagnostic(DIAGNOSTIC_DEFAULT_FILE_NAME, content);
    if (savedPath) {
      notify(`${MESSAGE_DIAGNOSTIC_EXPORT_SUCCESS}：${savedPath}`, NOTICE_TYPE_SUCCESS);
    }
  } catch (error) {
    notify(`${MESSAGE_DIAGNOSTIC_EXPORT_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  } finally {
    elements.diagnosticBtn.disabled = false;
  }
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

/**
 * 从后端拉取历史项目列表并按当前筛选框实时渲染。
 */
async function loadProjects() {
  try {
    const projects = await api('/api/projects');
    state.allProjects = Array.isArray(projects) ? projects : [];
    renderFilteredProjects();
  } catch (error) {
    appendLog(`读取项目历史失败：${error.message}`);
  }
}

/**
 * 按左侧项目筛选框关键词过滤并重绘历史项目列表。
 */
function renderFilteredProjects() {
  const keyword = elements.projectSearchInput.value.trim().toLowerCase();
  if (!keyword) {
    renderProjects(state.allProjects, false);
    return;
  }
  const filtered = state.allProjects.filter((project) => {
    const name = String(project.name || '').toLowerCase();
    const type = String(project.packageType || '').toLowerCase();
    const workspacePath = String(project.workspacePath || '').toLowerCase();
    return name.includes(keyword) || type.includes(keyword) || workspacePath.includes(keyword);
  });
  renderProjects(filtered, true);
}

/**
 * 渲染左侧历史项目列表，展示包类型徽标、更新时间与管理员专属删除操作。
 *
 * @param {Array<object>} projects 过滤后的项目历史列表
 * @param {boolean} isFiltering    当前是否处于关键词筛选态
 */
function renderProjects(projects, isFiltering = false) {
  elements.projectList.innerHTML = '';
  const totalCount = state.allProjects.length;
  elements.projectCountBadge.textContent = isFiltering
    ? `${projects.length}/${totalCount}`
    : String(totalCount);
  if (!projects.length) {
    const emptyText = isFiltering ? MESSAGE_PROJECT_FILTER_EMPTY : MESSAGE_PROJECT_HISTORY_EMPTY;
    elements.projectList.innerHTML = `<div class="empty">${emptyText}</div>`;
    return;
  }
  const canDelete = window.jarPatchAuth && window.jarPatchAuth.isAdmin();
  projects.forEach((project) => {
    const item = document.createElement('div');
    item.className = `project-item ${state.currentProject && state.currentProject.id === project.id ? 'active' : ''}`;

    const selectButton = document.createElement('button');
    selectButton.className = 'project-main';
    selectButton.type = 'button';
    selectButton.title = `${project.name}\n${project.workspacePath || ''}`;
    const badge = getPackageTypeBadge(project.packageType);
    const cleanedBadge = project.workspaceCleanedAt
      ? `<span class="pkg-badge pkg-cleaned">${MESSAGE_WORKSPACE_CLEANED_MARKER}</span>`
      : '';
    selectButton.innerHTML = `
      <strong>${escapeHtml(project.name)}</strong>
      <div class="project-meta-row">
        <span class="pkg-badge ${badge.className}">${escapeHtml(badge.label)}</span>
        ${cleanedBadge}
        <span class="project-time">${escapeHtml(formatProjectTime(project.updatedAt))}</span>
      </div>`;
    selectButton.disabled = Boolean(project.workspaceCleanedAt);
    selectButton.addEventListener('click', () => selectProject(project));
    item.appendChild(selectButton);

    if (canDelete) {
      const deleteButton = document.createElement('button');
      deleteButton.className = 'project-delete';
      deleteButton.type = 'button';
      deleteButton.title = MESSAGE_PROJECT_HISTORY_DELETE;
      deleteButton.setAttribute('aria-label', `${MESSAGE_PROJECT_HISTORY_DELETE}：${project.name}`);
      deleteButton.innerHTML = DELETE_ICON_SVG;
      deleteButton.addEventListener('click', () => deleteProjectHistory(project));
      item.appendChild(deleteButton);
    }

    elements.projectList.appendChild(item);
  });
}

async function selectProject(project) {
  if (project.workspaceCleanedAt || !(await confirmDiscardUnsavedChanges())) {
    return;
  }
  const projectSessionId = ++state.projectSessionId;
  state.fileOpenRequestId++;
  state.documentSessionId++;
  state.currentProject = project;
  state.projectSettings = null;
  state.currentFilePath = null;
  state.currentFileHash = null;
  state.currentFileEncoding = null;
  state.currentFileOriginalContent = null;
  state.currentTree = null;
  state.openTabs = [];
  state.activeSearchPath = null;
  state.lastSearchKeyword = '';
  state.expandedPaths = new Set(['']);
  elements.editor.closeDocument();
  updateCurrentProjectBadge(project);
  elements.currentProjectName.textContent = project.name;
  elements.currentProjectName.title = project.name;
  elements.currentProjectMeta.textContent = project.workspacePath;
  elements.currentProjectMeta.title = project.workspacePath;
  restoreWorkspaceControls();
  elements.searchInput.disabled = false;
  elements.searchBtn.disabled = false;
  elements.searchInput.value = '';
  elements.searchResults.innerHTML = '';
  updateSearchClearButton();
  elements.saveBtn.disabled = true;
  elements.saveBtn.classList.remove('busy');
  elements.activeFileDirtyDot.hidden = true;
  closeUnicodePreview();
  elements.editor.disabled = true;
  renderEditorTabs();
  renderEditorBreadcrumb(null);
  elements.activeFileKind.textContent = MESSAGE_ACTIVE_FILE_TIP;
  elements.activeFileKind.dataset.kind = '';
  updateLanguageStatus(null);
  updateCursorStatus(null);
  updateEditorEmptyState();
  resetAnalysisPanel();
  await loadTree(project.id);
  if (state.projectSessionId !== projectSessionId) {
    return;
  }
  await loadProjects();
  if (state.projectSessionId === projectSessionId) {
    await startLanguageIntelligence(project);
  }
}

/**
 * 管理员删除指定项目的历史记录，支持一步选择是否连同清理工作区目录。
 *
 * @param {object} project 待删除项目
 */
async function deleteProjectHistory(project) {
  if (!window.jarPatchAuth.isAdmin()) {
    notify(MESSAGE_ADMIN_ONLY_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  if (state.currentProject && state.currentProject.id === project.id && !(await confirmDiscardUnsavedChanges())) {
    return;
  }
  try {
    if (!project.workspaceCleanedAt) {
      const choice = await window.jarPatchDialogs.confirm({
        title: '删除项目历史记录',
        message: '请选择仅移除历史记录，或连同清理本地工作区目录。保留工作区目录后，可在「扫描孤立工作区」中查看。',
        tone: window.jarPatchDialogs.TONE_DANGER,
        cancelValue: DELETE_ACTION_CANCEL,
        details: [
          { label: '项目名称', value: project.name },
          { label: '工作区路径', value: project.workspacePath || '-' }
        ],
        actions: [
          { value: DELETE_ACTION_CANCEL, label: '取消', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
          { value: DELETE_ACTION_HISTORY_ONLY, label: '仅删除历史', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
          { value: DELETE_ACTION_WITH_WORKSPACE, label: '清理工作区并删除', variant: window.jarPatchDialogs.VARIANT_DANGER }
        ]
      });
      if (choice === DELETE_ACTION_CANCEL) {
        return;
      }
      if (choice === DELETE_ACTION_WITH_WORKSPACE) {
        const cleaned = await previewAndCleanProjectWorkspace(project);
        if (!cleaned) {
          return;
        }
      }
    } else {
      const confirmed = await window.jarPatchDialogs.confirm({
        title: '确认删除项目历史',
        message: `${MESSAGE_PROJECT_HISTORY_DELETE_CONFIRM_PREFIX}${project.name}${MESSAGE_PROJECT_HISTORY_DELETE_CONFIRM_SUFFIX}`,
        tone: window.jarPatchDialogs.TONE_DANGER,
        actions: [
          { value: false, label: '取消', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
          { value: true, label: '确认删除历史', variant: window.jarPatchDialogs.VARIANT_DANGER }
        ]
      });
      if (!confirmed) {
        return;
      }
    }
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
  state.projectSessionId++;
  state.fileOpenRequestId++;
  state.documentSessionId++;
  state.currentProject = null;
  state.projectSettings = null;
  state.currentFilePath = null;
  state.currentFileHash = null;
  state.currentFileEncoding = null;
  state.currentFileOriginalContent = null;
  state.currentTree = null;
  state.openTabs = [];
  state.activeSearchPath = null;
  state.lastSearchKeyword = '';
  state.expandedPaths = new Set(['']);
  updateCurrentProjectBadge(null);
  elements.currentProjectName.textContent = MESSAGE_PROJECT_NOT_OPENED;
  elements.currentProjectName.title = '';
  elements.currentProjectMeta.textContent = MESSAGE_PROJECT_OPEN_TIP;
  elements.currentProjectMeta.title = '';
  restoreWorkspaceControls();
  elements.searchBtn.disabled = true;
  elements.saveBtn.disabled = true;
  elements.fileEncodingSelect.disabled = true;
  elements.searchInput.value = '';
  elements.searchResults.innerHTML = '';
  updateSearchClearButton();
  elements.fileTree.classList.add('empty');
  elements.fileTree.textContent = MESSAGE_FILE_TREE_EMPTY;
  closeUnicodePreview();
  elements.editor.closeDocument();
  elements.editor.disabled = true;
  elements.activeFileDirtyDot.hidden = true;
  renderEditorTabs();
  renderEditorBreadcrumb(null);
  elements.activeFileKind.textContent = MESSAGE_ACTIVE_FILE_TIP;
  elements.activeFileKind.dataset.kind = '';
  elements.saveBtn.classList.remove('busy');
  updateLanguageStatus(null);
  updateCursorStatus(null);
  updateEditorEmptyState();
  elements.editor.stopLanguageServer().catch((error) => {
    handleLanguageStatus({ state: 'error', message: `${MESSAGE_LANGUAGE_SERVER_UNAVAILABLE}：${error.message}` });
  });
  handleLanguageStatus({ state: 'idle', message: MESSAGE_LANGUAGE_SERVER_NOT_CONFIGURED });
  resetAnalysisPanel();
}

async function loadTree(projectId) {
  const projectSessionId = state.projectSessionId;
  try {
    elements.fileTree.classList.add('empty');
    elements.fileTree.textContent = MESSAGE_FILE_TREE_LOADING;
    const tree = await api(`/api/projects/${projectId}/tree`);
    if (state.projectSessionId !== projectSessionId || !state.currentProject || state.currentProject.id !== projectId) {
      return;
    }
    state.currentTree = tree;
    renderCurrentTree(false);
  } catch (error) {
    if (state.projectSessionId === projectSessionId) {
      elements.fileTree.innerHTML = `<div class="empty">文件树加载失败：${escapeHtml(error.message)}</div>`;
    }
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

/**
 * 根据文件树节点类型与扩展名返回对应的 SVG 图标与语义着色类。
 *
 * @param {object} node     文件树节点
 * @param {boolean} expanded 目录是否展开
 * @returns {{svg: string, className: string}} 图标配置
 */
function getTreeNodeIcon(node, expanded) {
  if (node.kind === 'DIRECTORY') {
    return {
      svg: expanded ? TREE_ICON_SVG_MAP.dirOpen : TREE_ICON_SVG_MAP.dirClosed,
      className: 'tree-icon-dir'
    };
  }
  if (node.kind === 'SIGNATURE') {
    return { svg: TREE_ICON_SVG_MAP.signature, className: 'tree-icon-signature' };
  }
  if (node.kind === 'BINARY') {
    const lowerName = String(node.name || '').toLowerCase();
    if (lowerName.endsWith('.jar') || lowerName.endsWith('.war')) {
      return { svg: TREE_ICON_SVG_MAP.archive, className: 'tree-icon-archive' };
    }
    return { svg: TREE_ICON_SVG_MAP.binary, className: 'tree-icon-binary' };
  }
  const lowerName = String(node.name || '').toLowerCase();
  if (lowerName.endsWith('.java')) {
    return { svg: TREE_ICON_SVG_MAP.java, className: 'tree-icon-java' };
  }
  if (lowerName.endsWith('.yml') || lowerName.endsWith('.yaml') || lowerName.endsWith('.properties') || lowerName.endsWith('.conf')) {
    return { svg: TREE_ICON_SVG_MAP.config, className: 'tree-icon-config' };
  }
  if (lowerName.endsWith('.xml') || lowerName.endsWith('.json') || lowerName.endsWith('.html')) {
    return { svg: TREE_ICON_SVG_MAP.markup, className: 'tree-icon-markup' };
  }
  return { svg: TREE_ICON_SVG_MAP.file, className: 'tree-icon-file' };
}

/**
 * 初始化文件树“合并单子目录包路径（Compact Folders）”偏好状态。
 */
function initializeCompactFolders() {
  const saved = localStorage.getItem(STORAGE_KEY_COMPACT_FOLDERS);
  state.compactFolders = saved !== '0';
  syncCompactFoldersButton();
}

/**
 * 同步包路径合并切换按钮的激活态与无障碍属性。
 */
function syncCompactFoldersButton() {
  elements.compactFoldersBtn.classList.toggle('active', state.compactFolders);
  elements.compactFoldersBtn.setAttribute('aria-pressed', String(state.compactFolders));
}

/**
 * 切换文件树“合并单子目录包路径”显示模式并立即重绘。
 */
function toggleCompactFolders() {
  state.compactFolders = !state.compactFolders;
  localStorage.setItem(STORAGE_KEY_COMPACT_FOLDERS, state.compactFolders ? '1' : '0');
  syncCompactFoldersButton();
  renderCurrentTree(true);
}

/**
 * 在开启 Compact Folders 时，沿已加载的单子目录链折叠生成显示标签与末尾节点。
 *
 * @param {object} node 起始目录节点
 * @returns {{tail: object, chain: Array<object>, displayName: string}} 合并结果
 */
function resolveCompactChain(node) {
  const chain = [node];
  if (!state.compactFolders || node.kind !== 'DIRECTORY' || !node.path || !node.childrenLoaded) {
    return { tail: node, chain, displayName: node.name };
  }
  let current = node;
  while (current.childrenLoaded
      && Array.isArray(current.children)
      && current.children.length === 1
      && current.children[0].kind === 'DIRECTORY') {
    current = current.children[0];
    chain.push(current);
  }
  const javaPackageContext = node.path.startsWith('sources/') || node.path.includes('/classes/');
  const separator = javaPackageContext ? '.' : '/';
  return {
    tail: current,
    chain,
    displayName: chain.map((item) => item.name).join(separator)
  };
}

function renderTreeNode(node) {
  const { tail, chain, displayName } = resolveCompactChain(node);
  const wrapper = document.createElement('div');
  const hasChildren = Boolean(tail.hasChildren);
  const expanded = state.expandedPaths.has(tail.path);
  const isActive = Boolean(state.currentFilePath && tail.path === state.currentFilePath);
  wrapper.className = `tree-node ${hasChildren && !expanded ? 'collapsed' : ''}`;

  const label = document.createElement('button');
  label.type = 'button';
  label.className = `tree-label ${tail.editable || tail.kind === 'DIRECTORY' ? '' : 'disabled'} ${isActive ? 'active' : ''}`.trim();
  label.title = tail.path || tail.name;
  const toggle = hasChildren ? (expanded ? '▾' : '▸') : '';
  const icon = getTreeNodeIcon(tail, expanded);
  label.innerHTML = `<span class="tree-toggle">${toggle}</span><span class="tree-icon ${icon.className}" aria-hidden="true">${icon.svg}</span><span>${escapeHtml(displayName)}</span>`;
  label.addEventListener('click', () => handleTreeClick(tail, chain));
  wrapper.appendChild(label);

  if (tail.childrenLoaded && tail.children && tail.children.length && expanded) {
    const children = document.createElement('div');
    children.className = 'tree-children';
    tail.children.forEach((child) => children.appendChild(renderTreeNode(child)));
    wrapper.appendChild(children);
  }
  return wrapper;
}

async function handleTreeClick(node, chain = [node]) {
  const projectSessionId = state.projectSessionId;
  if (node.kind === 'DIRECTORY' && node.hasChildren) {
    if (state.expandedPaths.has(node.path)) {
      state.expandedPaths.delete(node.path);
    } else {
      try {
        let current = node;
        if (!current.childrenLoaded) {
          await loadTreeChildren(current);
          if (state.projectSessionId !== projectSessionId) {
            return;
          }
        }
        chain.forEach((item) => state.expandedPaths.add(item.path));
        // 开启 Compact Folders 时自动预载单子目录链（如 com/example/service），一次点击直达包内容。
        if (state.compactFolders && current.path) {
          let depth = 0;
          while (depth < COMPACT_FOLDER_MAX_DEPTH
              && current.childrenLoaded
              && Array.isArray(current.children)
              && current.children.length === 1
              && current.children[0].kind === 'DIRECTORY') {
            current = current.children[0];
            if (!current.childrenLoaded) {
              await loadTreeChildren(current);
              if (state.projectSessionId !== projectSessionId) {
                return;
              }
            }
            state.expandedPaths.add(current.path);
            depth++;
          }
        }
      } catch (error) {
        notify(`文件树加载失败：${error.message}`, NOTICE_TYPE_ERROR);
        return;
      }
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

/**
 * 从后端懒加载一个目录的直接子节点。
 *
 * @param node 待展开目录节点
 */
async function loadTreeChildren(node) {
  if (!state.currentProject || node.childrenLoaded) {
    return;
  }
  const projectSessionId = state.projectSessionId;
  const children = await api(`/api/projects/${state.currentProject.id}/tree/children?path=${encodeURIComponent(node.path)}`);
  if (state.projectSessionId !== projectSessionId) {
    return;
  }
  node.children = children;
  node.childrenLoaded = true;
  node.hasChildren = children.length > 0;
}

async function openFile(node, encoding = null) {
  if (!state.currentProject) {
    return;
  }
  if (node.path !== state.currentFilePath && !(await confirmDiscardUnsavedChanges())) {
    return;
  }
  const project = state.currentProject;
  const projectSessionId = state.projectSessionId;
  const requestId = ++state.fileOpenRequestId;
  const editorRevision = state.editorRevision;
  const isCurrentRequest = () => state.projectSessionId === projectSessionId
    && state.fileOpenRequestId === requestId;
  try {
    if (state.currentSavePromise) {
      await state.currentSavePromise;
    }
    if (!isCurrentRequest()) {
      return;
    }
    const encodingQuery = encoding ? `&encoding=${encodeURIComponent(encoding)}` : '';
    const contentView = await api(`/api/projects/${project.id}/files/content?path=${encodeURIComponent(node.path)}${encodingQuery}`);
    if (!isCurrentRequest()
        || (state.editorRevision !== editorRevision && !(await confirmDiscardUnsavedChanges()))) {
      return;
    }
    closeUnicodePreview();
    state.documentSessionId++;
    state.currentFilePath = node.path;
    elements.editor.disabled = false;
    const normalizedWorkspacePath = project.workspacePath.replace(/[\\/]$/, '');
    elements.editor.openDocument({
      content: contentView.content,
      path: node.path,
      absolutePath: `${normalizedWorkspacePath}/${node.path}`,
      language: window.jarPatchEditor.detectLanguage(node.path)
    });
    state.currentFileOriginalContent = elements.editor.value;
    state.savedEditorVersionId = elements.editor.alternativeVersionId;
    state.currentFileHash = contentView.contentHash;
    state.currentFileEncoding = contentView.encoding;
    elements.fileEncodingSelect.value = contentView.encoding;
    elements.fileEncodingSelect.disabled = false;
    elements.saveBtn.disabled = false;
    renderEditorBreadcrumb(node.path);
    elements.activeFileKind.textContent = node.kind;
    elements.activeFileKind.dataset.kind = node.kind;
    upsertEditorTab(node.path, node.kind);
    updateLanguageStatus(node.path);
    updateCursorStatus({ lineNumber: 1, column: 1 });
    updateEditorEmptyState();
    updateDirtyIndicator();
    updateEditorActionButtons();
    renderCurrentTree(true);
  } catch (error) {
    if (isCurrentRequest()) {
      notify(`${MESSAGE_FILE_OPEN_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
    }
  }
}

async function searchProject() {
  if (!state.currentProject) {
    return;
  }
  const keyword = elements.searchInput.value.trim();
  if (!keyword) {
    state.lastSearchKeyword = '';
    state.activeSearchPath = null;
    elements.searchResults.innerHTML = '';
    updateSearchClearButton();
    notify(MESSAGE_SEARCH_KEYWORD_EMPTY, NOTICE_TYPE_INFO);
    return;
  }
  try {
    state.lastSearchKeyword = keyword;
    state.activeSearchPath = null;
    const results = await api(`/api/projects/${state.currentProject.id}/search?keyword=${encodeURIComponent(keyword)}`);
    renderSearchResults(results);
    notify(`${MESSAGE_SEARCH_SUCCESS}：${keyword}，${results.length} 条结果`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_SEARCH_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 对搜索结果预览文本进行 HTML 转义并安全高亮命中关键词。
 *
 * @param {string} preview 原始预览片段
 * @param {string} keyword 搜索关键词
 * @returns {string} 高亮后的安全 HTML
 */
function highlightSearchPreview(preview, keyword) {
  const source = String(preview || '');
  const query = String(keyword || '').trim();
  if (!query) {
    return escapeHtml(source);
  }
  const lowerSource = source.toLowerCase();
  const lowerQuery = query.toLowerCase();
  let cursor = 0;
  let output = '';
  while (cursor < source.length) {
    const matchIndex = lowerSource.indexOf(lowerQuery, cursor);
    if (matchIndex < 0) {
      output += escapeHtml(source.slice(cursor));
      break;
    }
    output += escapeHtml(source.slice(cursor, matchIndex));
    output += `<mark class="search-hit">${escapeHtml(source.slice(matchIndex, matchIndex + query.length))}</mark>`;
    cursor = matchIndex + query.length;
  }
  return output;
}

function renderSearchResults(results) {
  elements.searchResults.innerHTML = '';
  if (!results.length) {
    elements.searchResults.innerHTML = '<div class="empty">没有搜索结果</div>';
    updateSearchClearButton();
    return;
  }
  const header = document.createElement('div');
  header.className = 'search-results-header';
  header.innerHTML = `<span>找到 ${results.length} 条结果</span><span>点击跳转定位</span>`;
  elements.searchResults.appendChild(header);

  results.forEach((result) => {
    const resultKey = `${result.path}:${result.lineNumber}`;
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `search-result ${state.activeSearchPath === resultKey ? 'active' : ''}`.trim();
    button.dataset.resultKey = resultKey;
    button.innerHTML = `<strong>${escapeHtml(result.path)}:${result.lineNumber}</strong><span>${highlightSearchPreview(result.preview, state.lastSearchKeyword)}</span>`;
    button.addEventListener('click', () => {
      state.activeSearchPath = resultKey;
      elements.searchResults.querySelectorAll('.search-result').forEach((node) => {
        node.classList.toggle('active', node.dataset.resultKey === resultKey);
      });
      void openSearchResult(result);
    });
    elements.searchResults.appendChild(button);
  });
  updateSearchClearButton();
}

async function openSearchResult(result) {
  await expandPathParents(result.path);
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

async function expandPathParents(path) {
  const parts = path.split('/');
  let current = '';
  let currentNode = state.currentTree;
  for (let index = 0; index < parts.length - 1; index++) {
    current = current ? `${current}/${parts[index]}` : parts[index];
    state.expandedPaths.add(current);
    if (!currentNode) {
      continue;
    }
    if (!currentNode.childrenLoaded) {
      await loadTreeChildren(currentNode);
    }
    currentNode = (currentNode.children || []).find((child) => child.path === current) || null;
  }
}

/**
 * 预览并确认清理指定项目工作区，供删除历史和独立清理入口复用。
 *
 * @param project 项目记录
 * @return 用户确认且清理完成时返回 true
 */
async function previewAndCleanProjectWorkspace(project) {
  const preview = await api(`/api/projects/${project.id}/workspace/cleanup-preview`);
  const confirmed = await window.jarPatchDialogs.confirm({
    title: '确认清理项目工作区',
    message: MESSAGE_WORKSPACE_CLEANUP_CONFIRM,
    tone: window.jarPatchDialogs.TONE_DANGER,
    details: [
      { label: '项目名称', value: preview.projectName },
      { label: '工作区路径', value: preview.workspacePath },
      { label: '文件数量', value: `${preview.fileCount} 个` },
      { label: '占用空间', value: formatBytes(preview.totalBytes) },
      { label: '最后使用', value: preview.lastUsedAt }
    ],
    actions: [
      { value: false, label: '取消', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
      { value: true, label: '确认清理工作区', variant: window.jarPatchDialogs.VARIANT_DANGER }
    ]
  });
  if (!confirmed) {
    return false;
  }
  await api(`/api/projects/${project.id}/workspace?confirmationId=${encodeURIComponent(preview.confirmationId)}`, {
    method: 'DELETE'
  });
  return true;
}

/**
 * 扫描并显式确认清理全部孤立工作区。
 */
async function cleanupOrphanWorkspaces() {
  if (!window.jarPatchAuth.isAdmin()) {
    notify(MESSAGE_ADMIN_ONLY_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  try {
    const preview = await api('/api/workspaces/orphans/cleanup-preview');
    if (!preview.entries || !preview.entries.length) {
      notify(MESSAGE_ORPHAN_WORKSPACE_EMPTY, NOTICE_TYPE_INFO);
      return;
    }
    const confirmed = await window.jarPatchDialogs.confirm({
      title: '清理孤立工作区',
      message: `扫描到 ${preview.entries.length} 个无项目记录关联的本地工作目录，确认后将永久删除这些目录及其内容。`,
      tone: window.jarPatchDialogs.TONE_DANGER,
      listItems: preview.entries.map((entry) => ({
        title: entry.workspacePath,
        meta: `${entry.fileCount} 个文件 · ${formatBytes(entry.totalBytes)} · 最后修改 ${entry.lastModifiedAt}`
      })),
      actions: [
        { value: false, label: '取消', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
        { value: true, label: `确认清理全部 ${preview.entries.length} 个目录`, variant: window.jarPatchDialogs.VARIANT_DANGER }
      ]
    });
    if (!confirmed) {
      return;
    }
    const cleanedCount = await api(`/api/workspaces/orphans?confirmationId=${encodeURIComponent(preview.confirmationId)}`, {
      method: 'DELETE'
    });
    notify(`${MESSAGE_ORPHAN_WORKSPACE_CLEAN_SUCCESS}：${cleanedCount} 个`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_ORPHAN_WORKSPACE_CLEAN_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 按用户明确选择的编码重新打开当前文件，不做编码猜测或自动写回。
 */
async function reopenCurrentFileWithEncoding() {
  if (!state.currentProject || !state.currentFilePath) {
    return;
  }
  if (!(await confirmDiscardUnsavedChanges())) {
    elements.fileEncodingSelect.value = state.currentFileEncoding || 'UTF-8';
    return;
  }
  await openFile({
    path: state.currentFilePath,
    kind: elements.activeFileKind.dataset.kind
  }, elements.fileEncodingSelect.value);
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
  if (state.currentSavePromise) {
    notify(MESSAGE_SAVE_IN_PROGRESS, NOTICE_TYPE_INFO);
    return state.currentSavePromise;
  }
  if (elements.editor.value === state.currentFileOriginalContent) {
    notify(MESSAGE_SAVE_UNCHANGED, NOTICE_TYPE_INFO);
    return;
  }
  const snapshot = {
    projectId: state.currentProject.id,
    projectSessionId: state.projectSessionId,
    documentSessionId: state.documentSessionId,
    path: state.currentFilePath,
    content: elements.editor.value,
    expectedHash: state.currentFileHash,
    encoding: state.currentFileEncoding,
    editorRevision: state.editorRevision,
    editorVersionId: elements.editor.alternativeVersionId
  };
  const saving = saveFileSnapshot(snapshot);
  state.currentSavePromise = saving;
  try {
    await saving;
  } finally {
    if (state.currentSavePromise === saving) {
      state.currentSavePromise = null;
    }
  }
}

/**
 * 保存明确的文档快照；只更新同一文档的保存基线，保留请求期间的新输入。
 *
 * @param snapshot 保存时的项目、文档会话、编辑版本和内容
 */
async function saveFileSnapshot(snapshot) {
  try {
    const contentView = await api(`/api/projects/${snapshot.projectId}/files/content`, {
      method: 'PUT',
      body: JSON.stringify({
        path: snapshot.path,
        content: snapshot.content,
        expectedHash: snapshot.expectedHash,
        encoding: snapshot.encoding
      })
    });
    if (state.projectSessionId === snapshot.projectSessionId
        && state.documentSessionId === snapshot.documentSessionId
        && state.currentFilePath === snapshot.path) {
      if (state.editorRevision === snapshot.editorRevision) {
        // 后端返回内容与编辑器一致时跳过整文写回，保留 undo 栈并避免整文重令牌化。
        if (contentView.content !== elements.editor.value) {
          elements.editor.value = contentView.content;
        }
        state.currentFileOriginalContent = elements.editor.value;
        // setValue 会推进模型版本号，保存基线必须读取写回后的版本。
        state.savedEditorVersionId = elements.editor.alternativeVersionId;
        elements.editor.didSave();
      } else {
        // 后端仅规范化换行，保存基线沿用提交时编辑器的换行形式。
        const lineEnding = snapshot.content.includes('\r\n') ? '\r\n' : '\n';
        state.currentFileOriginalContent = contentView.content.replace(/\r\n|\r|\n/g, lineEnding);
        // 保存期间已有新输入时，基线对齐提交时刻的编辑器版本，后续输入仍判为未保存。
        state.savedEditorVersionId = snapshot.editorVersionId;
      }
      state.currentFileHash = contentView.contentHash;
      state.currentFileEncoding = contentView.encoding;
      elements.fileEncodingSelect.value = contentView.encoding;
      if (state.unicodePreviewOpen) {
        refreshUnicodePreviewContent();
      }
      updateDirtyIndicator();
    }
    notify(`${MESSAGE_SAVE_SUCCESS}：${snapshot.path}`, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_SAVE_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 由 Monaco 显式 AI 行内补全 Provider 调用，自动输入过程不会访问网络。
 *
 * @param context 光标前后代码上下文
 * @return AI 补全结果
 */
async function provideAiInlineCompletion(context) {
  if (!state.developmentSettings || !state.developmentSettings.aiEnabled) {
    notify(MESSAGE_AI_NOT_CONFIGURED, NOTICE_TYPE_INFO);
    return null;
  }
  try {
    return await window.jarPatch.aiAssist({
      action: AI_ACTION_COMPLETE,
      instruction: '',
      path: state.currentFilePath,
      language: context.language,
      source: '',
      selection: '',
      prefix: context.prefix,
      suffix: context.suffix,
      diagnostics: elements.editor.getDiagnostics()
    });
  } catch (error) {
    notify(`${MESSAGE_AI_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
    return null;
  }
}

/**
 * 打开 AI 助手并冻结本次请求对应的原始选区和源码快照。
 */
function openAiAssistantDialog() {
  if (!state.currentFilePath) {
    notify(MESSAGE_AI_NO_FILE, NOTICE_TYPE_INFO);
    return;
  }
  if (!state.developmentSettings || !state.developmentSettings.aiEnabled) {
    notify(MESSAGE_AI_NOT_CONFIGURED, NOTICE_TYPE_INFO);
    return;
  }
  const context = elements.editor.getSelectionContext();
  state.aiRequestContext = {
    originalSource: context.source,
    source: context.source.length <= AI_CONTEXT_MAX_SOURCE_CHARACTERS ? context.source : '',
    selection: context.text,
    range: context.range
  };
  state.aiResult = null;
  const scopeText = context.text
    ? `${MESSAGE_AI_CONTEXT_SELECTION}：${context.text.length} ${MESSAGE_AI_CONTEXT_SUFFIX}`
    : `${MESSAGE_AI_CONTEXT_FILE}：${context.source.length} ${MESSAGE_AI_CONTEXT_SUFFIX}`;
  elements.aiContextSummary.textContent = `${state.currentFilePath} · ${scopeText}`;
  elements.aiResultSummary.textContent = MESSAGE_AI_PREVIEW_DEFAULT;
  elements.aiResultOutput.value = '';
  elements.applyAiResultBtn.disabled = true;
  elements.aiAssistantDialog.classList.add('open');
  elements.aiAssistantDialog.setAttribute('aria-hidden', 'false');
}

/**
 * 关闭 AI 助手弹窗，不改变编辑器内容。
 */
function closeAiAssistantDialog() {
  elements.aiAssistantDialog.classList.remove('open');
  elements.aiAssistantDialog.setAttribute('aria-hidden', 'true');
}

/**
 * 设置 AI 助手执行期间的控件状态。
 *
 * @param busy 是否正在等待模型响应
 */
function setAiAssistantBusy(busy) {
  elements.aiActionSelect.disabled = busy;
  elements.aiInstructionInput.disabled = busy;
  elements.closeAiAssistantBtn.disabled = busy;
  elements.runAiAssistantBtn.disabled = busy;
  setButtonLabel(elements.runAiAssistantBtn, busy ? BUTTON_TEXT_AI_RUNNING : BUTTON_TEXT_AI_RUN);
  if (busy) {
    elements.applyAiResultBtn.disabled = true;
  }
}

/**
 * 提交解释、修复或重构请求，并只把结构化结果放入预览区。
 */
async function runAiAssistant() {
  if (!state.aiRequestContext || !state.currentFilePath) {
    notify(MESSAGE_AI_NO_FILE, NOTICE_TYPE_INFO);
    return;
  }
  setAiAssistantBusy(true);
  elements.aiResultSummary.textContent = MESSAGE_AI_RUNNING;
  try {
    const result = await window.jarPatch.aiAssist({
      action: elements.aiActionSelect.value,
      instruction: elements.aiInstructionInput.value.trim(),
      path: state.currentFilePath,
      language: window.jarPatchEditor.detectLanguage(state.currentFilePath),
      source: state.aiRequestContext.source,
      selection: state.aiRequestContext.selection,
      prefix: '',
      suffix: '',
      diagnostics: elements.editor.getDiagnostics()
    });
    state.aiResult = result;
    elements.aiResultOutput.value = result.content;
    const replacement = result.resultType === AI_RESULT_TYPE_REPLACEMENT;
    elements.aiResultSummary.textContent = `${result.summary} · ${replacement
      ? MESSAGE_AI_RESULT_REPLACEMENT : MESSAGE_AI_RESULT_EXPLANATION}`;
    elements.applyAiResultBtn.disabled = !replacement;
  } catch (error) {
    state.aiResult = null;
    elements.aiResultSummary.textContent = `${MESSAGE_AI_FAILED}：${error.message}`;
    notify(`${MESSAGE_AI_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  } finally {
    setAiAssistantBusy(false);
    elements.applyAiResultBtn.disabled = !state.aiResult
      || state.aiResult.resultType !== AI_RESULT_TYPE_REPLACEMENT;
  }
}

/**
 * 在确认源码未变化后应用 AI 替换；解释类结果永远不能写入编辑器。
 */
function applyAiResult() {
  if (!state.aiRequestContext || !state.aiResult
      || state.aiResult.resultType !== AI_RESULT_TYPE_REPLACEMENT) {
    return;
  }
  if (elements.editor.value !== state.aiRequestContext.originalSource) {
    notify(MESSAGE_AI_STALE_RESULT, NOTICE_TYPE_ERROR);
    elements.applyAiResultBtn.disabled = true;
    return;
  }
  elements.editor.applyAiReplacement(state.aiRequestContext.range, state.aiResult.content);
  updateDirtyIndicator();
  notify(MESSAGE_AI_APPLIED, NOTICE_TYPE_SUCCESS);
  closeAiAssistantDialog();
}

/**
 * 打开语言服务器提供的标准快速修复菜单。
 */
async function showQuickFix() {
  await elements.editor.showQuickFix();
}

/**
 * 请求 JDT LS 整理 import，并把编辑保留在未保存状态供用户检查。
 */
async function organizeImports() {
  try {
    const applied = await elements.editor.organizeImports();
    notify(applied ? MESSAGE_ORGANIZE_IMPORTS_SUCCESS : MESSAGE_ORGANIZE_IMPORTS_EMPTY,
      applied ? NOTICE_TYPE_SUCCESS : NOTICE_TYPE_INFO);
  } catch (error) {
    notify(`${MESSAGE_LANGUAGE_SERVER_UNAVAILABLE}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 显式触发一次 AI 行内补全；模型返回后由 Monaco 以灰字展示，用户按 Tab 接受。
 */
async function triggerAiInlineCompletion() {
  if (!state.developmentSettings || !state.developmentSettings.aiEnabled) {
    notify(MESSAGE_AI_NOT_CONFIGURED, NOTICE_TYPE_INFO);
    return;
  }
  notify(MESSAGE_AI_INLINE_REQUESTED, NOTICE_TYPE_INFO);
  await elements.editor.triggerAiCompletion();
}

async function importArchive() {
  if (!(await confirmDiscardUnsavedChanges())) {
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
  elements.decompileFilterInput.value = '';
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
  if (!inspection) {
    return;
  }
  const candidates = inspection.candidates || [];
  const selectedCount = candidates.filter((candidate) => candidate.selected).length;
  const keyword = elements.decompileFilterInput.value.trim().toLowerCase();
  elements.decompileDialogMeta.textContent = `${inspection.packageType} · ${inspection.filePath}`;
  elements.decompileDialogSummary.textContent = candidates.length
    ? `pom.xml 模块：${renderPomModuleSummary(inspection.pomModules)}；当前已选择 ${selectedCount} ${MESSAGE_DECOMPILE_SELECTED_SUFFIX}`
    : MESSAGE_DECOMPILE_NO_CANDIDATE;
  elements.decompileSelectedBadge.textContent = `${selectedCount} / ${candidates.length}`;
  elements.decompileCandidateList.innerHTML = '';
  if (!candidates.length) {
    elements.decompileCandidateList.innerHTML = `<div class="empty">${MESSAGE_DECOMPILE_NO_CANDIDATE}</div>`;
    return;
  }
  const matched = [];
  candidates.forEach((candidate, index) => {
    if (!keyword) {
      matched.push({ candidate, index });
      return;
    }
    const haystack = `${candidate.name || ''} ${candidate.path || ''} ${candidate.reason || ''}`.toLowerCase();
    if (haystack.includes(keyword)) {
      matched.push({ candidate, index });
    }
  });
  if (!matched.length) {
    elements.decompileCandidateList.innerHTML = `<div class="empty">${MESSAGE_CANDIDATE_FILTER_EMPTY}</div>`;
    return;
  }
  matched.forEach(({ candidate, index }) => {
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

/**
 * 渲染右侧包结构分析报告，展示风险等级徽标及可点击跳转的已修改文件清单。
 *
 * @param {object} report 后端返回的结构分析报告
 */
function renderAnalysis(report) {
  state.analysisReport = report;
  elements.analysisResult.classList.remove('empty');
  const risks = report.risks.length
    ? report.risks.map((risk) => {
      const levelLabel = RISK_LEVEL_LABEL_MAP[risk.level] || risk.level;
      return `
        <div class="risk ${escapeHtml(risk.level)}">
          <div class="risk-head">
            <span class="risk-badge">${escapeHtml(levelLabel)}</span>
            <strong>${escapeHtml(risk.title)}</strong>
          </div>
          <div>${escapeHtml(risk.detail)}</div>
        </div>`;
    }).join('')
    : '<div class="empty">未发现导出风险</div>';
  updateAnalysisBadge(report);
  const modifiedFiles = Array.isArray(report.modifiedFiles) ? report.modifiedFiles : [];
  elements.analysisResult.innerHTML = `
    <div class="metric"><span>包类型</span><strong>${escapeHtml(report.packageType)}</strong></div>
    <div class="metric"><span>入口类</span><strong>${escapeHtml(report.entryClass || '未识别')}</strong></div>
    <div class="metric"><span>Manifest</span><strong>${report.manifestExists ? '存在' : '不存在'}</strong></div>
    <div class="metric"><span>Class数量</span><strong>${report.classCount}</strong></div>
    <div class="metric"><span>依赖数量</span><strong>${report.dependencyCount}</strong></div>
    <div class="metric"><span>修改文件</span><strong>${modifiedFiles.length}</strong></div>
    ${risks}
  `;
  if (modifiedFiles.length) {
    const modifiedSection = document.createElement('section');
    modifiedSection.className = 'analysis-modified-section';
    const subtitle = document.createElement('div');
    subtitle.className = 'analysis-subtitle';
    subtitle.textContent = `已修改文件（点击跳转打开）`;
    const list = document.createElement('div');
    list.className = 'analysis-modified-list';
    modifiedFiles.forEach((filePath) => {
      const link = document.createElement('button');
      link.type = 'button';
      link.className = 'analysis-file-link';
      link.textContent = filePath;
      link.title = `打开 ${filePath}`;
      link.addEventListener('click', () => {
        void openFileFromAnalysis(filePath);
      });
      list.appendChild(link);
    });
    modifiedSection.appendChild(subtitle);
    modifiedSection.appendChild(list);
    elements.analysisResult.appendChild(modifiedSection);
  }
}

/**
 * 从右侧分析面板的“已修改文件”清单展开目录树并在编辑器中打开对应文件。
 *
 * @param {string} filePath 工作区相对路径
 */
async function openFileFromAnalysis(filePath) {
  if (!state.currentProject || !filePath) {
    return;
  }
  await expandPathParents(filePath);
  if (state.currentTree) {
    renderCurrentTree(true);
  }
  await openFile({
    path: filePath,
    kind: filePath.toLowerCase().endsWith('.java') ? 'SOURCE' : 'RESOURCE',
    editable: true
  });
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
  const projectId = state.currentProject.id;
  try {
    const result = await executeTaskOperation(TASK_TYPE_COMPILE, projectId, '开始编译修改过的 Java 文件', (task, signal) => api(`/api/projects/${projectId}/compile`, {
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

/**
 * 选择外部目录，确认预览后应用整批补丁。
 */
async function importPatchDirectory() {
  if (!state.currentProject || state.patchDirectoryOpening || state.currentTask || state.taskStarting) {
    return;
  }
  if (isEditorDirty()) {
    notify(MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  const projectId = state.currentProject.id;
  const projectSessionId = state.projectSessionId;
  const dialog = elements.patchDirectoryDialog;
  state.patchDirectoryOpening = true;
  restoreWorkspaceControls();
  try {
    const directory = await window.jarPatch.pickDirectory(null, '选择改动类文件夹');
    if (!directory || state.projectSessionId !== projectSessionId || isEditorDirty()) {
      return;
    }

    elements.patchDirectoryPath.textContent = directory;
    elements.patchDirectoryEntries.replaceChildren();
    elements.patchDirectoryStatus.textContent = '正在匹配包内类…';
    elements.applyPatchDirectoryBtn.disabled = true;
    dialog.returnValue = '';
    dialog.showModal();
    const closed = new Promise((resolve) => {
      dialog.addEventListener('close', () => resolve(dialog.returnValue), { once: true });
    });
    const previewRequest = api(`/api/projects/${projectId}/patch-directory/preview`, {
      method: 'POST',
      body: JSON.stringify({ directory })
    }).then((preview) => ({ preview }), (error) => ({ error }));
    const scan = await Promise.race([previewRequest, closed.then(() => null)]);
    if (!scan || !dialog.open || state.projectSessionId !== projectSessionId) {
      return;
    }
    if (scan.error) {
      elements.patchDirectoryStatus.textContent = `无法预览补丁目录：${scan.error.message}`;
      elements.patchDirectoryStatus.focus();
      await closed;
      return;
    }

    const preview = scan.preview;
    renderPatchDirectoryPreview(preview);
    const decision = await closed;
    if (decision !== 'apply' || !preview.applicable || state.projectSessionId !== projectSessionId || isEditorDirty()) {
      return;
    }
    const result = await executeTaskOperation(TASK_TYPE_PATCH_DIRECTORY, projectId, '开始应用补丁目录',
      (task, signal) => api(`/api/projects/${projectId}/patch-directory/apply`, {
        method: 'POST',
        signal,
        headers: { 'X-Task-Id': task.id },
        body: JSON.stringify({ directory: preview.directory, confirmation: preview.confirmation })
      }));
    if (result && state.projectSessionId === projectSessionId) {
      notify(`已应用 ${result.changedFiles.length} 个 class，点击「导出」生成新包`, NOTICE_TYPE_SUCCESS);
      await loadTree(projectId);
    }
  } catch (error) {
    notify(`补丁目录处理失败：${error.message}`, NOTICE_TYPE_ERROR);
  } finally {
    state.patchDirectoryOpening = false;
    restoreWorkspaceControls();
    if (state.projectSessionId === projectSessionId && !elements.patchDirectoryBtn.disabled) {
      elements.patchDirectoryBtn.focus();
    }
  }
}

/** 使用文本节点展示外部文件名、目标和阻塞原因。 */
function renderPatchDirectoryPreview(preview) {
  const blocked = preview.entries.filter((entry) => entry.problem).length;
  elements.patchDirectoryPath.textContent = preview.directory;
  elements.patchDirectoryStatus.textContent = blocked
    ? `${blocked} 项需要处理，修正后请重新选择目录。`
    : `已匹配 ${preview.entries.length} 个文件。应用后可导出新包，Java 源码可能同时生成内部类。`;
  const rows = preview.entries.map((entry) => {
    const item = document.createElement('li');
    const title = document.createElement('strong');
    title.textContent = entry.input;
    item.append(title);
    if (entry.className) {
      const name = document.createElement('span');
      name.textContent = entry.className;
      item.append(name);
    }
    for (const target of entry.targets) {
      const path = document.createElement('code');
      path.textContent = target;
      item.append(path);
    }
    if (entry.problem) {
      const problem = document.createElement('span');
      problem.className = 'patch-directory-problem';
      problem.textContent = entry.problem;
      item.append(problem);
    }
    return item;
  });
  elements.patchDirectoryEntries.replaceChildren(...rows);
  elements.applyPatchDirectoryBtn.disabled = !preview.applicable;
  if (blocked) {
    elements.patchDirectoryStatus.focus();
  }
}

function renderDiffReport(report) {
  const sourceDiffs = report.sourceDiffs || [];
  const resourceDiffs = report.resourceDiffs || [];
  const artifacts = report.compiledArtifacts || [];
  const diffViews = new Map();
  elements.diffDialogSummary.textContent = `源码 ${sourceDiffs.length} 项 · 资源 ${resourceDiffs.length} 项 · class ${artifacts.length} 项`;
  elements.diffContent.replaceChildren();

  const fileDiffs = [...sourceDiffs, ...resourceDiffs];
  for (const diff of fileDiffs) {
    const details = document.createElement('details');
    details.className = 'diff-item';
    const summary = document.createElement('summary');
    const statusMeta = DIFF_STATUS_LABEL_MAP[diff.status] || { label: diff.status || '变更', className: 'diff-modified' };
    const badge = document.createElement('span');
    badge.className = `diff-status-badge ${statusMeta.className}`;
    badge.textContent = statusMeta.label;
    summary.appendChild(badge);
    summary.appendChild(document.createTextNode(diff.path));
    details.appendChild(summary);

    const labels = document.createElement('div');
    labels.className = 'diff-side-labels';
    for (const [title, content] of [['导入基线', diff.originalContent], ['当前内容', diff.currentContent]]) {
      const label = document.createElement('span');
      label.textContent = content == null ? `${title}（文件不存在）` : title;
      labels.appendChild(label);
    }
    details.appendChild(labels);

    if (diff.originalContent != null && diff.currentContent != null
        && diff.originalContent.replace(/\r\n?/g, '\n') === diff.currentContent.replace(/\r\n?/g, '\n')) {
      const note = document.createElement('p');
      note.className = 'diff-note';
      note.textContent = MESSAGE_DIFF_METADATA_ONLY;
      details.appendChild(note);
    }

    const editorHost = document.createElement('div');
    editorHost.className = 'diff-editor-host';
    details.appendChild(editorHost);
    details.addEventListener('toggle', () => {
      const view = diffViews.get(details);
      if (details.open && elements.diffDialog.open && details.isConnected) {
        if (!view) {
          diffViews.set(details, window.jarPatchEditor.createDiffView(editorHost, diff));
        }
      } else if (view) {
        view.dispose();
        diffViews.delete(details);
      }
    });
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
  return diffViews;
}

async function openDiffDialog(forExport = false) {
  const project = state.currentProject;
  if (!project || elements.diffContent.hasChildNodes()) {
    return false;
  }
  const projectSessionId = state.projectSessionId;
  const requestId = ++state.diffRequestId;
  let report;
  try {
    report = await api(`/api/projects/${project.id}/diff`);
  } catch (error) {
    if (projectSessionId === state.projectSessionId && requestId === state.diffRequestId) {
      notify(`${MESSAGE_DIFF_LOAD_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
    }
    return false;
  }
  if (projectSessionId !== state.projectSessionId || requestId !== state.diffRequestId) {
    return false;
  }

  const diffViews = renderDiffReport(report);
  const dialog = elements.diffDialog;
  elements.continueExportBtn.hidden = !forExport;
  dialog.returnValue = '';
  dialog.showModal();
  return new Promise((resolve) => {
    dialog.addEventListener('close', () => {
      for (const view of diffViews.values()) {
        view.dispose();
      }
      diffViews.clear();
      elements.diffContent.replaceChildren();
      resolve(forExport && dialog.returnValue === elements.continueExportBtn.value);
    }, { once: true });
  });
}

async function exportProject() {
  if (!state.currentProject) {
    return;
  }
  if (isEditorDirty()) {
    notify(MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  const projectSessionId = state.projectSessionId;
  const confirmed = await openDiffDialog(true);
  if (!confirmed || projectSessionId !== state.projectSessionId) {
    return;
  }
  if (isEditorDirty()) {
    notify(MESSAGE_UNSAVED_CHANGES_BLOCK_ACTION, NOTICE_TYPE_INFO);
    return;
  }
  const signaturePolicy = await window.jarPatchDialogs.confirm({
    title: '选择导出签名处理策略',
    message: MESSAGE_SIGNATURE_POLICY_CONFIRM,
    tone: window.jarPatchDialogs.TONE_WARN,
    cancelValue: null,
    actions: [
      { value: null, label: '取消导出', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
      { value: SIGNATURE_POLICY_PRESERVE, label: '保留原签名继续', variant: window.jarPatchDialogs.VARIANT_SECONDARY },
      { value: SIGNATURE_POLICY_REMOVE, label: '移除失效签名并导出', variant: window.jarPatchDialogs.VARIANT_PRIMARY }
    ]
  });
  if (!signaturePolicy) {
    notify(MESSAGE_EXPORT_CANCELED, NOTICE_TYPE_INFO);
    return;
  }
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
  try {
    const result = await executeTaskOperation(TASK_TYPE_EXPORT, state.currentProject.id, `开始导出修改后的包: ${outputPath}`, (task, signal) => api(`/api/projects/${state.currentProject.id}/export`, {
      method: 'POST',
      signal,
      body: JSON.stringify({
        outputPath,
        taskId: task.id,
        signaturePolicy
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
  setButtonLabel(button, runningText);
  button.classList.add('busy');
}

function restoreProjectActionButtons() {
  restoreWorkspaceControls();
}

/**
 * 切换并持久化工作台深色/浅色主题，同时同步所有 Monaco 编辑器实例配色。
 *
 * @param {string} theme 目标主题（dark 或 light）
 * @param {boolean} persist 是否保存到本地存储
 */
function applyTheme(theme, persist = false) {
  const normalized = theme === THEME_LIGHT ? THEME_LIGHT : THEME_DARK;
  state.uiTheme = normalized;
  document.body.dataset.theme = normalized;
  const label = normalized === THEME_DARK ? LABEL_SWITCH_TO_LIGHT : LABEL_SWITCH_TO_DARK;
  elements.themeToggleBtn.setAttribute('aria-label', label);
  elements.themeToggleBtn.title = label;
  window.jarPatchEditor.setTheme(normalized);
  if (persist) {
    localStorage.setItem(STORAGE_KEY_THEME, normalized);
  }
}

/**
 * 启动时读取本地主题偏好，默认启用专业深色 IDE 主题。
 */
function initializeTheme() {
  const saved = localStorage.getItem(STORAGE_KEY_THEME);
  applyTheme(saved === THEME_LIGHT ? THEME_LIGHT : THEME_DARK, false);
}

/**
 * 在深色与浅色工作台主题之间切换。
 */
function toggleTheme() {
  applyTheme(state.uiTheme === THEME_DARK ? THEME_LIGHT : THEME_DARK, true);
}

/**
 * 展开或收起顶部“更多”工具弹出菜单。
 *
 * @param {boolean} open 是否展开
 */
function setMoreToolsOpen(open) {
  elements.moreToolsPopover.hidden = !open;
  elements.moreToolsBtn.setAttribute('aria-expanded', String(open));
}

/**
 * 根据搜索输入或结果状态同步清空按钮可见性。
 */
function updateSearchClearButton() {
  const hasContent = Boolean(elements.searchInput.value.trim()) || elements.searchResults.hasChildNodes();
  elements.clearSearchBtn.hidden = !hasContent;
}

/**
 * 清空项目搜索输入和搜索结果列表。
 */
function clearSearchResults() {
  state.lastSearchKeyword = '';
  state.activeSearchPath = null;
  elements.searchInput.value = '';
  elements.searchResults.innerHTML = '';
  updateSearchClearButton();
  elements.searchInput.focus();
}

/**
 * 折叠文件树中的全部子目录，只保留根节点展开。
 */
function collapseFileTree() {
  if (!state.currentTree) {
    return;
  }
  state.expandedPaths = new Set(['']);
  renderCurrentTree(false);
}

/**
 * 初始化底部执行日志面板的高度和折叠状态。
 */
function initializeLogPanel() {
  const savedHeight = Number.parseInt(localStorage.getItem(STORAGE_KEY_LOG_HEIGHT), NUMBER_PARSE_RADIX);
  const initialHeight = Number.isNaN(savedHeight) ? LOG_PANEL_DEFAULT_HEIGHT : savedHeight;
  setLogPanelHeight(initialHeight, false);
  const collapsed = localStorage.getItem(STORAGE_KEY_LOG_COLLAPSED) === '1';
  setLogPanelCollapsed(collapsed, false);
}

/**
 * 设置底部执行日志面板高度并限制在安全范围内。
 *
 * @param {number} height 目标高度（像素）
 * @param {boolean} persist 是否持久化到本地存储
 */
function setLogPanelHeight(height, persist) {
  const clamped = Math.min(Math.max(Math.round(height), LOG_PANEL_MIN_HEIGHT), LOG_PANEL_MAX_HEIGHT);
  state.logPanelHeight = clamped;
  document.documentElement.style.setProperty('--log-panel-height', `${clamped}px`);
  if (persist) {
    localStorage.setItem(STORAGE_KEY_LOG_HEIGHT, String(clamped));
  }
}

/**
 * 切换底部执行日志面板的折叠/展开状态。
 *
 * @param {boolean} collapsed 是否折叠
 * @param {boolean} persist 是否持久化到本地存储
 */
function setLogPanelCollapsed(collapsed, persist) {
  state.logPanelCollapsed = Boolean(collapsed);
  elements.workspace.classList.toggle('log-collapsed', state.logPanelCollapsed);
  elements.toggleLogBtn.textContent = state.logPanelCollapsed ? BUTTON_TEXT_LOG_EXPAND : BUTTON_TEXT_LOG_COLLAPSE;
  elements.toggleLogBtn.setAttribute('aria-expanded', String(!state.logPanelCollapsed));
  if (persist) {
    localStorage.setItem(STORAGE_KEY_LOG_COLLAPSED, state.logPanelCollapsed ? '1' : '0');
  }
}

/**
 * 开始拖拽调整底部执行日志高度。
 *
 * @param {PointerEvent} event 指针按下事件
 */
function beginLogResize(event) {
  if (state.logPanelCollapsed) {
    return;
  }
  event.preventDefault();
  elements.logResizeHandle.classList.add('active');
  document.body.classList.add('resizing-log');
  document.addEventListener('pointermove', resizeLogPanel);
  document.addEventListener('pointerup', endLogResize);
}

/**
 * 根据指针纵坐标实时更新底部执行日志高度。
 *
 * @param {PointerEvent} event 指针移动事件
 */
function resizeLogPanel(event) {
  const workspaceRect = elements.workspace.getBoundingClientRect();
  setLogPanelHeight(workspaceRect.bottom - event.clientY - 8, false);
}

/**
 * 结束执行日志高度拖拽并持久化最终高度。
 */
function endLogResize() {
  elements.logResizeHandle.classList.remove('active');
  document.body.classList.remove('resizing-log');
  document.removeEventListener('pointermove', resizeLogPanel);
  document.removeEventListener('pointerup', endLogResize);
  setLogPanelHeight(state.logPanelHeight, true);
}

/**
 * 复制当前执行日志到系统剪贴板。
 */
async function copyLogOutput() {
  const content = elements.logOutput.textContent || '';
  if (!content.trim()) {
    notify(MESSAGE_LOG_EMPTY_TO_COPY, NOTICE_TYPE_INFO);
    return;
  }
  try {
    await navigator.clipboard.writeText(content);
    notify(MESSAGE_LOG_COPIED, NOTICE_TYPE_SUCCESS);
  } catch (error) {
    notify(`${MESSAGE_LOG_COPY_FAILED}：${error.message}`, NOTICE_TYPE_ERROR);
  }
}

/**
 * 清空当前界面显示的执行日志。
 */
function clearLogOutput() {
  elements.logOutput.textContent = '';
}

/**
 * 按 ID 关闭对应的非原生自定义弹窗。
 *
 * @param {string} modalId 弹窗 DOM ID
 */
function closeModalById(modalId) {
  if (modalId === 'settingsDialog' && !elements.closeSettingsBtn.disabled) {
    closeSettingsDialog();
  } else if (modalId === 'projectSettingsDialog') {
    closeProjectSettingsDialog();
  } else if (modalId === 'projectHistoryDialog') {
    closeProjectHistoryDialog();
  } else if (modalId === 'errorGuideDialog') {
    closeErrorGuideDialog();
  } else if (modalId === 'aiAssistantDialog' && !elements.closeAiAssistantBtn.disabled) {
    closeAiAssistantDialog();
  } else if (modalId === 'decompileDialog') {
    elements.cancelDecompileBtn.click();
  } else if (modalId === 'changePasswordDialog' && window.jarPatchAuth) {
    window.jarPatchAuth.closeChangePasswordDialog();
  } else if (modalId === 'userManagementDialog' && window.jarPatchAuth) {
    window.jarPatchAuth.closeUserManagement();
  }
}

/**
 * 退出登录或会话失效时，收起所有已打开的工作台非阻塞弹窗。
 */
function closeAllWorkbenchOverlays() {
  setMoreToolsOpen(false);
  closeSettingsDialog();
  closeProjectSettingsDialog();
  closeProjectHistoryDialog();
  closeErrorGuideDialog();
  closeAiAssistantDialog();
  if (elements.diffDialog.open) {
    elements.diffDialog.close();
  }
  if (elements.patchDirectoryDialog.open) {
    elements.patchDirectoryDialog.close();
  }
  const runtimeDialog = document.getElementById('runtimeDialog');
  if (runtimeDialog && runtimeDialog.open) {
    runtimeDialog.close();
  }
}

/**
 * 按 Esc 键优先关闭展开的顶部下拉菜单或最上层自定义弹窗。
 *
 * @returns {boolean} 已消费本次 Esc 按键时返回 true
 */
function closeTopmostOverlay() {
  if (window.jarPatchAuth && window.jarPatchAuth.closeTopmostAuthOverlay()) {
    return true;
  }
  if (!elements.moreToolsPopover.hidden) {
    setMoreToolsOpen(false);
    elements.moreToolsBtn.focus();
    return true;
  }
  const modalIds = [
    'aiAssistantDialog',
    'settingsDialog',
    'projectSettingsDialog',
    'projectHistoryDialog',
    'errorGuideDialog',
    'decompileDialog'
  ];
  for (const id of modalIds) {
    const node = elements[id];
    if (node && node.classList.contains('open')) {
      closeModalById(id);
      return true;
    }
  }
  return false;
}

function toggleSidebar() {
  const collapsed = elements.appShell.classList.toggle('sidebar-collapsed');
  const label = collapsed ? '展开项目栏' : '收起项目栏';
  elements.sidebarContent.hidden = collapsed;
  elements.sidebarQuickOpenBtn.hidden = !collapsed;
  elements.sidebarToggleBtn.setAttribute('aria-expanded', String(!collapsed));
  elements.sidebarToggleBtn.setAttribute('aria-label', label);
  elements.sidebarToggleBtn.title = label;
  setTreePanelWidth(state.treePanelWidth, false);
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
  const output = elements.logOutput;
  // 增量追加避免整段历史二次方拼接；超限后丢弃最早内容，防止长任务日志无界增长。
  output.insertAdjacentText('beforeend', `[${time}] ${message}\n`);
  if (output.textContent.length > LOG_OUTPUT_MAX_CHARS) {
    output.textContent = output.textContent.slice(-LOG_OUTPUT_MAX_CHARS);
  }
  output.scrollTop = output.scrollHeight;
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

elements.sidebarToggleBtn.addEventListener('click', toggleSidebar);
elements.sidebarQuickOpenBtn.addEventListener('click', importArchive);
elements.openArchiveBtn.addEventListener('click', importArchive);
elements.projectSearchInput.addEventListener('input', renderFilteredProjects);
elements.emptyOpenArchiveBtn.addEventListener('click', importArchive);
elements.emptyOpenRuntimeBtn.addEventListener('click', () => elements.runtimeBtn.click());
elements.themeToggleBtn.addEventListener('click', toggleTheme);
elements.moreToolsBtn.addEventListener('click', (event) => {
  event.stopPropagation();
  setMoreToolsOpen(elements.moreToolsPopover.hidden);
});
elements.moreToolsPopover.addEventListener('click', (event) => {
  if (event.target.closest('.dropdown-item')) {
    setMoreToolsOpen(false);
  }
});
document.addEventListener('click', (event) => {
  if (!elements.moreToolsPopover.hidden && !elements.moreToolsMenu.contains(event.target)) {
    setMoreToolsOpen(false);
  }
});
elements.compactFoldersBtn.addEventListener('click', toggleCompactFolders);
elements.collapseTreeBtn.addEventListener('click', collapseFileTree);
elements.copyFilePathBtn.addEventListener('click', copyCurrentFilePath);
elements.clearSearchBtn.addEventListener('click', clearSearchResults);
elements.searchInput.addEventListener('input', updateSearchClearButton);
elements.decompileFilterInput.addEventListener('input', renderDecompileDialog);
elements.diffExpandAllBtn.addEventListener('click', () => {
  elements.diffContent.querySelectorAll('details.diff-item').forEach((item) => {
    item.open = true;
  });
});
elements.diffCollapseAllBtn.addEventListener('click', () => {
  elements.diffContent.querySelectorAll('details.diff-item').forEach((item) => {
    item.open = false;
  });
});
elements.settingsTabs.addEventListener('click', (event) => {
  const tabButton = event.target.closest('[data-settings-tab]');
  if (tabButton) {
    selectSettingsTab(tabButton.dataset.settingsTab);
  }
});
elements.projectHistoryTabs.addEventListener('click', (event) => {
  const tabButton = event.target.closest('[data-history-tab]');
  if (tabButton) {
    selectHistoryTab(tabButton.dataset.historyTab);
  }
});
elements.languageServerEnabledInput.addEventListener('change', updateSettingsTabDots);
elements.aiEnabledInput.addEventListener('change', updateSettingsTabDots);
elements.logResizeHandle.addEventListener('pointerdown', beginLogResize);
elements.copyLogBtn.addEventListener('click', copyLogOutput);
elements.clearLogBtn.addEventListener('click', clearLogOutput);
elements.toggleLogBtn.addEventListener('click', () => setLogPanelCollapsed(!state.logPanelCollapsed, true));
document.querySelectorAll('[data-close-modal]').forEach((button) => {
  button.addEventListener('click', () => closeModalById(button.dataset.closeModal));
});
document.querySelectorAll('[data-close-dialog]').forEach((button) => {
  button.addEventListener('click', () => {
    const dialog = document.getElementById(button.dataset.closeDialog);
    if (dialog && dialog.open) {
      dialog.close();
    }
  });
});
elements.findInEditorBtn.addEventListener('click', () => {
  const activeView = state.unicodePreviewOpen ? state.unicodePreviewView : elements.editor;
  activeView.showFind();
});
elements.unicodePreviewBtn.addEventListener('click', toggleUnicodePreview);
elements.unicodePreviewPrevBtn.addEventListener('click', () => navigateUnicodePreviewMatch(UNICODE_PREVIEW_STEP_PREV));
elements.unicodePreviewNextBtn.addEventListener('click', () => navigateUnicodePreviewMatch(UNICODE_PREVIEW_STEP_NEXT));
elements.saveBtn.addEventListener('click', saveCurrentFile);
elements.quickFixBtn.addEventListener('click', showQuickFix);
elements.organizeImportsBtn.addEventListener('click', organizeImports);
elements.aiInlineBtn.addEventListener('click', triggerAiInlineCompletion);
elements.aiAssistantBtn.addEventListener('click', openAiAssistantDialog);
elements.fileEncodingSelect.addEventListener('change', reopenCurrentFileWithEncoding);
elements.analyzeBtn.addEventListener('click', analyzeProject);
elements.diffBtn.addEventListener('click', () => openDiffDialog());
elements.compileBtn.addEventListener('click', compileProject);
elements.patchDirectoryBtn.addEventListener('click', importPatchDirectory);
elements.exportBtn.addEventListener('click', exportProject);
elements.settingsBtn.addEventListener('click', openSettingsDialog);
elements.projectSettingsBtn.addEventListener('click', openProjectSettingsDialog);
elements.projectHistoryBtn.addEventListener('click', openProjectHistoryDialog);
elements.cleanupWorkspaceBtn.addEventListener('click', cleanupCurrentWorkspace);
elements.orphanWorkspacesBtn.addEventListener('click', cleanupOrphanWorkspaces);
elements.errorGuideBtn.addEventListener('click', openErrorGuideDialog);
elements.diagnosticBtn.addEventListener('click', exportDiagnostics);
/**
 * 获取帮助手册中全部章节节点。
 *
 * @author 黄杰
 * @returns {HTMLElement[]} 章节元素列表
 */
function getHelpSections() {
  return Array.from(elements.helpContent.querySelectorAll('section[data-help-section]'));
}

/**
 * 同步左侧帮助目录的当前激活章节样式与无障碍标记。
 *
 * @author 黄杰
 * @param {string} sectionId 目标章节标识
 * @returns {void}
 */
function setActiveHelpSection(sectionId) {
  const navLinks = Array.from(elements.helpNavigation.querySelectorAll('a[href^="#"]'));
  for (const link of navLinks) {
    const targetId = link.hash.slice(HELP_HASH_PREFIX_LENGTH);
    const active = Boolean(sectionId) && targetId === sectionId && !link.hidden;
    link.classList.toggle(HELP_ACTIVE_CLASS, active);
    if (active) {
      link.setAttribute('aria-current', HELP_ARIA_CURRENT_VALUE);
      // 仅在目录项滚出左侧导航可视区域时柔和滚入，避免干扰正文阅读
      link.scrollIntoView({ block: 'nearest' });
    } else {
      link.removeAttribute('aria-current');
    }
  }
}

/**
 * 根据帮助正文当前滚动位置计算并高亮对应目录项。
 *
 * @author 黄杰
 * @returns {void}
 */
function syncActiveHelpSectionFromScroll() {
  const visibleSections = getHelpSections().filter((section) => !section.hidden);
  if (visibleSections.length === 0) {
    setActiveHelpSection('');
    return;
  }
  const container = elements.helpContent;
  const reachedBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - HELP_SCROLL_BOTTOM_THRESHOLD_PX;
  // 滚到底部时直接高亮最后一个可见章节，避免末尾短章节无法被顶部阅读线触发
  if (reachedBottom) {
    setActiveHelpSection(visibleSections[visibleSections.length - 1].dataset.helpSection || '');
    return;
  }
  const containerRect = container.getBoundingClientRect();
  let activeId = visibleSections[0].dataset.helpSection || '';
  for (const section of visibleSections) {
    const topOffset = section.getBoundingClientRect().top - containerRect.top;
    if (topOffset <= HELP_SCROLL_ACTIVE_OFFSET_PX) {
      activeId = section.dataset.helpSection || activeId;
    } else {
      break;
    }
  }
  setActiveHelpSection(activeId);
}

/**
 * 通过动画帧节流同步帮助手册滚动高亮状态。
 *
 * @author 黄杰
 * @returns {void}
 */
function scheduleHelpScrollSync() {
  if (state.helpSuppressNextScrollSync) {
    state.helpSuppressNextScrollSync = false;
    return;
  }
  if (state.helpScrollSyncFrame) {
    return;
  }
  state.helpScrollSyncFrame = window.requestAnimationFrame(() => {
    state.helpScrollSyncFrame = 0;
    syncActiveHelpSectionFromScroll();
  });
}

/**
 * 按用户输入关键词筛选帮助手册章节与左侧目录分组。
 * <p>
 * 入口为搜索框输入事件或打开/重置帮助手册；按空格拆分多关键词并同时匹配
 * 章节正文与 data-help-keywords 别名，自动隐藏空分组并刷新匹配计数。
 * </p>
 *
 * @author 黄杰
 * @param {string} rawKeyword 原始搜索词
 * @returns {void}
 */
function filterHelpManual(rawKeyword) {
  const normalized = String(rawKeyword || '').trim().toLowerCase();
  const tokens = normalized ? normalized.split(/\s+/).filter(Boolean) : [];
  const sections = getHelpSections();
  const matchedIds = new Set();

  // 1. 逐章节判定是否命中全部关键词并控制显隐
  for (const section of sections) {
    const sectionId = section.dataset.helpSection || '';
    const searchableText = `${section.dataset.helpKeywords || ''} ${section.textContent || ''}`.toLowerCase();
    const matched = tokens.length === 0 || tokens.every((token) => searchableText.includes(token));
    section.hidden = !matched;
    if (matched) {
      matchedIds.add(sectionId);
    }
  }

  // 2. 同步过滤左侧目录链接，并自动隐藏无可见条目的目录分组
  const navGroups = Array.from(elements.helpNavigation.querySelectorAll('.help-nav-group'));
  for (const group of navGroups) {
    const groupLinks = Array.from(group.querySelectorAll('a[href^="#"]'));
    let visibleCount = 0;
    for (const link of groupLinks) {
      const targetId = link.hash.slice(HELP_HASH_PREFIX_LENGTH);
      const visible = matchedIds.has(targetId);
      link.hidden = !visible;
      if (visible) {
        visibleCount += 1;
      }
    }
    group.hidden = visibleCount === 0;
  }

  // 3. 更新清空按钮、匹配计数文案与空状态提示
  const hasFilter = tokens.length > 0;
  elements.helpSearchClearBtn.hidden = !hasFilter;
  elements.helpSearchEmpty.hidden = matchedIds.size > 0;
  if (hasFilter) {
    elements.helpSearchStatus.textContent = matchedIds.size > 0
      ? `${HELP_SEARCH_RESULT_PREFIX}${matchedIds.size}${HELP_SEARCH_RESULT_SUFFIX}`
      : HELP_SEARCH_EMPTY_TEXT;
    elements.helpSearchStatus.hidden = false;
  } else {
    elements.helpSearchStatus.textContent = '';
    elements.helpSearchStatus.hidden = true;
  }

  // 4. 筛选变更后回到正文顶部并高亮首个可见章节
  elements.helpContent.scrollTop = 0;
  syncActiveHelpSectionFromScroll();
}

/**
 * 打开离线帮助手册弹窗并重置筛选与阅读位置。
 *
 * @author 黄杰
 * @returns {void}
 */
function openHelpDialog() {
  elements.helpSearchInput.value = '';
  filterHelpManual('');
  elements.helpDialog.showModal();
  elements.helpContent.scrollTop = 0;
  syncActiveHelpSectionFromScroll();
}

elements.helpBtn.addEventListener('click', openHelpDialog);
elements.helpNavigation.addEventListener('click', (event) => {
  const link = event.target.closest('a');
  if (!link || link.hidden) {
    return;
  }
  event.preventDefault();
  const targetId = link.hash.slice(HELP_HASH_PREFIX_LENGTH);
  const heading = document.getElementById(targetId);
  if (!heading) {
    return;
  }
  state.helpSuppressNextScrollSync = true;
  setActiveHelpSection(targetId);
  heading.focus({ preventScroll: true });
  heading.scrollIntoView({ block: 'start' });
  window.requestAnimationFrame(() => {
    state.helpSuppressNextScrollSync = false;
  });
});
elements.helpContent.addEventListener('scroll', scheduleHelpScrollSync);
elements.helpSearchInput.addEventListener('input', () => {
  filterHelpManual(elements.helpSearchInput.value);
});
elements.helpSearchClearBtn.addEventListener('click', () => {
  elements.helpSearchInput.value = '';
  filterHelpManual('');
  elements.helpSearchInput.focus();
});
elements.helpSearchResetBtn.addEventListener('click', () => {
  elements.helpSearchInput.value = '';
  filterHelpManual('');
  elements.helpSearchInput.focus();
});
elements.helpDialog.addEventListener('click', (event) => {
  if (event.target === elements.helpDialog) {
    elements.helpDialog.close();
  }
});
elements.searchBtn.addEventListener('click', searchProject);
elements.cancelTaskBtn.addEventListener('click', cancelCurrentTask);
elements.retryCompileBtn.addEventListener('click', compileProject);
elements.dismissTaskBtn.addEventListener('click', dismissTaskFailure);
elements.browseJdkBtn.addEventListener('click', pickJdkHomeDirectory);
elements.browseJdtLsBtn.addEventListener('click', pickJdtLsHomeDirectory);
elements.browseJdtLsJavaBtn.addEventListener('click', pickJdtLsJavaHomeDirectory);
elements.closeSettingsBtn.addEventListener('click', closeSettingsDialog);
elements.saveJdkSettingsBtn.addEventListener('click', saveJdkSettings);
elements.aiProtocolSelect.addEventListener('change', updateAiProtocolFields);
elements.browseExportDirectoryBtn.addEventListener('click', pickExportDirectory);
elements.closeProjectSettingsBtn.addEventListener('click', closeProjectSettingsDialog);
elements.saveProjectSettingsBtn.addEventListener('click', saveProjectSettings);
elements.closeErrorGuideBtn.addEventListener('click', closeErrorGuideDialog);
elements.closeAiAssistantBtn.addEventListener('click', closeAiAssistantDialog);
elements.runAiAssistantBtn.addEventListener('click', runAiAssistant);
elements.applyAiResultBtn.addEventListener('click', applyAiResult);
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

/**
 * 初始化桌面工作台、Monaco 编辑器桥接与本地认证会话生命周期。
 * <p>
 * 登录或会话恢复成功后自动按角色同步界面权限并加载项目历史；
 * 退出或会话失效时立即清空当前工作区状态并回到登录遮罩。
 * </p>
 */
async function initializeApplication() {
  initializeTheme();
  initializeLogPanel();
  initializeCompactFolders();
  updateEditorEmptyState();
  const connection = await window.jarPatch.getBackendConnection();
  window.jarPatchApiClient.configure(connection);
  elements.editor.onLanguageStatus(handleLanguageStatus);
  elements.editor.onCursorChange(updateCursorStatus);
  elements.editor.setAiCompletionProvider(provideAiInlineCompletion);
  handleLanguageStatus({ state: 'idle', message: MESSAGE_LANGUAGE_SERVER_NOT_CONFIGURED });
  initializeTreePanelWidth();
  restoreWorkspaceControls();

  await window.jarPatchAuth.initialize({
    onAuthenticated: async (user) => {
      applyUserRoleUi(user);
      try {
        state.developmentSettings = await window.jarPatch.getDevelopmentSettings();
        renderDevelopmentSettings(state.developmentSettings);
        updateSettingsTabDots();
      } catch (error) {
        appendLog(`读取开发能力配置失败：${error.message}`);
      }
      restoreWorkspaceControls();
      await loadProjects();
    },
    onLoggedOut: () => {
      closeAllWorkbenchOverlays();
      applyUserRoleUi(null);
      state.allProjects = [];
      elements.projectSearchInput.value = '';
      renderProjects([], false);
      resetCurrentProject();
    },
    onNotify: notify
  });
}

initializeApplication().catch((error) => {
  notify(`连接本地后端失败：${error.message}`, NOTICE_TYPE_ERROR);
});
elements.projectSettingsDialog.addEventListener('click', (event) => {
  if (event.target === elements.projectSettingsDialog) {
    closeProjectSettingsDialog();
  }
});
elements.closeProjectHistoryBtn.addEventListener('click', closeProjectHistoryDialog);
elements.projectHistoryDialog.addEventListener('click', (event) => {
  if (event.target === elements.projectHistoryDialog) {
    closeProjectHistoryDialog();
  }
});
elements.errorGuideDialog.addEventListener('click', (event) => {
  if (event.target === elements.errorGuideDialog) {
    closeErrorGuideDialog();
  }
});
elements.aiAssistantDialog.addEventListener('click', (event) => {
  if (event.target === elements.aiAssistantDialog) {
    closeAiAssistantDialog();
  }
});
elements.editor.addEventListener('input', () => {
  state.editorRevision++;
  updateDirtyIndicator();
});
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    if (closeTopmostOverlay()) {
      event.preventDefault();
    }
    return;
  }
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault();
    if (!elements.diffDialog.open && !elements.patchDirectoryDialog.open && !elements.helpDialog.open) {
      saveCurrentFile();
    }
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
    elements.diffDialog.close();
  }
});
