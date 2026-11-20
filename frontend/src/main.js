const { app, BrowserWindow, Menu, dialog, ipcMain, shell } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');
const fs = require('fs');
const readline = require('readline');
const { createStartupDiagnostics } = require('./startup-diagnostics');
const startupDiagnostics = createStartupDiagnostics(app.getPath('userData'));
startupDiagnostics.write('DESKTOP_START', { electron: process.versions.electron, node: process.version,
  platform: process.platform, resourcesPath: process.resourcesPath });
process.on('uncaughtExceptionMonitor', (error) => startupDiagnostics.write('UNCAUGHT_EXCEPTION', error.stack || error.message));
const { DevelopmentServices } = require('./development-services');
const { RuntimeWorkbench } = require('./runtime-workbench');
const { listSshHosts, saveSshHost, removeSshHost, savedSshPassword } = require('./runtime-ssh-hosts');
const { MAX_JSON_BYTES, normalizeSuite } = require('./runtime-validation');
const { tunnelUrl } = require('./runtime-integrations');
const fetch = require('./http-fetch');

const BACKEND_PORT = 18765;
const BACKEND_HOST = '127.0.0.1';
const BACKEND_BASE_URL = `http://${BACKEND_HOST}:${BACKEND_PORT}`;
const BACKEND_JAR_NAME = 'jarpatch-studio-backend.jar';
const BUNDLED_RUNTIME_DIRECTORY = 'runtime';
const JAVAC_EXECUTABLE_NAME = process.platform === 'win32' ? 'javac.exe' : 'javac';
const BACKEND_READY_TIMEOUT_MS = 20000;
const BACKEND_POLL_INTERVAL_MS = 250;
const BACKEND_SHUTDOWN_TIMEOUT_MS = 5000;
const BACKEND_LOG_LIMIT = 80;
const DIAGNOSTIC_FILE_MAX_BYTES = 5 * 1024 * 1024;
const DIAGNOSTIC_FILE_DEFAULT_NAME = 'jarpatch-studio-diagnostics.json';
const DIAGNOSTIC_FILE_ENCODING = 'utf8';
const BACKEND_LOG_DIRECTORY_NAME = 'logs';
const BACKEND_LOG_FILE_NAME = 'backend.log';
const PRODUCT_NAME = 'JarPatch Studio';
const ENV_AUTH_TOKEN = 'JARPATCH_AUTH_TOKEN';
const ENV_INSTANCE_ID = 'JARPATCH_INSTANCE_ID';
const ENV_BACKEND_LOG_FILE = 'JARPATCH_LOG_FILE';
const SMOKE_CHECK_ARGUMENT = '--smoke-check';
const SMOKE_CHECK_STATUS = 'READY';
const smokeCheckRequested = process.argv.includes(SMOKE_CHECK_ARGUMENT);
let backendProcess = null;
let backendStopping = false;
let applicationQuitAllowed = false;
let backendShutdownPromise = null;
let backendToken = null;
let backendInstanceId = null;
let backendLogPath = null;
let mainWindow = null;
let backendReady = false;
const backendLogs = [];
const developmentServices = new DevelopmentServices();
const knownRuntimeHostKeys = new Map();
const runtimeWorkbench = new RuntimeWorkbench({ confirmSshHost: confirmRuntimeSshHost, requestRuntimeSource });
let runtimeOfflinePackage;
let runtimePatchFile;

async function requestRuntimeSource(action, payload, signal) {
  if (!backendReady) throw new Error('本地后端未就绪');
  const controller = new AbortController();
  const cancel = () => controller.abort();
  const timeout = setTimeout(cancel, 110000);
  signal.addEventListener('abort', cancel, { once: true });
  if (signal.aborted) cancel();
  try {
    const response = await fetch(BACKEND_BASE_URL + '/api/runtime/' + action, {
      method: 'POST', headers: { 'X-JarPatch-Token': backendToken, 'Content-Type': 'application/json' },
      body: JSON.stringify(payload), signal: controller.signal
    });
    if (!response.ok) throw new Error('本地运行时源码接口请求失败（HTTP ' + response.status + '）');
    const result = await response.json();
    if (!result.success || typeof result.data !== 'string') throw new Error(result.message || '本地运行时源码接口响应无效');
    return result.data;
  } catch (error) {
    if (controller.signal.aborted && !signal.aborted) throw new Error('本地运行时源码操作超时');
    throw error;
  } finally {
    clearTimeout(timeout);
    signal.removeEventListener('abort', cancel);
  }
}

