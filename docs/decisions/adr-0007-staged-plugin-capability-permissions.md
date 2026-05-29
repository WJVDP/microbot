# ADR 0007: Staged Plugin Capability Permissions

- Status: Accepted (2026-05-26)

## Context

The first TypeScript UI shell and Bridge V1 need plugin capability information,
but Runtime V2 does not yet provide reliable fine-grained enforcement for every
Java API, filesystem, process, network, or reflection operation. The project
needs a permission model that makes capabilities visible without claiming a
sandbox boundary that does not exist.

## Decision

Use a staged permission model. Capability metadata is required for Runtime V2
plugins and is displayed by the TypeScript shell, but first-shell enforcement is
limited to source-aware lifecycle gates that Runtime V2 and Bridge V1 can
reliably control: install, update, and start decisions. Stop and remove are
always allowed.

Each plugin exposes a capabilities manifest with:

- `id`: stable plugin id.
- `name`: display name.
- `version`: plugin version.
- `source`: `core`, `runelite_hub`, `microbot_hub`, or `local_directory`.
- `capabilities`: requested capability ids.
- `capabilityRationale`: optional map from capability id to short reason text.
- `permissionSchemaVersion`: manifest schema version.
- `declaredAtBuildTime`: whether metadata came from the packaged artifact.

The initial capability vocabulary is deliberately coarse and product-facing:

- `game_state.read`
- `game_input.control`
- `movement.control`
- `inventory.control`
- `combat.control`
- `network.local`
- `network.remote`
- `filesystem.read`
- `filesystem.write`
- `process.launch`
- `credentials.access`
- `settings.modify`

The UI shell classifies capability state as:

- `normal`: manifest exists, schema is supported, capabilities are known, and
  none are restricted by local policy.
- `missing`: plugin has no capability manifest.
- `unknown`: manifest contains an unsupported schema or unknown capability ids.
- `restricted`: plugin requests one or more capabilities restricted by policy.

Bridge V1 exposes:

- `capability_state`: `normal`, `missing`, `unknown`, or `restricted`.
- `capabilities`: declared capability ids.
- `restricted_capabilities`: capability ids restricted by local policy.
- `capability_policy_action`: `allow`, `warn`, or `block`.
- `capability_reason`: concise reason code for UI and logs.

Bridge V1 reason codes are:

| Code | Message |
| --- | --- |
| `capabilities_ok` | Plugin capability metadata is present. |
| `capabilities_missing` | Plugin does not declare capabilities. |
| `capabilities_unknown` | Plugin declares capabilities this client does not recognize. |
| `capabilities_restricted` | Plugin requests restricted capabilities. |
| `capabilities_local_warning` | Allowed because this is a local development plugin. |
| `capabilities_blocked_for_source` | This plugin source requires valid capability metadata. |

The shell uses "requests access to" language for declarative capabilities and
does not imply sandbox enforcement unless Runtime V2 blocks the operation at the
call site.

## Consequences

- Local-directory plugins may start, install, or update with missing, unknown,
  or restricted capability metadata by default, but must show warning status.
- Core and hub plugins require valid capability metadata. Missing or unknown
  capability metadata blocks install, update, or start until fixed.
- Core and hub restricted capabilities block unless source policy explicitly
  allows them.
- Capability metadata becomes review and future-policy input even when no
  fine-grained runtime enforcement point exists yet.
- Follow-up implementation should define the manifest schema, add parsing and
  validation, extend Bridge V1 fields, implement source-aware lifecycle policy,
  display UI warning/block states, support local warning-only behavior and an
  optional local dev override, add source/state/operation tests, and document
  the staged enforcement boundary.
