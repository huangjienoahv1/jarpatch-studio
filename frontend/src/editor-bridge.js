import * as monaco from 'monaco-editor/esm/vs/editor/editor.api.js';
import 'monaco-editor/esm/vs/basic-languages/java/java.contribution.js';
import 'monaco-editor/esm/vs/basic-languages/xml/xml.contribution.js';
import 'monaco-editor/esm/vs/basic-languages/ini/ini.contribution.js';
import 'monaco-editor/esm/vs/basic-languages/yaml/yaml.contribution.js';
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution.js';
import 'monaco-editor/esm/vs/basic-languages/css/css.contribution.js';

const LANGUAGE_SERVER_OWNER = 'eclipse-jdt-ls';
const LANGUAGE_SERVER_CHANGE_DELAY_MS = 250;
const DEFAULT_LANGUAGE = 'plaintext';
const AI_CONTEXT_SIDE_CHARACTERS = 12000;
const JSON_RPC_METHOD_NOT_FOUND = -32601;
const JSON_RPC_INTERNAL_ERROR = -32603;
const LSP_TEXT_DOCUMENT_SYNC_FULL = 1;
const MESSAGE_JAVA_INTELLIGENCE_STARTING = 'Java 智能正在启动';
const MESSAGE_JAVA_INTELLIGENCE_CONNECTED_PREFIX = 'Java 智能已连接（进程 ';
const MESSAGE_JAVA_INTELLIGENCE_CONNECTED_SUFFIX = '）';
const MESSAGE_LANGUAGE_SERVER_STOPPED = 'Java 语言服务器已停止';
const MESSAGE_LANGUAGE_SERVER_NOT_STARTED = 'Java 语言服务器尚未启动';
const MESSAGE_LANGUAGE_SERVER_REQUEST_FAILED = 'Java 语言服务器请求失败';
const MESSAGE_JAVA_COMMUNICATION_FAILED_PREFIX = 'Java 智能通信失败：';
const MESSAGE_JAVA_PROCESS_EXITED_PREFIX = 'Java 智能进程已退出：';
const MESSAGE_UNSUPPORTED_SERVER_REQUEST_PREFIX = 'JarPatch Studio 未实现服务端请求：';
const MESSAGE_JAVA_DIAGNOSTICS_PREFIX = 'Java 智能：';
const MESSAGE_JAVA_DIAGNOSTICS_ERROR_SUFFIX = ' 个错误，';
const MESSAGE_JAVA_DIAGNOSTICS_WARNING_SUFFIX = ' 个警告';
const MARKER_SOURCE_JAVA = 'Java';

self.MonacoEnvironment = {
  getWorkerUrl: () => './generated/editor-worker.js'
};

const COMPLETION_KIND_MAP = new Map([
  [1, monaco.languages.CompletionItemKind.Text],
  [2, monaco.languages.CompletionItemKind.Method],
  [3, monaco.languages.CompletionItemKind.Function],
  [4, monaco.languages.CompletionItemKind.Constructor],
  [5, monaco.languages.CompletionItemKind.Field],
  [6, monaco.languages.CompletionItemKind.Variable],
  [7, monaco.languages.CompletionItemKind.Class],
  [8, monaco.languages.CompletionItemKind.Interface],
  [9, monaco.languages.CompletionItemKind.Module],
  [10, monaco.languages.CompletionItemKind.Property],
  [11, monaco.languages.CompletionItemKind.Unit],
  [12, monaco.languages.CompletionItemKind.Value],
  [13, monaco.languages.CompletionItemKind.Enum],
  [14, monaco.languages.CompletionItemKind.Keyword],
  [15, monaco.languages.CompletionItemKind.Snippet],
  [16, monaco.languages.CompletionItemKind.Color],
  [17, monaco.languages.CompletionItemKind.File],
  [18, monaco.languages.CompletionItemKind.Reference],
  [19, monaco.languages.CompletionItemKind.Folder],
  [20, monaco.languages.CompletionItemKind.EnumMember],
  [21, monaco.languages.CompletionItemKind.Constant],
  [22, monaco.languages.CompletionItemKind.Struct],
  [23, monaco.languages.CompletionItemKind.Event],
  [24, monaco.languages.CompletionItemKind.Operator],
  [25, monaco.languages.CompletionItemKind.TypeParameter]
]);

const MARKER_SEVERITY_MAP = new Map([
  [1, monaco.MarkerSeverity.Error],
  [2, monaco.MarkerSeverity.Warning],
  [3, monaco.MarkerSeverity.Info],
  [4, monaco.MarkerSeverity.Hint]
]);

/**
 * 维护渲染进程与 Eclipse JDT LS 之间的 JSON-RPC 会话。
 *
 * Electron 主进程只负责启动进程和转发标准 LSP 帧；本类负责协议初始化、文档同步、
 * 诊断落到 Monaco marker，以及补全、悬浮、跳转、代码操作和格式化能力注册。
 */
