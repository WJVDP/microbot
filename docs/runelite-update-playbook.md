# RuneLite Update Playbook

This playbook describes how to merge RuneLite upstream changes into the
Microbot fork without mixing version updates with feature work.

## Scope

Use this process when updating from `runelite-upstream/master`. Do not merge
RuneLite upstream directly into `main` or into feature branches.

Expected remotes:

```text
origin             writable Microbot fork
runelite-upstream  https://github.com/runelite/runelite
microbot-upstream  https://github.com/chsami/Microbot
```

## Preparation

Start from a clean working tree:

```bash
git status --short
git fetch origin
git fetch runelite-upstream master
```

Create a report before merging:

```bash
scripts/update-runelite-report.sh runelite-upstream master integration/runelite
```

Review the report for:

- RuneLite API changes.
- RuneLite client plugin changes.
- Dependency and Gradle metadata changes.
- Client lifecycle, config, event bus, overlay, and injected-client-sensitive changes.
- Microbot files that may depend on changed RuneLite behavior.

## Merge Flow

Use `integration/runelite` as the only branch that receives RuneLite upstream
merges:

```bash
git checkout integration/runelite
git pull --ff-only origin integration/runelite
git merge --no-ff runelite-upstream/master
```

Resolve conflicts with the smallest compatible change. Avoid unrelated cleanup,
renames, formatting, or Microbot feature work in the same merge.

After conflict resolution:

```bash
git status --short
git diff --check
```

If generated resources or version metadata changed upstream, regenerate or
update them in the same branch and document the command used in the PR.

## Conflict Review Checklist

Inspect these areas after every RuneLite merge, even when Git reports no
conflicts:

- `runelite-api`
- `runelite-client/src/main/java/net/runelite/client/plugins`
- `runelite-client/src/main/java/net/runelite/client/config`
- `runelite-client/src/main/java/net/runelite/client/eventbus`
- `runelite-client/src/main/java/net/runelite/client/ui/overlay`
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot`
- `libs.versions.toml`
- `gradle.properties`
- `settings.gradle.kts`
- `build.gradle.kts`

Pay special attention to API signature changes, plugin lifecycle changes,
event thread assumptions, dependency upgrades, and resource paths used by the
client bootstrap.

## Required Verification

Run the same baseline required by the fork workflow:

```bash
./gradlew :client:compileJava
./gradlew :client:runUnitTests
./gradlew :client:runUnitTests --tests net.runelite.client.plugins.microbot.threadsafety.ClientThreadGuardrailTest --tests net.runelite.client.plugins.microbot.threadsafety.QueryableTerminalGuardrailTest
./gradlew :client:runUnitTests --tests net.runelite.client.plugins.PluginManagerTest.testLoadPlugins
./gradlew :client:shadowJar
```

If the default Gradle cache is not writable:

```bash
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew :client:compileJava
```

For launcher, packaging, dependency, or resource changes, also run:

```bash
./gradlew buildAll
./gradlew :client:assemble
```

## PR Rules

Open the PR from `integration/runelite` into `main` only after verification
passes. The PR description should include:

- RuneLite upstream commit range.
- Summary of changed API, client, plugin, and dependency areas.
- Conflict files and resolution notes.
- Verification commands and results.
- Follow-up work that belongs on feature branches.

Do not merge `integration/runelite` into `main` until the PR is reviewed and
green.

## Updating Feature Branches

After the RuneLite integration PR lands in `main`, update feature branches from
the verified integration point:

```bash
git checkout feature/plugin-runtime-v2
git merge main

git checkout feature/ui-shell
git merge main
```

Resolve feature-branch conflicts in those branches. Do not push feature fixes
back into `integration/runelite` unless they are required for RuneLite
compatibility and verification.
