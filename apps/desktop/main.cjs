const { app, BrowserWindow, session, shell } = require("electron");
const path = require("node:path");
const { pathToFileURL } = require("node:url");
const CHAT_ORIGIN = "https://chat.jigar.au";
function trusted(url) {
  try { const u = new URL(url); return u.origin === CHAT_ORIGIN || u.origin === "http://localhost:5173" || u.protocol === "file:"; }
  catch { return false; }
}
function createWindow() {
  const win = new BrowserWindow({
    width: 1440, height: 900, minWidth: 850, minHeight: 600,
    backgroundColor: "#111113", title: "JVC", autoHideMenuBar: true,
    webPreferences: { preload: path.join(__dirname, "preload.cjs"), contextIsolation: true, nodeIntegration: false, sandbox: true, webviewTag: false }
  });
  const entry = app.isPackaged ? pathToFileURL(path.resolve(__dirname, "../web/dist/index.html")).href : "http://localhost:5173";
  win.loadURL(entry);
  win.webContents.setWindowOpenHandler(({url}) => { if (trusted(url)) return {action:"allow"}; shell.openExternal(url); return {action:"deny"}; });
  win.webContents.on("will-navigate", (event,url) => { if (!trusted(url)) { event.preventDefault(); shell.openExternal(url); } });
}
app.whenReady().then(() => {
  session.defaultSession.setPermissionRequestHandler((wc, permission, callback, details) => {
    const origin = details.requestingUrl || wc.getURL();
    const allowed = origin.startsWith(CHAT_ORIGIN) && ["media","notifications","clipboard-read","clipboard-sanitized-write","fullscreen"].includes(permission);
    callback(allowed);
  });
  createWindow();
  app.on("activate", () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});
app.on("window-all-closed", () => { if (process.platform !== "darwin") app.quit(); });