class JavaLanguageClient {
  /**
   * 创建 Java LSP 客户端。
   *
   * @param {MonacoEditorFacade} facade Monaco 编辑器门面
   * @param {(status: object) => void} statusListener 状态回调
   */
  constructor(facade, statusListener) {
    this.facade = facade;
    this.statusListener = statusListener;
    this.nextRequestId = 1;
    this.pendingRequests = new Map();
    this.disposables = [];
    this.project = null;
    this.rootUri = null;
    this.started = false;
    this.initialized = false;
    this.stopping = false;
    this.changeTimer = null;
    this.documentVersion = 0;
    this.documentUri = null;
    this.unsubscribe = window.jarPatch.onLanguageServerMessage((message) => this.handleMessage(message));
  }

  /**
   * 启动项目专属语言服务器并完成 LSP initialize 握手。
   *
   * @param {object} project 当前项目
   * @returns {Promise<void>} 初始化完成后结束
   */
  async start(project) {
    await this.stop();
    this.project = project;
    this.statusListener({ state: 'starting', message: MESSAGE_JAVA_INTELLIGENCE_STARTING });
    const launch = await window.jarPatch.startLanguageServer({
      projectId: project.id,
      workspacePath: project.workspacePath
    });
    this.started = true;
    try {
      this.rootUri = monaco.Uri.file(project.workspacePath).toString();
      const result = await this.request('initialize', this.createInitializeParams());
      this.serverCapabilities = result && result.capabilities ? result.capabilities : {};
      this.notify('initialized', {});
      this.notify('workspace/didChangeConfiguration', { settings: this.createJavaSettings() });
      this.initialized = true;
      this.registerProviders();
      this.statusListener({
        state: 'ready',
        message: `${MESSAGE_JAVA_INTELLIGENCE_CONNECTED_PREFIX}${launch.processId}${MESSAGE_JAVA_INTELLIGENCE_CONNECTED_SUFFIX}`
      });
      if (this.facade.model) {
        this.openDocument(this.facade.model);
      }
    } catch (error) {
      // 初始化失败时立即回收本次精确启动的进程，避免残留 JDT LS 占用工作区。
      await this.stop();
      throw error;
    }
  }

  /**
   * 关闭当前文档并停止语言服务器进程。
   *
   * @returns {Promise<void>} 清理完成后结束
   */
  async stop() {
    this.stopping = true;
    try {
      if (this.changeTimer) {
        clearTimeout(this.changeTimer);
        this.changeTimer = null;
      }
      if (this.documentUri && this.initialized) {
        this.notify('textDocument/didClose', { textDocument: { uri: this.documentUri } });
      }
      this.documentUri = null;
      for (const disposable of this.disposables.splice(0)) {
        disposable.dispose();
      }
      monaco.editor.removeAllMarkers(LANGUAGE_SERVER_OWNER);
      if (this.started) {
        try {
          // 先按 LSP 协议有序退出；即使服务端拒绝响应，也必须停止精确子进程。
          if (this.initialized) {
            await this.request('shutdown', null);
            this.notify('exit');
          }
        } finally {
          await window.jarPatch.stopLanguageServer();
        }
      }
    } finally {
      this.started = false;
      this.initialized = false;
      this.project = null;
      this.rootUri = null;
      for (const pending of this.pendingRequests.values()) {
        pending.reject(new Error(MESSAGE_LANGUAGE_SERVER_STOPPED));
      }
      this.pendingRequests.clear();
      this.stopping = false;
    }
  }

  /**
   * 释放长期注册的 Electron 消息监听。
   */
  dispose() {
    this.stop().catch((error) => this.statusListener({ state: 'error', message: error.message }));
    if (this.unsubscribe) {
      this.unsubscribe();
      this.unsubscribe = null;
    }
  }

  /**
   * 为当前 Monaco 模型发送 didOpen，并关闭上一个文档。
   *
   * @param {monaco.editor.ITextModel} model 当前模型
   */
  openDocument(model) {
    if (!this.initialized) {
      return;
    }
    const nextUri = model.uri.toString();
    if (this.documentUri && this.documentUri !== nextUri) {
      this.notify('textDocument/didClose', { textDocument: { uri: this.documentUri } });
    }
    this.documentUri = nextUri;
    this.documentVersion = 1;
    this.notify('textDocument/didOpen', {
      textDocument: {
        uri: nextUri,
        languageId: model.getLanguageId(),
        version: this.documentVersion,
        text: model.getValue()
      }
    });
  }

  /**
   * 以固定防抖窗口发送完整文档变更，避免每个按键都占用语言服务器队列。
   *
   * @param {monaco.editor.ITextModel} model 已变化模型
   */
  scheduleDocumentChange(model) {
    if (!this.initialized || model.uri.toString() !== this.documentUri) {
      return;
    }
    if (this.changeTimer) {
      clearTimeout(this.changeTimer);
    }
    this.changeTimer = setTimeout(() => {
      this.changeTimer = null;
      this.documentVersion++;
      this.notify('textDocument/didChange', {
        textDocument: { uri: this.documentUri, version: this.documentVersion },
        contentChanges: [{ text: model.getValue() }]
      });
    }, LANGUAGE_SERVER_CHANGE_DELAY_MS);
  }

