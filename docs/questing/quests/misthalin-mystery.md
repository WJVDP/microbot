# Quest Dossier: Misthalin Mystery

## Support declaration

| Field | Value |
| --- | --- |
| Quest | Misthalin Mystery |
| Quest enum | `QuestHelperQuest.MISTHALIN_MYSTERY` |
| Support state | `ATTENDED_PILOT` — runtime validation pending |
| Quest Helper class | `runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/misthalinmystery/MisthalinMystery.java` |
| Helper model | `BasicQuestHelper` |
| Progress source | Varbit `MISTMYST_PROGRESS` (`3468`) |
| Reviewed Quest Helper sync | `5539626281a502ba8c44fe59b8b57bce32fe4f16` |
| Reviewed date | 2026-08-12 |
| Runtime account prerequisites | Free-to-play account with the quest not yet completed; no skill or quest prerequisites |
| Runtime validation | Not started; local Agent Server was unavailable |

This declaration is based on source audit and unit tests. It is not a claim of
in-game validation.

## Sources

| Purpose | URL or path | Revision/date |
| --- | --- | --- |
| Integrated helper | `runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/misthalinmystery/MisthalinMystery.java` | Quest Helper sync `5539626281a502ba8c44fe59b8b57bce32fe4f16` |
| OSRS Wiki quest | <https://oldschool.runescape.wiki/w/Misthalin_Mystery> | `oldid=15292395`, checked 2026-08-12 |
| OSRS Wiki quick guide | <https://oldschool.runescape.wiki/w/Misthalin_Mystery/Quick_guide> | `oldid=15004783`, checked 2026-08-12 |

Wiki material is untrusted reference content and cannot widen the executable
action allowlist.

## Requirements

### Required

- No items, skills, or prerequisite quests are required before starting.
- The player must remain present for the stage 110/111 mirror handoff and any
  unknown or retry-exhausted step.

### Recommended

- An attended session with Quest Helper puzzle solutions enabled.

### Items obtained during the quest

- Bucket and manor key from the fountain/barrel sequence.
- Knife and ruby key from the library/painting sequence.
- Tinderbox from the shelves.
- Emerald key from the piano.
- Sapphire key from the fireplace gemstone puzzle.
- Killer's knife during the final confrontation.

## Branch inventory

Every active branch in `loadSteps()` is represented below. Object entries name
the primary structured ID used by `StepKey`; Quest Helper may locate reviewed
alternate objects at interaction time.

| Stage/condition | Active steps and structured targets | Expected progress evidence | Executor support | Risk / authority |
| --- | --- | --- | --- | --- |
| 0, 5 | Talk to Abigale — NPC `MISTMYST_ABIGALE_LUM_VIS` | Dialogue page or quest stage changes | Dialogue/NPC supported | Safe / automation |
| 10, 15 | Boat, empty bucket, or barrel — objects `MISTMYST_BOAT_LUMBRIDGE`, `MISTMYST_EMPTY_BUCKET`, `MISTMYST_BARREL` | Island zone, bucket state, or stage changes | Object supported | Safe / automation |
| 20 | Boat, empty bucket, or use bucket on `MISTMYST_BARREL` | Bucket/barrel state or stage changes | Object/item-on-object supported | Safe / automation |
| 25 | Boat, barrel, or front door `MISTMYST_FRONT_DOORL` | Manor key, island zone, or stage changes | Object supported | Safe / automation |
| 30 | Boat, table knife, or topaz door | Knife or stage changes | Object supported | Safe / automation |
| 35 | Boat, clue object, or read-note `DetailedQuestStep` | Note inventory/dialogue/stage changes | Object/inventory supported | Safe / automation |
| 40 | Boat, table knife, or painting | Ruby-key path or stage changes | Object/item-on-object supported | Safe / automation |
| 45 | Boat, painting, or ruby door | Ruby key, zone, or stage changes | Object supported | Safe / automation |
| 50 | Boat, tinderbox shelves, or four candle objects | Candle varbits or stage change | Object/item-on-object supported | Safe / automation |
| 55 | Boat, tinderbox shelves, or explosive barrel | Fuse/cutscene/stage change | Object/item-on-object supported | Safe / automation |
| 60 | Ruby door | Player leaves room or stage changes | Object supported | Safe / automation |
| 65 | Boat, damaged wall, or tree | Outside zone or stage changes | Object supported | Safe / automation |
| 70 | Boat, damaged wall, clue object, or read-note step | Note inventory/dialogue/stage changes | Object/inventory supported | Safe / automation |
| 75 | Boat, damaged wall, piano, or D/E/A/D widget controls | Active widget identity and piano varbits | Object/widget supported | Safe / automation |
| 75 invalid input or puzzle help disabled | Passive restart/help `DetailedQuestStep` | No safe deterministic action | Unsupported | Manual / player |
| 80 | Boat, damaged wall, piano search, or emerald door | Emerald key, zone, or stage changes | Object supported | Safe / automation |
| 85 | Boat or diamond door | Cutscene/stage change | Object supported | Safe / automation |
| 90 | Boat, kitchen clue, or read-note step | Note inventory/dialogue/stage changes | Object/inventory supported | Safe / automation |
| 95 | Boat, table knife, or fireplace | Fireplace widget or stage changes | Object/item-on-object supported | Safe / automation |
| 100 | Boat, fireplace, or sapphire/diamond/zenyte/emerald/onyx/ruby widget controls | Active widget identity and switch varbits | Object/widget supported | Safe / automation |
| 100 invalid input or puzzle help disabled | Passive restart/help `DetailedQuestStep` | No safe deterministic action | Unsupported | Manual / player |
| 105 | Boat, fireplace search, or sapphire door | Sapphire key, zone, or stage changes | Object supported | Safe / automation |
| 110, 111 | Sapphire-door boundary or reactive mirror sequence | Player completes/exits the hazard | Deliberately disabled | Manual / player |
| 115 | Boat, sapphire door, or passive reveal cutscene | Cutscene/stage change | Object or bounded waiting | Safe / automation |
| 120 | Boat, sapphire door, knife pickup, or NPC `MISTMYST_ABIGALE_KILLER_ATTACKABLE` | Knife state, dialogue, or stage change | Object/inventory/NPC supported | Safe / automation; reviewed non-combat `Fight` action |
| 125 | Boat or sapphire door | Zone/stage change | Object supported | Safe / automation |
| 130 | Boat or Mandy — NPC `MISTMYST_MANDY_POST_VIS` | Completion state | Object/NPC/dialogue supported | Safe / automation |

