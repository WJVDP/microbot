# Plan plugin template

Run from repository root:

```sh
./scripts/create-plan-plugin Woodcutter
./gradlew :client:compileJava
```

This creates package `microbot.planwoodcutter` with:

- `PlanWoodcutterPlugin`: RuneLite lifecycle and dependency wiring.
- `PlanWoodcutterConfig`: loop timing and future user settings.
- `PlanWoodcutterScript`: background state-machine loop.

Naming convention: every planned plugin class starts with `Plan<PluginName>`.
Its descriptor uses `PluginDescriptor.Plan`, rendering green `[Plan]` in the
Microbot plugin list.

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