const singleInstanceLockAcquired = app.requestSingleInstanceLock();

/**
 * 创建并记录主工作台窗口，供第二次启动时激活现有实例。
 *
 * @returns {BrowserWindow} 新建的主窗口
 */
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1180,
    minHeight: 760,
    title: 'JarPatch Studio',
    backgroundColor: '#f4f1ea',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.on('closed', () => {
    developmentServices.stopLanguageServer()
      .catch((error) => appendBackendLog('JDTLS', `关闭语言服务器失败：${error.message}`));
    releaseRuntimeWorkbench();
    mainWindow = null;
  });
  mainWindow.loadFile(path.join(__dirname, 'index.html'));
  return mainWindow;
}

/**
 * 启动当前 Electron 实例专属的后端进程，并把实例 ID、访问令牌和日志路径传入后端。
 */
function startBackend() {
  const backendJar = resolveBackendJarPath();
  if (!fs.existsSync(backendJar)) {
    throw new Error(`未找到后端程序：${backendJar}`);
  }
  backendToken = crypto.randomBytes(32).toString('hex');
  startupDiagnostics.protectSecret(backendToken);
  backendInstanceId = crypto.randomUUID();
  backendLogPath = resolveBackendLogPath();
  const javaExecutable = resolveJavaExecutable();
  startupDiagnostics.write('BACKEND_LAUNCH', { javaExecutable, backendJar,
    bundledJavacExists: fs.existsSync(path.join(path.dirname(javaExecutable), JAVAC_EXECUTABLE_NAME)) });
  backendProcess = spawn(javaExecutable, ['-jar', backendJar], {
    cwd: path.dirname(backendJar),
    env: {
      ...process.env,
      [ENV_AUTH_TOKEN]: backendToken,
      [ENV_INSTANCE_ID]: backendInstanceId,
      [ENV_BACKEND_LOG_FILE]: backendLogPath
    },
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true
  });
  captureBackendOutput(backendProcess.stdout, 'OUT');
  captureBackendOutput(backendProcess.stderr, 'ERR');
  backendProcess.once('error', (error) => appendBackendLog('ERR', error.message));
  backendProcess.once('exit', (code, signal) => {
    appendBackendLog('EXIT', `code=${code}, signal=${signal || ''}`);
    if (!backendStopping && BrowserWindow.getAllWindows().length > 0) {
      dialog.showErrorBox('JarPatch Studio 后端已停止', formatBackendFailure('后端进程异常退出'));
    }
  });
}

/**
 * 创建桌面端可定位的后端滚动日志目录。
 *
 * @returns {string} 后端主日志文件绝对路径
 */
function resolveBackendLogPath() {
  const logDirectory = path.join(app.getPath('userData'), BACKEND_LOG_DIRECTORY_NAME);
  fs.mkdirSync(logDirectory, { recursive: true });
  return path.join(logDirectory, BACKEND_LOG_FILE_NAME);
}

function resolveBackendJarPath() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, 'backend', BACKEND_JAR_NAME);
  }
  return path.resolve(__dirname, '..', '..', 'backend', 'target', BACKEND_JAR_NAME);
}

function resolveJavaExecutable() {
  const javaName = process.platform === 'win32' ? 'java.exe' : 'java';
  if (app.isPackaged) {
    const bundledJava = path.join(process.resourcesPath, BUNDLED_RUNTIME_DIRECTORY, 'bin', javaName);
    if (!fs.existsSync(bundledJava)) {
      throw new Error(`发布包缺少 Java 运行时：${bundledJava}`);
    }
    return bundledJava;
  }
  if (process.env.JAVA_HOME) {
    const candidate = path.join(process.env.JAVA_HOME, 'bin', javaName);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return javaName;
}

/**
 * 从发布包的 Java release 文件读取内置编译 JDK 版本。
 *
 * @returns {Promise<string|null>} 完整 JDK 的版本号，未内置编译器时为 null
 */
async function getBundledJdkVersion() {
  if (!app.isPackaged) {
    return null;
  }
  const runtimeHome = path.join(process.resourcesPath, BUNDLED_RUNTIME_DIRECTORY);
  if (!fs.existsSync(path.join(runtimeHome, 'bin', JAVAC_EXECUTABLE_NAME))) {
    return null;
  }
  const release = await fs.promises.readFile(path.join(runtimeHome, 'release'), 'utf8');
  const version = release.match(/^JAVA_VERSION="([^"\r\n]+)"$/m)?.[1];
  if (!version) {
    throw new Error('内置 JDK 的 release 文件缺少 JAVA_VERSION');
  }
  return version;
}

