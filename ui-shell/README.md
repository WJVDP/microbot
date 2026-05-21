# Microbot UI Shell

Minimal TypeScript desktop shell for the Bridge V1 API. This milestone keeps
the existing Swing UI untouched and adds a separate Electron workspace.

## Shell Decision

This workspace uses Electron rather than Tauri for Milestone 4. Electron keeps
the repo requirements to Node/npm for now, can read the local bridge token file
from the desktop process, and avoids adding a Rust toolchain requirement to the
RuneLite Java checkout. Tauri can still be reconsidered after the shell needs
packaging, auto-update integration, or smaller release artifacts.

## Token And Bridge Context

The shell sends Bridge V1 requests to `http://127.0.0.1:8081/bridge/v1` by
default and includes the token in `X-Agent-Token`.

Token lookup order:

1. `MICROBOT_TOKEN`
2. `MICROBOT_TOKEN_FILE`
3. `~/.runelite/.agent-token`

Bridge URL overrides:

- `MICROBOT_BRIDGE_URL`, for example `http://127.0.0.1:8081`
- `MICROBOT_HOST` and `MICROBOT_PORT`, when `MICROBOT_BRIDGE_URL` is unset

The fallback browser dev view cannot read local files, so it uses local storage
for the bridge URL and token if it is opened outside Electron.

## Run

```bash
cd ui-shell
npm install
npm run build
npm start
```

For renderer-only development:

```bash
npm run dev
```

For an Electron window after a build:

```bash
npm run electron:dev
```

The Java client must be running with the Agent Server plugin enabled. Bridge V1
uses the same local transport and token guard documented in
`docs/bridge-api-v1.md` and `docs/AGENT_SERVER.md`.
