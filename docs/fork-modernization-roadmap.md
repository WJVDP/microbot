# Microbot Fork Modernization Roadmap

This document is the working plan for turning the Microbot fork into a maintainable, modernized RuneLite-based client. It should be kept current as implementation decisions change.

## Goals

- Build a cleaner UI around the OSRS game experience.
- Replace the current fragmented plugin loading paths with one explicit plugin runtime.
- Make RuneLite upstream version increments repeatable and low-risk.
- Move non-core concerns away from Java where that lowers complexity.
- Improve startup time, plugin loading time, runtime stability, and profiling visibility.
- Keep RuneLite compatibility intact while changing Microbot-specific systems.

## Current Assessment

The project is a RuneLite fork with Microbot functionality built into `runelite-client`. The core runtime is still Java because RuneLite's client, event bus, overlays, plugin lifecycle, injected client, and game APIs are Java-native.

Important current boundaries:

- `MicrobotPlugin` is always-on, hidden, and owns the Microbot sidebar entrypoint.
- `Microbot` is a large static runtime singleton used by scripts and utilities.
- Plugin loading is split across core RuneLite scanning, RuneLite external plugins, local sideloaded jars, and Microbot's plugin hub.
- Microbot's Gradle project version can lag RuneLite upstream, so regular merges need a documented process.
- UI is Swing/FlatLaf-based, which makes a full modern UI rewrite inside the current process awkward.

## Target Architecture

```text
RuneLite/Microbot Java core
  - game canvas
  - RuneLite APIs
  - plugin lifecycle
  - script execution
  - overlays
  - game/event caches

Local bridge API
  - typed commands
  - plugin install/update/start/stop
  - runtime state
  - logs/status/events
  - auth/token guard

Modern UI shell
  - TypeScript frontend
  - Tauri or Electron desktop shell
  - plugin marketplace
  - script dashboard
  - settings editor
  - update manager
```

The Java client remains the game/runtime core. Non-core surfaces such as launcher, updater, marketplace UI, plugin metadata tooling, and dashboard UI can move to TypeScript or Rust over time.

## Guiding Decisions

1. Preserve RuneLite compatibility as the highest-priority constraint.
2. Avoid a full rewrite of the game client. That would effectively mean rebuilding RuneLite.
3. Establish upstream update discipline before deep feature rewrites.
4. Build new systems behind explicit interfaces, then migrate old code incrementally.
5. Prefer manifest-based plugin metadata over classpath scanning.
6. Measure performance before optimizing.
7. Treat scripts/plugins as semi-untrusted extensions with compatibility and health checks.

## Phase 1: Fork Hygiene

Purpose: make the fork reproducible and safe to change.

Status as of 2026-05-19:

- Imported the real Microbot source tree into `Microbot/` inside this workspace.
- Configured `microbot-upstream` and `runelite-upstream` remotes with disabled push URLs.
- Configured `origin` as the writable GitHub fork at `https://github.com/WJVDP/microbot.git`.
- Created local durable branches for RuneLite integration, Microbot integration, plugin runtime work, and UI shell work.
- Made CI's Phase 1 checks explicit in `ci/build.sh`.
- Added `docs/fork-workflow.md` and `docs/release-workflow.md`.
- Added `docs/runelite-update-playbook.md` and `scripts/update-runelite-report.sh`.
- Ran the baseline build/test commands with a workspace-local Gradle cache.
- Began Phase 3 with `docs/plugin-runtime-v2-inventory.md`.
- Added read-only Plugin Runtime V2 artifact/repository scaffolding under
  `net.runelite.client.plugins.runtime`, with manifest-backed metadata
  discovery tests and shared validation scaffolding.
- Decided that Microbot Hub jars should reuse RuneLite's jar-local
  `runelite_plugin.json` stub for entry classes. Microbot-specific hub,
  compatibility, permission, and capability metadata remains in Microbot
  manifests or a future extension only when RuneLite's stub cannot represent it.

