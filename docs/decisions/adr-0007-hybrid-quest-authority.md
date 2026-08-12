# ADR 0007: Hybrid Quest Authority and Recovery Boundary

- Status: Proposed (2026-08-12)

## Context

The integrated Quest Helper already selects the active quest step, while
`QuestScript` performs many of those interactions automatically. Some steps can
stall because the guide is desynchronized, a target is not reachable, or the
executor does not understand the active step. Letting an LLM control unrestricted
Agent Server actions would make these failures harder to bound and could allow a
model to enter dangerous content. Running a second wrapper beside `QuestScript`
would also allow both controllers to interact concurrently.

## Decision

Keep Quest Helper as the source of truth and add a single exclusive authority
model: deterministic automation, one bounded agent recovery proposal, or player
control. Extract shared step execution and progress observation from
`QuestScript`; serialize all quest actions through an execution lease.

Run the reasoning agent outside the client process. Expose focused recovery
requests through the existing token-authenticated Agent Server. Accept only
structured, allowlisted, revision-bound proposals and validate them again before
every action. The client plugin remains the safety enforcement point.

Use reviewed risk catalog entries for dangerous steps. A model may raise risk or
abstain but cannot lower a catalogued manual gate. Unknown pilot steps fail
closed. Manual resume always takes a fresh Quest Helper snapshot and does not
authorize the old step.

Pilot with Misthalin Mystery for recovery behavior and Vampyre Slayer for a
mandatory pre-boss handoff.

## Consequences

- AI latency and availability do not affect normal deterministic quest steps.
- Wiki content is fetched narrowly only during recovery and cannot widen the
  action allowlist.
- The agent cannot perform boss combat or bypass a player gate.
- Existing Quest Helper autoplay must be refactored to participate in the same
  execution lease before the Plan plugin can safely act.
- Quest support is incremental and based on reviewed structured steps rather
  than a claim of universal automation.
- More code is required for snapshots, typed execution results, proposal
  validation, and explicit authority transfer, but each boundary is testable.
