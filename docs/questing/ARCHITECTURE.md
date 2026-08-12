# Hybrid Questing Architecture

## Purpose

`PlanQuesting` executes the current Quest Helper step when that step is supported
and safe. If deterministic recovery cannot make progress, an external agent may
propose a small, validated recovery action using focused OSRS Wiki context. If the
agent cannot recover safely, or the current step is dangerous, the player takes
control.

Quest Helper remains the source of truth for quest progress and branching. The
agent is a recovery mechanism, not a second quest planner.

For the required workflow to add another quest, see
[Adding a Quest to PlanQuesting](ADDING_A_QUEST.md).

## Authority model

Exactly one actor owns game interaction at a time:

| Authority | May act | Exit condition |
| --- | --- | --- |
| `AUTOMATION` | The deterministic Quest Helper executor | Progress, retry exhaustion, safety gate, or emergency |
| `AGENT` | One validated, bounded proposal | Progress, proposal failure, timeout, abstention, or safety rejection |
| `PLAYER` | The player only | Explicit resume followed by a fresh Quest Helper snapshot |
| `NONE` | Nobody | Plugin start, stop, completion, or emergency reset |

An authority change is fail-closed. The outgoing actor stops and releases its
walking route before the incoming actor can act. The legacy Quest Helper autoplay
loop and `PlanQuesting` must share an exclusive execution lease so they cannot
click concurrently.

## Components

### First-party plugin (`microbot-plugins`)

- `PlanQuestingPlugin`: lifecycle, configuration, overlay/panel wiring, and
  control-center registration.
- `PlanQuestingScript`: the high-level `StateMachineScript` orchestration loop.
- `PlanQuestingConfig`: pilot quest, retry limits, and safety settings. Safety
  settings may make policy stricter but cannot downgrade mandatory manual gates.
- `PlanQuestingOverlay` and panel: current authority, active step, recovery
  reason, and explicit resume/stop controls.

### Shared quest runtime (`runelite-client`)

- `QuestStepSnapshotFactory`: captures immutable, thread-safe facts about the
  active Quest Helper step.
- `QuestStepExecutor`: executes one supported deterministic action and returns a
  typed result instead of assuming that a click made progress.
- `QuestProgressMonitor`: compares semantic snapshots and owns retry accounting.
- `QuestRiskPolicy`: classifies a step before execution.
- `QuestAutomationLease`: serializes ownership between legacy autoplay and
  `PlanQuesting`.
- `QuestRecoveryBroker`: holds the current recovery request and accepts or
  rejects an agent proposal.

These belong in `runelite-client` because both the existing Quest Helper and the
new Plan plugin use them. Quest-specific orchestration remains in
`microbot-plugins`, consistent with ADR 0006.

### Agent Server

The existing loopback, token-authenticated Agent Server exposes bounded hybrid
quest state. It does not call an LLM itself.

Proposed machine routes:

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/quest-supervisor/status` | Current state, authority, step, risk, and progress |
| `GET` | `/quest-supervisor/recovery` | Current recovery request, or no-content when none exists |
| `POST` | `/quest-supervisor/proposal` | Submit one structured recovery proposal |
| `POST` | `/quest-supervisor/manual/resume` | Request re-evaluation after player intervention |
| `POST` | `/quest-supervisor/stop` | Stop the hybrid quest session |

The local dashboard may display these fields later, but browser routes keep the
separate session/CSRF boundary required by ADR 0005.

### External agent supervisor

The supervisor runs outside the client process. It:

1. Waits for a recovery request.
2. Reads the focused, redacted context.
3. Retrieves only the relevant Wiki quest section when needed.
4. Diagnoses the mismatch.
5. Submits a structured proposal or abstains.

The supervisor never receives the long-lived machine token in prompts, never
submits arbitrary code, and never calls unrestricted game endpoints during a
hybrid quest session. The plugin remains the enforcement point.

## State machine

```text
IDLE
  -> PRECHECK
  -> CLASSIFY_STEP
       -> EXECUTE_AUTOMATED -> VERIFY_PROGRESS -> CLASSIFY_STEP
                 |                  |
                 |                  -> LOCAL_RECOVERY
                 |                         |
                 |                         -> WAITING_FOR_AGENT
                 |                                  |
                 |                                  -> EXECUTE_AGENT_PROPOSAL
                 |                                  -> MANUAL_REQUIRED
                 |
                 -> MANUAL_REQUIRED

