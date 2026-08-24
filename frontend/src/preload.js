const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('jarPatch', {
  openArchive: () => ipcRenderer.invoke('dialog:openArchive'),
  saveArchive: (defaultName) => ipcRenderer.invoke('dialog:saveArchive', defaultName),
  saveDiagnostic: (defaultName, content) => ipcRenderer.invoke('dialog:saveDiagnostic', defaultName, content),
  pickDirectory: (defaultPath, title) => ipcRenderer.invoke('dialog:pickDirectory', defaultPath, title),
  getBackendConnection: () => ipcRenderer.invoke('backend:connection'),
  getDevelopmentSettings: () => ipcRenderer.invoke('development:getSettings'),
  saveDevelopmentSettings: (settings) => ipcRenderer.invoke('development:saveSettings', settings),
  startLanguageServer: (request) => ipcRenderer.invoke('development:startLanguageServer', request),
  sendLanguageServerMessage: (message) => ipcRenderer.invoke('development:sendLanguageServerMessage', message),
  stopLanguageServer: () => ipcRenderer.invoke('development:stopLanguageServer'),
  aiAssist: (request) => ipcRenderer.invoke('development:aiAssist', request),
  onLanguageServerMessage: (listener) => {
    const handler = (_, message) => listener(message);
    ipcRenderer.on('development:languageServerMessage', handler);
    return () => ipcRenderer.removeListener('development:languageServerMessage', handler);
  }
});
