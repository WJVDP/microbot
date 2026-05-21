import { app, BrowserWindow, ipcMain } from "electron";
import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const electronDir = dirname(fileURLToPath(import.meta.url));
const tokenFile = process.env.MICROBOT_TOKEN_FILE ?? join(homedir(), ".runelite", ".agent-token");

function bridgeBaseUrl(): string {
  if (process.env.MICROBOT_BRIDGE_URL) {
    return process.env.MICROBOT_BRIDGE_URL.replace(/\/+$/, "");
  }

  const host = process.env.MICROBOT_HOST ?? "127.0.0.1";
  const port = process.env.MICROBOT_PORT ?? "8081";
  return `http://${host}:${port}`;
}

function readBridgeToken(): string {
  const envToken = process.env.MICROBOT_TOKEN?.trim();
  if (envToken) {
    return envToken;
  }

  if (!existsSync(tokenFile)) {
    return "";
  }

  return readFileSync(tokenFile, "utf8").trim();
}

function createWindow(): void {
  const window = new BrowserWindow({
    width: 1180,
    height: 760,
    minWidth: 860,
    minHeight: 560,
    title: "Microbot Shell",
    backgroundColor: "#f7f8fa",
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: join(electronDir, "preload.js")
    }
  });

  const devServer = process.env.MICROBOT_UI_DEV_SERVER;
  if (devServer) {
    void window.loadURL(devServer);
    return;
  }

  void window.loadFile(join(electronDir, "..", "dist", "index.html"));
}

ipcMain.handle("bridge-context", () => ({
  baseUrl: bridgeBaseUrl(),
  token: readBridgeToken(),
  tokenFile,
  tokenSource: process.env.MICROBOT_TOKEN?.trim() ? "MICROBOT_TOKEN" : tokenFile
}));

app.whenReady().then(() => {
  createWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
