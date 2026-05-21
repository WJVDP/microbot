import "./styles.css";
import type {
  BridgeError,
  BridgePlugin,
  BridgePluginArtifact,
  BridgePluginArtifacts,
  BridgePluginCommandResponse,
  BridgePluginList,
  BridgeRuntimeHealth,
  BridgeStatus
} from "../../docs/bridge-api-v1.types";
import type { BridgeLaunchContext } from "./preload";

interface ShellState {
  context: BridgeLaunchContext;
  loading: boolean;
  error: string;
  status: BridgeStatus | null;
  health: BridgeRuntimeHealth | null;
  plugins: BridgePlugin[];
  artifacts: BridgePluginArtifact[];
  commandBusy: Record<string, boolean>;
  updatedAt: string | null;
}

const fallbackContext: BridgeLaunchContext = {
  baseUrl: localStorage.getItem("microbot.bridge.baseUrl") ?? "http://127.0.0.1:8081",
  token: localStorage.getItem("microbot.bridge.token") ?? "",
  tokenFile: "~/.runelite/.agent-token",
  tokenSource: "browser local storage"
};

const state: ShellState = {
  context: fallbackContext,
  loading: true,
  error: "",
  status: null,
  health: null,
  plugins: [],
  artifacts: [],
  commandBusy: {},
  updatedAt: null
};

const appElement = document.querySelector("#app");

if (!(appElement instanceof HTMLDivElement)) {
  throw new Error("Missing #app mount element");
}

const mountElement = appElement;

function bridgeUrl(path: string): string {
  return `${state.context.baseUrl.replace(/\/+$/, "")}/bridge/v1${path}`;
}

function pluginKey(plugin: BridgePlugin): string {
  return plugin.id || plugin.className;
}

function h(value: unknown): string {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function classifyArtifact(artifact: BridgePluginArtifact): string {
  if (artifact.loadable && artifact.installed) {
    return "ready";
  }

  if (artifact.loadable) {
    return "available";
  }

  return "blocked";
}

function artifactSummary(artifact: BridgePluginArtifact): string {
  if (artifact.errors.length > 0) {
    return artifact.errors.join(", ");
  }

  if (artifact.loadable) {
    return artifact.installed ? "Installed and loadable" : "Available to install";
  }

  return "Blocked by runtime validation";
}

async function readError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as Partial<BridgeError>;
    if (body.error) {
      return body.error;
    }
  } catch {
    // Fall through to status text.
  }

  return `${response.status} ${response.statusText}`.trim();
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(bridgeUrl(path), {
    ...init,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      "X-Agent-Token": state.context.token,
      ...init?.headers
    }
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return (await response.json()) as T;
}

async function loadContext(): Promise<void> {
  if (window.microbotShell) {
    state.context = await window.microbotShell.getBridgeContext();
    return;
  }

  state.context = fallbackContext;
}

async function refresh(): Promise<void> {
  state.loading = true;
  state.error = "";
  render();

  try {
    const [status, health, pluginList, artifactList] = await Promise.all([
      requestJson<BridgeStatus>("/status"),
      requestJson<BridgeRuntimeHealth>("/runtime-health"),
      requestJson<BridgePluginList>("/plugins"),
      requestJson<BridgePluginArtifacts>("/plugin-artifacts")
    ]);

    state.status = status;
    state.health = health;
    state.plugins = pluginList.plugins;
    state.artifacts = artifactList.artifacts;
    state.updatedAt = new Date().toLocaleTimeString();
  } catch (error) {
    state.error = error instanceof Error ? error.message : String(error);
  } finally {
    state.loading = false;
    render();
  }
}

async function runPluginCommand(plugin: BridgePlugin, action: "start" | "stop"): Promise<void> {
  const key = pluginKey(plugin);
  state.commandBusy[key] = true;
  state.error = "";
  render();

  try {
    const updated = await requestJson<BridgePluginCommandResponse>(
      `/plugins/${encodeURIComponent(key)}/${action}`,
      { method: "POST", body: "{}" }
    );

    state.plugins = state.plugins.map((item) => (pluginKey(item) === key ? updated : item));
    state.updatedAt = new Date().toLocaleTimeString();
  } catch (error) {
    state.error = error instanceof Error ? error.message : String(error);
  } finally {
    state.commandBusy[key] = false;
    render();
  }
}

function statusPill(label: string, value: boolean | null | undefined): string {
  const className = value ? "ok" : value === false ? "bad" : "muted";
  const text = value ? "Online" : value === false ? "Offline" : "Unknown";
  return `<span class="pill ${className}"><span>${h(label)}</span><strong>${h(text)}</strong></span>`;
}

function renderToolbar(): string {
  return `
    <header class="toolbar">
      <div>
        <h1>Microbot Shell</h1>
        <p>${h(state.context.baseUrl)} · token: ${h(state.context.token ? state.context.tokenSource : "missing")}</p>
      </div>
      <div class="toolbar-actions">
        <button id="refresh" ${state.loading ? "disabled" : ""}>Refresh</button>
      </div>
    </header>
  `;
}

