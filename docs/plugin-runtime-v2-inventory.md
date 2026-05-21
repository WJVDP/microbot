# Plugin Runtime V2 Inventory

This document records the current plugin loading paths before introducing a
unified plugin runtime. It is intentionally descriptive: the first migration
step is to make every existing source explicit before changing behavior.

## Startup Sequence

Main startup currently loads plugins in `RuneLite.start()`:

1. `pluginManager.loadCorePlugins()`
2. `pluginManager.loadSideLoadPlugins()`
3. `externalPluginManager.loadExternalPlugins()`
4. `microbotPluginManager.loadSideLoadPlugins()`
5. `pluginManager.loadDefaultPluginConfiguration(null)`
6. `pluginManager.startPlugins()`

The `ExternalPluginManager` and `MicrobotPluginManager` are registered on the
event bus after plugin discovery and before `startPlugins()`.

## Current Loading Paths

| Source | Owner | Artifact Source | Discovery | Verification | Class Loading | Lifecycle |
| --- | --- | --- | --- | --- | --- | --- |
| Core RuneLite plugins | `PluginManager` | Client classpath | `ClassPath.from(...).getTopLevelClassesRecursive("net.runelite.client.plugins")` | Annotation and dependency checks only | Application classloader | `PluginManager.loadPlugins`, `startPlugin`, `stopPlugin` |
| Legacy sideloaded jars | `PluginManager` | `${RUNELITE_DIR}/sideloaded-plugins/*.jar` | Whole-jar classpath scan | None beyond annotation checks | `PluginClassLoader` | `PluginManager.loadPlugins`; started later by `startPlugins()` |
| RuneLite Plugin Hub | `ExternalPluginManager` | `RuneLite.PLUGINS_DIR`, downloaded from RuneLite Plugin Hub | Remote lite/full manifests plus jar `runelite_plugin.json` stub | SHA-256 hash from manifest; manifest download verification handled by `ExternalPluginClient` | `PluginHubClassLoader` | Delegates to `PluginManager.loadPlugins`; can start immediately outside startup |
| Built-in external plugins | `ExternalPluginManager` | Classes registered with `ExternalPluginManager.loadBuiltin(...)` | Explicit class array | None beyond annotation checks | Application classloader | Delegates to `PluginManager.loadPlugins` |
| Microbot core plugins | `MicrobotPluginManager` | Client classpath under `net.runelite.client.plugins.microbot` | Whole-classpath scan filtered by package and annotations | Annotation checks, `minClientVersion`, disabled flag | Application classloader | Custom copy of `loadPlugins`, then registers with `PluginManager` |
| Microbot hub plugins | `MicrobotPluginManager` | `${RUNELITE_DIR}/microbot-plugins/<internalName>.jar`, downloaded from Microbot Hub/GitHub releases | Remote `plugins.json` plus whole-jar scan | SHA-256 from current or stored manifest version; disabled flag; HTTPS URL check | `PluginJarClassLoader` | Custom copy of `loadPlugins`; can start immediately outside startup |
| Microbot sideloaded plugins | `MicrobotPluginManager` | `${RUNELITE_DIR}/microbot-plugins/*.jar` | Whole-jar scan | Hash check only when matching manifest/version data exists | `PluginJarClassLoader` | Custom copy of `loadPlugins`; posts `ExternalPluginsChanged` |

## Shared Runtime Behavior

`PluginManager.loadPlugins(...)` is the canonical RuneLite path for:

- Filtering classes with `@PluginDescriptor`.
- Rejecting annotated classes that do not directly extend `Plugin`.
- Applying safe mode through `PluginDescriptor.loadInSafeMode()`.
- Building the `@PluginDependency` graph.
- Rejecting dependency cycles.
- Instantiating plugins and Guice child injectors.
- Tracking loaded plugins in `plugins`.

`PluginManager.startPlugin(...)` and `stopPlugin(...)` are the canonical
lifecycle methods. They run on the Swing EDT, manage conflicts, call
`startUp()`/`shutDown()`, register or unregister the event bus, update scheduled
methods, and post `PluginChanged`.

`MicrobotPluginManager` duplicates substantial portions of this behavior:

- `loadPlugins(...)`
- dependency graph sorting
- plugin instantiation
- Guice child injector creation
- compatibility filtering
- disabled-plugin filtering

That duplication is the main consolidation target for Plugin Runtime V2.

## Current Metadata Shapes

RuneLite Plugin Hub:

- Remote manifest: `PluginHubManifest.ManifestLite` and `ManifestFull`.
- Jar identity: `PluginHubManifest.JarData`.
- Display metadata: `PluginHubManifest.DisplayData`.
- Jar-local stub: `runelite_plugin.json`, parsed as `PluginHubManifest.Stub`.
- Entry classes: `Stub.plugins`.

Microbot Hub:

