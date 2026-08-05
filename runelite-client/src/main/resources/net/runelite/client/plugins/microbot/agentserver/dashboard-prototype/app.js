/*
 * THROWAWAY PROTOTYPE — GitHub issue #27.
 * Three variants of the proposed local control center, switchable via ?variant=.
 * All data and actions are browser-memory simulations; there is no backend.
 */

const VARIANTS = {
  A: "Operations desk",
  B: "Signal board",
  C: "Field monitor"
};

const logSets = {
  woodcutter: [
    { sequence: 245, time: "14:32:11", level: "INFO", message: "Banking complete — 27 yew logs deposited" },
    { sequence: 246, time: "14:32:13", level: "DEBUG", message: "Transition BANKING → WALKING_TO_TREES" },
    { sequence: 247, time: "14:32:20", level: "INFO", message: "Walking to Yew tree at Edgeville" },
    { sequence: 248, time: "14:32:28", level: "DEBUG", message: "Transition WALKING_TO_TREES → CHOPPING" },
    { sequence: 249, time: "14:32:31", level: "INFO", message: "Interacting with Yew tree" }
  ],
  miner: [
    { sequence: 78, time: "14:31:42", level: "INFO", message: "Sack contains 54 pay-dirt" },
    { sequence: 79, time: "14:31:51", level: "DEBUG", message: "Searching for available ore vein" },
    { sequence: 80, time: "14:31:58", level: "WARN", message: "Heartbeat delayed beyond 10 second threshold" },
    { sequence: 81, time: "14:32:08", level: "WARN", message: "No script heartbeat for 21 seconds" }
  ],
  birdhouse: [
    { sequence: 13, time: "13:18:02", level: "INFO", message: "Script stopped cleanly" },
    { sequence: 14, time: "13:18:02", level: "DEBUG", message: "Status provider unregistered" }
  ],
  essence: [
    { sequence: 31, time: "14:26:44", level: "INFO", message: "Starting Plan Essence Runner" },
    { sequence: 32, time: "14:26:45", level: "ERROR", message: "Start failed: required rune pouch was not found" },
    { sequence: 33, time: "14:26:45", level: "INFO", message: "Plugin returned to a stopped-safe state" }
  ]
};

const plugins = [
  {
    id: "plan-woodcutter",
    short: "WC",
    name: "Plan Woodcutter",
    lifecycle: "RUNNING",
    health: "HEALTHY",
    startedAt: Date.now() - 2 * 60 * 60 * 1000 - 14 * 60 * 1000 - 33 * 1000,
    runtimeMs: 8073000,
    heartbeatAgeMs: 420,
    loopCount: 1842,
    currentState: "CHOPPING",
    stateEnteredAt: Date.now() - 44000,
    msInCurrentState: 44000,
    transitionCount: 119,
    currentAction: "Interacting with Yew tree",
    phases: ["CHECK_REQUIREMENTS", "WALKING_TO_TREES", "CHOPPING", "BANKING"],
    phaseIndex: 2,
    logs: logSets.woodcutter,
    lastError: null
  },
  {
    id: "plan-motherlode-miner",
    short: "ML",
    name: "Plan Motherlode Miner",
    lifecycle: "RUNNING",
    health: "STALLED",
    startedAt: Date.now() - 41 * 60 * 1000 - 5 * 1000,
    runtimeMs: 2465000,
    heartbeatAgeMs: 21800,
    loopCount: 633,
    currentState: "FINDING_VEIN",
    stateEnteredAt: Date.now() - 29000,
    msInCurrentState: 29000,
    transitionCount: 48,
    currentAction: "Searching for available ore vein",
    phases: ["CHECK_REQUIREMENTS", "MINING", "DEPOSITING", "COLLECTING"],
    phaseIndex: 1,
    logs: logSets.miner,
    lastError: null
  },
  {
    id: "plan-bird-house-runner",
    short: "BH",
    name: "Plan Bird House Runner",
    lifecycle: "STOPPED",
    health: "UNKNOWN",
    startedAt: null,
    runtimeMs: null,
    heartbeatAgeMs: null,
    loopCount: 0,
    currentState: null,
    stateEnteredAt: null,
    msInCurrentState: null,
    transitionCount: 0,
    currentAction: "Waiting to start",
    phases: ["CHECK_REQUIREMENTS", "TRAVELLING", "SERVICING", "RETURNING"],
    phaseIndex: -1,
    logs: logSets.birdhouse,
    lastError: null
  },
  {
    id: "plan-essence-runner",
    short: "ER",
    name: "Plan Essence Runner",
    lifecycle: "FAILED",
    health: "FAILED",
    startedAt: null,
    runtimeMs: null,
    heartbeatAgeMs: null,
    loopCount: 22,
    currentState: "CHECK_REQUIREMENTS",
    stateEnteredAt: Date.now() - 5 * 60 * 1000,
    msInCurrentState: 900,
    transitionCount: 3,
    currentAction: "Start aborted safely",
    phases: ["CHECK_REQUIREMENTS", "BANKING", "TRAVELLING", "CRAFTING"],
    phaseIndex: 0,
    logs: logSets.essence,
    lastError: "Required rune pouch was not found"
  }
];

