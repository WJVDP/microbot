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

- Should the first UI shell be Tauri or Electron?
- How much of Microbot upstream should remain tracked after the fork diverges?
- What plugin signing model is acceptable for community plugins?
- Should plugin permissions/capabilities be enforced initially or only declared?
- What telemetry should be kept, removed, or made explicitly opt-in?
- Should the bridge API use JSON schema, protobuf, or another contract format?

## Immediate Next Actions

1. Import or clone the actual Microbot source into this workspace.
2. Configure fork and upstream remotes.
3. Add the RuneLite update playbook.
4. Add the RuneLite update report script.
5. Run the baseline build/test commands.
6. Begin Phase 3 design with a full inventory of current plugin loading code.
