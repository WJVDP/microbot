"use strict";

const state = {
  csrfToken: null,
  expiresAt: 0,
  plugins: [],
  selectedId: null,
  logs: new Map(),
  lastSequences: new Map(),
  pendingAction: null,
  captureUrl: null,
  polling: false
};

const byId = id => document.getElementById(id);
const elements = {
  sessionTime: byId("session-time"), pluginCount: byId("plugin-count"), pluginList: byId("plugin-list"),
  empty: byId("empty-state"), detail: byId("plugin-detail"), name: byId("plugin-name"), id: byId("plugin-id"),
  lifecycle: byId("lifecycle-pill"), health: byId("health-pill"), actionButton: byId("lifecycle-button"),
  feedback: byId("feedback"), feedbackTitle: byId("feedback-title"), feedbackMessage: byId("feedback-message"),
  runtime: byId("runtime"), heartbeat: byId("heartbeat"), loops: byId("loops"), transitions: byId("transitions"),
  stateTime: byId("state-time"), currentAction: byId("current-action"), phaseList: byId("phase-list"),
  extension: byId("extension-details"), logLevel: byId("log-level"), logCount: byId("log-count"),
  logList: byId("log-list"), capture: byId("capture"), refreshCapture: byId("refresh-capture"),
  framePlaceholder: byId("frame-placeholder"), frame: byId("client-frame"), captureSize: byId("capture-size"),
  captureTime: byId("capture-time"), lastPoll: byId("last-poll")
};

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.method === "POST" && state.csrfToken) headers.set("X-CSRF-Token", state.csrfToken);
  const response = await fetch(`api/${path}`, { credentials: "same-origin", cache: "no-store", ...options, headers });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try { message = (await response.json()).error || message; } catch (_) { /* response is intentionally opaque */ }
    throw new Error(message);
  }
  return response;
}

async function establishSession() {
  const response = await api("session/bootstrap", { method: "POST" });
  const session = await response.json();
  state.csrfToken = session.csrfToken;
  state.expiresAt = session.expiresAt;
  updateSessionClock();
}

async function poll() {
  if (!state.csrfToken || state.polling) return;
  state.polling = true;
  try {
    const response = await api("plugins");
    const payload = await response.json();
    state.plugins = Array.isArray(payload.plugins) ? payload.plugins : [];
    if (!state.plugins.some(plugin => plugin.id === state.selectedId)) state.selectedId = state.plugins[0]?.id || null;
    render();
    if (state.selectedId) await pollLogs(state.selectedId);
    elements.lastPoll.textContent = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  } catch (error) {
    showFeedback("error", "Status refresh failed", error.message);
    if (/session/i.test(error.message)) state.csrfToken = null;
  } finally {
    state.polling = false;
  }
}

async function pollLogs(pluginId) {
  const after = state.lastSequences.get(pluginId) || 0;
  const response = await api(`plugins/${encodeURIComponent(pluginId)}/logs?after=${after}`);
  const payload = await response.json();
  const existing = state.logs.get(pluginId) || [];
  const next = existing.concat(payload.events || []).slice(-500);
  state.logs.set(pluginId, next);
  if (next.length) state.lastSequences.set(pluginId, next[next.length - 1].sequence);
  if (pluginId === state.selectedId) renderLogs();
}

function selectedPlugin() { return state.plugins.find(plugin => plugin.id === state.selectedId) || null; }

function render() {
  elements.pluginCount.textContent = `${state.plugins.length} eligible`;
  renderPluginRail();
  const plugin = selectedPlugin();
  elements.empty.hidden = Boolean(plugin);
  elements.detail.hidden = !plugin;
  if (!plugin) return;

  elements.name.textContent = plugin.name;
  elements.id.textContent = plugin.id;
  setPill(elements.lifecycle, plugin.lifecycle);
  setPill(elements.health, plugin.health);
  elements.runtime.textContent = formatDuration(plugin.runtimeMs);
  elements.heartbeat.textContent = formatDuration(plugin.heartbeatAgeMs);
  elements.loops.textContent = Number(plugin.loopCount || 0).toLocaleString();
  elements.transitions.textContent = Number(plugin.transitionCount || 0).toLocaleString();
  elements.stateTime.textContent = plugin.currentState ? `${formatDuration(plugin.msInCurrentState)} in phase` : "Not active";
  elements.currentAction.textContent = plugin.currentAction || (plugin.active ? "Running" : "Waiting to start");
  renderPhases(plugin);
  renderExtension(plugin.details || {});
  configureAction(plugin);
  if (plugin.lastError && state.pendingAction !== plugin.id) showFeedback("error", "Plugin reported a failure", plugin.lastError);
  renderLogs();
}

