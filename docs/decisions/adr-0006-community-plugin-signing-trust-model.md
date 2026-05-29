# ADR 0006: Community Plugin Signing Trust Model

- Status: Accepted (2026-05-26)

## Context

Plugin Runtime V2 carries plugin signature metadata and verifies it through an
extension point, but the project needs a source-aware policy before signatures
can drive loading decisions or Bridge V1 status. The policy must distinguish
official distribution paths from local development so that hub/core artifacts
are verifiable while sideloaded local plugins remain ergonomic.

## Decision

Use a developer-ergonomic trust model: official distribution paths are enforced,
and local-directory sideloading is allowed by default when unsigned and clearly
reported as a development state.

Core Microbot and Microbot Hub artifacts require a valid signature from a
pinned or Microbot-approved signing authority. RuneLite Hub artifacts are
trusted through RuneLite Hub provenance rather than treated as Microbot-signed
artifacts. Runtime V2 and Bridge V1 must preserve that distinction in status.

Local-directory artifacts may load unsigned by default with warning status. A
bad signature is stricter than no signature: unknown signers, invalid
signatures, and malformed signature metadata are blocked by default, with a
narrow explicit local developer override available only for local-directory
artifacts.

Runtime V2 classifies signature state with explicit outcomes:

- `TRUSTED_MICROBOT`: valid signature from a pinned Microbot authority.
- `TRUSTED_RUNELITE_HUB`: accepted through RuneLite Hub provenance.
- `UNSIGNED_LOCAL`: unsigned local-directory artifact, allowed with warning.
- `UNKNOWN_SIGNER`: structurally valid signature from an untrusted signer.
- `INVALID_SIGNATURE`: signature exists but verification fails.
- `MALFORMED_SIGNATURE`: signature metadata cannot be parsed or is inconsistent.
- `UNSIGNED_BLOCKED`: unsigned artifact from a source that requires signing.

Bridge V1 exposes concise reason codes and messages:

| Code | Message |
| --- | --- |
| `trusted_microbot` | Verified Microbot signature. |
| `trusted_runelite_hub` | Loaded through RuneLite Hub trust path. |
| `unsigned_local` | Unsigned local plugin. Allowed for development. |
| `unsigned_blocked` | Unsigned plugin is not allowed from this source. |
| `unknown_signer` | Plugin was signed by an untrusted signer. |
| `invalid_signature` | Plugin signature did not match the artifact. |
| `malformed_signature` | Plugin signature metadata could not be read. |
| `dev_override` | Loaded by explicit local developer override. |

## Consequences

- Core and Microbot Hub artifacts block unsigned, unknown-signer, invalid, and
  malformed signature states.
- RuneLite Hub artifacts report external trust provenance and block malformed
  or failed signatures when signature metadata is present.
- Local unsigned plugins remain loadable and visible as warnings.
- Local invalid, malformed, or unknown-signer artifacts are blocked unless an
  explicit local developer override is active.
- Follow-up implementation should add Runtime V2 signature classification,
  source-aware policy evaluation, Bridge V1 reason fields, official-channel
  blocking, local unsigned warnings, local-only developer override, and tests
  for each source and signature-state combination.
