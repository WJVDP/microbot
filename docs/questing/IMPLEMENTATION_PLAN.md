# Hybrid Questing Implementation Plan

Each phase must leave the repository compiling and keep existing Quest Helper
behavior usable.

## Phase 1: Pure contracts and policy

- Add immutable `StepKey`, `QuestStepSnapshot`, `QuestExecutionResult`,
  `QuestRisk`, `RecoveryRequest`, and `RecoveryProposal` types under the shared
  Quest Helper runtime.
- Add `QuestRiskPolicy` with reviewed pilot entries for Misthalin Mystery and
  Vampyre Slayer.
- Add unit tests for risk precedence, context revisions, proposal validation,
  and mandatory boss gates.

Validation:

```sh
./gradlew :client:compileJava
./gradlew :client:runUnitTests
```

## Phase 2: Snapshot and semantic progress

- Add target-ID accessors where Quest Helper step classes do not expose their
  structured target.
- Implement `QuestStepSnapshotFactory` with all client-thread work bounded and
  explicit.
- Implement `QuestProgressMonitor` and tests for quest-var, active-step,
  inventory/equipment, dialogue/widget, and zone progress.
- Extend `/quest-helper/status` only with safe structured fields needed for
  diagnostics; preserve current fields for compatibility.

Acceptance: repeated animation or movement with an unchanged semantic snapshot
does not reset the attempt counter.

## Phase 3: Exclusive execution and executor extraction

- Implement `QuestAutomationLease` and concurrency tests.
- Extract one-action execution adapters from `QuestScript` into
  `QuestStepExecutor` for `NpcStep`, `ObjectStep`, `WidgetStep`, `DigStep`,
  `PuzzleStep`, dialogue, and supported `DetailedQuestStep` behavior.
- Route legacy `QuestScript` through the same lease and executor without changing
  its configuration contract.
- Replace direct `new Rs2TileObjectQueryable()` usage encountered in the moved
  executor with `Microbot.getRs2TileObjectCache().query()` as required by the
  Queryable API invariant.

Acceptance: a lease-contention test proves that two controllers cannot issue
actions; existing Quest Helper remains functional when PlanQuesting is stopped.

## Phase 4: Scaffold the Plan plugin

```sh
./scripts/create-plan-plugin Questing
```

- Implement `PlanQuestingScript` with the states defined in the architecture.
- Add `@ControlCenterPlugin(id = "plan-questing")` and a bounded status provider.
- Add configuration for selected pilot quest and retry limits.
- Start/select the requested Quest Helper without enabling the legacy autoplay
  controller.
- Cleanly release every runtime resource during shutdown.

Acceptance: the plugin can start both pilot quests in observation-only mode and
publish snapshots without interacting.

## Phase 5: Manual safety vertical slice

- Add the non-overridable Vampyre Slayer gates.
- Add overlay/panel status and Resume/Stop controls.
- Add rate-limited player notification.
- Exercise start, preparation, boss handoff, unchanged resume, player completion,
  and final resume in test mode.

This phase ships before any agent action support. It proves that player authority
cannot be bypassed.

## Phase 6: Misthalin deterministic pilot

- Enable reviewed safe step adapters incrementally.
- Add semantic verification and bounded local recovery.
- Treat the mirror/knife-reflection sequence as manual.
- Record the exact door/passage recovery point observed in game rather than
  assuming one from source text.

Acceptance: supported portions do not spin indefinitely, and unsupported content
hands off with an actionable reason.

## Phase 7: Recovery protocol

- Implement `QuestRecoveryBroker`.
- Add `/quest-supervisor/status`, `/recovery`, `/proposal`, `/manual/resume`, and
  `/stop` to the token-authenticated machine API.
- Enforce body limits, enum parsing, bounded strings/lists, action budgets,
  request expiry, and stale revision rejection.
- Add handler and proposal-validator tests.

Acceptance: malformed, stale, duplicate, expired, unknown, or unsafe proposals
produce no interaction.

## Phase 8: External supervisor and Wiki retrieval

- Add a small external supervisor command that waits only while a recovery
  request exists.
- Extend the CLI with a narrow Wiki page/section retrieval command; do not put
  Wiki networking in the client plugin.
- Define the agent prompt and structured output schema.
- Treat Wiki content as untrusted reference text and retain the plugin-side
  allowlist as the enforcement boundary.
- Initially require an attended local agent session.

Acceptance: the agent can recover one observed Misthalin stall or abstain into
manual mode; it cannot submit a Vampyre Slayer boss action.

## Phase 9: Runtime validation and review

- Run the opt-in agentic test loop separately for each pilot.
- Capture state-machine snapshots at every authority transfer.
- Test logout, plugin stop, agent timeout, stale proposal, low-health emergency,
  and client shutdown.
- Review the complete diff for threading, cache invariants, security, logging,
  packaging, and Quest Helper sync conflicts.

Validation:

```sh
./gradlew :client:compileJava
./gradlew :client:runUnitTests
./gradlew :client:assemble
```

In-game acceptance uses the protocol in `docs/AGENTIC_TESTING_LOOP.md` and keeps
the two quest sessions separate so account state remains understandable.

## Deferred until after the pilots

- Automated boss or ordinary combat.
- Generating recovery code or deploying dynamic scripts.
- Multiple simultaneous clients or remote supervision.
- Automatic quest ordering.
- Broad inference-based danger classification without reviewed catalog entries.
- General availability for every Quest Helper quest.