function renderPluginRail() {
  elements.pluginList.replaceChildren();
  for (const plugin of state.plugins) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `plugin-row${plugin.id === state.selectedId ? " selected" : ""}`;
    button.setAttribute("aria-current", plugin.id === state.selectedId ? "true" : "false");
    button.addEventListener("click", () => selectPlugin(plugin.id));
    const dot = document.createElement("span"); dot.className = `dot ${statusClass(plugin.health)}`;
    const body = document.createElement("span");
    const name = document.createElement("strong"); name.textContent = plugin.name;
    const meta = document.createElement("small"); meta.textContent = `${plugin.lifecycle} · ${plugin.currentState || "No active phase"}`;
    body.append(name, meta);
    const runtime = document.createElement("span"); runtime.className = "row-runtime"; runtime.textContent = formatDuration(plugin.runtimeMs);
    button.append(dot, body, runtime);
    elements.pluginList.append(button);
  }
}

function selectPlugin(pluginId) {
  state.selectedId = pluginId;
  render();
  pollLogs(pluginId).catch(error => showFeedback("error", "Logs unavailable", error.message));
}

function setPill(element, value) {
  element.className = `pill ${statusClass(value)}`;
  element.textContent = value || "UNKNOWN";
}

function configureAction(plugin) {
  const stopping = plugin.lifecycle === "STOPPING";
  const starting = plugin.lifecycle === "STARTING";
  const shouldStop = plugin.active || stopping;
  elements.actionButton.textContent = stopping ? "Stopping…" : starting ? "Starting…" : shouldStop ? "Stop plugin" : plugin.lifecycle === "FAILED" ? "Retry start" : "Start plugin";
  elements.actionButton.className = `button ${shouldStop ? "danger" : "primary"}`;
  elements.actionButton.disabled = Boolean(state.pendingAction) || starting || stopping;
  elements.actionButton.onclick = () => lifecycleAction(plugin, shouldStop ? "stop" : "start");
}

async function lifecycleAction(plugin, action) {
  state.pendingAction = plugin.id;
  configureAction({ ...plugin, lifecycle: action === "start" ? "STARTING" : "STOPPING" });
  showFeedback("pending", `${action === "start" ? "Starting" : "Stopping"} ${plugin.name}`, "The lifecycle action is serialized on the local client.");
  try {
    const response = await api(`plugins/${encodeURIComponent(plugin.id)}/${action}`, { method: "POST" });
    const updated = await response.json();
    state.plugins = state.plugins.map(item => item.id === updated.id ? updated : item);
    showFeedback("success", `${plugin.name} ${action === "start" ? "started" : "stopped"}`, `Actual lifecycle is ${updated.lifecycle}.`);
  } catch (error) {
    showFeedback("error", `Could not ${action} ${plugin.name}`, error.message);
  } finally {
    state.pendingAction = null;
    render();
    poll();
  }
}

function renderPhases(plugin) {
  elements.phaseList.replaceChildren();
  const phases = Array.isArray(plugin.phases) ? plugin.phases : [];
  if (!phases.length) {
    const empty = document.createElement("p"); empty.className = "muted phase-empty"; empty.textContent = "No StateMachineScript snapshot is available.";
    elements.phaseList.append(empty); return;
  }
  const currentIndex = phases.indexOf(plugin.currentState);
  phases.forEach((phase, index) => {
    const item = document.createElement("div");
    item.className = `phase${index < currentIndex ? " done" : index === currentIndex ? " current" : ""}`;
    item.textContent = phase.replaceAll("_", " ");
    elements.phaseList.append(item);
  });
}

function renderExtension(details) {
  elements.extension.replaceChildren();
  for (const [key, value] of Object.entries(details).slice(0, 20)) {
    const dt = document.createElement("dt"); dt.textContent = key;
    const dd = document.createElement("dd"); dd.textContent = value ?? "—";
    elements.extension.append(dt, dd);
  }
}

