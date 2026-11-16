const { contextBridge } = require("electron");
contextBridge.exposeInMainWorld("jvc", Object.freeze({ platform: process.platform, appName: "JVC", version: "1.0.0" }));
