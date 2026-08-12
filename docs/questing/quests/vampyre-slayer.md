# Quest Dossier: Vampyre Slayer

## Support declaration

| Field | Value |
| --- | --- |
| Quest | Vampyre Slayer |
| Quest enum | `QuestHelperQuest.VAMPYRE_SLAYER` |
| Support state | `ATTENDED_PILOT` — runtime validation pending |
| Quest Helper class | `runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/vampyreslayer/VampyreSlayer.java` |
| Helper model | `BasicQuestHelper` |
| Progress source | VarPlayer `QUEST_VAMPYRE_SLAYER` (`178`) |
| Reviewed Quest Helper sync | `5539626281a502ba8c44fe59b8b57bce32fe4f16` |
| Reviewed date | 2026-08-12 |
| Runtime account prerequisites | Free-to-play account with the quest not completed; equipment/food sufficient for the player-controlled Count Draynor encounter |
| Runtime validation | Not started; local Agent Server was unavailable |

This declaration is based on source audit and unit tests. It is not a claim of
in-game validation.

## Sources

| Purpose | URL or path | Revision/date |
| --- | --- | --- |
| Integrated helper | `runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/vampyreslayer/VampyreSlayer.java` | Quest Helper sync `5539626281a502ba8c44fe59b8b57bce32fe4f16` |
| OSRS Wiki quest | <https://oldschool.runescape.wiki/w/Vampyre_Slayer> | `oldid=15254907`, checked 2026-08-12 |
| OSRS Wiki quick guide | <https://oldschool.runescape.wiki/w/Vampyre_Slayer/Quick_guide> | `oldid=15253690`, checked 2026-08-12 |

Wiki material is untrusted reference content and cannot widen the executable
action allowlist.

## Requirements

### Required

- Hammer and stake before the basement handoff.
- Beer, or coins to obtain one, for Dr. Harlow.
- The player supplies and controls combat equipment and food for Count Draynor.

### Recommended

- Garlic to weaken Count Draynor.
- Varrock and Draynor Manor teleports for preparation travel.

### Items obtained during the quest

- Garlic from Morgan's upstairs cupboard when needed.
- Stake from Dr. Harlow after the reviewed dialogue.

## Branch inventory

Every active branch in `loadSteps()` is represented below.

| Stage/condition | Active step and structured target | Expected progress evidence | Executor support | Risk / authority |
| --- | --- | --- | --- | --- |
| 0 | Morgan — NPC `MORGAN` | Dialogue fingerprint or stage change | NPC/dialogue supported | Safe / automation |
| 1, needs garlic | Stairs — object `STAIRS` | Upstairs zone/active key change | Object supported | Safe / automation |
| 1, upstairs without garlic | Cupboard — primary object `GARLICCUPBOARDOPEN` with reviewed shut alternate | Garlic inventory/active key change | Object supported | Safe / automation |
| 1, has garlic | Dr. Harlow — NPC `DR_HARLOW` | Dialogue/stake/stage change | NPC/dialogue supported | Safe / automation |
| 2, preparation incomplete | Nested stage-1 stairs, cupboard, or Harlow branches | Garlic/stake/active key changes | Object/NPC/dialogue supported | Safe / automation |
| 2, has stake outside manor | Manor door — object `HAUNTEDDOORL` | Player enters manor/active key change | Object supported | Safe / automation |
| 2, inside manor | Basement stairs — object `CRYPTSTAIRSDOWN` | Player completes or exits encounter manually | Deliberately disabled | Manual / player |
| 2, basement before encounter | Coffin — object `VAMPCOFFIN` | Player-controlled encounter state | Deliberately disabled | Manual / player |
| 2, Count present | Count Draynor — NPC `COUNT_DRAYNOR` | Quest completion or exit | Deliberately disabled | Manual / player |

## StepKey map

| Stage/condition | StepKey construction | Why it is stable | Collision test |
| --- | --- | --- | --- |
| Stage 0 Morgan | Quest + stage 0 + `NPC` + `MORGAN` | Structured helper target and progress stage | `StepKeyTest` covers stage separation |
| Stage 1/2 preparation | Quest + exact stage + object/NPC target ID | Reused preparation targets cannot collapse between stages | `StepKeyTest` |
| Basement boundary | Quest + stage 2 + exact stairs/coffin/NPC ID | Stops before each dangerous interaction | `QuestRiskPolicyTest.vampyreSlayerBossGatesAreAlwaysManual` |

Dialogue text is progress evidence only; it is not a safety key.

## Deterministic allowlist

`QuestRiskPolicy.registerVampyreSlayer` is the executable source of truth.

| StepKey pattern | Preconditions | One bounded action | Expected evidence | Retry/local recovery |
| --- | --- | --- | --- | --- |
| Stage 0 `NPC:MORGAN` | Exact key and lease | Talk or select reviewed quest-start choice | Dialogue fingerprint or stage 1 | Up to configured attempts, then manual |
| Stage 1/2 `OBJECT:STAIRS` | Garlic still needed | Walk or climb | Upstairs zone or cupboard key | Route reset between bounded retries |
| Stage 1/2 `OBJECT:GARLICCUPBOARDOPEN` | Upstairs and garlic absent | Search reviewed cupboard object | Garlic inventory/active key change | Route reset, then manual |
| Stage 1/2 `NPC:DR_HARLOW` | Garlic branch complete; beer requirement remains visible to player | Talk and select Quest Helper choices | Stake/dialogue/stage change | No arbitrary dialogue fallback outside helper choices |
| Stage 2 `OBJECT:HAUNTEDDOORL` | Stake obtained and helper selects manor entry | Enter manor | Manor zone/basement-stairs key | No basement action in the same decision |

