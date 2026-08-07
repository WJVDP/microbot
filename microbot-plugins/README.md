# First-party Microbot plugins

This directory owns first-party automation plugins without mixing them into
the RuneLite fork's source tree.

Create a Plan plugin from the repository root:

```sh
./scripts/create-plan-plugin Woodcutter
./gradlew :client:compileJava
```

Production sources use the package root:

```text
src/main/java/net/runelite/client/plugins/microbot/
```

Tests and resources follow the standard Gradle layout under `src/test` and
`src/main/resources`. The `:client` project includes these source directories,
so its run, test, Checkstyle, PMD, and shaded-jar tasks cover these plugins.

Keep client integration, the hidden `MicrobotPlugin`, shared APIs, caches,
utilities, state-machine infrastructure, UI, and Agent Server code under
`runelite-client`. Use the external plugin mechanism for third-party plugins or
plugins requiring independent releases.
