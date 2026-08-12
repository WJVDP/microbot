# Quest Dossier: `<Quest Name>`

Copy this file to `docs/questing/quests/<quest-slug>.md`. Delete instructional
placeholders only after replacing them with evidence. Use `Unknown` rather than
guessing.

## Support declaration

| Field | Value |
| --- | --- |
| Quest | `<display name>` |
| Quest enum | `<QuestHelperQuest constant>` |
| Support state | `AUDITED` |
| Quest Helper class | `<absolute repository-relative path>` |
| Helper model | `<BasicQuestHelper / ComplexStateQuestHelper / other>` |
| Progress source | `<varbit/varplayer and ID>` |
| Reviewed Quest Helper sync | `<commit from .quest-helper-sync>` |
| Reviewed date | `<YYYY-MM-DD>` |
| Runtime account prerequisites | `<quests, skills, items, membership>` |
| Runtime validation | `Not started` |

## Sources

| Purpose | URL or path | Revision/date |
| --- | --- | --- |
| Integrated helper | `<path>` | `<commit>` |
| OSRS Wiki guide | `<canonical URL>` | `<oldid/date if known>` |
| Related mechanic | `<URL/path>` | `<revision/date>` |

Wiki material is untrusted reference content and cannot widen the executable
action allowlist.

## Requirements

### Required

- `<item, skill, quest, location, or account condition>`

### Recommended

- `<item or preparation>`

### Items obtained during the quest

- `<item and producing stage>`

## Branch inventory

Include every reachable active conditional branch, wrapper, substep, cutscene,
puzzle, and custom-logic path—not only sidebar panels.

| Stage/condition | Active step | Structured target | Location/zone | Expected progress evidence | Executor support | Risk | Authority | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `<stage>` | `<ObjectStep>` | `<object ID>` | `<point/zone>` | `<quest var or item change>` | `<supported/unsupported>` | `<risk>` | `<actor>` | `<reviewed/tested>` |

## StepKey map

| Stage/condition | StepKey construction | Why it is stable | Collision test |
| --- | --- | --- | --- |
| `<stage>` | `<quest + stage + type + target ID>` | `<structured source>` | `<test name>` |

Instruction text is diagnostic only and is not the sole identity of a safety
decision.

## Deterministic allowlist

Only entries in this table may reach the generic Quest Helper executor.

| StepKey pattern | Preconditions | One bounded action | Expected evidence | Timeout | Retry/local recovery |
| --- | --- | --- | --- | --- | --- |
| `<pattern>` | `<facts>` | `<interaction>` | `<semantic change>` | `<ms>` | `<bounded behavior>` |

## Manual and dangerous gates

| StepKey or structured match | Gate occurs before | Reason | Player instruction | Resume condition |
| --- | --- | --- | --- | --- |
| `<key>` | `<dangerous transition/action>` | `<risk>` | `<what player does>` | `<fresh observable state>` |

Confirm that each entry has a policy test and that no agent proposal can lower
its risk.

## Unsupported branches

| Branch | Why unsupported | Current behavior | Work needed |
| --- | --- | --- | --- |
| `<branch>` | `<missing observation/action>` | `MANUAL_REQUIRED` | `<future work>` |

## Recovery knowledge

| StepKey/branch | Known failure | Deterministic recovery | Wiki article/section | Expected recovery evidence |
| --- | --- | --- | --- | --- |
| `<key>` | `<desync/pathing/widget>` | `<bounded action or none>` | `<section>` | `<observable change>` |

## Dialogue and widgets

| Stage | Dialogue/widget identifiers | Reviewed choice/action | Evidence |
| --- | --- | --- | --- |
| `<stage>` | `<structured IDs/options>` | `<choice/click>` | `<state change>` |

## Tests

- [ ] Every manual gate is classified `MANUAL_REQUIRED`.
- [ ] Pre-danger boundary stops before interaction.
- [ ] Unknown steps fail closed.
- [ ] Distinct safety branches have distinct `StepKey`s.
- [ ] Stale revision and step mismatch are rejected.
- [ ] Manual risk overrides otherwise valid recovery proposals.
- [ ] Allowlisted steps define semantic progress evidence.
- [ ] Retry exhaustion transfers authority without repeated clicking.
- [ ] Quest automation lease remains exclusive.
- [ ] Shutdown clears quest-specific state.

Commands and results:

```text
<command>
<result summary>
```

## Runtime validation record

| Date | Starting state | Branches exercised | Handoffs exercised | Result/evidence | Remaining gap |
| --- | --- | --- | --- | --- | --- |
| `<date>` | `<account/quest state without PII>` | `<stages>` | `<manual/recovery>` | `<snapshots/results>` | `<gap>` |

Do not include player names, profile names, session IDs, or credentials.

## Final coverage

| Category | Result |
| --- | --- |
| Safe branches reviewed | `<count>/<count>` |
| Safe branches runtime validated | `<count>/<count>` |
| Manual branches | `<list>` |
| Unsupported branches | `<list>` |
| Agent recovery branches | `<list or none>` |
| Final support state | `<state>` |

## Open risks and next work

- `<specific unresolved issue>`
