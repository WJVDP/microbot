# Fork Workflow

This document records the repository rules for the Microbot modernization fork.
It exists so upstream syncs, feature branches, and release branches are handled
the same way by every maintainer.

## Workspace Layout

In the Codex workspace used to bootstrap this fork, `/root/projects/microbotfork`
contains a mounted placeholder `.git` directory that cannot be replaced. The
real Git checkout lives in `/root/projects/microbotfork/Microbot`.

Run all Git and Gradle commands from the `Microbot` directory unless a task
explicitly says otherwise.

If the default Gradle cache location is not writable, keep Gradle state inside
the checkout:

```bash
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew :client:compileJava
```

## Remotes

Expected remotes:

```text
origin             fork repository with write access
microbot-upstream  https://github.com/chsami/Microbot
runelite-upstream  https://github.com/runelite/runelite
```

Bootstrap commands:

```bash
git remote add origin <fork-url>
git remote add microbot-upstream https://github.com/chsami/Microbot
git remote add runelite-upstream https://github.com/runelite/runelite
git remote set-url --push microbot-upstream DISABLED
git remote set-url --push runelite-upstream DISABLED
```

If the repository was cloned directly from Microbot, rename the default remote:

```bash
git remote rename origin microbot-upstream
git remote add origin <fork-url>
git remote add runelite-upstream https://github.com/runelite/runelite
```

## Branch Ownership

- `main`: stable release branch. Only merge verified release candidates.
- `integration/runelite`: RuneLite upstream merge branch. Do not build features here.
- `integration/microbot`: Microbot upstream merge branch. Do not build features here.
- `feature/plugin-runtime-v2`: plugin loading and plugin metadata work.
- `feature/ui-shell`: desktop shell, bridge, and modern UI work.

Integration branches should only receive upstream merges plus the minimum fixes
required to compile and pass smoke tests. Feature branches should rebase or merge
from integration branches after those branches are verified.

## Required Verification

Before opening or merging a PR that changes Java runtime behavior, run:

```bash
./gradlew :client:compileJava
./gradlew :client:runUnitTests
./gradlew :client:runUnitTests --tests net.runelite.client.plugins.microbot.threadsafety.ClientThreadGuardrailTest --tests net.runelite.client.plugins.microbot.threadsafety.QueryableTerminalGuardrailTest
./gradlew :client:runUnitTests --tests net.runelite.client.plugins.PluginManagerTest.testLoadPlugins
./gradlew :client:shadowJar
```

CI runs the same baseline through `ci/build.sh`.

## Upstream Sync Rules

1. Fetch the upstream remote.
2. Merge upstream into the matching integration branch.
3. Resolve conflicts without unrelated refactors.
4. Run the required verification commands.
5. Merge the integration branch into feature branches only after it is green.
6. Merge to `main` only through a reviewed PR.

Use `docs/runelite-update-playbook.md` for RuneLite-specific version bumps once
that playbook is added.
