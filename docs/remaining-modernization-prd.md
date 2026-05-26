# Remaining Modernization PRD

## Purpose

This PRD defines the remaining modernization scope in the most practical
delivery order. It is separate from `docs/fork-modernization-roadmap.md`: the
roadmap records long-term architecture and phase status, while this document
turns the open gaps into an ordered product and engineering backlog.

## Product Goal

Deliver a maintainable Microbot fork where plugin discovery, validation,
runtime status, UI control, observability, and release distribution are
explicit, testable, and safe to evolve without breaking RuneLite compatibility.

## Non-Goals

- Rewriting the RuneLite game client.
- Moving plugin lifecycle, event bus, overlays, injected client, game canvas, or
  core script execution out of Java.
- Removing legacy plugin loading before Plugin Runtime V2 has full compatibility
  coverage.
- Building a marketplace or dashboard UI before Bridge V1 exposes the necessary
  contract.

## Target Users

- Maintainers updating RuneLite upstream and releasing the fork.
- Plugin/script authors who need clear compatibility and failure reasons.
- End users who need a cleaner dashboard for plugin status and control.
- Future UI shell code that needs a stable local API contract.

## Success Metrics

- Plugin artifacts can be discovered, verified, and reported without loading
  arbitrary plugin classes when manifest metadata exists.
- Incompatible, disabled, duplicate, malformed, or tampered plugins fail with
  structured reasons visible through Bridge V1.
- A TypeScript UI shell can list runtime status, plugin state, artifact status,
  and start/stop plugins through versioned local endpoints.
- Startup timing and plugin health metrics identify slow or repeatedly failing
  plugins.
- Stable releases publish jar, checksum, update metadata, release notes, and
  signatures through CI.

## Ordered Scope

### Milestone 1: Runtime V2 Verification Backbone - Done

**Why first:** verification must exist before Runtime V2 can safely become a
loading or UI source of truth.

Status: completed in `113493715d Add Runtime V2 artifact verification`.

Requirements:

- Add a `PluginArtifactVerifier` that validates installed jar checksums before
  class loading.
- Add a signature field and signature verification extension point to
  `PluginArtifact`.
- Return checksum/signature/malformed-manifest failures as structured runtime
  status, not only logs or thrown exceptions.
- Make duplicate IDs consistently status-based at the Runtime V2 layer.
- Add tests for valid checksum, checksum mismatch, missing artifact file,
  malformed `runelite_plugin.json`, disabled plugin, incompatible version, and
  duplicate IDs.

Acceptance criteria:

- Bridge/runtime callers can distinguish loadable and blocked artifacts.
- Verification happens before class loading for artifacts with files.
- Local sideload and Microbot Hub hash policy is consistent and documented.

Implementation policy:

- Runtime V2 verifies checksum metadata before status reports mark an artifact
  loadable. A declared checksum requires a local artifact file and the file must
  match the declared SHA-256.
- Local sideloaded jars without declared checksum metadata remain loadable when
  their manifest and compatibility checks pass. If a local artifact later gains
  checksum metadata, Runtime V2 applies the same SHA-256 check used for hub
  artifacts.
- Signature metadata is carried on `PluginArtifact` and checked through the
  `PluginArtifactSignatureVerifier` extension point. The default verifier does
  not enforce a trust model until the community plugin signing model is decided.

Verification:

- `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.runtime.PluginRuntimeTest --tests net.runelite.client.plugins.runtime.PluginRepositoryTest`
- `./gradlew :client:check`

### Milestone 2: Complete Artifact Metadata Coverage - Done

**Why second:** the UI and lifecycle adapter need complete metadata across all
plugin sources.

Status: completed in the current Runtime V2 workspace slice.

Requirements:

- Add artifact-file-aware `runelite_plugin.json` stub reading for RuneLite Hub
  artifacts.
- Keep whole-jar `@PluginDescriptor` scanning as an explicit legacy
  compatibility adapter for jars without stubs.
- Represent core, RuneLite Hub, Microbot Hub, and local-directory sources
  through one Runtime V2 discovery/status API.
- Document the legacy fallback rules and sunset conditions.

Acceptance criteria:

- Runtime V2 can report metadata for all plugin sources through one API.
- Manifest-backed jars expose entry classes without arbitrary class loading.
- Legacy scanning is isolated and named as compatibility behavior.

Implementation policy:

- `PluginRuntime.discoverStatus()` is the single Runtime V2 discovery/status
  API for core, RuneLite Hub, Microbot Hub, and local-directory artifacts. Each
  status carries the artifact source and the metadata source used during
  discovery.
- Core plugins use classpath `@PluginDescriptor` metadata because they are
  already part of the client classpath.
- RuneLite Hub, Microbot Hub, and local-directory jars first read
  `runelite_plugin.json` from the artifact file when the file is available.
  Stub-backed jars expose entry classes from `PluginHubManifest.Stub.plugins`
  without loading plugin classes.
- Whole-jar `@PluginDescriptor` scanning is retained only as
  `LEGACY_PLUGIN_DESCRIPTOR_SCAN` compatibility behavior for artifact jars that
  do not contain a valid `runelite_plugin.json` stub. This adapter may load
  classes and must not be used for new artifacts.
- The legacy adapter can be removed after released RuneLite Hub, Microbot Hub,
  and documented local sideload tooling all emit `runelite_plugin.json`, and
  after Runtime V2 status telemetry shows no loadable artifact still depending
  on `LEGACY_PLUGIN_DESCRIPTOR_SCAN` for one stable release cycle.

Verification:

- `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.runtime.PluginRuntimeTest --tests net.runelite.client.plugins.runtime.PluginRepositoryTest`