function renderStatus(): string {
  return `
    <section class="status-grid" aria-label="Bridge status">
      <div class="metric">
        <span>Bridge</span>
        <strong>${h(state.status?.bridgeVersion ?? "V1")}</strong>
      </div>
      <div class="metric">
        <span>Plugins</span>
        <strong>${h(state.status?.pluginCount ?? state.plugins.length)}</strong>
      </div>
      <div class="metric">
        <span>Artifacts</span>
        <strong>${h(state.health?.artifactCount ?? state.artifacts.length)}</strong>
      </div>
      <div class="metric">
        <span>Updated</span>
        <strong>${h(state.updatedAt ?? "Not loaded")}</strong>
      </div>
      ${statusPill("Plugin manager", state.status?.pluginManagerAvailable ?? state.health?.pluginManagerAvailable)}
      ${statusPill("Artifact status", state.health?.artifactStatusAvailable)}
    </section>
  `;
}

function renderError(): string {
  if (!state.error && state.context.token) {
    return "";
  }

  const message = state.error || `No bridge token found. Start Electron with MICROBOT_TOKEN or create ${state.context.tokenFile}.`;
  return `<div class="notice" role="alert">${h(message)}</div>`;
}

function renderPlugins(): string {
  const rows = state.plugins
    .map((plugin, index) => {
      const key = pluginKey(plugin);
      const busy = state.commandBusy[key] ?? false;
      const action = plugin.active ? "stop" : "start";
      return `
        <tr>
          <td>
            <strong>${h(plugin.displayName)}</strong>
            <span>${h(plugin.className)}</span>
          </td>
          <td><span class="tag ${plugin.active ? "ok" : "muted"}">${plugin.active ? "Active" : "Stopped"}</span></td>
          <td><span class="tag ${plugin.enabled ? "ok" : "muted"}">${plugin.enabled ? "Enabled" : "Disabled"}</span></td>
          <td>${h(plugin.external ? "External" : "Core")}</td>
          <td class="align-right">
            <button class="plugin-command" data-plugin-index="${index}" data-action="${action}" ${busy ? "disabled" : ""}>${busy ? "Working" : action === "stop" ? "Stop" : "Start"}</button>
          </td>
        </tr>
      `;
    })
    .join("");

  return `
    <section class="panel table-panel">
      <div class="panel-heading">
        <h2>Loaded Plugins</h2>
        <span>${h(state.plugins.length)} loaded</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Plugin</th>
              <th>Runtime</th>
              <th>Config</th>
              <th>Source</th>
              <th></th>
            </tr>
          </thead>
          <tbody>${rows || `<tr><td colspan="5" class="empty">No plugins returned by Bridge V1.</td></tr>`}</tbody>
        </table>
      </div>
    </section>
  `;
}

function renderArtifacts(): string {
  const rows = state.artifacts
    .map((artifact) => `
      <tr>
        <td>
          <strong>${h(artifact.displayName ?? artifact.id)}</strong>
          <span>${h(artifact.id)}</span>
        </td>
        <td><span class="tag ${classifyArtifact(artifact)}">${h(classifyArtifact(artifact))}</span></td>
        <td>${h(artifact.source)}</td>
        <td>${h(artifact.version ?? "n/a")}</td>
        <td>${h(artifactSummary(artifact))}</td>
      </tr>
    `)
    .join("");

  return `
    <section class="panel table-panel">
      <div class="panel-heading">
        <h2>Plugin Artifacts</h2>
        <span>${h(state.artifacts.filter((artifact) => !artifact.loadable).length)} blocked</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Artifact</th>
              <th>Status</th>
              <th>Source</th>
              <th>Version</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>${rows || `<tr><td colspan="5" class="empty">No artifacts returned by Bridge V1.</td></tr>`}</tbody>
        </table>
      </div>
    </section>
  `;
}

function render(): void {
  mountElement.innerHTML = `
    ${renderToolbar()}
    <main>
      ${renderStatus()}
      ${renderError()}
      ${state.loading ? `<div class="loading">Loading Bridge V1 status...</div>` : ""}
      <div class="content-grid">
        ${renderPlugins()}
        ${renderArtifacts()}
      </div>
    </main>
  `;

  document.querySelector("#refresh")?.addEventListener("click", () => {
    void refresh();
  });

  document.querySelectorAll<HTMLButtonElement>(".plugin-command").forEach((button) => {
    button.addEventListener("click", () => {
      const pluginIndex = Number(button.dataset.pluginIndex);
      const plugin = Number.isInteger(pluginIndex) ? state.plugins[pluginIndex] : undefined;
      const action = button.dataset.action === "stop" ? "stop" : "start";
      if (plugin) {
        void runPluginCommand(plugin, action);
      }
    });
  });
}

async function start(): Promise<void> {
  await loadContext();
  if (!window.microbotShell) {
    localStorage.setItem("microbot.bridge.baseUrl", state.context.baseUrl);
    if (state.context.token) {
      localStorage.setItem("microbot.bridge.token", state.context.token);
    }
  }
  await refresh();
}

void start();