- Remote manifest: `MicrobotPluginManifest`.
- Important fields: `internalName`, `displayName`, `version`, `artifactId`,
  `downloadUrl`, `releaseTag`, `minClientVersion`, `sha256`, `url`, `disable`,
  `tags`, and `availableVersions`.
- Accepted migration target: jar-local `runelite_plugin.json`, parsed as
  `PluginHubManifest.Stub`.
- Entry classes: `Stub.plugins`.
- Existing jars without `runelite_plugin.json` are scanned and classes are
  filtered by `@PluginDescriptor` through the legacy compatibility path.

Runtime V2 now records the metadata path on each artifact:

- `CORE_ANNOTATION` for core classpath plugins.
- `HUB_MANIFEST` for hub records that do not have an installed artifact file
  available yet.
- `JAR_STUB` for artifact jars with `runelite_plugin.json`.
- `LEGACY_PLUGIN_DESCRIPTOR_SCAN` for installed jars without a valid stub that
  still need the whole-jar annotation compatibility adapter.
- `FILE_NAME` for local jars that have no stub and no descriptor-backed entry
  classes.

Core and sideloaded plugins:

- Metadata comes from Java annotations, primarily `@PluginDescriptor` and
  `@PluginDependency`.
- Legacy sideloaded jars do not have a manifest-based metadata path.

## Current Compatibility And Trust Checks

Implemented today:

- Safe mode filtering for core/RuneLite plugin manager loading.
- RuneLite Plugin Hub jar hash verification before loading.
- Microbot Hub SHA-256 verification where manifest/version metadata exists.
- Microbot plugin `minClientVersion` check using `Rs2UiHelper.isClientVersionCompatible(...)`.
- Microbot disabled-plugin checks through `MicrobotPluginManifest.disable` and
  `PluginDescriptor.disable()`.
- Dependency cycle rejection.

Gaps:

- No unified plugin id model.
- No single repository abstraction.
- No single artifact metadata shape.
- Existing Microbot Hub jars do not consistently declare entry classes outside
  loaded bytecode.
- Legacy sideloaded jars have no checksum/signature metadata.
- Classpath and jar scanning loads classes before metadata is fully known.
- Duplicate plugin detection differs between managers.
- User-facing failure reasons are mostly logs, not structured status.
- Plugin unloading is partial; stale scheduler/event-bus/classloader cleanup is
  called out as a TODO in the RuneLite external plugin path.

## Proposed Repository Mapping

| Plugin Runtime V2 repository | Current implementation |
| --- | --- |
| `CoreRepository` | `PluginManager.loadCorePlugins()` and Microbot core scan |
| `RuneLiteHubRepository` | `ExternalPluginManager` plus `ExternalPluginClient` |
| `MicrobotHubRepository` | `MicrobotPluginManager` plus `MicrobotPluginClient` |
| `LocalDirectoryRepository` | `${RUNELITE_DIR}/sideloaded-plugins` and `${RUNELITE_DIR}/microbot-plugins` |

## First Migration Slice

1. Introduce read-only `PluginArtifact` and `PluginRepository` types without
   changing startup behavior.
2. Add adapters that describe existing sources as artifacts:
   `CoreRepository`, `RuneLiteHubRepository`, `MicrobotHubRepository`, and
   `LocalDirectoryRepository`.
3. Add tests for metadata-only discovery with no plugin class instantiation for
   sources that already have manifest data.
4. Keep `PluginManager.loadPlugins(...)` as the lifecycle backend until the
   repository and artifact model is covered by tests.
5. Move Microbot compatibility/disabled checks out of the duplicate loader and
   into a shared validation step.

## Milestone 2 Fallback Rules

Runtime V2 discovery uses this order for artifact jars:

1. Read `runelite_plugin.json` from the installed artifact file.
2. If the stub is absent and the jar is otherwise readable, use the named
   legacy descriptor scanner to find `Plugin` subclasses annotated with
   `@PluginDescriptor`.
3. If neither path finds entry classes, keep the artifact in status output and
   let validation report `Plugin entry classes are missing`.

Malformed `runelite_plugin.json` does not fall through to descriptor scanning;
it remains a verifier error so bad metadata is visible to Bridge/runtime
callers. The descriptor scanner is sunset once all supported build and sideload
paths emit `runelite_plugin.json` and one stable release cycle reports no
loadable `LEGACY_PLUGIN_DESCRIPTOR_SCAN` artifacts.

## Open Design Questions

- Should `PluginArtifact.id` use manifest `internalName`, class simple name, or
  a normalized `source:id` pair during migration?
- How strict should local sideload validation be before the compatibility
  adapter exists?
- Should checksum/signature failure prevent all class loading, including local
  sideloads, or only remote hub artifacts?
- What structured status object should power the Swing UI now and the future
  TypeScript bridge later?