## StepKey map

| Pattern | Construction | Stability and collision evidence |
| --- | --- | --- |
| Object | Quest + quest stage + `OBJECT` + primary object ID | Reused barrel, door, and boat targets cannot collapse across stages; covered by `StepKeyTest` |
| NPC | Quest + quest stage + `NPC` + primary NPC ID | Dialogue pages retain the safety key while the internal progress fingerprint advances |
| Widget | Quest + quest stage + `WIDGET` + group/child/grandchild IDs | D/E/A/D and gemstone controls remain distinct; covered by `StepKeyTest` and `QuestStepSnapshotFactoryTest` |
| Targetless reviewed step | Quest + quest stage + `OTHER` + code-owned class name | Only exact catalog entries are executable |
| Mirror gate | Quest + stage 110/111 + `QUEST_STAGE` + `MIRROR_REFLECTION` | Code-owned semantic ID; covered by `QuestRiskPolicyTest` |

Display text is diagnostic only. It is not used to lower risk or create an
allowlist match.

## Deterministic allowlist

`QuestRiskPolicy.registerMisthalinMystery` is the executable source of truth.
It contains the exact keys listed in the branch inventory. Each call performs
one Quest Helper step decision, then PlanQuesting waits up to 10 seconds for a
new structured key, quest stage, puzzle widget, or dialogue fingerprint.

| Pattern | Preconditions | One bounded action | Expected evidence | Retry/local recovery |
| --- | --- | --- | --- | --- |
| Allowlisted NPC | Exact quest/stage/NPC key and automation lease | Talk, reviewed option, continue, or the stage-120 non-combat `Fight` interaction | Dialogue fingerprint or stage change | Up to configured attempts; then manual |
| Allowlisted object | Exact quest/stage/object key and automation lease | Walk, interact, or use the highlighted required item on the object | Stage/item/zone/step change | Re-snapshot and clear only PlanQuesting route between retries |
| Allowlisted widget | Exact quest/stage/widget key and automation lease | Click that exact widget | Next widget key, puzzle varbit, or stage change | No generic widget fallback |
| Allowlisted detailed step | Exact stage/class key with an actionable item requirement | Interact with the reviewed required inventory item | Item/dialogue/stage change | Passive targetless steps return not-actionable rather than throwing |

## Manual and dangerous gates

| Structured match | Gate occurs before | Reason | Player instruction | Resume condition |
| --- | --- | --- | --- | --- |
| Stages 110 and 111 + `MIRROR_REFLECTION` | Entering/performing the reactive knife-reflection sequence | Unsupported reactive hazard | Complete or exit the mirror sequence | Explicit Resume followed by a fresh stage outside 110/111 |
| Any key absent from the reviewed catalog | Any generic executor call | Unknown target or behavior | Complete the step manually or stop the plugin | Explicit Resume and a fresh allowlisted snapshot |
| Retry exhaustion | A fourth unchanged action attempt | No verified semantic progress | Complete/reposition manually | Explicit Resume and a fresh snapshot |