function captureBackendOutput(stream, channel) {
  if (!stream) {
    return;
  }
  const reader = readline.createInterface({ input: stream });
  reader.on('line', (line) => appendBackendLog(channel, line));
}

function appendBackendLog(channel, message) {
  if (!backendReady) startupDiagnostics.write(`BACKEND_${channel}`, message);
  backendLogs.push(`[${channel}] ${message}`);
  if (backendLogs.length > BACKEND_LOG_LIMIT) {
    backendLogs.shift();
  }
}

async function waitForBackendReady() {
  const deadline = Date.now() + BACKEND_READY_TIMEOUT_MS;
  while (Date.now() < deadline) {
    if (backendProcess && backendProcess.exitCode != null) {
      throw new Error('后端进程在就绪前退出');
    }
    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/system/health`, {
        headers: { 'X-JarPatch-Token': backendToken }
      });
      if (response.ok) {
        const body = await response.json();
        const status = body.data || {};
        if (body.success && status.product === PRODUCT_NAME && status.instanceId === backendInstanceId && status.status === 'UP') {
          return;
        }
      }
    } catch (error) {
      appendBackendLog('WAIT', error.message);
    }
    await new Promise((resolve) => setTimeout(resolve, BACKEND_POLL_INTERVAL_MS));
  }
  throw new Error('等待后端健康检查超时，端口可能被其他进程占用');
}

function formatBackendFailure(message) {
  const details = backendLogs.slice(-20).join('\n');
  const identity = `实例 ID：${backendInstanceId || '未生成'}\n启动日志：${startupDiagnostics.filePath}\n后端日志：${backendLogPath || '未生成'}`;
  return details ? `${message}\n${identity}\n\n${details}` : `${message}\n${identity}`;
}

async function stopBackendGracefully() {
  backendStopping = true;
  if (!backendProcess || backendProcess.exitCode != null) {
    return;
  }
  try {
    await fetch(`${BACKEND_BASE_URL}/api/system/shutdown`, {
      method: 'POST',
      headers: { 'X-JarPatch-Token': backendToken }
    });
  } catch (error) {
    appendBackendLog('SHUTDOWN', error.message);
  }
  await Promise.race([
    new Promise((resolve) => backendProcess.once('exit', resolve)),
    new Promise((resolve) => setTimeout(resolve, BACKEND_SHUTDOWN_TIMEOUT_MS))
  ]);
  if (backendProcess.exitCode == null) {
    backendProcess.kill();
  }
}

ipcMain.handle('dialog:openArchive', async () => {
  const result = await dialog.showOpenDialog({
    title: '选择 Jar 或 War 文件',
    properties: ['openFile'],
    filters: [
      { name: 'Java Archive', extensions: ['jar', 'war'] }
    ]
  });
  if (result.canceled || result.filePaths.length === 0) {
    return null;
  }
  return result.filePaths[0];
});

ipcMain.handle('dialog:saveArchive', async (_, defaultName) => {
  const result = await dialog.showSaveDialog({
    title: '导出修改后的包',
    defaultPath: defaultName || 'patched.jar',
    filters: [
      { name: 'Java Archive', extensions: ['jar', 'war'] }
    ]
  });
  if (result.canceled) {
    return null;
  }
  return result.filePath;
});

ipcMain.handle('dialog:pickDirectory', async (_, defaultPath, title) => {
  const result = await dialog.showOpenDialog({
    title: title || '选择目录',
    defaultPath: defaultPath || undefined,
    properties: ['openDirectory']
  });
  if (result.canceled || result.filePaths.length === 0) {
    return null;
  }
  return result.filePaths[0];
});

/**
 * 通过系统保存对话框写出后端已经脱敏的诊断 JSON。
 *
 * @param {Electron.IpcMainInvokeEvent} _ IPC 事件
 * @param {string} defaultName 默认文件名
 * @param {string} content 诊断 JSON 文本
 * @returns {Promise<string|null>} 保存路径；取消时返回 null
 */
async function saveDiagnosticFile(_, defaultName, content) {
  if (typeof content !== 'string' || Buffer.byteLength(content, DIAGNOSTIC_FILE_ENCODING) > DIAGNOSTIC_FILE_MAX_BYTES) {
    throw new Error('诊断信息为空或超过允许大小。');
  }
  const result = await dialog.showSaveDialog({
    title: '导出脱敏诊断信息',
    defaultPath: defaultName || DIAGNOSTIC_FILE_DEFAULT_NAME,
    filters: [
      { name: 'JSON', extensions: ['json'] }
    ]
  });
  if (result.canceled || !result.filePath) {
    return null;
  }
  await fs.promises.writeFile(result.filePath, content, { encoding: DIAGNOSTIC_FILE_ENCODING });
  return result.filePath;
}

ipcMain.handle('dialog:saveDiagnostic', saveDiagnosticFile);
ipcMain.handle('development:getBundledJdkVersion', getBundledJdkVersion);

ipcMain.handle('backend:connection', () => ({
  apiBase: BACKEND_BASE_URL,
  token: backendToken,
  instanceId: backendInstanceId
}));

// 开发能力 IPC 只暴露固定的 JDT LS 与 AI 操作，不允许渲染页面启动任意进程或读取密钥。
developmentServices.registerIpc(ipcMain);

function releaseRuntimeWorkbench() {
  return runtimeWorkbench.shutdown()
    .catch((error) => appendBackendLog('RUNTIME', '运行时工作台远端清理未确认：' + error.message));
}

async function confirmRuntimeSshHost({ host, port, fingerprint }) {
  const address = host + ':' + port;
  const trusted = knownRuntimeHostKeys.get(address);
  if (trusted) {
    if (trusted !== fingerprint) throw new Error('SSH 主机指纹已变化，请向管理员核对服务器');
    return true;
  }
  if (!mainWindow || mainWindow.isDestroyed()) return false;
  const result = await dialog.showMessageBox(mainWindow, {
    type: 'question', title: '确认 SSH 主机指纹',
    message: '请核对服务器 ' + address + ' 的主机指纹',
    detail: fingerprint,
    buttons: ['取消', '已核对，继续连接'], defaultId: 0, cancelId: 0, noLink: true
  });
  if (result.response !== 1) return false;
  knownRuntimeHostKeys.set(address, fingerprint);
  return true;
}

function handleRuntimeIpc(channel, handler) {
  ipcMain.handle(channel, (event, ...args) => {
    if (!mainWindow || event.sender !== mainWindow.webContents || event.senderFrame !== event.sender.mainFrame) {
      throw new Error('运行时工作台仅可从主窗口使用');
    }
    return handler(event, ...args);
  });
}

handleRuntimeIpc('runtime:choosePackage', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: '选择 Arthas 官方完整包 arthas-bin.zip',
    properties: ['openFile'], filters: [{ name: 'Arthas 完整包', extensions: ['zip'] }]
  });
  if (result.canceled || !result.filePaths.length) return null;
  runtimeOfflinePackage = result.filePaths[0];
  return path.basename(runtimeOfflinePackage);
});

handleRuntimeIpc('runtime:listSshHosts', () => listSshHosts());
handleRuntimeIpc('runtime:saveSshHost', (_, data) => saveSshHost(data));
handleRuntimeIpc('runtime:removeSshHost', (_, id) => removeSshHost(id));

handleRuntimeIpc('runtime:run', (event, id, request) => {
  const file = request?.action === 'attach' && request.offline ? runtimeOfflinePackage
    : request?.action === 'applyPatch' ? runtimePatchFile : undefined;
  if (request?.action === 'attach' && request.offline && !file) throw new Error('请先选择官方离线包');
  const data = { ...request, file };
  if (data.action === 'passwordContainers' && !data.password) data.password = savedSshPassword(data);
  return runtimeWorkbench.run(id, data, (kind, value) => {
    if (!event.sender.isDestroyed()) event.sender.send('runtime:update', { id, kind, value });
  });
});
handleRuntimeIpc('runtime:cancel', (_, id) => runtimeWorkbench.cancel(id));

handleRuntimeIpc('runtime:openTunnel', async (_, data) => {
  const url = tunnelUrl(data);
  await shell.openExternal(url);
});

handleRuntimeIpc('runtime:choosePatch', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: '选择用于热替换的 class', properties: ['openFile'],
    filters: [{ name: 'Java class', extensions: ['class'] }]
  });
  if (result.canceled || !result.filePaths.length) return null;
  runtimePatchFile = result.filePaths[0];
  return path.basename(runtimePatchFile);
});

handleRuntimeIpc('runtime:openSuite', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: '打开请求集合', properties: ['openFile'],
    filters: [{ name: '请求集合', extensions: ['json'] }]
  });
  if (result.canceled || !result.filePaths.length) return null;
  const file = await fs.promises.open(result.filePaths[0], 'r');
  try {
    const bytes = Buffer.alloc(MAX_JSON_BYTES + 1);
    const { bytesRead } = await file.read(bytes, 0, bytes.length, 0);
    if (bytesRead > MAX_JSON_BYTES) throw new Error('请求集合超过 512 KiB');
    let suite;
    try {
      suite = JSON.parse(bytes.subarray(0, bytesRead).toString('utf8').replace(/^\uFEFF/, ''));
    } catch {
      throw new Error('请求集合文件不是有效 JSON');
    }
    return normalizeSuite(suite);
  } finally {
    await file.close();
  }
});

handleRuntimeIpc('runtime:saveSuite', async (_, value) => {
  const suite = normalizeSuite(value);
  const result = await dialog.showSaveDialog(mainWindow, {
    title: '保存请求集合', defaultPath: 'jarpatch-requests.json',
    filters: [{ name: '请求集合', extensions: ['json'] }]
  });
  if (result.canceled || !result.filePath) return null;
  await fs.promises.writeFile(result.filePath, JSON.stringify(suite, null, 2), 'utf8');
  return result.filePath;
});

handleRuntimeIpc('runtime:saveProfile', async (_, type) => {
  const profile = runtimeWorkbench.profiles.get(type);
  if (!profile) throw new Error('请先完成性能采集');
  const result = await dialog.showSaveDialog(mainWindow, {
    title: '保存 ' + profile.label, defaultPath: profile.filename,
    filters: [{ name: profile.label, extensions: [type === 'jfr' ? 'jfr' : 'html'] }]
  });
  if (result.canceled || !result.filePath) return null;
  await fs.promises.writeFile(result.filePath, profile.contents);
  return result.filePath;
});

if (!singleInstanceLockAcquired) {
  startupDiagnostics.write('EXISTING_INSTANCE', '已通知现有实例；本次不启动后端');
  app.quit();
} else {
  app.on('second-instance', () => {
    const existingWindow = mainWindow || BrowserWindow.getAllWindows()[0];
    if (!existingWindow) {
      return;
    }
    if (existingWindow.isMinimized()) {
      existingWindow.restore();
    }
    existingWindow.show();
    existingWindow.focus();
  });

  app.whenReady().then(async () => {
    Menu.setApplicationMenu(null);
    try {
      startBackend();
      await waitForBackendReady();
      backendReady = true;
      startupDiagnostics.write('READY', '后端健康检查通过');
      if (smokeCheckRequested) {
        process.stdout.write(`${JSON.stringify({
          product: PRODUCT_NAME,
          status: SMOKE_CHECK_STATUS,
          instanceId: backendInstanceId
        })}\n`);
        await Promise.all([stopBackendGracefully(), developmentServices.shutdown()]);
        applicationQuitAllowed = true;
        app.quit();
        return;
      }
      createWindow();
    } catch (error) {
      startupDiagnostics.write('STARTUP_FAILED', error.stack || error.message);
      if (smokeCheckRequested) {
        process.stderr.write(`${formatBackendFailure(error.message)}\n`);
      } else {
        dialog.showErrorBox('JarPatch Studio 启动失败', formatBackendFailure(error.message));
      }
      backendStopping = true;
      if (backendProcess && backendProcess.exitCode == null) {
        backendProcess.kill();
      }
      applicationQuitAllowed = true;
      app.exit(1);
    }
  });
}

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('before-quit', (event) => {
  if (applicationQuitAllowed) {
    return;
  }
  event.preventDefault();
  if (!backendShutdownPromise) {
    backendShutdownPromise = Promise.all([
      stopBackendGracefully(),
      developmentServices.shutdown(),
      releaseRuntimeWorkbench()
    ]);
  }
  backendShutdownPromise.finally(() => {
    applicationQuitAllowed = true;
    app.quit();
  });
});

app.on('activate', () => {
  if (!smokeCheckRequested && BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
