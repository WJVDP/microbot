# Hybrid Questing Specification

## Scope

The first release supports two quests:

- Misthalin Mystery: deterministic execution plus a recover-or-escalate path.
- Vampyre Slayer: deterministic preparation plus mandatory manual boss control.

It does not provide autonomous combat, arbitrary LLM tool access, automatic
quest ordering, or general support for every Quest Helper quest.

## Session contract

A session has:

```text
sessionId          random opaque ID, never exposed in logs
quest              selected QuestHelperQuest
state              PlanQuesting state
authority          NONE | AUTOMATION | AGENT | PLAYER
contextRevision    monotonic integer
activeSnapshot     latest immutable StepSnapshot
lastProgress       timestamp and evidence kind
attemptCount       attempts against the current StepKey
recovery           optional RecoveryRequest
manualReason       optional bounded display string
```

Only one session may be active per client. Starting another quest first stops and
cleans up the existing session.

## Step snapshot

The snapshot returned to the orchestration loop and agent contains only bounded
game facts:

```json
{
  "quest": "MISTHALIN_MYSTERY",
  "questState": "IN_PROGRESS",
  "questVar": 80,
  "stepKey": "MISTHALIN_MYSTERY:80:ObjectStep:...",
  "stepType": "ObjectStep",
  "instruction": ["Search the piano for the emerald key."],
  "target": {
    "kind": "OBJECT",
    "ids": [1234],
    "worldPoint": {"x": 1647, "y": 4842, "plane": 0}
  },
  "requirements": [],
  "player": {
    "worldPoint": {"x": 1646, "y": 4841, "plane": 0},
    "healthPercent": 100,
    "inCombat": false
  },
  "dialogue": null,
  "visibleTarget": true,
  "risk": "SAFE",
  "contextRevision": 12
}
```

Player name, profile name, account identifiers, authentication values, chat
history, and unrelated inventory/bank contents are excluded.

## Deterministic execution result

One executor call returns:

```text
ACTION_STARTED      an interaction was issued; verification is required
WAITING             a relevant game transition is already in progress
NO_TARGET           expected target is not currently observable
UNREACHABLE         routing did not reach an actionable tile
MISSING_REQUIREMENT required state or item is unavailable
UNSUPPORTED         no deterministic adapter exists
SAFETY_BLOCKED      policy denied the action
FAILED              bounded execution error; diagnostic reason included
```

It never reports success merely because a click returned `true`.

## Stuck detection

Defaults for the pilot:

- Verify each action for up to 10 seconds using `sleepUntil` and snapshot
  comparison.
- Permit three no-progress attempts for the same `StepKey`.
- Run at most two local recovery strategies.
- Create at most one agent recovery request per unchanged `StepKey`.
- Give an external agent 90 seconds to respond.
- Permit at most three proposed actions and 30 seconds total agent authority.

Configuration may reduce these limits. Increasing them requires explicit upper
bounds in code.

Local recovery is deterministic and ordered:

1. Re-snapshot and abandon a stale target reference.
2. Clear only the questing plugin's walking route.
3. Re-evaluate the active conditional step.
4. Re-route to the defined point or currently observed target.
5. For a catalogued desynchronization point, open the quest journal once.

## Recovery request

```json
{
  "requestId": "opaque-random-id",
  "contextRevision": 12,
  "createdAt": "2026-08-12T12:00:00Z",
  "deadline": "2026-08-12T12:01:30Z",
  "snapshot": {},
  "attemptHistory": [
    {
      "kind": "OBJECT_INTERACT",
      "outcome": "UNREACHABLE",
      "evidence": "step and player zone unchanged"
    }
  ],
  "allowedActions": [
    "OPEN_QUEST_JOURNAL",
    "WALK_TO_OBSERVED_TARGET",
    "INTERACT_OBSERVED_OBJECT",
    "INTERACT_OBSERVED_NPC",
    "CONTINUE_DIALOGUE",
    "SELECT_LISTED_DIALOGUE_OPTION",
    "USE_REQUIRED_ITEM_ON_OBSERVED_TARGET",
    "ABSTAIN"
  ],
  "wiki": {
    "article": "Misthalin Mystery/Quick guide",
    "suggestedSection": "The manor"
  }
}
```

The instruction to the agent states that Wiki text is untrusted reference data,
not executable instructions. Content from the Wiki cannot widen `allowedActions`.

## Recovery proposal