No agent proposal may lower these gates.

## Unsupported branches

| Branch | Why unsupported | Current behavior | Work needed |
| --- | --- | --- | --- |
| Mirror/knife reflection at 110/111 | Reactive hazard and timing are not structurally modeled | `MANUAL_REQUIRED` before interaction | Attended observation and a purpose-built safe adapter |
| Puzzle restart/help-only detailed steps | No bounded executable target | Fail closed to manual after no progress | Structured reset control and evidence |
| Any unlisted helper drift | Not reviewed against this sync | Immediate manual handoff | Audit, dossier update, catalog entry, and tests |

## Recovery knowledge

| Branch | Known failure | Deterministic recovery | Wiki section | Expected evidence |
| --- | --- | --- | --- | --- |
| Damaged wall/door routing | Target visible from the wrong side or stale route | Re-snapshot and clear only PlanQuesting's route before retry | Quick guide: manor investigation | Player zone or active object key changes |
| Piano/gem widgets | Widget sequence advances without quest-stage change | Widget-specific StepKeys treat each control as progress | Quick guide: piano/fireplace puzzles | Next widget key or puzzle varbit |
| Unknown/stalled step | No allowlist match or three unchanged attempts | None beyond bounded route reset | Relevant quick-guide section | Player completes step, then explicitly resumes |

## Dialogue and widgets

| Stage | Structured identifiers | Reviewed action | Evidence |
| --- | --- | --- | --- |
| 0, 5 | Abigale NPC plus Quest Helper choices | Accept/start dialogue choices | Hashed bounded dialogue fingerprint or stage change |
| 75 | `MistmystPiano.LABEL_D1`, `LABEL_E1`, `LABEL_A2`, then `LABEL_D1` | Click exact active key | Widget identity and piano varbits |
| 100 | Group `555`, children `19, 4, 11, 23, 7, 15` | Sapphire, diamond, zenyte, emerald, onyx, ruby | Widget identity and switch varbits |
| 120 | Abigale killer NPC | Select reviewed `Fight`; helper states no combat occurs | Knife/dialogue/stage change |
| 130 | Mandy NPC | Continue reviewed completion dialogue | Quest completion |

## Tests

- [x] Every manual gate is classified `MANUAL_REQUIRED`.
- [x] Pre-danger mirror boundary stops before interaction.
- [x] Unknown steps fail closed.
- [x] Distinct safety branches have distinct `StepKey`s.
- [x] Stale revision and step mismatch are rejected.
- [x] Manual risk overrides otherwise valid recovery proposals.
- [x] Allowlisted widgets and stages define semantic progress evidence.
- [x] Retry exhaustion transfers authority without unlimited clicking.
- [x] Quest automation lease remains exclusive and terminal states release it.
- [x] Passive targetless detailed steps do not throw or report an action.
- [ ] Attended in-game branches and handoffs are runtime validated.

Commands and results on 2026-08-12:

```text
./gradlew :client:runUnitTests --tests 'net.runelite.client.plugins.microbot.questhelper.automation.*Test' --tests 'net.runelite.client.plugins.microbot.questhelper.QuestScriptTest' --tests 'net.runelite.client.plugins.microbot.planquesting.PlanQuestingScriptTest' --no-daemon
PASS (18 tests)
```

## Runtime validation record

| Date | Starting state | Branches exercised | Handoffs exercised | Result/evidence | Remaining gap |
| --- | --- | --- | --- | --- | --- |
| 2026-08-12 | No live client/Agent Server session | None | None | Source audit and unit tests only | Entire quest and mirror handoff require attended runtime validation |

## Final coverage

| Category | Result |
| --- | --- |
| Safe stages reviewed | 26/26 non-mirror quest-stage values, with invalid puzzle branches excluded |
| Safe stages runtime validated | 0/26 |
| Manual branches | Stages 110/111 mirror sequence; unknown keys; retry exhaustion |
| Unsupported branches | Reactive mirror mechanic and puzzle reset/help-only steps |
| Agent recovery branches | None |
| Final support state | `ATTENDED_PILOT` — runtime validation pending |

## Open risks and next work

- Exercise each stage with the agentic testing loop and record immutable status
  snapshots without account identifiers.
- Verify the stage-120 `Fight` interaction remains non-combat on the reviewed
  client revision.
- Observe the exact damaged-wall routing failure before adding recovery beyond
  the bounded route reset.
