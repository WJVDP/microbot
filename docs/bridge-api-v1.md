# Bridge API V1

Bridge API V1 is the first UI-facing local contract for a modern Microbot shell.
It intentionally reuses the Agent Server localhost transport and token guard,
but it is narrower than the automation endpoints.

## Transport And Auth

- Base path: `/bridge/v1`
- Bindings: existing Agent Server TCP or UDS binding.
- Auth header: `X-Agent-Token`
- Token source: existing Agent Server token file and config value.
- Versioning: breaking response or command changes require a new `/bridge/v2`
  path.

The bridge is for dashboard and plugin-management UI. Automation endpoints such
as widget inspection, walking, inventory actions, and dynamic script deployment
are intentionally outside this contract.

## Endpoints

### `GET /bridge/v1/status`

Returns bridge and client status.

```json
{
  "bridgeVersion": "1",
  "serverTime": "2026-05-21T00:00:00Z",
  "runeliteVersion": "1.10.0",
  "microbotVersion": "1.10.0",
  "pluginManagerAvailable": true,
  "pluginCount": 120
}
```

### `GET /bridge/v1/plugins`

Returns loaded plugin state from the Java runtime.

```json
{
  "count": 1,
  "plugins": [
    {
      "id": "net.runelite.client.plugins.example.ExamplePlugin",
      "displayName": "Example",
      "className": "net.runelite.client.plugins.example.ExamplePlugin",
      "enabled": true,
      "active": true,
      "hidden": false,
      "external": false,
      "description": "Example plugin"
    }
  ]
}
```

### `POST /bridge/v1/plugins/{id}/start`

Starts the loaded plugin with class-name `id` through `PluginManager`.

### `POST /bridge/v1/plugins/{id}/stop`

Stops the loaded plugin with class-name `id` through `PluginManager`.

Both command endpoints return the same plugin object shape as the list endpoint
plus a `changed` boolean.

### `GET /bridge/v1/plugins/{id}/config/schema`

Returns a loaded plugin's RuneLite config schema when the plugin exposes one.

```json
{
  "group": "example",
  "sections": [],
  "items": [
    {
      "key": "enabled",
      "name": "Enabled",
      "description": "Enable example setting",
      "type": "boolean",
      "position": -1,
      "hidden": false,
      "secret": false,
      "section": "",
      "warning": ""
    }
  ]
}
```

### `GET /bridge/v1/plugins/{id}/config`

Returns raw stored config values for non-secret config items. Secret values are
reported as `null`.

```json
{
  "group": "example",
  "values": {
    "enabled": "true"
  }
}
```

### `POST /bridge/v1/plugins/{id}/config`

Writes non-secret config values for a loaded plugin. The request body can be a
single key/value pair or a `values` object. Use `null` to unset a value.

```json
{
  "key": "enabled",
  "value": false
}
```

The response returns the current values plus `success` and `changed` keys.

### `GET /bridge/v1/plugin-artifacts`

Returns Plugin Runtime V2 artifact metadata and validation status for Microbot
Hub artifacts.

```json
{
  "count": 1,
  "hasErrors": false,
  "artifacts": [
    {
      "id": "example",
      "displayName": "Example",
      "version": "1.0.0",
      "source": "MICROBOT_HUB",
      "metadataSource": "HUB_MANIFEST",
      "entryClasses": ["example.ExamplePlugin"],
      "minClientVersion": "1.10.0",
      "checksumSha256": "abc123",
      "signature": null,
      "installed": true,
      "loadable": true,
      "warnings": [],
      "pluginApiVersion": 1,
      "clientPluginApiVersion": 1,
      "compatibilityPolicyAction": "allow",
      "compatibilityReasonCode": "plugin_api_compatible",
      "compatibilityReason": "Plugin API version is supported.",
      "signatureClassification": "TRUSTED_MICROBOT",
      "signaturePolicyAction": "allow",
      "signatureReasonCode": "trusted_microbot",
      "signatureReason": "Verified Microbot signature.",
      "capability_state": "normal",
      "capabilities": ["game_state.read"],
      "restricted_capabilities": [],
      "capability_policy_action": "allow",
      "capability_reason": "capabilities_ok",
      "capability_reason_message": "Plugin capability metadata is present.",
      "errors": []
    }
  ]
}
```

