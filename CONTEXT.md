# Microbot

Microbot is a RuneLite fork that distributes a shaded client jar and hosts automation-oriented plugin/script surfaces. This glossary keeps release, runtime, and plugin trust language distinct.

## Language

**Release Signing**:
Authenticity signing for public Microbot client release artifacts, especially the shaded client jar. It proves where a published artifact came from and is separate from Java/JAR code signing and Runtime V2 plugin trust.
_Avoid_: Java code signing, plugin signing, jar signing

**Release Verification**:
The user or CI check that combines a SHA-256 checksum for artifact integrity with a release signature for workflow provenance. A checksum alone does not prove who produced the artifact.
_Avoid_: Checksum-only verification, signature-only verification

**Stable Release**:
The public Microbot client release channel built from `main` and intended for end users. Stable releases are the only release artifacts covered by the initial release-signing policy.
_Avoid_: Beta, nightly, prerelease

**Beta Release**:
A release-candidate channel for builds intended to be tested before they become stable. Beta artifacts use beta-specific names and are published through an explicit beta workflow rather than automatic stable or nightly publication.
_Avoid_: Stable, nightly

**Nightly Release**:
A frequently refreshed development-channel build for early testing from the development line. Nightly artifacts use nightly-specific names and do not inherit stable release signing until nightly ownership and signing expectations are decided.
_Avoid_: Stable, beta

**Runtime V2 Plugin Trust**:
The source-aware trust policy used by Plugin Runtime V2 to classify plugin artifacts by signature state, provenance, and source. It governs plugin artifact status and lifecycle decisions, not the public client release artifact.
_Avoid_: Release signing, CI release signing

## Example Dialogue

Dev: "Do we need release signing before publishing the stable jar?"

Maintainer: "Yes. Release signing proves the published Microbot client artifact came from our release workflow."

Dev: "Does that make Runtime V2 plugins trusted?"

Maintainer: "No. Runtime V2 plugin trust is a separate policy for plugin artifacts and their sources."

Dev: "Can users just compare the SHA-256 file?"

Maintainer: "They should check the SHA-256 for integrity and the release signature for workflow provenance."

Dev: "Should nightly artifacts use the same release signature?"

Maintainer: "Not in the initial policy. Stable releases are signed first; beta and nightly need their own channel ownership decision before signing is enabled."

Dev: "Can beta and nightly still publish update metadata?"

Maintainer: "Yes. Channel metadata and checksums can ship before beta/nightly signing is decided."

Dev: "Should prerelease jars share the same filename as stable?"

Maintainer: "No. Beta and nightly artifacts should use channel-specific names so metadata and hosted files are not ambiguous."

Dev: "Where does a beta build come from?"

Maintainer: "From an explicit beta workflow for a chosen release-candidate ref, not from nightly automation."