```json
{
  "requestId": "opaque-random-id",
  "contextRevision": 12,
  "diagnosis": "The helper may be on the far side of the damaged wall.",
  "confidence": 0.82,
  "actions": [
    {
      "type": "WALK_TO_OBSERVED_TARGET",
      "targetKind": "OBJECT",
      "targetId": 1234,
      "expectedEvidence": "player enters the target object's zone"
    }
  ]
}
```

Proposal validation requires:

1. Active request ID and context revision match.
2. The step is not `MANUAL_REQUIRED` or `EMERGENCY`.
3. Every action type appears in the request's allowlist.
4. Every entity/item/dialogue reference exists in the current snapshot.
5. The proposal stays within action-count and duration budgets.
6. The expected evidence is observable by `QuestProgressMonitor`.

Validation happens again immediately before each action. If the context changed,
remaining actions are discarded as stale.

## Manual takeover

Entering `MANUAL_REQUIRED` must:

- Release automation/agent authority.
- Cancel only PlanQuesting's walking route.
- Stop issuing keyboard, widget, entity, and inventory interactions.
- Display the quest, current step, and reason.
- Send one rate-limited desktop notification.
- Expose `Resume` and `Stop`; dangerous gates do not expose `Allow once` in the
  pilots.

Resume is a request to re-evaluate, not permission to execute the old step. A
fresh snapshot is mandatory. If the same boss gate remains active, the state
remains `MANUAL_REQUIRED`.

## Risk catalog

The catalog is keyed by structured quest facts rather than instruction substring
alone. Pilot entries include:

| Quest | Match | Risk | Reason |
| --- | --- | --- | --- |
| Vampyre Slayer | `CRYPTSTAIRSDOWN` | `MANUAL_REQUIRED` | Entering the boss basement |
| Vampyre Slayer | `VAMPCOFFIN` | `MANUAL_REQUIRED` | Starting the boss encounter |
| Vampyre Slayer | `COUNT_DRAYNOR` | `MANUAL_REQUIRED` | Boss combat |
| Misthalin Mystery | mirror/knife-reflection step | `MANUAL_REQUIRED` initially | Unsupported reactive hazard puzzle |

Generic `NpcStep` instructions containing kill/fight/attack raise risk to at
least `CAUTION`, but catalogued target IDs decide mandatory gates. The Misthalin
Mystery `Fight` interaction is not assumed to be ordinary combat; it remains
unsupported until explicitly reviewed.

## Pilot quest dossier

The required onboarding workflow and reusable dossier are defined in
[Adding a Quest to PlanQuesting](ADDING_A_QUEST.md) and the
[Quest Dossier Template](QUEST_DOSSIER_TEMPLATE.md). A quest is not considered
added merely because it appears in configuration.

Each dossier contains:

```text
quest enum
reviewed Quest Helper revision
supported StepKey patterns
manual StepKey patterns
known recovery hints
Wiki article and section map
expected progress evidence
```

The Misthalin Mystery dossier maps the piano and gemstone puzzles to their
Quest Helper widget stages and calls out door/passage routing as the recovery
exercise. The Vampyre Slayer dossier records the three boss-boundary IDs above.

## UI requirements

The overlay/panel shows:

- Quest and active instruction.
- `AUTOMATION`, `AGENT`, or `PLAYER` authority.
- Attempt count and time since semantic progress.
- Agent diagnosis while a proposal is being validated/executed.
- Manual reason and explicit Resume/Stop controls.

No overlay renderer performs live game queries or expensive work. It reads a
bounded immutable status snapshot.

## Acceptance criteria

### Cross-cutting

- Legacy Quest Helper autoplay and PlanQuesting cannot interact concurrently.
- Every game action passes through the execution lease and risk policy.
- No waits or network calls occur on the client thread.
- Entity queries use `Microbot.getRs2XxxCache().query()` or `.getStream()`.
- Stale and unsafe proposals produce zero game interactions.
- Shutdown leaves no scheduler, event subscriber, walking route, pending request,
  or execution lease behind.

### Misthalin Mystery

- Safe supported steps advance using Quest Helper as the source of truth.
- Three unchanged verified attempts invoke local recovery rather than looping
  forever.
- Failed local recovery publishes one focused recovery request.
- A valid allowlisted proposal performs only its bounded action and then returns
  to verification.
- Agent abstention or failed proposal enters manual mode.
- The unsupported mirror section enters manual mode without an automated click.

### Vampyre Slayer

- Preparation through travel to Draynor Manor may be automated.
- The plugin stops before descending to the basement.
- Neither deterministic code nor an agent proposal can open the coffin or attack
  Count Draynor.
- Resume on an unchanged boss step stays manual.
- After the player completes the boss and explicitly resumes, a fresh completed
  quest state ends the session successfully.