Any active state -> EMERGENCY_STOP
Any active state -> COMPLETE
```

Transition guards inspect immutable snapshots and have no side effects. Game
interactions occur only in state actions on the script executor. Client-thread
reads use `runOnClientThreadOptional`; entity access uses the singleton Queryable
API and re-queries immediately before acting.

## Semantic progress

Activity is not progress. Animation, movement, or a menu click only proves that
an attempt occurred.

Progress is any relevant change in a fresh snapshot, including:

- Quest state or Quest Helper var changed.
- Active conditional step/fingerprint changed.
- A required item was gained, consumed, equipped, or transformed as expected.
- A relevant dialogue or puzzle stage advanced.
- The player entered the expected destination zone or plane.
- The target disappeared or changed in the way the step predicts.

Each attempted action has a bounded verification window. Repeating an identical
snapshot increments the no-progress counter. The counter resets only on semantic
progress or a genuinely different active step.

## Step identity and freshness

Most Quest Helper steps do not have stable explicit IDs. A `StepKey` therefore
combines:

- Quest enum.
- Quest progress var value.
- Active step type.
- Target NPC/object/widget identifiers where available.
- Defined world point where available.

Text is diagnostic context, not the primary identity. Every snapshot also has a
monotonic `contextRevision`. Agent proposals must echo both the recovery request
ID and revision. A proposal is rejected if either is stale.

## Safety policy

Risk is classified before every deterministic or proposed action:

| Risk | Behavior |
| --- | --- |
| `SAFE` | Deterministic execution and allowlisted agent recovery are permitted |
| `CAUTION` | Deterministic actions may run; ambiguous recovery escalates to the player |
| `MANUAL_REQUIRED` | No plugin or agent interaction; notify and wait for the player |
| `EMERGENCY` | Cancel the session and release all automation authority |

Mandatory manual gates include:

- Boss encounters and the transition into a boss arena.
- Unknown or unsupported combat.
- Wilderness or other explicitly catalogued dangerous regions.
- Irreversible, destructive, valuable, or account-sensitive actions.
- Any action not represented by the recovery allowlist.

Risk catalog entries override inference. Inference can raise risk but never lower
a catalogued risk. Unknown steps fail closed for the pilots.

## Quest knowledge

Quest Helper supplies executable structure. A small reviewed dossier supplies
facts that Quest Helper does not encode as policy:

- Supported/unsupported step fingerprints.
- Mandatory manual gates.
- Known desynchronization points.
- Wiki article and relevant section names.
- Expected progress evidence for recovery.

The dossier stores facts, identifiers, source URLs, and revision metadata rather
than copied guide prose. At runtime, the external supervisor retrieves a narrow
Wiki section only when recovery needs it.

## Failure behavior

- Agent unavailable or timed out: enter `MANUAL_REQUIRED`.
- Agent abstains: enter `MANUAL_REQUIRED`.
- Stale or malformed proposal: reject without acting; remain waiting until the
  bounded recovery deadline, then enter `MANUAL_REQUIRED`.
- Unsafe proposal: reject and immediately enter `MANUAL_REQUIRED`.
- Player presses Resume while the same mandatory gate is active: remain manual
  and explain that the dangerous section must first be completed or exited.
- Plugin shutdown, logout, or emergency: cancel walking, clear pending proposals,
  release the execution lease, and perform no further actions.

## Pilot boundaries

### Misthalin Mystery

The deterministic pilot covers ordinary dialogue, item, object, walking, piano,
and gemstone widget steps already represented by Quest Helper. Repeated failure
at a door/passage or an unsupported mirror sequence exercises recovery and
manual escalation. Its finale is not treated as permission to generalize combat
from instruction text.

### Vampyre Slayer

Automation may start the quest, obtain garlic/stake/beer, and travel to Draynor
Manor. A mandatory gate is raised before descending `CRYPTSTAIRSDOWN`, and is
also enforced for `VAMPCOFFIN` and `COUNT_DRAYNOR`. The agent cannot override
this gate. The player completes or exits the encounter and explicitly resumes;
the plugin then re-reads Quest Helper state.