Compatibility status values come from Runtime V2 policy evaluation. Bridge V1
reports the plugin's declared Plugin API Compatibility Version, the client's
supported compatibility version, and concise compatibility action/reason fields.
See `docs/plugin-api-compatibility.md` for the policy and author guidance.

Signature status values come from Runtime V2 policy evaluation. Unsigned local
plugins are reported as warning-only `UNSIGNED_LOCAL`; official-channel
unsigned artifacts are reported as blocked `UNSIGNED_BLOCKED`. RuneLite Hub
artifacts use `TRUSTED_RUNELITE_HUB` to preserve external trust provenance
rather than reporting as Microbot-signed.

Capability status values are declarative first-shell policy signals. Bridge V1
reports `normal`, `missing`, `unknown`, or `restricted` and the lifecycle policy
action for install, update, and start decisions. Local-directory artifacts warn
by default for missing, unknown, or restricted capabilities; core and hub
artifacts block missing or unknown capability metadata, and block restricted
capabilities unless source policy explicitly allows them. These fields do not
claim fine-grained Java API, filesystem, process, network, or reflection
sandbox enforcement.

### `POST /bridge/v1/plugin-artifacts/{id}/install`

Queues installation of a Microbot Hub artifact through `MicrobotPluginManager`.
Optional request body:

```json
{
  "version": "1.0.0"
}
```

### `POST /bridge/v1/plugin-artifacts/{id}/update`

Queues update/redownload of a Microbot Hub artifact. Accepts the same optional
`version` body as install.

### `POST /bridge/v1/plugin-artifacts/{id}/remove`

Queues removal of an installed Microbot Hub artifact.

Install, update, and remove return an asynchronous command envelope:

```json
{
  "commandId": "6f3e9f5e-2a53-4db4-87a6-6f2b48c5740f",
  "action": "install",
  "targetType": "pluginArtifact",
  "id": "example",
  "accepted": true,
  "status": "queued",
  "version": "1.0.0"
}
```

### `GET /bridge/v1/events`

Returns a bounded, read-only event buffer for Bridge V1 plugin commands,
plugin-state changes, and config writes. This is intentionally a polling
endpoint for V1 rather than a long-lived automation channel.

```json
{
  "count": 1,
  "events": [
    {
      "id": "f2c7d0f0-f81d-4a31-8fe0-850d3c7b43a0",
      "time": "2026-05-21T00:00:00Z",
      "type": "plugin.install",
      "level": "info",
      "source": "bridge-v1",
      "pluginId": "example",
      "action": "install",
      "status": "queued",
      "message": "Plugin artifact install queued"
    }
  ]
}
```

### `GET /bridge/v1/runtime-health`

Returns a compact read-only runtime health probe for shell status views.

```json
{
  "serverTime": "2026-05-21T00:00:00Z",
  "pluginManagerAvailable": true,
  "configManagerAvailable": true,
  "pluginCount": 120,
  "pluginHealth": {
    "slowCallThresholdMs": 50,
    "count": 1,
    "plugins": [
      {
        "pluginId": "net.runelite.client.plugins.example.ExamplePlugin",
        "exceptionCount": 0,
        "slowCallCount": 1,
        "totalCallCount": 42,
        "totalDurationMs": 125,
        "maxDurationMs": 53,
        "lastOperation": "overlay-render",
        "lastFailure": null,
        "lastFailureStackTrace": null,
        "lastFailureTime": null,
        "disabledOrBlockedReason": null
      }
    ]
  },
  "startupTiming": {
    "count": 1,
    "timings": [
      {
        "time": "2026-05-21T00:00:00Z",
        "stage": "config.load",
        "detail": "user configuration",
        "durationMs": 17,
        "durationNanos": 17000000
      }
    ]
  },
  "artifactStatusAvailable": true,
  "artifactCount": 1,
  "artifactErrors": false
}
```

### `GET /bridge/v1/startup-timing`

Returns the same startup/call timing object embedded in runtime health, without
the artifact and plugin-health summary.

## Contract Artifacts

- JSON snapshots: `docs/bridge-api-v1.snapshots.json`
- TypeScript DTOs: `docs/bridge-api-v1.types.ts`
