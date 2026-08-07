# Microbot

RuneLite fork with a hidden always-on plugin hosting automation scripts. Composite Gradle build, Java 11 target (JDK 17+ to develop).

## Build / validate
- Compile: `./gradlew :client:compileJava`
- Full: `./gradlew buildAll`
- Shaded jar: `./gradlew :client:assemble`
- Tests (opt-in): `:client:runUnitTests`, `:client:runTests`, `:client:runIntegrationTest` (needs running game)

## Non-negotiable rules
- Never instantiate caches/queryables directly — use `Microbot.getRs2XxxCache().query()` / `.getStream()`. See `runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md`.
- Never block/sleep on the client thread.
- Never use static sleeps to wait for game state — use `sleepUntil(condition, timeoutMs)`.
- Keep `MicrobotPlugin` hidden/always-on; don't break its config panel wiring.
- Respect existing Checkstyle/Lombok patterns; don't weaken security (telemetry tokens, HTTP clients).
- Minimal logging; no PII or session identifiers.

## Plan plugin convention
- New automation plugins created for this workspace must use the `Plan<PluginName>` Java prefix, for example `PlanWoodcutterPlugin`, `PlanWoodcutterScript`, and `PlanWoodcutterConfig`.
- Keep new first-party automation plugins under `microbot-plugins/src/main/java/net/runelite/client/plugins/microbot`; runtime infrastructure and shared utilities remain under `runelite-client`.
- Use `PluginDescriptor.Plan + "PluginName"` so the Microbot plugin list renders a green `[Plan]` badge.
- Scaffold with `./scripts/create-plan-plugin PluginName`; template and decision-model guidance live in `scripts/plan-plugin-template/README.md`.
- Prefer `StateMachineScript` for 3+ phases. Order urgent recovery/requirement guards before normal work; guards inspect state, state actions perform interactions.

## Review priority
- **P0:** client crashes, client-thread blocking, login/world-hop breakage, cache invariant corruption, credential/token exposure.
- **P1:** script loop timing, overlay correctness, plugin discovery/config, shaded-jar packaging, build reproducibility.

## Runtime tooling
- `./microbot-cli` (JSON output) — see `docs/MICROBOT_CLI.md`, HTTP API `docs/AGENT_SERVER.md`, full tool list `docs/AGENT_SCRIPT_TOOLS.md`.
- Offline OSRS Wiki lookup for development context: `./microbot-cli wiki "<item, NPC, object, or mechanic>" [--limit N]`.
- Agent Server plugin runs on port 8081 by default.
- Offline client-thread lookup: `./microbot-cli ct <method>`.
- Test mode: `-Dmicrobot.test.mode=true -Dmicrobot.test.script=<PluginName>` → results in `~/.runelite/test-results/`. Protocol: `docs/AGENTIC_TESTING_LOOP.md`.

## In-game settings
Use the settings search bar — tab indices shift on updates. Verify changes via `./microbot-cli varbit <id>`.

## Before touching `microbot/util/`
Read `docs/entity-guides/README.md`. Add a gotcha there when you fix an entity-assumption bug.

## Docs maintenance
- Keep root `README.md`, `docs/README.md`, and `docs/INDEX.md` short routing pages.
- Put volatile command details, API examples, endpoint lists, generated inventories, and screenshots in the narrowest owning doc.
- Prefer links to owner docs over copying the same guidance into multiple high-level files.

## Model routing and handoffs

Choose models by the reasoning and autonomy the task needs: think expensive, type cheap. Official guidance positions GPT-5.6 Sol for frontier capability, Terra for the intelligence/cost balance, and Luna for efficient high-volume work. GPT-5.6 supports `none`, `low`, `medium`, `high`, `xhigh`, and `max` reasoning effort; use the lowest effort that reliably meets the task.

| Task | Preferred model / effort |
| --- | --- |
| Architecture, system design, or requirements to technical specification | GPT-5.6 Sol — high |
| Implementation planning | GPT-5.6 Sol — high (medium when the design is already settled) |
| Substantial feature implementation | GPT-5.6 Sol — medium/high |
| Normal everyday coding | GPT-5.6 Terra — medium |
| Difficult debugging or cross-cutting root-cause analysis | GPT-5.6 Sol — high |
| Simple refactors, boilerplate, or mechanical changes | GPT-5.6 Luna — low/medium, or Terra — low/medium |
| Code review | GPT-5.6 Sol — medium/high; use high for security, concurrency, or architecture |
| Tests | GPT-5.6 Terra — medium |
| README, API docs, and comments | GPT-5.6 Terra or Luna — low/medium |
| Tiny interactive questions | GPT-5.5 Instant when that ChatGPT mode is available; it is not a subagent model target |

Before substantial work, classify the task using this table:

- If the task divides cleanly into a concrete, bounded subtask and subagents with model overrides are available, delegate that subtask using the preferred model and reasoning effort. Give the subagent the goal, relevant repository context, constraints, expected deliverable, and validation commands. Do not delegate solely to change models when coordination would cost more than the work.
- Follow any active system limits on delegation, concurrency, model availability, and model overrides. Never invent a model slug or silently claim that a requested model was used.
- If the current agent is materially underpowered for a substantial task and cannot start an appropriate subagent, pause before implementation, recommend the model/effort to the user, and provide a copy-ready handoff message. Include completed work, current repository state, decisions, unresolved questions, exact next action, relevant files, and validation commands.
- For routine or low-risk work, continue with the closest available model when the likely quality difference is immaterial, and mention the substitution briefly.

Use this handoff shape:

```text
Switch to: <model> — <reasoning effort>
Goal: <desired outcome>
Repository state: <branch/worktree and completed work>
Decisions and constraints: <source-of-truth requirements>
Relevant files: <paths>
Next action: <specific work to perform>
Validate with: <commands and acceptance criteria>
Open questions/risks: <remaining uncertainty>
```

For a non-trivial new application or feature, prefer this sequence:

1. Architecture — Sol high: establish boundaries, components, data flows, APIs, persistence, failure modes, security, alternatives, and trade-offs.
2. Specification — Sol high: make interfaces, data models, contracts, validation, edge cases, error handling, and acceptance criteria the source of truth.
3. Implementation plan — Sol high/medium: create small dependency-ordered tasks that leave the repository working after each step.
4. Implementation — Codex with Sol medium/high for important features or Terra medium for routine tickets; inspect the repository, edit, test, diagnose failures, and iterate.
5. Review — Sol high: compare the diff with the specification, focusing on correctness, omissions, security, concurrency, abstractions, and unnecessary complexity.

For serious projects, keep durable decisions in the narrowest appropriate owner documents, typically `docs/architecture.md`, `docs/specification.md`, and `docs/implementation-plan.md`; update existing domain docs or ADRs instead of duplicating them. GPT-5.6 has a 1.05M-token context window, but source-of-truth artifacts are still preferable to repeatedly reconstructing decisions from prompts.

Current model guidance: https://developers.openai.com/api/docs/guides/latest-model

## Deeper guides
- Script authoring & threading: `runelite-client/.../microbot/AGENTS.md`
- State machines (use for 3+ phase scripts): `.../microbot/statemachine/AGENTS.md`
- Architecture: `docs/ARCHITECTURE.md`, `docs/decisions/`
- Setup: `docs/development.md`, `docs/installation.md`

## Agent skills

### Issue tracker

Issues and specs are tracked in GitHub Issues using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the five canonical triage labels without repository-specific aliases. See `docs/agents/triage-labels.md`.

### Domain docs

This is a single-context repository with root domain context and ADRs under `docs/decisions/`. See `docs/agents/domain.md`.
