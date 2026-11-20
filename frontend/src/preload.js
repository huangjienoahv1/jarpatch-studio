const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('jarPatch', {
  openArchive: () => ipcRenderer.invoke('dialog:openArchive'),
  saveArchive: (defaultName) => ipcRenderer.invoke('dialog:saveArchive', defaultName),
  saveDiagnostic: (defaultName, content) => ipcRenderer.invoke('dialog:saveDiagnostic', defaultName, content),
  pickDirectory: (defaultPath, title) => ipcRenderer.invoke('dialog:pickDirectory', defaultPath, title),
  getBackendConnection: () => ipcRenderer.invoke('backend:connection'),
  getDevelopmentSettings: () => ipcRenderer.invoke('development:getSettings'),
  getBundledJdkVersion: () => ipcRenderer.invoke('development:getBundledJdkVersion'),
  saveDevelopmentSettings: (settings) => ipcRenderer.invoke('development:saveSettings', settings),
  startLanguageServer: (request) => ipcRenderer.invoke('development:startLanguageServer', request),
  sendLanguageServerMessage: (message) => ipcRenderer.invoke('development:sendLanguageServerMessage', message),
  stopLanguageServer: () => ipcRenderer.invoke('development:stopLanguageServer'),
  aiAssist: (request) => ipcRenderer.invoke('development:aiAssist', request),
  chooseArthasPackage: () => ipcRenderer.invoke('runtime:choosePackage'),
  listSshHosts: () => ipcRenderer.invoke('runtime:listSshHosts'),
  saveSshHost: (data) => ipcRenderer.invoke('runtime:saveSshHost', data),
  removeSshHost: (id) => ipcRenderer.invoke('runtime:removeSshHost', id),
  runRuntimeOperation: (id, data) => ipcRenderer.invoke('runtime:run', id, data),
  cancelRuntimeOperation: (id) => ipcRenderer.invoke('runtime:cancel', id),
  openRuntimeTunnel: (data) => ipcRenderer.invoke('runtime:openTunnel', data),
  chooseRuntimePatch: () => ipcRenderer.invoke('runtime:choosePatch'),
  openRuntimeSuite: () => ipcRenderer.invoke('runtime:openSuite'),
  saveRuntimeSuite: (suite) => ipcRenderer.invoke('runtime:saveSuite', suite),
  saveRuntimeProfile: (type) => ipcRenderer.invoke('runtime:saveProfile', type),
  onRuntimeUpdate: (listener) => {
    ipcRenderer.on('runtime:update', (_, message) => listener(message));
  },
  onLanguageServerMessage: (listener) => {
    const handler = (_, message) => listener(message);
    ipcRenderer.on('development:languageServerMessage', handler);
    return () => ipcRenderer.removeListener('development:languageServerMessage', handler);
  }
});
