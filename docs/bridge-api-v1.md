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
      "errors": []
    }
  ]
}
```

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
  "artifactStatusAvailable": true,
  "artifactCount": 1,
  "artifactErrors": false
}
```

## Contract Artifacts

- JSON snapshots: `docs/bridge-api-v1.snapshots.json`
- TypeScript DTOs: `docs/bridge-api-v1.types.ts`
