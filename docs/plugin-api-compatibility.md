# Plugin API Compatibility

Plugin API compatibility defines which plugin-facing Microbot client contracts a plugin targets. It is separate from the published Microbot client version and from plugin artifact trust or signing.

See `docs/decisions/adr-0009-plugin-api-compatibility-version.md` for the accepted policy decision.

## Metadata

Runtime V2 plugins declare:

- `pluginApiVersion`: the Plugin API Compatibility Version the plugin targets.
- `minClientVersion`: the minimum published Microbot client version required by the plugin.

The initial current Plugin API Compatibility Version is `1`.

Plugin authors declare compatibility in the author-facing metadata surface for their distribution path:

- `@PluginDescriptor(pluginApiVersion = 1)` for core, classpath, local, and legacy annotation-discovered plugins.
- Remote Microbot Hub manifest `pluginApiVersion` for Microbot Hub artifacts.

When both annotation metadata and hub manifest metadata exist for a hub artifact, the remote hub manifest is authoritative because it is the distribution policy surface.

RuneLite's jar-local `runelite_plugin.json` remains the entry-class stub. Do not add Microbot-specific compatibility metadata there unless a future Microbot jar-local extension is explicitly adopted.

## Compatibility Rules

Runtime V2 accepts a plugin when:

- `pluginApiVersion` is present and well-formed.
- `pluginApiVersion` is less than or equal to the client's supported Plugin API Compatibility Version.
- The declared Plugin API Compatibility Version has not been explicitly retired.
- `minClientVersion` is absent or less than or equal to the running Microbot client version.

Runtime V2 blocks every source, including local-directory plugins, when:

- `pluginApiVersion` is newer than the client supports.
- `pluginApiVersion` is explicitly retired.
- `pluginApiVersion` is present but malformed.
- `minClientVersion` is newer than the running client version.

During the one-stable-release migration window, artifacts without `pluginApiVersion` are interpreted as Legacy Plugin API Compatibility version `1` and reported with a warning. After that window, hub and core Runtime V2 artifacts must declare `pluginApiVersion`; local-directory artifacts may continue to warn and resolve to version `1` for development ergonomics.

## Compatibility Boundaries

Increment the Plugin API Compatibility Version only for plugin-facing contract changes, including:

- `PluginDescriptor` or plugin metadata semantics.
- Runtime V2 discovery, validation, or lifecycle behavior that plugin authors must adapt to.
- Bridge V1 plugin status contracts.
- Supported Microbot plugin author APIs under `microbot/api` or documented supported `microbot/util` surfaces.
- Capability or trust policy fields that affect install, update, start, or load decisions.
- Removal of a documented compatibility adapter.

Do not increment the Plugin API Compatibility Version for:

- Internal refactors.
- Bug fixes that preserve plugin-facing behavior.
- UI-only changes.
- Release signing or release workflow changes.
- Packaging changes that preserve plugin loading.
- Additive APIs that do not change existing behavior.

Older Plugin API Compatibility Versions remain supported until explicitly retired. Retiring an older compatibility version is itself a Plugin Compatibility Boundary and must be documented in release notes or the policy doc.

## Bridge V1 Reporting

Bridge V1 plugin artifact status should expose structured compatibility fields:

```json
{
  "pluginApiVersion": 1,
  "clientPluginApiVersion": 1,
  "compatibilityPolicyAction": "allow",
  "compatibilityReasonCode": "plugin_api_compatible",
  "compatibilityReason": "Plugin API version is supported."
}
```

Initial compatibility reason codes:

| Code | Meaning |
| --- | --- |
| `plugin_api_compatible` | Plugin API version is supported. |
| `plugin_api_too_new` | Plugin targets a newer Plugin API Compatibility Version than this client supports. |
| `plugin_api_retired` | Plugin targets a Plugin API Compatibility Version this client no longer supports. |
| `client_version_too_old` | Plugin requires a newer published Microbot client version. |
| `plugin_api_missing` | Plugin does not declare `pluginApiVersion`. |
| `plugin_api_malformed` | Plugin declared `pluginApiVersion`, but the value could not be parsed. |

Bridge V1 should keep `errors` and `warnings` as human-readable aggregate lists, but UI and automation should consume the structured compatibility fields.

## Author Testing

Before publishing a plugin for a compatibility boundary:

1. Declare `pluginApiVersion` and `minClientVersion`.
2. Build the plugin artifact with its `runelite_plugin.json` entry stub.
3. Run the target Microbot client in test mode or local plugin directory mode.
4. Verify `GET /bridge/v1/plugin-artifacts` reports `loadable: true`, `compatibilityPolicyAction: "allow"`, and no compatibility errors.
5. For behavior changes, run the plugin's smoke test against the target Microbot client version.
