const { app, BrowserWindow, Menu, dialog, ipcMain } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const fs = require('fs');

const BACKEND_PORT = 18765;
const BACKEND_JAR_NAME = 'jarpatch-studio-backend.jar';
let backendProcess = null;

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
  const backendJar = path.resolve(__dirname, '..', '..', 'backend', 'target', BACKEND_JAR_NAME);
  if (!fs.existsSync(backendJar)) {
    return;
  }
  backendProcess = spawn('java', ['-jar', backendJar], {
    cwd: path.dirname(backendJar),
    stdio: 'ignore',
    windowsHide: true
  });
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

ipcMain.handle('dialog:pickDirectory', async (_, defaultPath) => {
  const result = await dialog.showOpenDialog({
    title: '选择 JDK 安装目录',
    defaultPath: defaultPath || undefined,
    properties: ['openDirectory']
  });
  if (result.canceled || result.filePaths.length === 0) {
    return null;
  }
  return result.filePaths[0];
});

app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  startBackend();
  createWindow();
});

app.on('window-all-closed', () => {
  if (backendProcess) {
    backendProcess.kill();
  }
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
