const { app, BrowserWindow, Menu, dialog, ipcMain } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');
const fs = require('fs');
const readline = require('readline');
const { DevelopmentServices } = require('./development-services');

const BACKEND_PORT = 18765;
const BACKEND_HOST = '127.0.0.1';
const BACKEND_BASE_URL = `http://${BACKEND_HOST}:${BACKEND_PORT}`;
const BACKEND_JAR_NAME = 'jarpatch-studio-backend.jar';
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
const backendLogs = [];
const developmentServices = new DevelopmentServices();

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
  backendInstanceId = crypto.randomUUID();
  backendLogPath = resolveBackendLogPath();
  const javaExecutable = resolveJavaExecutable();
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
    const bundledJava = path.join(process.resourcesPath, 'runtime', 'bin', javaName);
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

function captureBackendOutput(stream, channel) {
  if (!stream) {
    return;
  }
  const reader = readline.createInterface({ input: stream });
  reader.on('line', (line) => appendBackendLog(channel, line));
}

function appendBackendLog(channel, message) {
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
  const identity = `实例 ID：${backendInstanceId || '未生成'}\n后端日志：${backendLogPath || '未生成'}`;
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

ipcMain.handle('backend:connection', () => ({
  apiBase: BACKEND_BASE_URL,
  token: backendToken,
  instanceId: backendInstanceId
}));

// 开发能力 IPC 只暴露固定的 JDT LS 与 AI 操作，不允许渲染页面启动任意进程或读取密钥。
developmentServices.registerIpc(ipcMain);

if (!singleInstanceLockAcquired) {
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
      app.quit();
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
      developmentServices.shutdown()
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
