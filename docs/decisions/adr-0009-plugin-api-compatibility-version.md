# ADR 0009: Plugin API Compatibility Version

- Status: Accepted (2026-05-27)

## Context

Plugin Runtime V2 already supports a narrow `minClientVersion` check, but Microbot needs a stable policy for deciding when plugin authors must adapt to client-side contract changes. Published Microbot client versions move for release, packaging, and bug-fix reasons that do not always affect plugins, while plugin compatibility needs to describe plugin-facing API, runtime, metadata, and Bridge V1 contract boundaries.

## Decision

Use a project-owned Plugin API Compatibility Version as the primary compatibility boundary for Runtime V2 plugins. The initial current value is `1`. Plugin artifacts declare a single required `pluginApiVersion` target, and `minClientVersion` remains a separate minimum build gate for plugins that need a specific Microbot client release.

The Plugin API Compatibility Version advances only for plugin-facing contract changes: `PluginDescriptor` or plugin metadata semantics, Runtime V2 discovery or lifecycle behavior, Bridge V1 plugin status contracts, supported Microbot plugin author APIs, capability or trust policy fields that affect lifecycle decisions, or removal of a documented compatibility adapter. Internal refactors, release-signing changes, packaging changes that preserve plugin loading, bug fixes, UI-only changes, and additive APIs that preserve existing behavior do not create a compatibility boundary.

Runtime V2 hard-blocks artifacts whose declared `pluginApiVersion` is newer than the client supports, for every artifact source. Older Plugin API Compatibility Versions remain supported until the client explicitly retires them; retiring an older compatibility version is itself a Plugin Compatibility Boundary. Bridge V1 exposes structured compatibility status fields rather than requiring automation to parse human-readable `errors`: `pluginApiVersion`, `clientPluginApiVersion`, `compatibilityPolicyAction`, `compatibilityReasonCode`, and `compatibilityReason`. Initial reason codes are `plugin_api_compatible`, `plugin_api_too_new`, `plugin_api_retired`, `client_version_too_old`, `plugin_api_missing`, and `plugin_api_malformed`.

During one stable release migration window, artifacts without `pluginApiVersion` are interpreted as Legacy Plugin API Compatibility version `1` and reported with a warning. After that window, hub and core Runtime V2 artifacts require explicit compatibility metadata; local-directory artifacts may continue to warn and resolve to version `1` for development ergonomics. If `pluginApiVersion` is present but malformed, Runtime V2 blocks every source because the intended compatibility contract is unknowable.

Authors declare compatibility through `@PluginDescriptor(pluginApiVersion = 1)` for core, classpath, local, and legacy annotation-discovered plugins, and through remote Microbot Hub manifest `pluginApiVersion` for hub-distributed artifacts. When both are present for a hub artifact, the remote hub manifest is authoritative because it is the distribution policy surface. RuneLite's jar-local `runelite_plugin.json` remains the entry-class stub and does not receive Microbot-specific compatibility metadata unless a later jar-local Microbot extension is needed.

Plugin author guidance must require declaring `pluginApiVersion` and `minClientVersion`, building an artifact with its entry stub, running the target Microbot client in test or local plugin mode, verifying Bridge V1 `/bridge/v1/plugin-artifacts` reports `loadable: true`, `compatibilityPolicyAction: "allow"`, and no compatibility errors, and running plugin smoke tests when behavior changed.

## Consequences

- Plugin compatibility is not tied to every published Microbot client version.
- Plugin authors declare a current compatibility target instead of predicting future min/max ranges.
- Runtime V2 and Bridge V1 get a stable policy surface for incompatibility reporting.
- Follow-up implementation should add metadata fields, validation, Bridge V1 DTO fields, migration-window warnings, author docs, and tests for compatible, too-new, missing, malformed, and minimum-client-version cases.
