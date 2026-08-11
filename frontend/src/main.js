const { app, BrowserWindow, Menu, dialog, ipcMain } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');
const fs = require('fs');
const readline = require('readline');

const BACKEND_PORT = 18765;
const BACKEND_HOST = '127.0.0.1';
const BACKEND_BASE_URL = `http://${BACKEND_HOST}:${BACKEND_PORT}`;
const BACKEND_JAR_NAME = 'jarpatch-studio-backend.jar';
const BACKEND_READY_TIMEOUT_MS = 20000;
const BACKEND_POLL_INTERVAL_MS = 250;
const BACKEND_SHUTDOWN_TIMEOUT_MS = 5000;
const BACKEND_LOG_LIMIT = 80;
const PRODUCT_NAME = 'JarPatch Studio';
const ENV_AUTH_TOKEN = 'JARPATCH_AUTH_TOKEN';
const ENV_INSTANCE_ID = 'JARPATCH_INSTANCE_ID';
let backendProcess = null;
let backendStopping = false;
let applicationQuitAllowed = false;
let backendShutdownPromise = null;
let backendToken = null;
let backendInstanceId = null;
const backendLogs = [];

function createWindow() {
  const window = new BrowserWindow({
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

  window.loadFile(path.join(__dirname, 'index.html'));
}

function startBackend() {
  const backendJar = resolveBackendJarPath();
  if (!fs.existsSync(backendJar)) {
    throw new Error(`未找到后端程序：${backendJar}`);
  }
  backendToken = crypto.randomBytes(32).toString('hex');
  backendInstanceId = crypto.randomUUID();
  const javaExecutable = resolveJavaExecutable();
  backendProcess = spawn(javaExecutable, ['-jar', backendJar], {
    cwd: path.dirname(backendJar),
    env: {
      ...process.env,
      [ENV_AUTH_TOKEN]: backendToken,
      [ENV_INSTANCE_ID]: backendInstanceId
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
      throw new Error(formatBackendFailure('后端进程在就绪前退出'));
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
  throw new Error(formatBackendFailure('等待后端健康检查超时，端口可能被其他进程占用'));
}

function formatBackendFailure(message) {
  const details = backendLogs.slice(-20).join('\n');
  return details ? `${message}\n\n${details}` : message;
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

ipcMain.handle('backend:connection', () => ({
  apiBase: BACKEND_BASE_URL,
  token: backendToken,
  instanceId: backendInstanceId
}));

app.whenReady().then(async () => {
  Menu.setApplicationMenu(null);
  try {
    startBackend();
    await waitForBackendReady();
    createWindow();
  } catch (error) {
    dialog.showErrorBox('JarPatch Studio 启动失败', error.message);
    backendStopping = true;
    if (backendProcess && backendProcess.exitCode == null) {
      backendProcess.kill();
    }
    applicationQuitAllowed = true;
    app.quit();
  }
});

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
    backendShutdownPromise = stopBackendGracefully();
  }
  backendShutdownPromise.finally(() => {
    applicationQuitAllowed = true;
    app.quit();
  });
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