function renderLogs() {
  const plugin = selectedPlugin();
  if (!plugin) return;
  const all = state.logs.get(plugin.id) || [];
  const level = elements.logLevel.value;
  const logs = all.filter(event => level === "ALL" || event.level === level);
  elements.logCount.textContent = `${all.length} / 500`;
  elements.logList.replaceChildren();
  if (!logs.length) {
    const empty = document.createElement("p"); empty.className = "log-empty"; empty.textContent = "No dashboard log events match this filter.";
    elements.logList.append(empty); return;
  }
  for (const event of logs) {
    const row = document.createElement("div"); row.className = "log-line";
    const time = document.createElement("time"); time.textContent = new Date(event.timestamp).toLocaleTimeString([], { hour12: false });
    const levelText = document.createElement("strong"); levelText.className = statusClass(event.level); levelText.textContent = event.level;
    const message = document.createElement("span"); message.textContent = event.message || "";
    if (event.exception) { const exception = document.createElement("small"); exception.textContent = event.exception; message.append(document.createElement("br"), exception); }
    row.append(time, levelText, message); elements.logList.append(row);
  }
  elements.logList.scrollTop = elements.logList.scrollHeight;
}

async function captureFrame() {
  elements.capture.disabled = true; elements.refreshCapture.disabled = true; elements.capture.textContent = "Capturing…";
  try {
    const response = await api("screenshot");
    const blob = await response.blob();
    if (state.captureUrl) URL.revokeObjectURL(state.captureUrl);
    state.captureUrl = URL.createObjectURL(blob);
    elements.frame.src = state.captureUrl; elements.frame.hidden = false; elements.framePlaceholder.hidden = true;
    elements.captureSize.textContent = `${response.headers.get("X-Image-Width") || "?"} × ${response.headers.get("X-Image-Height") || "?"} PNG`;
    elements.captureTime.textContent = `Captured ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}`;
  } catch (error) {
    showFeedback("error", "Frame capture failed", error.message);
  } finally {
    elements.capture.disabled = false; elements.refreshCapture.disabled = false; elements.capture.textContent = "Capture";
  }
}

function showFeedback(type, title, message) {
  elements.feedback.hidden = false;
  elements.feedback.className = `feedback ${type}`;
  elements.feedback.querySelector(".dot").className = `dot ${type === "error" ? "failed" : type === "pending" ? "starting" : "healthy"}`;
  elements.feedbackTitle.textContent = title;
  elements.feedbackMessage.textContent = message;
}

function updateSessionClock() {
  const remaining = Math.max(0, state.expiresAt - Date.now());
  elements.sessionTime.textContent = remaining ? `Browser session · ${formatDuration(remaining)} left` : "Browser session expired";
}

function statusClass(value) { return String(value || "unknown").toLowerCase().replace(/[^a-z]/g, ""); }

function formatDuration(ms) {
  if (ms === null || ms === undefined) return "—";
  if (ms < 1000) return `${Math.max(0, Math.round(ms))} ms`;
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ${seconds % 60}s`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${String(minutes % 60).padStart(2, "0")}m`;
}

elements.logLevel.addEventListener("change", renderLogs);
elements.capture.addEventListener("click", captureFrame);
elements.refreshCapture.addEventListener("click", captureFrame);
window.addEventListener("beforeunload", () => { if (state.captureUrl) URL.revokeObjectURL(state.captureUrl); });
window.addEventListener("keydown", event => {
  if (!state.plugins.length || !["ArrowUp", "ArrowDown"].includes(event.key)) return;
  const index = Math.max(0, state.plugins.findIndex(plugin => plugin.id === state.selectedId));
  const delta = event.key === "ArrowDown" ? 1 : -1;
  selectPlugin(state.plugins[(index + delta + state.plugins.length) % state.plugins.length].id);
});

(async () => {
  try {
    await establishSession();
    showFeedback("success", "Dashboard session ready", "Connected to this local RuneLite client. Status refreshes every 1.5 seconds.");
    await poll();
    setInterval(poll, 1500);
    setInterval(updateSessionClock, 1000);
  } catch (error) {
    elements.sessionTime.textContent = "No authenticated browser session";
    showFeedback("error", "Dashboard session unavailable", error.message);
  }
})();