Tasks:

- Clone or import the real Microbot source tree into this workspace.
- Configure remotes:
  - `origin`: our fork.
  - `microbot-upstream`: `https://github.com/chsami/Microbot`.
  - `runelite-upstream`: `https://github.com/runelite/runelite`.
- Create durable branches:
  - `main`: stable releases only.
  - `integration/runelite`: RuneLite upstream merge branch.
  - `integration/microbot`: Microbot upstream merge branch.
  - `feature/plugin-runtime-v2`: plugin loading work.
  - `feature/ui-shell`: new UI shell work.
- Add or verify CI for:
  - `./gradlew :client:compileJava`
  - `./gradlew :client:runUnitTests`
  - client-thread guardrail tests
  - plugin loading smoke tests
  - shaded jar build
- Add project docs for local development and release workflow.

Acceptance criteria:

- A fresh checkout builds.
- Remotes are configured and documented.
- CI catches compile, unit, and basic plugin loading failures.
- The fork has clear branch ownership rules.

## Phase 2: RuneLite Update Discipline

Purpose: make upstream RuneLite version increments predictable.

Tasks:

- Create a `docs/runelite-update-playbook.md`.
- Create a helper script such as `scripts/update-runelite-report.sh`.
- The report should show:
  - current fork version
  - latest RuneLite version
  - changed files under `runelite-api`
  - changed files under `runelite-client/src/main/java/net/runelite/client/plugins`
  - dependency changes in `libs.versions.toml`
  - injected-client/version-sensitive changes
- Define the merge flow:
  - fetch `runelite-upstream/master`
  - merge into `integration/runelite`
  - resolve conflicts
  - run verification
  - bump versions
  - merge to `main` only after smoke tests
- Document resources that may need regeneration after RuneLite bumps.

Acceptance criteria:

- A maintainer can follow the playbook without prior context.
- The update script identifies likely conflict and compatibility areas.
- Version bumps and generated resources are tracked explicitly.

## Phase 3: Plugin Runtime V2

Purpose: replace fragmented plugin loading with one coherent model.

Status as of 2026-05-26:

- Inventory and manifest-format decision are documented in
  `docs/plugin-runtime-v2-inventory.md` and
  `docs/decisions/adr-0005-plugin-jar-entry-manifest.md`.
- Read-only repository adapters exist for core, RuneLite Hub, Microbot Hub, and
  local-directory plugin sources.
- RuneLite Hub, Microbot Hub, and local-directory artifacts read jar-local
  `runelite_plugin.json` entry classes without loading plugin classes.
- `PluginRuntime.discoverStatus()` returns structured artifact statuses for
  compatibility, disabled, duplicate-id, missing-entry-class, checksum,
  signature, and capability-policy failures.
- `MicrobotPluginManager` now reuses the shared artifact validator for
  descriptor and manifest checks, and install is blocked for disabled or
  incompatible Microbot Hub manifests before download/load.
- Runtime V2 centralizes checksum verification and signature verification
  through explicit verifier and policy types.
- Community plugin signing and staged capability policy are accepted in
  `docs/decisions/adr-0006-community-plugin-signing-trust-model.md` and
  `docs/decisions/adr-0007-staged-plugin-capability-permissions.md`.
- Existing classpath and whole-jar scanning remain only as compatibility paths
  until manifest-backed coverage is complete for released plugins.

Proposed abstractions:

```text
PluginRepository
  - CoreRepository
  - RuneLiteHubRepository
  - MicrobotHubRepository
  - LocalDirectoryRepository

PluginArtifact
  - id
  - displayName
  - version
  - minClientVersion
  - sha256
  - signature
  - source
  - entry plugin classes
  - permissions/capabilities

PluginRuntime
  - discover
  - install
  - verify
  - load
  - start
  - stop
  - unload where possible
  - report health
```

Tasks:

