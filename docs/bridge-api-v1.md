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

## Next Contract Additions

- `GET /bridge/v1/plugin-artifacts` backed by Plugin Runtime V2 discovery.
- Install, update, and remove commands backed by `MicrobotPluginManager`.
- Typed config schema for RuneLite and Microbot plugin settings.
- Read-only event stream for plugin state changes, logs, and health metrics.