const requestedVariant = new URLSearchParams(window.location.search).get("variant")?.toUpperCase();
const state = {
  variant: VARIANTS[requestedVariant] ? requestedVariant : "A",
  selectedPluginId: "plan-woodcutter",
  feedback: {
    type: "success",
    title: "Dashboard session ready",
    message: "Connected to the local RuneLite client. Status refreshes every 1.5 seconds."
  },
  pendingAction: null,
  logLevel: "ALL",
  capture: {
    busy: false,
    version: 3,
    capturedAt: "14:31:58",
    displayedAt: "14:31:58",
    width: 765,
    height: 503
  },
  plugins
};

const app = document.querySelector("#app");
const prototypeTools = document.querySelector("#prototype-tools");

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function icon(name) {
  const paths = {
    lock: '<rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
    play: '<path d="m8 5 11 7-11 7V5Z"/>',
    stop: '<rect x="6" y="6" width="12" height="12" rx="1"/>',
    camera: '<path d="M14.5 6 13 4h-2L9.5 6H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-4.5Z"/><circle cx="12" cy="13" r="3.2"/>',
    refresh: '<path d="M20 6v5h-5"/><path d="M19 11a7 7 0 1 0 1 5"/>',
    bolt: '<path d="m13 2-8 12h7l-1 8 8-12h-7l1-8Z"/>',
    alert: '<path d="M12 3 2.8 20h18.4L12 3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
    arrowLeft: '<path d="m15 18-6-6 6-6"/>',
    arrowRight: '<path d="m9 18 6-6-6-6"/>',
    shield: '<path d="M12 3 5 6v5c0 4.6 2.8 8 7 10 4.2-2 7-5.4 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    pulse: '<path d="M3 12h4l2-6 4 12 2-6h6"/>',
    chevron: '<path d="m9 18 6-6-6-6"/>'
  };
  return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.bolt}</svg>`;
}

function selectedPlugin() {
  return state.plugins.find(plugin => plugin.id === state.selectedPluginId) || state.plugins[0];
}

function statusClass(value) {
  return String(value || "unknown").toLowerCase();
}

function formatDuration(ms, fallback = "—") {
  if (ms === null || ms === undefined) return fallback;
  if (ms < 1000) return `${ms} ms`;
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ${seconds % 60}s`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${String(minutes % 60).padStart(2, "0")}m`;
}

function statusPill(value) {
  return `<span class="pill ${statusClass(value)}"><span class="status-dot ${statusClass(value)}"></span>${escapeHtml(value)}</span>`;
}

function lifecycleAction(plugin, extraClass = "") {
  const pending = state.pendingAction?.pluginId === plugin.id;
  if (plugin.lifecycle === "RUNNING" || plugin.lifecycle === "STOPPING") {
    return `<button class="button danger ${extraClass}" data-plugin-action="stop" data-plugin-id="${plugin.id}" ${pending ? "disabled" : ""}>${icon("stop")}<span>${plugin.lifecycle === "STOPPING" ? "Stopping…" : "Stop plugin"}</span></button>`;
  }
  return `<button class="button primary ${extraClass}" data-plugin-action="start" data-plugin-id="${plugin.id}" ${pending ? "disabled" : ""}>${icon("play")}<span>${plugin.lifecycle === "STARTING" ? "Starting…" : plugin.lifecycle === "FAILED" ? "Retry start" : "Start plugin"}</span></button>`;
}

function feedbackMarkup() {
  const feedback = state.feedback;
  const dotClass = feedback.type === "error" ? "failed" : feedback.type === "pending" ? "starting" : "healthy";
  return `<div class="feedback ${feedback.type}" role="status"><span class="status-dot ${dotClass}"></span><div><strong>${escapeHtml(feedback.title)}</strong><br>${escapeHtml(feedback.message)}</div></div>`;
}

function phasesMarkup(plugin, mode = "mini") {
  const phaseClass = mode === "route" ? "state-route" : "mini-flow";
  const stepClass = mode === "route" ? "route-stop" : "flow-step";
  return `<div class="${phaseClass}">${plugin.phases.map((phase, index) => {
    const phaseState = index < plugin.phaseIndex ? "done" : index === plugin.phaseIndex ? "current" : "";
    const time = index === plugin.phaseIndex ? formatDuration(plugin.msInCurrentState) : index < plugin.phaseIndex ? "complete" : "waiting";
    return `<div class="${stepClass} ${phaseState}"><strong>${escapeHtml(phase.replaceAll("_", " "))}</strong>${mode === "route" ? `<span>${escapeHtml(time)}</span>` : ""}</div>`;
  }).join("")}</div>`;
}

function filteredLogs(plugin) {
  if (state.logLevel === "ALL") return plugin.logs;
  if (state.logLevel === "WARN") return plugin.logs.filter(log => log.level === "WARN" || log.level === "ERROR");
  return plugin.logs.filter(log => log.level === state.logLevel);
}

function logLevelSelect() {
  return `<label><span class="sr-only">Filter logs by level</span><select data-log-level>
    ${["ALL", "INFO", "WARN", "ERROR", "DEBUG"].map(level => `<option value="${level}" ${state.logLevel === level ? "selected" : ""}>${level === "ALL" ? "All levels" : level}</option>`).join("")}
  </select></label>`;
}

function logsMarkup(plugin) {
  const logs = filteredLogs(plugin);
  if (!logs.length) return '<div class="log-line"><span class="log-message muted">No events match this filter.</span></div>';
  return logs.map(log => `<div class="log-line">
    <span class="log-time">${escapeHtml(log.time)}</span>
    <span class="log-level ${statusClass(log.level)}">${escapeHtml(log.level)}</span>
    <span class="log-message">${escapeHtml(log.message)}</span>
  </div>`).join("");
}

function gameFrame(size = "") {
  const capture = state.capture;
  return `<div class="game-frame ${size}" role="img" aria-label="Mock captured RuneLite frame showing a player chopping a tree">
    <div class="game-rock one"></div>
    <div class="game-rock two"></div>
    <div class="game-rock three"></div>
    <div class="game-tree"></div>
    <div class="game-player"></div>
    <div class="game-click"></div>
    <div class="game-minimap"></div>
    <div class="game-orb"><span>82</span><span>71</span><span>63</span></div>
    <div class="game-chat">You swing your axe at the tree.<br><span style="color:#f3c75f">A bird's nest falls out of the tree.</span></div>
    <span class="capture-stamp">frame ${capture.version} · ${escapeHtml(capture.displayedAt)}</span>
  </div>`;
}

function captureButtons(compact = false) {
  return `<button class="button ${compact ? "square" : ""}" data-refresh ${state.capture.busy ? "disabled" : ""}>${icon("refresh")}${compact ? '<span class="sr-only">Refresh screenshot</span>' : "<span>Refresh</span>"}</button>
    <button class="button primary ${compact ? "square" : ""}" data-capture ${state.capture.busy ? "disabled" : ""}>${icon("camera")}${compact ? '<span class="sr-only">Capture screenshot</span>' : `<span>${state.capture.busy ? "Capturing…" : "Capture"}</span>`}</button>`;
}

function renderVariantA() {
  const plugin = selectedPlugin();
  return `<main class="variant variant-a">
    <header class="a-topbar">
      <div class="a-brand">
        <span class="brand-mark">MB</span>
        <div><h1>Microbot Control Center</h1><p>Local automation overview</p></div>
      </div>
      <div class="prototype-kicker">Throwaway prototype · Issue 27</div>
      <div class="local-session"><span class="status-dot healthy"></span><span>127.0.0.1:8081<br><span class="muted">Browser session · 08:42 left</span></span><span class="session-lock">${icon("lock")}</span></div>
    </header>

    <div class="a-grid">
      <aside class="a-sidebar" aria-label="Eligible plugins">
        <div class="sidebar-heading"><h2>Plugins</h2><span>${state.plugins.length} eligible</span></div>
        <div class="plugin-list">${state.plugins.map(item => `<button class="plugin-row ${item.id === plugin.id ? "selected" : ""}" data-select-plugin="${item.id}" aria-current="${item.id === plugin.id ? "true" : "false"}">
          <span class="status-dot ${statusClass(item.health)}"></span>
          <span><span class="plugin-row-name">${escapeHtml(item.name)}</span><span class="plugin-row-meta">${escapeHtml(item.lifecycle)} · ${escapeHtml(item.currentState || "No active phase")}</span></span>
          <span class="plugin-row-runtime">${formatDuration(item.runtimeMs)}</span>
        </button>`).join("")}</div>
        <p class="eligibility-note">Only plugins with an explicit control-center marker appear here. Infrastructure and login helpers stay hidden.</p>
      </aside>

      <section class="a-detail">
        <div class="a-detail-head">
          <div>
            <p class="eyebrow">Selected automation</p>
            <h2>${escapeHtml(plugin.name)}</h2>
            <p class="id mono">${escapeHtml(plugin.id)}</p>
            <div class="a-state-line">${statusPill(plugin.lifecycle)}${statusPill(plugin.health)}</div>
          </div>
          <div class="head-actions">${lifecycleAction(plugin)}</div>
        </div>

        ${feedbackMarkup()}

        <div class="stats-strip">
          <div class="stat"><span class="stat-label">Runtime</span><span class="stat-value">${formatDuration(plugin.runtimeMs)}</span></div>
          <div class="stat"><span class="stat-label">Heartbeat age</span><span class="stat-value">${formatDuration(plugin.heartbeatAgeMs)}</span></div>
          <div class="stat"><span class="stat-label">Loops</span><span class="stat-value">${plugin.loopCount.toLocaleString()}</span></div>
          <div class="stat"><span class="stat-label">Transitions</span><span class="stat-value">${plugin.transitionCount.toLocaleString()}</span></div>
        </div>

        <section class="a-phase">
          <div class="section-title-row"><h3>State machine</h3><span>${plugin.currentState ? `${formatDuration(plugin.msInCurrentState)} in phase` : "Not active"}</span></div>
          <div class="action-callout"><span class="action-icon">${icon(plugin.lastError ? "alert" : "bolt")}</span><div><span>Current action</span><strong>${escapeHtml(plugin.currentAction)}</strong></div></div>
          ${phasesMarkup(plugin)}
        </section>

        <section class="a-logs">
          <div class="log-toolbar"><h3>Recent logs <span class="muted">${plugin.logs.length} / 500</span></h3>${logLevelSelect()}</div>
          <div class="log-list">${logsMarkup(plugin)}</div>
        </section>
      </section>

      <aside class="a-capture">
        <div class="capture-layout">
          <div class="section-title-row"><div><p class="eyebrow">Visual check</p><h3>Client frame</h3></div><div class="capture-actions">${captureButtons(true)}</div></div>
          <div>${gameFrame()}<div class="capture-meta"><span>${state.capture.width} × ${state.capture.height} PNG</span><span>Captured ${escapeHtml(state.capture.capturedAt)}</span></div><p class="capture-note">Manual capture only. The frame stays in memory for this response and is not written to disk.</p></div>
          <div class="connection-card"><p class="eyebrow">Session boundary</p><div class="connection-row"><span>Transport</span><strong>Loopback only</strong></div><div class="connection-row"><span>Origin</span><strong>Same-origin</strong></div><div class="connection-row"><span>Storage</span><strong>In memory</strong></div><div class="connection-row"><span>Last poll</span><strong>Just now</strong></div></div>
        </div>
      </aside>
    </div>
  </main>`;
}

function renderVariantB() {
  const plugin = selectedPlugin();
  return `<main class="variant variant-b">
    <header class="b-header">
      <div class="b-brand"><span class="brand-mark">MB</span><div><h1>Control Center / Signals</h1><p>One client · ${state.plugins.length} opted-in automations</p></div></div>
      <div class="prototype-kicker">Throwaway prototype · Issue 27</div>
      <div class="system-health">${icon("shield")} Local session secured · polling 1.5s</div>
    </header>

    <div class="b-content">
      <section aria-labelledby="signal-title">
        <p class="eyebrow" id="signal-title">Automation signals</p>
        <div class="signal-board" role="list">
          <div class="signal-head" aria-hidden="true"><span>Plugin</span><span>Lifecycle</span><span>Health</span><span>Runtime</span><span>Phase</span><span></span></div>
          ${state.plugins.map(item => `<button class="signal-row ${item.id === plugin.id ? "selected" : ""}" data-select-plugin="${item.id}" role="listitem">
            <span class="signal-name"><span class="status-dot ${statusClass(item.health)}"></span><strong>${escapeHtml(item.name)}</strong></span>
            <span class="signal-value">${escapeHtml(item.lifecycle)}</span>
            <span class="signal-value health-${statusClass(item.health)}">${escapeHtml(item.health)}</span>
            <span class="signal-value">${formatDuration(item.runtimeMs)}</span>
            <span class="signal-value">${escapeHtml(item.currentState || "—")}</span>
            <span class="signal-arrow">›</span>
          </button>`).join("")}
        </div>
      </section>

      <div class="b-workspace">
        <section class="b-main">
          <div class="b-focus-title"><div><p class="eyebrow">Inspecting</p><h2>${escapeHtml(plugin.name)}</h2><p class="mono">${escapeHtml(plugin.id)} · transition ${plugin.transitionCount}</p></div><div>${statusPill(plugin.health)}</div></div>
          ${feedbackMarkup()}
          <div class="route-line"><div class="route-caption"><span>State route</span><span>${escapeHtml(plugin.currentAction)}</span></div>${phasesMarkup(plugin, "route")}</div>
          <section class="terminal">
            <div class="terminal-bar"><span class="terminal-title">bounded plugin log · ${plugin.logs.length}/500</span>${logLevelSelect()}</div>
            <div class="log-list">${logsMarkup(plugin)}</div>
          </section>
        </section>

        <aside class="b-side">
          <section class="command-block"><h3>Lifecycle command</h3>${lifecycleAction(plugin)}</section>
          <section class="command-block"><h3>Live snapshot</h3><div class="command-state">
            <div><span>Lifecycle</span><strong>${escapeHtml(plugin.lifecycle)}</strong></div>
            <div><span>Health</span><strong>${escapeHtml(plugin.health)}</strong></div>
            <div><span>Heartbeat</span><strong>${formatDuration(plugin.heartbeatAgeMs)}</strong></div>
            <div><span>Runtime</span><strong>${formatDuration(plugin.runtimeMs)}</strong></div>
            <div><span>Loops</span><strong>${plugin.loopCount}</strong></div>
            <div><span>In phase</span><strong>${formatDuration(plugin.msInCurrentState)}</strong></div>
          </div></section>
          <section class="command-block b-capture-preview"><div class="section-title-row"><h3>Client frame</h3><div class="capture-actions">${captureButtons(true)}</div></div>${gameFrame()}<div class="capture-meta"><span>Manual only</span><span>${escapeHtml(state.capture.displayedAt)}</span></div></section>
        </aside>
      </div>
    </div>
  </main>`;
}

function renderVariantC() {
  const plugin = selectedPlugin();
  const latestLog = filteredLogs(plugin).at(-1) || plugin.logs.at(-1);
  const actionClass = plugin.health === "STALLED" ? "stalled" : plugin.health === "FAILED" ? "failed" : plugin.health === "UNKNOWN" ? "unknown" : "";
  return `<main class="variant variant-c">
    <div class="c-stage">
      ${gameFrame("large")}
      <header class="c-top">
        <div class="c-brand"><span class="brand-mark">MB</span><div><strong>Field monitor</strong><span>Live client frame · 127.0.0.1</span></div></div>
        <div class="prototype-kicker">Throwaway prototype · Issue 27</div>
        <div class="c-secure">${icon("lock")} Same-origin session</div>
      </header>

      <nav class="c-plugin-dock" aria-label="Eligible plugins">${state.plugins.map(item => `<button class="dock-plugin ${item.id === plugin.id ? "selected" : ""}" data-select-plugin="${item.id}" title="${escapeHtml(item.name)}"><span>${escapeHtml(item.short)}</span><span class="status-dot ${statusClass(item.health)}"></span></button>`).join("")}</nav>

      <aside class="c-hud">
        <div class="c-hud-head"><p class="eyebrow">Focused automation</p><h1>${escapeHtml(plugin.name)}</h1><p>${escapeHtml(plugin.id)}</p><div class="c-hud-status">${statusPill(plugin.lifecycle)}${statusPill(plugin.health)}</div></div>
        <div class="c-action"><p class="eyebrow">Current action</p><strong>${escapeHtml(plugin.currentAction)}</strong><div class="heartbeat-meter ${actionClass}" aria-label="Heartbeat visualization"><span></span><span></span><span></span><span></span><span></span><span></span></div></div>
        <div class="c-metrics"><div class="c-metric"><span>Runtime</span><strong>${formatDuration(plugin.runtimeMs)}</strong></div><div class="c-metric"><span>Heartbeat</span><strong>${formatDuration(plugin.heartbeatAgeMs)}</strong></div><div class="c-metric"><span>Phase</span><strong>${escapeHtml(plugin.currentState || "—")}</strong></div></div>
        ${feedbackMarkup()}
        <div class="c-controls">${lifecycleAction(plugin, "lifecycle")} ${captureButtons(false)}</div>
      </aside>

      <div class="c-log-ribbon"><span class="ribbon-label">${escapeHtml(latestLog?.level || "IDLE")}</span><span class="ribbon-message">${escapeHtml(latestLog?.message || "No log events")}</span><span class="ribbon-meta">${escapeHtml(latestLog?.time || "—")} · ${plugin.logs.length}/500</span></div>
    </div>
  </main>`;
}

function stateSnapshot() {
  return {
    variant: `${state.variant} — ${VARIANTS[state.variant]}`,
    selectedPluginId: state.selectedPluginId,
    feedback: state.feedback,
    pendingAction: state.pendingAction,
    logLevel: state.logLevel,
    capture: state.capture,
    plugins: state.plugins.map(plugin => ({
      id: plugin.id,
      lifecycle: plugin.lifecycle,
      health: plugin.health,
      runtimeMs: plugin.runtimeMs,
      heartbeatAgeMs: plugin.heartbeatAgeMs,
      currentState: plugin.currentState,
      msInCurrentState: plugin.msInCurrentState,
      transitionCount: plugin.transitionCount,
      currentAction: plugin.currentAction,
      lastError: plugin.lastError,
      logCount: plugin.logs.length,
      lastLogSequence: plugin.logs.at(-1)?.sequence ?? null
    }))
  };
}

function renderPrototypeTools() {
  if (document.documentElement.dataset.prototype !== "true") {
    prototypeTools.replaceChildren();
    return;
  }
  prototypeTools.innerHTML = `<div class="prototype-switcher" aria-label="Prototype variant switcher">
    <button class="switch-button" data-variant-cycle="-1" aria-label="Previous variant">${icon("arrowLeft")}</button>
    <div class="switch-label"><strong>${state.variant} — ${VARIANTS[state.variant]}</strong><span>Use ← → · shareable query</span></div>
    <button class="switch-button" data-variant-cycle="1" aria-label="Next variant">${icon("arrowRight")}</button>
  </div>
  <details class="prototype-state" open><summary>Prototype · in-memory state</summary><pre>${escapeHtml(JSON.stringify(stateSnapshot(), null, 2))}</pre></details>`;
}

function render() {
  const renders = { A: renderVariantA, B: renderVariantB, C: renderVariantC };
  app.innerHTML = renders[state.variant]();
  renderPrototypeTools();
  document.title = `${state.variant} — ${VARIANTS[state.variant]} · Microbot prototype`;
}

function setVariant(nextVariant) {
  if (!VARIANTS[nextVariant]) return;
  state.variant = nextVariant;
  const url = new URL(window.location.href);
  url.searchParams.set("variant", nextVariant);
  window.history.replaceState({}, "", url);
  render();
}

function cycleVariant(direction) {
  const keys = Object.keys(VARIANTS);
  const current = keys.indexOf(state.variant);
  setVariant(keys[(current + direction + keys.length) % keys.length]);
}

function nowTime() {
  return new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });
}

function appendLog(plugin, level, message) {
  const lastSequence = plugin.logs.at(-1)?.sequence || 0;
  plugin.logs.push({ sequence: lastSequence + 1, time: nowTime(), level, message });
  if (plugin.logs.length > 500) plugin.logs.shift();
}

function runPluginAction(pluginId, action) {
  const plugin = state.plugins.find(item => item.id === pluginId);
  if (!plugin || state.pendingAction?.pluginId === pluginId) return;

  state.selectedPluginId = pluginId;
  state.pendingAction = { pluginId, action };
  if (action === "start") {
    plugin.lifecycle = "STARTING";
    plugin.health = "UNKNOWN";
    plugin.currentAction = "Requesting plugin start";
    state.feedback = { type: "pending", title: `Starting ${plugin.name}`, message: "Lifecycle request is serialized. Waiting for PluginManager confirmation…" };
    appendLog(plugin, "INFO", `Start requested for ${plugin.id}`);
  } else {
    plugin.lifecycle = "STOPPING";
    plugin.currentAction = "Stopping script safely";
    state.feedback = { type: "pending", title: `Stopping ${plugin.name}`, message: "Waiting for script shutdown and PluginManager confirmation…" };
    appendLog(plugin, "INFO", `Stop requested for ${plugin.id}`);
  }
  render();

  window.setTimeout(() => {
    if (action === "start" && plugin.id === "plan-essence-runner") {
      plugin.lifecycle = "FAILED";
      plugin.health = "FAILED";
      plugin.currentAction = "Start aborted safely";
      plugin.lastError = "Required rune pouch was not found";
      appendLog(plugin, "ERROR", `Start failed: ${plugin.lastError}`);
      state.feedback = { type: "error", title: `${plugin.name} did not start`, message: `${plugin.lastError}. Actual PluginManager state remains stopped.` };
    } else if (action === "start") {
      plugin.lifecycle = "RUNNING";
      plugin.health = "HEALTHY";
      plugin.startedAt = Date.now();
      plugin.runtimeMs = 0;
      plugin.heartbeatAgeMs = 180;
      plugin.currentState = plugin.phases[0];
      plugin.phaseIndex = 0;
      plugin.msInCurrentState = 0;
      plugin.currentAction = "Checking requirements";
      plugin.lastError = null;
      appendLog(plugin, "INFO", "Plugin started; first heartbeat received");
      state.feedback = { type: "success", title: `${plugin.name} is running`, message: "PluginManager confirmed the lifecycle state and the script heartbeat is healthy." };
    } else {
      plugin.lifecycle = "STOPPED";
      plugin.health = "UNKNOWN";
      plugin.startedAt = null;
      plugin.runtimeMs = null;
      plugin.heartbeatAgeMs = null;
      plugin.currentState = null;
      plugin.phaseIndex = -1;
      plugin.msInCurrentState = null;
      plugin.currentAction = "Waiting to start";
      appendLog(plugin, "INFO", "Plugin stopped cleanly");
      state.feedback = { type: "success", title: `${plugin.name} stopped`, message: "Script shutdown completed and the runtime status provider was released." };
    }
    state.pendingAction = null;
    render();
  }, 850);
}

function captureFrame() {
  if (state.capture.busy) return;
  state.capture.busy = true;
  state.feedback = { type: "pending", title: "Capturing next client frame", message: "Waiting on DrawManager with a bounded timeout…" };
  render();
  window.setTimeout(() => {
    state.capture.busy = false;
    state.capture.version += 1;
    state.capture.capturedAt = nowTime();
    state.capture.displayedAt = state.capture.capturedAt;
    state.feedback = { type: "success", title: "Client frame captured", message: "The in-memory PNG response is displayed. Nothing was written to disk." };
    render();
  }, 700);
}

function refreshFrame() {
  if (state.capture.busy) return;
  state.capture.displayedAt = state.capture.capturedAt;
  state.feedback = { type: "success", title: "Latest capture refreshed", message: `Showing frame ${state.capture.version}, captured at ${state.capture.capturedAt}. No new capture was requested.` };
  render();
}

document.addEventListener("click", event => {
  const select = event.target.closest("[data-select-plugin]");
  if (select) {
    state.selectedPluginId = select.dataset.selectPlugin;
    const plugin = selectedPlugin();
    state.feedback = plugin.lastError
      ? { type: "error", title: `${plugin.name} needs attention`, message: plugin.lastError }
      : { type: "success", title: `${plugin.name} selected`, message: "Status, state-machine snapshot, logs, and controls are now in focus." };
    render();
    return;
  }

  const action = event.target.closest("[data-plugin-action]");
  if (action) {
    runPluginAction(action.dataset.pluginId, action.dataset.pluginAction);
    return;
  }

  if (event.target.closest("[data-capture]")) {
    captureFrame();
    return;
  }

  if (event.target.closest("[data-refresh]")) {
    refreshFrame();
    return;
  }

  const cycle = event.target.closest("[data-variant-cycle]");
  if (cycle) cycleVariant(Number(cycle.dataset.variantCycle));
});

document.addEventListener("change", event => {
  if (event.target.matches("[data-log-level]")) {
    state.logLevel = event.target.value;
    render();
  }
});

window.addEventListener("keydown", event => {
  const target = event.target;
  if (target instanceof HTMLElement && (target.matches("input, textarea, select, [contenteditable]") || target.isContentEditable)) return;
  if (event.key === "ArrowLeft") {
    event.preventDefault();
    cycleVariant(-1);
  }
  if (event.key === "ArrowRight") {
    event.preventDefault();
    cycleVariant(1);
  }
});

window.addEventListener("popstate", () => {
  const variant = new URLSearchParams(window.location.search).get("variant")?.toUpperCase();
  if (VARIANTS[variant]) {
    state.variant = variant;
    render();
  }
});

render();
