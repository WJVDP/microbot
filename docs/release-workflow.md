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

For changes that touch launcher, packaging, or dependency metadata, also run:

```bash
./gradlew buildAll
./gradlew :client:assemble
```

## Artifact Expectations

- The shaded client jar is produced by `:client:shadowJar`.
- Release packaging is produced by `:client:assemble`.
- Stable releases publish `microbot-<version>.jar`,
  `microbot-<version>.jar.sha256`, and `update-stable.json`.
- Generate checksum and update metadata locally with:

```bash
scripts/generate-release-metadata.sh stable <version> runelite-client/build/libs/microbot-<version>.jar https://files.microbot.cloud/releases/microbot/stable
```

- Signing is required before publishing a public release.
- Release notes must include upstream RuneLite and Microbot merge points when
  either integration branch changed since the prior release.

## Release Channels

- `stable`: built from `main` by `.github/workflows/release.yml`.
- `beta`: reserved for release candidates; publish `update-beta.json` before
  exposing to end users.
- `nightly`: built by nightly workflows and should publish
  `update-nightly.json`.

Rollback behavior is metadata-forward: publish a newer channel metadata file
that points back to the previous known-good artifact and checksum. Do not
rewrite or delete published release tags.

## Tagging

After verification passes and the release PR is approved:

```bash
git checkout main
git pull --ff-only origin main
git tag -a <version> -m "Release <version>"
git push origin <version>
```

Use a rollback release instead of rewriting or deleting published tags.
