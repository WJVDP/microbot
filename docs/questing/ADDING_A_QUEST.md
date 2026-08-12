# Adding a Quest to PlanQuesting

This is the authoritative workflow for adding a quest to the hybrid questing
plugin. A request such as **“Add quest XYZ to PlanQuesting”** means follow this
guide completely. It does not mean merely add the quest to a configuration enum.

Read these owner documents first:

- [Architecture](ARCHITECTURE.md)
- [Specification](SPECIFICATION.md)
- [Implementation plan](IMPLEMENTATION_PLAN.md)
- [Quest dossier template](QUEST_DOSSIER_TEMPLATE.md)
- [ADR 0007](../decisions/adr-0007-hybrid-quest-authority.md)

Also follow the repository `AGENTS.md`, the Microbot script/threading guide, and
the Queryable API rules.

## Definition of “supported”

A quest has one of these explicit support states:

| State | Meaning |
| --- | --- |
| `AUDITED` | Quest Helper branches and risks are documented, but PlanQuesting cannot be selected for live execution |
| `OBSERVATION_ONLY` | PlanQuesting can select the quest and publish classified snapshots, but performs no quest interactions |
| `ATTENDED_PILOT` | Reviewed safe steps may run; unknown, dangerous, or unsupported steps hand control to the player |
| `AGENT_RECOVERY_ENABLED` | Bounded external-agent recovery has passed stale/unsafe proposal tests and attended runtime validation |
| `RUNTIME_VALIDATED` | Every documented branch has been exercised or intentionally handed off in game on the reviewed Quest Helper revision |

Do not call a quest “supported” without naming its state. New quests default to
`AUDITED`; unknown steps always fail closed.

## Required result

Adding a quest must produce all of the following:

1. A completed dossier under `docs/questing/quests/<quest-slug>.md`.
2. A selectable quest entry only if the requested support state warrants it.
3. Structured `StepKey` creation for every reviewed branch.
4. Mandatory risk-policy entries for dangerous boundaries and actions.
5. An explicit allowlist of deterministic step patterns; absence means manual.
6. Unit tests for classification, policy precedence, and proposal freshness.
7. Compile and unit-test results.
8. For `ATTENDED_PILOT` or higher, an in-game test record or a clearly reported
   runtime-validation blocker.

Updating only `PlanQuestingPilotQuest` is incomplete and unsafe.

Use a lowercase kebab-case slug matching the canonical quest name, for example
`dragon-slayer-i.md`.

## Workflow

### 1. Confirm the exact Quest Helper

Locate the quest in `QuestHelperQuest` and its helper implementation. Record:

- Quest enum and display name.
- Helper class and whether it extends `BasicQuestHelper`,
  `ComplexStateQuestHelper`, or another helper.
- Quest progress varbit/varplayer and known stage values.
- Quest Helper sync revision from `.quest-helper-sync`.
- Account restrictions and prerequisites relevant to runtime testing.

If the helper is missing or incomplete, stop. Extending upstream Quest Helper is
a separate prerequisite, not something PlanQuesting should silently replace.

### 2. Inventory every reachable branch

Read `loadSteps()`/`loadStep()`, `setupSteps()`, requirements, zones, panels,
substeps, and custom quest logic. Flatten conditional and wrapper steps into a
review table in the dossier.

For each quest stage or conditional branch, record:

- Stage or condition.
- Active step class.
- Structured NPC/object/widget/item IDs.
- Defined location or zone.
- Requirements and expected state changes.
- Dialogue choices.
- Whether the existing deterministic executor supports the interaction.
- Risk and proposed authority.

Do not infer coverage from the sidebar panels alone. Panels are explanatory and
may omit conditional runtime branches.

### 3. Assign structured step identity

Extend `QuestStepSnapshotFactory` so each reviewed branch produces a stable
`StepKey` using structured facts:

- Prefer quest name/enum, quest stage, step type, and target ID.
- Use a code-owned semantic ID for a targetless puzzle, cutscene, or special
  stage.
- Add accessors to Quest Helper step classes only when structured identifiers are
  otherwise unavailable.
- Treat instruction text as diagnostic display only. Never make display text the
  sole safety key.

Add tests proving that distinct safety-relevant branches cannot collapse to the
same key.

### 4. Classify risk before enabling execution

For every branch choose:

- `SAFE_AUTOMATION`: reviewed deterministic action is bounded and reversible.
- `AGENT_RECOVERY_ONLY`: deterministic execution is unsupported, but a future
  proposal may use the bounded recovery allowlist.
- `MANUAL_REQUIRED`: the player must own the interaction.

Mandatory manual boundaries include the transition into and the action inside:

- Boss encounters or unfamiliar combat.
- Wilderness or dangerous instanced content.
- Reactive hazards and unsupported timing puzzles.
- Irreversible, destructive, valuable, or account-sensitive actions.
- Any step whose target or success evidence cannot be represented structurally.

