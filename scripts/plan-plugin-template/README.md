# Plan plugin template

Run from repository root:

```sh
./scripts/create-plan-plugin Woodcutter
./gradlew :client:compileJava
```

This creates package `microbot.planwoodcutter` with:

- `PlanWoodcutterPlugin`: RuneLite lifecycle, dependency wiring, and local
  control-center opt-in as `plan-woodcutter`.
- `PlanWoodcutterConfig`: loop timing and future user settings.
- `PlanWoodcutterScript`: background state-machine loop.

Naming convention: every planned plugin class starts with `Plan<PluginName>`.
Its descriptor uses `PluginDescriptor.Plan`, rendering green `[Plan]` in the
Microbot plugin list.

## Control-center hook

The generated plugin class includes:

```java
@ControlCenterPlugin(id = "plan-woodcutter")
```

This marker is required for the local dashboard to discover, start, and stop
the plugin. Keep the id stable after release; it must be unique and lowercase
kebab-case. The scaffold converts PascalCase names to that format, including
names such as `MotherlodeMine` (`plan-motherlode-mine`).

No extra hook is needed for the generated `StateMachineScript`: the dashboard
matches it by plugin package and reports its heartbeat, state, and transitions.
If a plugin needs additional safe status fields, register a bounded snapshot
provider during startup and unregister the same id during shutdown:

```java
ControlCenterStatusRegistry.register("plan-woodcutter", () ->
        new ControlCenterStatusSnapshot(
                currentAction.get(),
                Map.of("Ore", Integer.toString(oreCount.get()))));

// In shutDown(), before or after script.shutdown():
ControlCenterStatusRegistry.unregister("plan-woodcutter");
```

Snapshot providers run on an Agent Server worker. They must only read already
safe, thread-safe values; do not query live RuneLite entities or perform game
interactions there. The snapshot is immutable and limited to 20 details. See
[Agent Server: plugin eligibility and optional status](../../docs/AGENT_SERVER.md#plugin-eligibility-and-optional-status)
for the full dashboard contract.

## Decision rules

Transitions are ordered. First true guard wins. Keep urgent recovery checks at
the top, then requirement checks, then normal work. Guards only inspect state;
actions change state. Re-query entities immediately before interacting.

Prefer this state machine for automation with three or more phases. Plain
`if/else` is fine inside one small state action. Add a behavior tree only when
you need reusable nested sequences/selectors shared by several plugins; it adds
abstraction without improving a normal bank/equip/work loop.

Replace template requirement methods and state actions. Never use fixed sleeps
to await game state; use `sleepUntil(condition, timeoutMs)`. Never block the
client thread.