### Milestone 3: Bridge V1 Contract Completion

**Why third:** the UI shell should consume a stable contract rather than bind to
Swing panels or automation endpoints.

Status: done.

Requirements:

- Add Bridge V1 endpoints for plugin install, update, and remove.
- Add plugin config schema and settings read/write endpoints.
- Add read-only logs/events or event stream for plugin state, install progress,
  and runtime health.
- Add JSON contract snapshots and generated TypeScript types.
- Add tests for token-required behavior, status shape, plugin list shape,
  artifact status shape, and plugin command responses.

Acceptance criteria:

- A non-Java UI can manage common plugin workflows through `/bridge/v1`.
- Bridge V1 remains narrower than the automation Agent Server surface.
- Contract changes are reviewable through snapshots or generated schema diffs.

### Milestone 4: Minimal TypeScript UI Shell

**Why fourth:** the shell becomes useful once status, plugin controls, and
artifact status exist.

Status: implemented in `ui-shell/` as a minimal Electron and TypeScript
workspace.

Requirements:

- Add a `ui-shell/` workspace using TypeScript.
- Choose Tauri or Electron and document the decision.
- Implement first screen:
  - client/bridge status
  - loaded plugin list
  - start/stop controls
  - plugin artifact status and error reasons
- Read the local bridge token from the documented token file or launch context.
- Keep the existing Swing UI available during migration.

Acceptance criteria:

- The shell can show plugin runtime status from the Java client.
- Starting and stopping loaded plugins works through Bridge V1.
- Artifact failures are visible with clear user-facing reasons.

Implementation policy:

- The shell is intentionally separate from the Java/Swing client. The Swing UI
  remains available while the Bridge V1-backed shell matures.
- Electron was selected for this milestone because it can read the local
  `~/.runelite/.agent-token` file from the desktop process using the existing
  Node/npm toolchain. Tauri can be reconsidered when packaging size or native
  updater integration becomes a release requirement.
- Bridge context is read from `MICROBOT_BRIDGE_URL` or `MICROBOT_HOST` /
  `MICROBOT_PORT`, and the token is read from `MICROBOT_TOKEN`, then
  `MICROBOT_TOKEN_FILE`, then `~/.runelite/.agent-token`.

Verification:

- `cd ui-shell && npm ci`
- `cd ui-shell && npm run build`
- `cd ui-shell && npm run typecheck`

### Milestone 5: Startup Timing And Plugin Health

**Why fifth:** optimization should follow measurement, and health data should
feed both Bridge V1 and UI shell surfaces.

Status: implemented in the current startup timing and plugin health workspace
slice.

Requirements:

- Add startup timing around:
  - classpath/plugin discovery
  - manifest loading
  - jar verification
  - plugin instantiation
  - config loading
  - splash stages
- Add a `PluginHealthRegistry` with exception count, slow-call count, last
  failure, and disabled/blocked reason.
- Add timing wrappers for event handlers, scheduled tasks, and overlays at the
  existing central invocation points.
- Expose health and timing read-only status through Bridge V1.

Acceptance criteria:

- Maintainers can identify slow startup stages.
- Slow or repeatedly failing plugins are visible in status output.
- Baseline captured during targeted test runs: Runtime V2 manifest loading and
  jar verification now emit per-artifact timing records, and central event,
  scheduler, and overlay calls emit per-call timings. No safe startup
  optimization was made in this slice because the available automated runs do
  not exercise a real plugin directory or full GUI startup; the measured
  bottleneck work was intentionally limited to establishing the baseline and
  read-only Bridge V1 reporting.

Verification:

- `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.health.PluginHealthRegistryTest --tests net.runelite.client.plugins.microbot.agentserver.handler.BridgeV1HandlerTest --tests net.runelite.client.plugins.runtime.PluginRuntimeTest --tests net.runelite.client.plugins.runtime.PluginRepositoryTest`

### Milestone 6: Runtime Service Boundaries

**Why sixth:** service boundaries should be introduced after the runtime and
bridge contracts show the shape of the real dependencies.

Requirements:

- Define service interfaces for:
  - plugin runtime
  - bridge API
  - script lifecycle
  - game state/cache access
  - telemetry/update checks
- Add compatibility shims for existing static `Microbot` callers.
- Document migration examples for plugin/script authors.
- Add unit tests that instantiate core services without launching the full
  client.

Acceptance criteria:

- New code can depend on injectable services instead of static `Microbot`.
- Existing scripts continue to compile and run.
- Migration guidance covers common old-to-new call patterns.

### Milestone 7: Release Distribution Hardening

**Why seventh:** checksum metadata exists, but signing and channel automation
need to be complete before public distribution is considered mature.

Requirements:

- Wire release signing into CI using documented secrets/properties.
- Publish beta and nightly update metadata from their workflows.
- Generate release notes from commit range and upstream merge points.
- Validate update metadata and checksum URLs in CI.
- Define plugin API compatibility policy per client version.

Acceptance criteria:

- Stable, beta, and nightly artifacts have channel metadata.
- Users can verify downloads through checksum and signature.
- Rollback is handled by publishing newer metadata pointing to a previous
  known-good artifact.
- Release notes are generated before GitHub release publication.

## Delivery Principles

- Keep RuneLite compatibility as the primary constraint.
- Prefer read-only/status integrations before replacing lifecycle behavior.
- Do not remove legacy scanning until Runtime V2 can load the same plugin set.
- Treat every new bridge endpoint as a versioned contract.
- Measure before optimizing.
- Commit in small slices that compile independently.

## Open Decisions

- Initial plugin signing model for community plugins.
- Whether plugin permissions/capabilities are enforceable in the first UI
  shell or declarative only.
