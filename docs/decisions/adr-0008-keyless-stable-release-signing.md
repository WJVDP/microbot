# ADR 0008: Keyless Stable Release Signing

- Status: Accepted (2026-05-26)

## Context

Public Microbot client releases need artifact authenticity before CI publishes signed artifacts. The project already publishes SHA-256 checksums and channel metadata, but checksum verification only proves file integrity; it does not prove the artifact came from the expected release workflow.

## Decision

Use keyless Sigstore/cosign signing for stable release artifacts produced from `main`. The stable release workflow signs the shaded client jar with GitHub Actions OIDC identity and publishes detached signature and verification material beside the jar, checksum, and update metadata.

Initial release signing applies only to the stable channel. Beta and nightly artifacts must not inherit the stable signing policy until their channel ownership and publication workflow expectations are decided.

## Consequences

- CI does not store a long-lived production release-signing private key in repository secrets.
- The stable release workflow needs `contents: write` for release publication and `id-token: write` for keyless signing.
- No production `RELEASE_SIGNING_KEY`, GPG private key, or signing passphrase secret is used for stable release signing.
- Public verification requires both the SHA-256 checksum for integrity and the cosign signature for workflow provenance.
- Verification docs should pin expected GitHub repository and workflow identity rather than asking users to establish GPG trust.
- Local dry runs may use unsigned artifacts or test-only cosign keys, but production release signatures come only from the stable CI workflow.
- Release metadata should eventually expose signature URLs, but metadata schema
  changes are implementation work outside this policy decision.
- There is no long-lived release-signing key to rotate. Recovery focuses on
  disabling or repairing the trusted GitHub repository/workflow identity and
  publishing a superseding stable release from a restored trusted workflow.