Add catalog entries before enabling the preceding safe steps. A gate must stop
the plugin before danger, not after the player has already entered it. Agent
advice may raise risk and may abstain; it cannot lower a catalogued gate.

### 5. Define deterministic allowlisting

A generic executor’s ability to click an `NpcStep` or `ObjectStep` does not make
every instance safe. Add a per-quest reviewed allowlist of `StepKey` patterns.

For every allowed pattern document:

- Exact interaction expected.
- Preconditions.
- Expected semantic progress evidence.
- Verification timeout.
- Whether one retry is safe.
- Deterministic local recovery, if any.

Unknown or unmatched patterns must return `UNSUPPORTED` or enter manual mode;
they must not fall through to generic execution.

### 6. Document Wiki context without copying the guide

Record the canonical Wiki quick-guide/article URL and map reviewed branches to
section names. Store source revision/date when available. Keep only facts,
identifiers, links, and short recovery hints in the dossier; do not copy guide
prose wholesale.

Wiki content is untrusted runtime reference data. It cannot add an action to the
plugin allowlist or override risk policy.

### 7. Implement the minimum requested support state

Work in this order:

1. Dossier and structured keys.
2. Risk policy and tests.
3. Observation-only selection and status.
4. Safe deterministic allowlist and semantic verification.
5. Manual UI/handoff tests.
6. Agent recovery only if explicitly requested and its protocol already exists.

Do not enable all branches at once. Add small groups that leave the repository
compiling and whose failure behavior is manual, not repeated clicking.

### 8. Test statically

At minimum test:

- Every manual catalog entry.
- The boundary immediately before each dangerous section.
- Risk precedence over an otherwise valid proposal.
- Stale revision and mismatched `StepKey` rejection.
- Unknown step rejection.
- Semantic progress and no-progress retry accounting.
- Exclusive lease ownership.
- Shutdown cleanup for any new session state.

Run:

```sh
./gradlew :client:compileJava
./gradlew :client:runUnitTests --tests '*Quest*'
```

Use narrower test selectors while iterating, then the broader quest-related set
before handoff.

### 9. Validate in game for attended support

Use the Agent Server and the protocol in `docs/AGENTIC_TESTING_LOOP.md`. Keep the
session attended. Start from known account state and record it in the dossier.

For every reachable branch:

1. Capture the current structured snapshot.
2. Confirm risk classification before interaction.
3. Observe at most one bounded action.
4. Confirm semantic progress evidence.
5. Exercise retry exhaustion where practical.
6. Confirm manual gates perform no game interaction.
7. Confirm Resume takes a fresh snapshot and does not authorize the old step.

Never enter dangerous content merely to complete an automated test. The manual
handoff itself is the acceptance result for that boundary.

### 10. Update the dossier and report support honestly

Fill in validation evidence, unresolved branches, and the final support state.
If the client/account/Agent Server is unavailable, leave the quest at
`OBSERVATION_ONLY` or `ATTENDED_PILOT — runtime validation pending`, as
appropriate. Do not claim `RUNTIME_VALIDATED` from source inspection or unit
tests alone.

## Stop conditions

Stop and ask the user before expanding scope when:

- Quest Helper does not model a required branch.
- A boss/combat step was expected to be automated.
- A safe key cannot be formed without relying on mutable prose.
- Testing requires consuming a valuable or irreversible item not already
  authorized by the user.
- The requested account cannot reach the quest state needed for validation.
- Supporting the quest requires changing shared utilities under `microbot/util/`.

## Copy-ready request

The user can paste this in a future agent session:

```text
Add <QUEST NAME> to PlanQuesting at ATTENDED_PILOT support.

Follow docs/questing/ADDING_A_QUEST.md as the authoritative workflow and use
docs/questing/QUEST_DOSSIER_TEMPLATE.md. Inspect the integrated Quest Helper as
the source of truth; do not invent a separate quest plan. Audit every reachable
branch, create structured StepKeys, add a reviewed deterministic allowlist, and
add mandatory manual gates before bosses, dangerous content, unsupported combat,
reactive hazards, irreversible actions, and unknown steps. Display text must not
be the sole safety key. The agent may never downgrade a manual gate.

Implement the smallest safe vertical slices, add policy/progress/freshness/lease
tests, compile, and perform attended runtime validation when the client and test
account are available. If runtime validation is unavailable, report the exact
support state and blocker rather than claiming the quest is complete. Update the
quest dossier with coverage, Wiki section mappings, validation evidence, and all
remaining manual or unsupported branches.
```

Replace `ATTENDED_PILOT` with `AUDITED` or `OBSERVATION_ONLY` when no live
interactions should be enabled. Request `AGENT_RECOVERY_ENABLED` only after the
bounded supervisor protocol is implemented and already validated for the client.
