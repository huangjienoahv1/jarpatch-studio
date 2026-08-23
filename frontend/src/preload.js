const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('jarPatch', {
  openArchive: () => ipcRenderer.invoke('dialog:openArchive'),
  saveArchive: (defaultName) => ipcRenderer.invoke('dialog:saveArchive', defaultName),
  saveDiagnostic: (defaultName, content) => ipcRenderer.invoke('dialog:saveDiagnostic', defaultName, content),
  pickDirectory: (defaultPath, title) => ipcRenderer.invoke('dialog:pickDirectory', defaultPath, title),
  getBackendConnection: () => ipcRenderer.invoke('backend:connection')
});