- Inventory all current plugin loading paths.
- Define a manifest format embedded in plugin jars.
- Add compatibility checks before class loading.
- Add checksum/signature verification.
- Move plugin discovery away from whole-classpath scanning.
- Keep a compatibility adapter for existing plugins during migration.
- Add tests for:
  - valid plugin load
  - invalid manifest
  - incompatible client version
  - duplicate plugin ids
  - missing dependencies
  - disabled/blocked plugin

Acceptance criteria:

- All plugin sources are represented through one runtime interface.
- Plugin metadata can be read without loading arbitrary plugin classes.
- Existing plugins still load through the compatibility path.
- Incompatible plugins fail with clear user-facing reasons.

## Phase 4: Modern UI Around The Game

Purpose: build a cleaner UI without destabilizing the RuneLite game canvas.

Status as of 2026-05-26:

- Added `docs/bridge-api-v1.md` as the first versioned local bridge contract.
- Reused the existing Agent Server localhost/UDS transport and `X-Agent-Token`
  guard for the UI-facing bridge.
- Added `/bridge/v1/status`, `/bridge/v1/plugins`, and
  `/bridge/v1/plugins/{id}/start|stop` endpoints for runtime status and loaded
  plugin control.
- Added `/bridge/v1/plugin-artifacts` backed by Plugin Runtime V2 discovery
  status for Microbot Hub artifacts.
- The Agent Server remains an automation surface; Bridge V1 is the narrower
  dashboard/UI contract.
- Bridge V1 now also covers plugin artifact install/update/remove, plugin
  config schema and read/write endpoints, recent events, runtime health,
  startup timing, generated TypeScript types, and status fields for signature
  and capability policy.
- `ui-shell/` contains the first Electron and TypeScript shell scaffold that can
  consume Bridge V1 status, plugin, artifact, health, and timing surfaces.

Recommended first architecture:

- Keep the OSRS game canvas in the Java client window.
- Replace or supplement Microbot's Swing plugin/config UI with a modern local UI shell.
- Expose local runtime state through a typed bridge API.
- Build the UI in TypeScript.
- Use Tauri if small desktop packaging matters most.
- Use Electron if UI iteration speed and ecosystem matter most.

Bridge API surfaces:

- client status
- logged-in/session status
- plugin list
- plugin install/update/remove
- plugin start/stop
- plugin config schema
- script status
- logs/events
- version/update status

Acceptance criteria:

- UI shell can list plugins and runtime status from the Java client.
- Starting/stopping a plugin works through the bridge.
- The old Swing UI can coexist during migration.
- The bridge is versioned and guarded by a local token.

## Phase 5: Move Non-Core Work Away From Java

Purpose: reduce Java surface area where it is not required by RuneLite.

Status as of 2026-05-26:

- Bridge V1 defines the Java-to-non-Java contract that a TypeScript dashboard
  consumes.
- `ui-shell/` is the first migrated non-core subsystem. Java continues to own
  actual plugin lifecycle and game runtime behavior.
- Future practical migration targets are launcher/update UX, plugin
  marketplace/dashboard UI, diagnostics, and release automation.

Good candidates:

- launcher/updater
- plugin marketplace frontend
- plugin metadata tooling
- release automation
- UI shell
- diagnostics dashboard

Poor first candidates:

- RuneLite API
- injected client
- game canvas/input
- overlays
- plugin lifecycle
- event bus
- core script execution

Acceptance criteria:

- At least one non-core subsystem is moved out of Java without changing game runtime behavior.
- Java remains the stable runtime boundary.
- Cross-language contracts are typed and versioned.

## Phase 6: Performance And Observability

Purpose: improve performance based on measurements.

Status as of 2026-05-26:

- Startup and Runtime V2 discovery timing are captured for manifest loading and
  jar verification.
- Central event, scheduler, and overlay calls emit per-call timing and health
  records.