  /**
   * 通知语言服务器当前文档已经由 JarPatch Studio 原子保存。
   */
  didSave() {
    if (!this.initialized || !this.documentUri || !this.facade.model) {
      return;
    }
    this.notify('textDocument/didSave', {
      textDocument: { uri: this.documentUri },
      text: this.facade.model.getValue()
    });
  }

  /**
   * 请求 JDT LS 生成并应用 source.organizeImports 编辑。
   *
   * @returns {Promise<boolean>} 是否应用了 import 编辑
   */
  async organizeImports() {
    if (!this.initialized || !this.facade.model || this.facade.model.getLanguageId() !== 'java') {
      return false;
    }
    const actions = await this.request('textDocument/codeAction', {
      textDocument: { uri: this.facade.model.uri.toString() },
      range: toLspRange(this.facade.editor.getSelection()),
      context: { diagnostics: [], only: ['source.organizeImports'] }
    });
    for (const action of actions || []) {
      if (action.edit && this.applyWorkspaceEdit(action.edit)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 注册当前语言服务器会话对应的 Monaco 语义能力。
   */
  registerProviders() {
    this.disposables.push(monaco.languages.registerCompletionItemProvider('java', {
      triggerCharacters: ['.', '@', '#'],
      provideCompletionItems: async (model, position) => {
        const response = await this.request('textDocument/completion', {
          textDocument: { uri: model.uri.toString() },
          position: toLspPosition(position),
          context: { triggerKind: 1 }
        });
        const items = Array.isArray(response) ? response : (response && response.items) || [];
        return { suggestions: items.map((item) => toMonacoCompletion(item, model, position)) };
      },
      resolveCompletionItem: async (item) => {
        if (!item._lspItem) {
          return item;
        }
        const resolved = await this.request('completionItem/resolve', item._lspItem);
        return Object.assign(item, toMonacoCompletion(resolved, this.facade.model, this.facade.editor.getPosition()));
      }
    }));

    this.disposables.push(monaco.languages.registerHoverProvider('java', {
      provideHover: async (model, position) => {
        const hover = await this.request('textDocument/hover', {
          textDocument: { uri: model.uri.toString() },
          position: toLspPosition(position)
        });
        return hover ? {
          range: hover.range ? toMonacoRange(hover.range) : undefined,
          contents: toMarkdownContents(hover.contents)
        } : null;
      }
    }));

    this.disposables.push(monaco.languages.registerDefinitionProvider('java', {
      provideDefinition: async (model, position) => {
        const definitions = await this.request('textDocument/definition', {
          textDocument: { uri: model.uri.toString() },
          position: toLspPosition(position)
        });
        const values = definitions ? (Array.isArray(definitions) ? definitions : [definitions]) : [];
        return values.map((definition) => ({
          uri: monaco.Uri.parse(definition.targetUri || definition.uri),
          range: toMonacoRange(definition.targetSelectionRange || definition.targetRange || definition.range)
        }));
      }
    }));

    this.disposables.push(monaco.languages.registerCodeActionProvider('java', {
      provideCodeActions: async (model, range, context) => {
        const actions = await this.request('textDocument/codeAction', {
          textDocument: { uri: model.uri.toString() },
          range: toLspRange(range),
          context: { diagnostics: context.markers.map(toLspDiagnostic) }
        });
        return {
          actions: (actions || []).filter((action) => action.edit).map((action) => ({
            title: action.title,
            kind: action.kind,
            isPreferred: action.isPreferred,
            diagnostics: context.markers,
            edit: toMonacoWorkspaceEdit(action.edit)
          })),
          dispose() {}
        };
      }
    }, { providedCodeActionKinds: ['quickfix', 'source.organizeImports'] }));

    this.disposables.push(monaco.languages.registerDocumentFormattingEditProvider('java', {
      provideDocumentFormattingEdits: async (model, options) => {
        const edits = await this.request('textDocument/formatting', {
          textDocument: { uri: model.uri.toString() },
          options: { tabSize: options.tabSize, insertSpaces: options.insertSpaces }
        });
        return (edits || []).map(toMonacoTextEdit);
      }
    }));
  }

  /**
   * 构造 JDT LS 初始化参数与客户端能力声明。
   *
   * @returns {object} initialize 请求参数
   */
  createInitializeParams() {
    return {
      processId: null,
      clientInfo: { name: 'JarPatch Studio', version: '0.1.0' },
      rootUri: this.rootUri,
      workspaceFolders: [{ uri: this.rootUri, name: this.project.name }],
      capabilities: {
        workspace: {
          applyEdit: true,
          configuration: true,
          workspaceFolders: true,
          workspaceEdit: { documentChanges: true, resourceOperations: [] }
        },
        textDocument: {
          synchronization: { dynamicRegistration: true, didSave: true },
          completion: {
            dynamicRegistration: true,
            completionItem: {
              snippetSupport: true,
              documentationFormat: ['markdown', 'plaintext'],
              resolveSupport: { properties: ['documentation', 'detail', 'additionalTextEdits'] }
            }
          },
          hover: { contentFormat: ['markdown', 'plaintext'] },
          definition: { linkSupport: true },
          codeAction: {
            codeActionLiteralSupport: {
              codeActionKind: { valueSet: ['quickfix', 'refactor', 'source', 'source.organizeImports'] }
            }
          },
          publishDiagnostics: {
            relatedInformation: true,
            tagSupport: { valueSet: [1, 2] },
            versionSupport: true
          }
        }
      },
      initializationOptions: {
        bundles: [],
        settings: this.createJavaSettings()
      },
      trace: 'off'
    };
  }

  /**
   * 返回适配 JarPatch 工作区目录结构的 JDT LS 设置。
   *
   * @returns {object} Java 语言服务器设置
   */
  createJavaSettings() {
    return {
      java: {
        project: {
          sourcePaths: ['sources'],
          outputPath: 'compiled',
          referencedLibraries: ['extracted/**/*.jar']
        },
        completion: { importOrder: ['java', 'javax', 'org', 'com'] },
        errors: { incompleteClasspath: { severity: 'warning' } },
        format: { enabled: true },
        signatureHelp: { enabled: true }
      }
    };
  }

  /**
   * 发送 JSON-RPC 请求并等待对应响应。
   *
   * @param {string} method LSP 方法
   * @param {object|null} params 请求参数
   * @returns {Promise<any>} 服务端结果
   */
  request(method, params) {
    if (!this.started && method !== 'initialize') {
      return Promise.reject(new Error(MESSAGE_LANGUAGE_SERVER_NOT_STARTED));
    }
    const id = this.nextRequestId++;
    return new Promise((resolve, reject) => {
      this.pendingRequests.set(id, { resolve, reject });
      window.jarPatch.sendLanguageServerMessage({ jsonrpc: '2.0', id, method, params }).catch((error) => {
        this.pendingRequests.delete(id);
        reject(error);
      });
    });
  }

  /**
   * 发送无需响应的 JSON-RPC 通知。
   *
   * @param {string} method LSP 方法
   * @param {object|null} params 通知参数
   */
  notify(method, params) {
    if (!this.started) {
      return;
    }
    const message = { jsonrpc: '2.0', method };
    if (params !== undefined) {
      message.params = params;
    }
    window.jarPatch.sendLanguageServerMessage(message).catch((error) => {
      this.statusListener({ state: 'error', message: `${MESSAGE_JAVA_COMMUNICATION_FAILED_PREFIX}${error.message}` });
    });
  }

  /**
   * 分派语言服务器响应、请求和通知。
   *
   * @param {object} message JSON-RPC 消息
   */
  async handleMessage(message) {
    if (Object.prototype.hasOwnProperty.call(message, 'id') && !message.method) {
      const pending = this.pendingRequests.get(message.id);
      if (!pending) {
        return;
      }
      this.pendingRequests.delete(message.id);
      if (message.error) {
        pending.reject(new Error(message.error.message || MESSAGE_LANGUAGE_SERVER_REQUEST_FAILED));
      } else {
        pending.resolve(message.result);
      }
      return;
    }
    if (message.method === 'textDocument/publishDiagnostics') {
      this.publishDiagnostics(message.params);
      return;
    }
    if (message.method === 'jarpatch/languageServerExit') {
      this.started = false;
      this.initialized = false;
      if (!this.stopping) {
        this.statusListener({ state: 'error', message: `${MESSAGE_JAVA_PROCESS_EXITED_PREFIX}${message.params.code}` });
      }
      return;
    }
    if (Object.prototype.hasOwnProperty.call(message, 'id') && message.method) {
      await this.handleServerRequest(message);
    }
  }

  /**
   * 处理 JDT LS 主动发给客户端的标准请求。
   *
   * @param {object} message JSON-RPC 请求
   */
  async handleServerRequest(message) {
    try {
      let result;
      switch (message.method) {
        case 'workspace/configuration':
          result = (message.params.items || []).map((item) => this.configurationForSection(item.section));
          break;
        case 'workspace/workspaceFolders':
          result = [{ uri: this.rootUri, name: this.project.name }];
          break;
        case 'workspace/applyEdit':
          result = { applied: this.applyWorkspaceEdit(message.params.edit) };
          break;
        case 'client/registerCapability':
        case 'client/unregisterCapability':
        case 'window/workDoneProgress/create':
        case 'window/showMessageRequest':
          result = null;
          break;
        default:
          this.respond(message.id, null, {
            code: JSON_RPC_METHOD_NOT_FOUND,
            message: `${MESSAGE_UNSUPPORTED_SERVER_REQUEST_PREFIX}${message.method}`
          });
          return;
      }
      this.respond(message.id, result);
    } catch (error) {
      this.respond(message.id, null, { code: JSON_RPC_INTERNAL_ERROR, message: error.message });
    }
  }

  /**
   * 按 workspace/configuration 的 section 返回对应配置对象。
   *
   * @param {string|undefined} section 配置节
   * @returns {object|null} 配置值
   */
  configurationForSection(section) {
    const settings = this.createJavaSettings();
    if (!section) {
      return settings;
    }
    return section.split('.').reduce((value, key) => value && value[key], settings) || null;
  }

  /**
   * 向语言服务器返回 JSON-RPC 结果或错误。
   *
   * @param {number|string} id 请求 ID
   * @param {any} result 响应结果
   * @param {object|null} error 错误对象
   */
  respond(id, result, error = null) {
    const response = { jsonrpc: '2.0', id };
    if (error) {
      response.error = error;
    } else {
      response.result = result;
    }
    window.jarPatch.sendLanguageServerMessage(response).catch((sendError) => {
      this.statusListener({ state: 'error', message: `${MESSAGE_JAVA_COMMUNICATION_FAILED_PREFIX}${sendError.message}` });
    });
  }

  /**
   * 把 JDT LS 诊断转换为 Monaco marker 并通知页面状态栏。
   *
   * @param {object} params publishDiagnostics 参数
   */
  publishDiagnostics(params) {
    const model = monaco.editor.getModel(monaco.Uri.parse(params.uri));
    if (!model) {
      return;
    }
    const markers = (params.diagnostics || []).map((diagnostic) => ({
      ...toMonacoRange(diagnostic.range),
      severity: MARKER_SEVERITY_MAP.get(diagnostic.severity) || monaco.MarkerSeverity.Info,
      message: diagnostic.message,
      source: diagnostic.source || MARKER_SOURCE_JAVA,
      code: diagnostic.code == null ? undefined : String(diagnostic.code),
      tags: diagnostic.tags
    }));
    monaco.editor.setModelMarkers(model, LANGUAGE_SERVER_OWNER, markers);
    const errors = markers.filter((marker) => marker.severity === monaco.MarkerSeverity.Error).length;
    const warnings = markers.filter((marker) => marker.severity === monaco.MarkerSeverity.Warning).length;
    this.statusListener({
      state: 'ready',
      message: `${MESSAGE_JAVA_DIAGNOSTICS_PREFIX}${errors}${MESSAGE_JAVA_DIAGNOSTICS_ERROR_SUFFIX}${warnings}${MESSAGE_JAVA_DIAGNOSTICS_WARNING_SUFFIX}`,
      errors,
      warnings
    });
  }

  /**
   * 应用仅涉及当前已打开模型的 WorkspaceEdit。
   *
   * 未打开文件不会被静默写盘；这类编辑返回未应用，由用户显式打开文件后再操作。
   *
   * @param {object} workspaceEdit LSP WorkspaceEdit
   * @returns {boolean} 是否完整应用
   */
  applyWorkspaceEdit(workspaceEdit) {
    const groups = collectWorkspaceTextEdits(workspaceEdit);
    if (!groups.length) {
      return false;
    }
    for (const group of groups) {
      const model = monaco.editor.getModel(monaco.Uri.parse(group.uri));
      if (!model) {
        return false;
      }
    }
    for (const group of groups) {
      const model = monaco.editor.getModel(monaco.Uri.parse(group.uri));
      model.pushEditOperations([], group.edits.map((edit) => ({
        range: toMonacoRange(edit.range),
        text: edit.newText,
        forceMoveMarkers: true
      })), () => null);
    }
    return true;
  }
}

/**
 * 用 Monaco 替代原 textarea，同时保留 renderer.js 已依赖的 value、disabled、focus 等接口。
 */
class MonacoEditorFacade {
  /**
   * 创建编辑器门面。
   *
   * @param {HTMLElement} container 编辑器容器
   */
  constructor(container) {
    this.container = container;
    this.model = null;
    this.readOnly = true;
    this.inputListeners = new Set();
    this.statusListener = () => {};
    this.aiCompletionProvider = null;
    this.programmaticChange = false;
    this.editor = monaco.editor.create(container, {
      theme: 'vs-dark',
      language: DEFAULT_LANGUAGE,
      automaticLayout: true,
      readOnly: true,
      fontFamily: 'JetBrains Mono, Consolas, monospace',
      fontSize: 14,
      lineHeight: 22,
      minimap: { enabled: true },
      bracketPairColorization: { enabled: true },
      guides: { bracketPairs: true, indentation: true },
      glyphMargin: true,
      folding: true,
      formatOnPaste: true,
      formatOnType: true,
      quickSuggestions: { other: true, comments: false, strings: false },
      suggestOnTriggerCharacters: true,
      inlineSuggest: { enabled: true },
      renderValidationDecorations: 'on',
      scrollBeyondLastLine: false,
      smoothScrolling: true,
      tabSize: 4,
      insertSpaces: true,
      wordWrap: 'off'
    });
    this.languageClient = new JavaLanguageClient(this, (status) => this.statusListener(status));
    this.editor.onDidChangeModelContent(() => this.handleModelContentChanged());
    this.editor.addCommand(monaco.KeyMod.Alt | monaco.KeyCode.Backslash, () => this.triggerAiCompletion());
    this.registerAiInlineProvider();
  }

  /**
   * 打开带真实文件 URI 的模型，使语言服务器能够解析项目和自动 import。
   *
   * @param {object} document 文件内容、绝对路径和语言
   */
  openDocument(document) {
    const uri = monaco.Uri.file(document.absolutePath);
    let nextModel = monaco.editor.getModel(uri);
    this.programmaticChange = true;
    try {
      if (nextModel) {
        nextModel.setValue(document.content);
        monaco.editor.setModelLanguage(nextModel, document.language || detectLanguage(document.path));
      } else {
        nextModel = monaco.editor.createModel(
          document.content,
          document.language || detectLanguage(document.path),
          uri
        );
      }
      const previousModel = this.model;
      this.model = nextModel;
      this.editor.setModel(nextModel);
      if (previousModel && previousModel !== nextModel) {
        previousModel.dispose();
      }
    } finally {
      this.programmaticChange = false;
    }
    this.languageClient.openDocument(nextModel);
  }

  /**
   * 关闭当前文件模型并清空编辑区。
   */
  closeDocument() {
    if (this.model) {
      this.model.dispose();
      this.model = null;
    }
    this.editor.setModel(null);
    monaco.editor.removeAllMarkers(LANGUAGE_SERVER_OWNER);
  }

  /**
   * 启动当前项目的 Java 语言服务器。
   *
   * @param {object} project 当前项目
   * @returns {Promise<void>} 初始化完成后结束
   */
  startLanguageServer(project) {
    return this.languageClient.start(project);
  }

  /**
   * 停止当前 Java 语言服务器。
   *
   * @returns {Promise<void>} 停止完成后结束
   */
  stopLanguageServer() {
    return this.languageClient.stop();
  }

  /**
   * 注册 Java 智能状态监听。
   *
   * @param {(status: object) => void} listener 状态监听器
   */
  onLanguageStatus(listener) {
    this.statusListener = listener;
  }

  /**
   * 注册仅在用户显式触发时调用的 AI 补全函数。
   *
   * @param {(context: object) => Promise<object>} provider AI 补全函数
   */
  setAiCompletionProvider(provider) {
    this.aiCompletionProvider = provider;
  }

  /**
   * 显式触发 Monaco 行内 AI 建议。
   *
   * @returns {Promise<void>} 建议请求结束后完成
   */
  async triggerAiCompletion() {
    if (!this.aiCompletionProvider || !this.model || this.readOnly) {
      return;
    }
    await this.editor.getAction('editor.action.inlineSuggest.trigger').run();
  }

  /**
   * 打开 Monaco 的标准快速修复菜单。
   *
   * @returns {Promise<void>} 菜单命令结束后完成
   */
  async showQuickFix() {
    await this.editor.getAction('editor.action.quickFix').run();
  }

  /**
   * 执行语言服务器提供的整理 import 操作。
   *
   * @returns {Promise<boolean>} 是否应用编辑
   */
  organizeImports() {
    return this.languageClient.organizeImports();
  }

  /**
   * 通知语言服务器当前文件已成功保存。
   */
  didSave() {
    this.languageClient.didSave();
  }

  /**
   * 获取当前选区及其原文，用于 AI 审阅和精确应用。
   *
   * @returns {object} 选区信息
   */
  getSelectionContext() {
    if (!this.model) {
      return { text: '', range: null, source: '' };
    }
    const selection = this.editor.getSelection();
    return {
      text: this.model.getValueInRange(selection),
      range: selection,
      source: this.model.getValue()
    };
  }

  /**
   * 把 AI 返回内容应用到调用时记录的选区或整个模型。
   *
   * @param {monaco.Selection|null} range 原始选区；空表示替换全文
   * @param {string} content 新内容
   */
  applyAiReplacement(range, content) {
    if (!this.model) {
      return;
    }
    const targetRange = range && !range.isEmpty()
      ? range
      : this.model.getFullModelRange();
    this.editor.executeEdits('jarpatch-ai', [{ range: targetRange, text: content, forceMoveMarkers: true }]);
    this.editor.focus();
  }

  /**
   * 获取当前 JDT LS marker，作为 AI 修复的结构化上下文。
   *
   * @returns {Array<object>} 诊断列表
   */
  getDiagnostics() {
    if (!this.model) {
      return [];
    }
    return monaco.editor.getModelMarkers({ resource: this.model.uri, owner: LANGUAGE_SERVER_OWNER })
      .map((marker) => ({
        message: marker.message,
        severity: marker.severity,
        line: marker.startLineNumber,
        column: marker.startColumn,
        source: marker.source,
        code: marker.code
      }));
  }

  /**
   * 注册兼容原 textarea 的输入监听。
   *
   * @param {string} type 事件名
   * @param {Function} listener 监听器
   */
  addEventListener(type, listener) {
    if (type === 'input') {
      this.inputListeners.add(listener);
    }
  }

  /**
   * 聚焦编辑器。
   */
  focus() {
    this.editor.focus();
  }

  /**
   * 根据 textarea 字符偏移兼容设置 Monaco 光标位置。
   *
   * @param {number} start 起始偏移
   */
  setSelectionRange(start) {
    if (!this.model) {
      return;
    }
    const position = this.model.getPositionAt(start);
    this.editor.setPosition(position);
    this.editor.revealPositionInCenter(position);
  }

  /**
   * 注册 AI 行内补全 Provider；自动触发不访问网络，只有显式操作才调用 AI。
   */
  registerAiInlineProvider() {
    monaco.languages.registerInlineCompletionsProvider({ pattern: '**' }, {
      provideInlineCompletions: async (model, position, context) => {
        if (context.triggerKind !== monaco.languages.InlineCompletionTriggerKind.Explicit
            || !this.aiCompletionProvider || this.readOnly || model !== this.model) {
          return { items: [] };
        }
        const offset = model.getOffsetAt(position);
        const source = model.getValue();
        const result = await this.aiCompletionProvider({
          language: model.getLanguageId(),
          prefix: source.slice(Math.max(0, offset - AI_CONTEXT_SIDE_CHARACTERS), offset),
          suffix: source.slice(offset, offset + AI_CONTEXT_SIDE_CHARACTERS)
        });
        if (!result || !result.content) {
          return { items: [] };
        }
        return {
          items: [{
            insertText: result.content,
            range: new monaco.Range(position.lineNumber, position.column, position.lineNumber, position.column)
          }]
        };
      },
      freeInlineCompletions() {}
    });
  }

  /**
   * 分派模型内容变化到页面和语言服务器。
   */
  handleModelContentChanged() {
    if (this.programmaticChange || !this.model) {
      return;
    }
    for (const listener of this.inputListeners) {
      listener({ type: 'input', target: this });
    }
    this.languageClient.scheduleDocumentChange(this.model);
  }

  get value() {
    return this.model ? this.model.getValue() : '';
  }

  set value(content) {
    if (!this.model) {
      if (content) {
        this.openDocument({ content, absolutePath: `untitled-${Date.now()}.txt`, path: '', language: DEFAULT_LANGUAGE });
      }
      return;
    }
    this.programmaticChange = true;
    try {
      this.model.setValue(content || '');
    } finally {
      this.programmaticChange = false;
    }
  }

  get disabled() {
    return this.readOnly;
  }

  set disabled(value) {
    this.readOnly = Boolean(value);
    this.editor.updateOptions({ readOnly: this.readOnly });
    this.container.classList.toggle('disabled', this.readOnly);
  }

  get hidden() {
    return this.container.hidden;
  }

  set hidden(value) {
    this.container.hidden = Boolean(value);
    if (!value) {
      this.editor.layout();
    }
  }

  get scrollTop() {
    return this.editor.getScrollTop();
  }

  set scrollTop(value) {
    this.editor.setScrollTop(value || 0);
  }
}

/**
 * 根据文件后缀选择 Monaco 基础语言。
 *
 * @param {string} filePath 文件路径
 * @returns {string} Monaco languageId
 */
function detectLanguage(filePath) {
  const extension = String(filePath || '').split('.').pop().toLowerCase();
  const languageMap = {
    java: 'java', xml: 'xml', properties: 'ini', yml: 'yaml', yaml: 'yaml',
    js: 'javascript', css: 'css', json: 'json', html: 'html'
  };
  return languageMap[extension] || DEFAULT_LANGUAGE;
}

/**
 * 把 Monaco 位置转为 LSP 零基位置。
 *
 * @param {monaco.Position} position Monaco 位置
 * @returns {object} LSP 位置
 */
function toLspPosition(position) {
  return { line: position.lineNumber - 1, character: position.column - 1 };
}

/**
 * 把 Monaco 范围转为 LSP 范围。
 *
 * @param {monaco.Range} range Monaco 范围
 * @returns {object} LSP 范围
 */
function toLspRange(range) {
  return {
    start: { line: range.startLineNumber - 1, character: range.startColumn - 1 },
    end: { line: range.endLineNumber - 1, character: range.endColumn - 1 }
  };
}

/**
 * 把 LSP 范围转为 Monaco 范围字段。
 *
 * @param {object} range LSP 范围
 * @returns {object} Monaco 范围字段
 */
function toMonacoRange(range) {
  return {
    startLineNumber: range.start.line + 1,
    startColumn: range.start.character + 1,
    endLineNumber: range.end.line + 1,
    endColumn: range.end.character + 1
  };
}

/**
 * 把 LSP TextEdit 转为 Monaco TextEdit。
 *
 * @param {object} edit LSP 编辑
 * @returns {object} Monaco 编辑
 */
function toMonacoTextEdit(edit) {
  return { range: toMonacoRange(edit.range), text: edit.newText };
}

/**
 * 转换补全项，包括 JDT LS 用于自动 import 的 additionalTextEdits。
 *
 * @param {object} item LSP CompletionItem
 * @param {monaco.editor.ITextModel} model 当前模型
 * @param {monaco.Position} position 光标位置
 * @returns {object} Monaco CompletionItem
 */
function toMonacoCompletion(item, model, position) {
  const edit = item.textEdit;
  const editRange = edit && (edit.replace || edit.range);
  const word = model.getWordUntilPosition(position);
  return {
    label: item.label,
    kind: COMPLETION_KIND_MAP.get(item.kind) || monaco.languages.CompletionItemKind.Text,
    detail: item.detail,
    documentation: toMonacoDocumentation(item.documentation),
    insertText: edit ? edit.newText : (item.insertText || item.label),
    insertTextRules: item.insertTextFormat === 2
      ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
      : monaco.languages.CompletionItemInsertTextRule.None,
    range: editRange ? toMonacoRange(editRange) : new monaco.Range(
      position.lineNumber,
      word.startColumn,
      position.lineNumber,
      word.endColumn
    ),
    additionalTextEdits: (item.additionalTextEdits || []).map(toMonacoTextEdit),
    sortText: item.sortText,
    filterText: item.filterText,
    preselect: item.preselect,
    _lspItem: item
  };
}

/**
 * 转换 LSP 文档字段。
 *
 * @param {string|object} documentation 文档内容
 * @returns {object|undefined} Monaco MarkdownString
 */
function toMonacoDocumentation(documentation) {
  if (!documentation) {
    return undefined;
  }
  return { value: typeof documentation === 'string' ? documentation : documentation.value };
}

/**
 * 转换 LSP hover 的多种内容表示。
 *
 * @param {any} contents hover 内容
 * @returns {Array<object>} Monaco MarkdownString 数组
 */
function toMarkdownContents(contents) {
  const values = Array.isArray(contents) ? contents : [contents];
  return values.filter(Boolean).map((value) => {
    if (typeof value === 'string') {
      return { value };
    }
    if (value.language && value.value) {
      return { value: `\`\`\`${value.language}\n${value.value}\n\`\`\`` };
    }
    return { value: value.value || '' };
  });
}

/**
 * 把 Monaco marker 转为代码操作请求中的 LSP Diagnostic。
 *
 * @param {object} marker Monaco marker
 * @returns {object} LSP Diagnostic
 */
function toLspDiagnostic(marker) {
  return {
    range: {
      start: { line: marker.startLineNumber - 1, character: marker.startColumn - 1 },
      end: { line: marker.endLineNumber - 1, character: marker.endColumn - 1 }
    },
    message: marker.message,
    severity: marker.severity === monaco.MarkerSeverity.Error ? 1
      : marker.severity === monaco.MarkerSeverity.Warning ? 2 : 3,
    source: marker.source,
    code: marker.code
  };
}

/**
 * 汇总 WorkspaceEdit 中的文本编辑，拒绝静默文件创建、删除或重命名。
 *
 * @param {object} workspaceEdit LSP WorkspaceEdit
 * @returns {Array<object>} 按 URI 分组的文本编辑
 */
function collectWorkspaceTextEdits(workspaceEdit) {
  const groups = [];
  for (const [uri, edits] of Object.entries(workspaceEdit.changes || {})) {
    groups.push({ uri, edits });
  }
  for (const change of workspaceEdit.documentChanges || []) {
    if (!change.textDocument || !Array.isArray(change.edits)) {
      return [];
    }
    groups.push({ uri: change.textDocument.uri, edits: change.edits });
  }
  return groups;
}

/**
 * 把 LSP WorkspaceEdit 转为 Monaco WorkspaceEdit。
 *
 * @param {object} workspaceEdit LSP WorkspaceEdit
 * @returns {object} Monaco WorkspaceEdit
 */
function toMonacoWorkspaceEdit(workspaceEdit) {
  return {
    edits: collectWorkspaceTextEdits(workspaceEdit).flatMap((group) => group.edits.map((edit) => ({
      resource: monaco.Uri.parse(group.uri),
      textEdit: { range: toMonacoRange(edit.range), text: edit.newText },
      versionId: undefined
    })))
  };
}

window.jarPatchEditor = Object.freeze({
  create: (container) => new MonacoEditorFacade(container),
  detectLanguage
});
