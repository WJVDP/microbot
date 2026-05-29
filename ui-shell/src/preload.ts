import { contextBridge, ipcRenderer } from "electron";

export interface BridgeLaunchContext {
  baseUrl: string;
  token: string;
  tokenFile: string;
  tokenSource: string;
}

contextBridge.exposeInMainWorld("microbotShell", {
  getBridgeContext: (): Promise<BridgeLaunchContext> => ipcRenderer.invoke("bridge-context")
});

declare global {
  interface Window {
    microbotShell?: {
      getBridgeContext: () => Promise<BridgeLaunchContext>;
    };
  }
}
