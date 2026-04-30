const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("adventura", {
  getRuntimeInfo: () => ipcRenderer.invoke("runtime:getInfo"),
  loadSettings: () => ipcRenderer.invoke("settings:load"),
  saveSettings: (settings) => ipcRenderer.invoke("settings:save", settings),
  launch: (payload) => ipcRenderer.invoke("launcher:launch", payload),
  preflight: (payload) => ipcRenderer.invoke("launcher:preflight", payload),
  stop: (target) => ipcRenderer.invoke("launcher:stop", target),
  openWorkspace: () => ipcRenderer.invoke("workspace:open"),
  onLog: (callback) => {
    const listener = (_event, entry) => callback(entry);
    ipcRenderer.on("launcher:log", listener);
    return () => ipcRenderer.removeListener("launcher:log", listener);
  }
});