- Bridge V1 exposes read-only runtime health and startup timing status.
- No startup optimization has been claimed yet because the automated runs do not
  exercise a real plugin directory or full GUI startup.

Tasks:

- Add timing around startup phases:
  - classpath/plugin discovery
  - manifest loading
  - jar verification
  - plugin instantiation
  - config loading
  - splash stages
- Add per-plugin health metrics:
  - event handler cost
  - overlay render cost
  - scheduled task cost
  - script executor queue depth
  - exception count
- Profile plugin class loading and replace scanning where possible.
- Audit overlays and event handlers for heavy client-thread work.
- Add a plugin watchdog that can flag or disable repeatedly failing plugins.
- Track cache/query costs for hot paths.

Acceptance criteria:

- Startup performance can be measured per stage.
- Slow plugins can be identified.
- Repeated plugin failures are visible and actionable.
- At least one measured bottleneck is improved with before/after numbers.

## Phase 7: Runtime Cleanup

Purpose: reduce coupling and make future changes easier.

Status as of 2026-05-26:

- Initial service interfaces exist for plugin runtime, bridge API, script
  lifecycle, game state/cache access, and telemetry/update checks.
- Compatibility shims preserve existing static `Microbot` callers while new code
  can depend on injectable services.
- Focused unit tests cover core service construction without launching the full
  client.

Tasks:

- Gradually replace static `Microbot` access with injectable services.
- Define service interfaces for:
  - script lifecycle
  - game state/cache access
  - plugin runtime
  - bridge API
  - telemetry/update checks
- Preserve compatibility shims for existing scripts.
- Add tests around service boundaries.
- Document migration patterns for plugin/script authors.

Acceptance criteria:

- New code can depend on services instead of static global state.
- Existing scripts continue to work.
- Tests can instantiate core services without launching the full client.

## Phase 8: Release And Distribution

Purpose: make releases repeatable and understandable for users.

Status as of 2026-05-21:

- Stable release CI now generates and uploads `microbot-<version>.jar.sha256`
  and `update-stable.json` beside the shaded jar.
- Added `scripts/generate-release-metadata.sh` for channel metadata generation
  across `stable`, `beta`, and `nightly`.
- Documented channel ownership and metadata-forward rollback behavior in
  `docs/release-workflow.md`.

Remaining gaps:

- Signing is still documented as required but not wired to CI secrets.
- Beta/nightly workflows still need to publish their channel metadata.
- Automated release notes are still manual.

Tasks:

- Define release channels:
  - stable
  - beta
  - nightly
- Produce signed release artifacts.
- Publish checksums.
- Add automated release notes.
- Add update metadata for the launcher/UI shell.
- Document rollback behavior.
- Define compatibility policy for plugin API versions.

Acceptance criteria:

- A release can be produced from CI.
- Users can verify downloads.
- Plugin compatibility is clear per client version.
- Rollback is documented and tested.

## Open Questions

- How much of Microbot upstream should remain tracked after the fork diverges?
- What telemetry should be kept, removed, or made explicitly opt-in?
- Should the bridge API use JSON schema, protobuf, or another contract format?

Resolved decisions:

- The first UI shell uses Electron; see `docs/remaining-modernization-prd.md`.
- Community plugin signing uses the source-aware trust model in
  `docs/decisions/adr-0006-community-plugin-signing-trust-model.md`.
- Plugin permissions/capabilities use the staged policy in
  `docs/decisions/adr-0007-staged-plugin-capability-permissions.md`.

## Immediate Next Actions

1. Finish and review the current Runtime V2 trust-policy slice for signature
   classification, capability policy, Bridge V1 status fields, and tests.
2. Wire release signing into CI using documented secrets/properties.
3. Publish beta and nightly channel metadata from their workflows.
4. Generate release notes from commit ranges and upstream merge points.
5. Validate update metadata and checksum URLs in CI.
6. Define plugin API compatibility policy per client version.
