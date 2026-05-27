# Release Workflow

This is the initial release checklist for the modernization fork. It is scoped
to Phase 1 hygiene and should be expanded as release automation matures.

## Release Branches

Create release candidates from `main` only:

```bash
git checkout main
git pull --ff-only origin main
git checkout -b release/<version>
```

Do not release directly from `integration/*` or long-running feature branches.

## Verification

Run the baseline checks before tagging:

```bash
./gradlew :client:compileJava
./gradlew :client:runUnitTests
./gradlew :client:runUnitTests --tests net.runelite.client.plugins.microbot.threadsafety.ClientThreadGuardrailTest --tests net.runelite.client.plugins.microbot.threadsafety.QueryableTerminalGuardrailTest
./gradlew :client:runUnitTests --tests net.runelite.client.plugins.PluginManagerTest.testLoadPlugins
./gradlew :client:shadowJar
```

The current supported Plugin API Compatibility Version is `1`. Before tagging,
confirm release notes and plugin author guidance still point to
`docs/plugin-api-compatibility.md` when plugin-facing Runtime V2, Bridge V1,
metadata, or supported author API contracts changed.

For changes that touch launcher, packaging, or dependency metadata, also run:

```bash
./gradlew buildAll
./gradlew :client:assemble
```

## Artifact Expectations

- The shaded client jar is produced by `:client:shadowJar`.
- Release packaging is produced by `:client:assemble`.
- Stable releases publish `microbot-<version>.jar`,
  `microbot-<version>.jar.sha256`, `microbot-<version>.jar.bundle`, cosign
  verification material, and `update-stable.json`.
- Generate checksum and update metadata locally with:

```bash
scripts/generate-release-metadata.sh stable <version> runelite-client/build/libs/microbot-<version>.jar https://files.microbot.cloud/releases/microbot/stable
```

- Signing is required before publishing a public release.
- Stable release signing uses keyless Sigstore/cosign signing from GitHub
  Actions OIDC identity. See
  `docs/decisions/adr-0008-keyless-stable-release-signing.md`.
- The stable release workflow requires `contents: write` and `id-token: write`
  permissions. No production release-signing private key, GPG key, or signing
  passphrase is stored in repository secrets.
- Existing deploy and API secrets, such as `PROD_SSH_KEY`, `API_EMAIL`, and
  `API_PASSWORD`, are not signing secrets.
- Release notes are generated before GitHub release publication. They must
  include upstream RuneLite and Microbot merge points when either integration
  branch changed since the prior release.

## Release Notes

Generate a local draft from the previous stable tag to the current release
commit with:

```bash
scripts/generate-release-notes.sh --version <version> --output build/release-notes-<version>.md
```

The script automatically detects the previous tag before the current commit.
For rehearsals or backfills, pass explicit refs:

```bash
scripts/generate-release-notes.sh \
  --previous-tag <previous-version> \
  --current <release-commit> \
  --version <version> \
  --output build/release-notes-<version>.md
```

The generated markdown lists the commit range, fork changes, upstream RuneLite
merge updates, and upstream Microbot merge updates. Edit the draft before final
publication if a merge subject needs clearer maintainer-facing wording.

The stable release workflow writes the same notes file and creates a draft
GitHub Release with `gh release create --draft --notes-file`. Maintainers review
and edit that draft release body in GitHub, then publish the release manually
after the artifacts and release notes are correct.

## Local Signing Dry Runs

Local release rehearsal should generate the jar, SHA-256 file, and update
metadata. It must not create a production-trusted release signature.

Developers may use a clearly test-only cosign key to exercise signature-file
plumbing locally:

```bash
COSIGN_KEY=./dev/cosign-test.key cosign sign-blob --key "$COSIGN_KEY" runelite-client/build/libs/microbot-<version>.jar
```

Any locally generated signature is test material only. Production release
signatures come from the stable CI workflow.

## Public Verification

Users and release validation jobs should verify both:

- the SHA-256 checksum for artifact integrity
- the cosign signature for stable workflow provenance

Download `microbot-<version>.jar`, `microbot-<version>.jar.sha256`, and
`microbot-<version>.jar.bundle` from the same stable release location, then run:

```bash
sha256sum -c microbot-<version>.jar.sha256
cosign verify-blob \
  --bundle microbot-<version>.jar.bundle \
  --certificate-identity "https://github.com/WJVDP/microbot/.github/workflows/release.yml@refs/heads/main" \
  --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
  microbot-<version>.jar
```

Signature URLs may be added to `update-stable.json` by a later metadata schema
change. Until then, publish the signature and verification material beside the
jar and checksum.

## Signing Recovery

There is no long-lived production release-signing key to rotate. If the stable
release workflow or repository identity is compromised, disable the workflow or
affected GitHub access, investigate, and publish a superseding stable release
from a restored trusted workflow.

If a bad artifact was already published, mark the GitHub release as compromised
where practical and use metadata-forward rollback or a fixed stable release.
Do not silently rewrite or delete published release tags.

## Release Channels

- `stable`: built from `main` by `.github/workflows/release.yml`.
- `beta`: reserved for release candidates; publish `microbot-beta-<version>.jar`,
  its SHA-256 file, and `update-beta.json` from an explicit beta workflow before
  exposing to end users. The GitHub `beta` release tag is a moving channel tag.
- `nightly`: built by nightly workflows and should publish
  `microbot-nightly-<version>.jar`, its SHA-256 file, and
  `update-nightly.json`. The GitHub `nightly` release tag is a moving channel
  tag.

Rollback behavior is metadata-forward: publish a newer channel metadata file
that points back to the previous known-good artifact and checksum. Do not
rewrite or delete immutable stable release tags. Moving beta/nightly channel
tags may be advanced by their workflows to represent the current channel head.

## Tagging

After verification passes and the release PR is approved:

```bash
git checkout main
git pull --ff-only origin main
git tag -a <version> -m "Release <version>"
git push origin <version>
```

Use a rollback release instead of rewriting or deleting published tags.
