# ADR 0005: Embedded Local Plugin Control Center

- Status: Accepted (2026-08-05)

## Context

Automation lifecycle, heartbeat, state-machine debug data, logs, and screenshots were individually available through RuneLite or Agent Server, but there was no single visual surface for one client. Reusing the machine API token in browser code would expose a long-lived secret and weakening `AgentHandler` browser-Origin rejection would expand every existing endpoint's attack surface. Automatically exposing every loaded Microbot plugin would also make infrastructure, login helpers, and test utilities controllable by accident.

## Decision

Serve a plain HTML/CSS/JavaScript dashboard from the existing Agent Server TCP listener at `/dashboard/`. Keep it local, in-memory, offline, and single-client; bundle its resources in the canonical shaded jar. Do not add another server, persistent store, runtime Node dependency, remote access, or periodic image capture.

Give browser routes a security boundary separate from the machine-token API. A RuneLite config action creates a 30-second one-time nonce. The first exact-loopback navigation moves that nonce into a path-limited `HttpOnly`, `SameSite=Strict` bootstrap cookie, and a same-origin POST consumes it to create a 15-minute in-memory browser session. The session cookie is `HttpOnly`; JavaScript receives only an in-memory CSRF token. Validate the peer, exact Host and Origin, require CSRF for lifecycle mutations, disable caching, and serve a restrictive self-only Content Security Policy. Leave generic `AgentHandler` authentication and Origin rejection unchanged.

Expose only plugin classes bearing `@ControlCenterPlugin` with a unique stable lowercase id. A shared `ControlCenterService` owns discovery, per-plugin lifecycle serialization, EDT dispatch with bounded worker-side waits, and immutable DTO aggregation. It combines `PluginManager` lifecycle, `ScriptHeartbeatRegistry` health, automatically matched `StateMachineScript` snapshots, optional bounded status providers, and a dedicated redacting 500-event Logback ring buffer. Manual screenshots reuse `DrawManager` next-frame capture, serialize concurrent requests, return in-memory PNG bytes, and do not persist the frame.

## Consequences

- Browser access does not reveal or reduce protection of the rotatable machine API token.
- Infrastructure plugins are excluded unless their class intentionally opts in; stopped eligible plugin instances remain discoverable through `PluginManager`.
- Lifecycle and heartbeat health are distinct, so an enabled plugin is not automatically considered healthy.
- Plugin authors who register an optional status provider must publish already-safe immutable data and unregister during shutdown.
- Dashboard availability requires TCP mode. An explicit open action temporarily holds stealth-bind mode open for the short browser session.
- Status, sessions, logs, lifecycle errors, and captures do not survive an Agent Server or client restart.
- Static assets flow through the existing `processResources` and shaded-jar tasks; the dashboard requires no internet access.
