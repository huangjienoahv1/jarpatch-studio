const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('jarPatch', {
  openArchive: () => ipcRenderer.invoke('dialog:openArchive'),
  saveArchive: (defaultName) => ipcRenderer.invoke('dialog:saveArchive', defaultName),
  pickDirectory: (defaultPath, title) => ipcRenderer.invoke('dialog:pickDirectory', defaultPath, title),
  getBackendConnection: () => ipcRenderer.invoke('backend:connection')
});