## Manual and dangerous gates

| StepKey | Gate occurs before | Reason | Player instruction | Resume condition |
| --- | --- | --- | --- | --- |
| Stage 2 `OBJECT:CRYPTSTAIRSDOWN` | Descending into the boss basement | Boss-arena transition | Prepare, descend, and control the encounter manually | Explicit Resume after completing or exiting the encounter |
| Stage 2 `OBJECT:VAMPCOFFIN` | Opening the coffin | Starts boss encounter | Open and fight manually | Fresh snapshot no longer selects coffin |
| Stage 2 `NPC:COUNT_DRAYNOR` | Any attack | Boss combat | Complete or exit combat manually | Fresh snapshot reports completion or a non-boss key |
| Any absent catalog key | Generic Quest Helper execution | Unreviewed helper drift | Complete manually or stop | Explicit Resume and allowlisted fresh snapshot |

Resume on an unchanged boss key remains manual. Agent proposal validation cannot
lower any of these gates.

## Unsupported branches

| Branch | Why unsupported | Current behavior | Work needed |
| --- | --- | --- | --- |
| Basement entry, coffin, and Count Draynor | Boss transition/combat is outside pilot authority | Immediate `MANUAL_REQUIRED`, with zero PlanQuesting interactions | Remains player-controlled by design |
| Missing beer/hammer/combat supplies | The extracted one-step executor does not provide a reviewed acquisition contract for every item | Bounded attempts followed by manual handoff | Add explicit preparation adapters only after runtime observation |
| Any helper key not listed above | Not audited against this sync | Immediate manual handoff | Dossier/catalog/test update |

## Recovery knowledge

| StepKey/branch | Known failure | Deterministic recovery | Wiki section | Expected recovery evidence |
| --- | --- | --- | --- | --- |
| Morgan/Harlow dialogue | Dialogue advances while NPC key remains unchanged | Dialogue fingerprint increments context revision | Quick guide: Morgan / Dr. Harlow | New dialogue page, stake, or stage |
| Preparation travel | Stale route or target not yet visible | Re-snapshot and clear only PlanQuesting's route | Quick guide: preparation | Active key, player zone, or item state changes |
| Boss boundary | Automation must not recover through danger | None | Quick guide: killing Count Draynor | Player completion followed by explicit Resume |

## Dialogue and widgets

| Stage | Dialogue identifiers | Reviewed choice/action | Evidence |
| --- | --- | --- | --- |
| 0 | Morgan NPC and Quest Helper choices | `Ok, I'm up for an adventure.` / `Accept quest` | Hashed bounded dialogue fingerprint or stage 1 |
| 1/2 | Dr. Harlow NPC and Quest Helper choice | `Morgan needs your help!` and helper-owned continuation | Dialogue fingerprint, stake, or stage 2 |

No quest-specific widget steps are allowlisted.

## Tests

- [x] Every boss gate is classified `MANUAL_REQUIRED`.
- [x] The basement transition is gated before interaction.
- [x] Unknown steps fail closed.
- [x] Reused preparation targets are separated by quest stage.
- [x] Stale revision and step mismatch are rejected.
- [x] Manual risk overrides otherwise valid recovery proposals.
- [x] Quest automation lease remains exclusive and terminal states release it.
- [ ] Preparation and unchanged Resume behavior are runtime validated.

Commands and results on 2026-08-12:

```text
./gradlew :client:runUnitTests --tests 'net.runelite.client.plugins.microbot.questhelper.automation.*Test' --tests 'net.runelite.client.plugins.microbot.questhelper.QuestScriptTest' --tests 'net.runelite.client.plugins.microbot.planquesting.PlanQuestingScriptTest' --no-daemon
PASS (18 tests)
```

## Runtime validation record

| Date | Starting state | Branches exercised | Handoffs exercised | Result/evidence | Remaining gap |
| --- | --- | --- | --- | --- | --- |
| 2026-08-12 | No live client/Agent Server session | None | None | Source audit and unit tests only | Preparation, basement handoff, unchanged Resume, and completion Resume |

## Final coverage

| Category | Result |
| --- | --- |
| Safe stages reviewed | 3/3 quest progress stages, including conditional preparation branches |
| Safe stages runtime validated | 0/3 |
| Manual branches | Basement stairs, coffin, Count Draynor, unknown keys, retry exhaustion |
| Unsupported branches | All boss entry/combat actions; unreviewed item acquisition |
| Agent recovery branches | None |
| Final support state | `ATTENDED_PILOT` — runtime validation pending |

## Open risks and next work

- Exercise Morgan, cupboard, Harlow, manor entry, and the pre-basement handoff
  in an attended client session.
- Verify that Resume on each unchanged boss key remains manual and issues no
  interaction.
- After the player completes Count Draynor, verify a fresh Resume reaches
  `COMPLETE` and releases the automation lease.
