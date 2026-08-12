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

## Hybrid quest onboarding

- An instruction to “add quest X to PlanQuesting” means follow `docs/questing/ADDING_A_QUEST.md` completely; adding only a config enum is not sufficient.
- Create or update `docs/questing/quests/<quest-slug>.md` from `docs/questing/QUEST_DOSSIER_TEMPLATE.md`.
- State the achieved support level (`AUDITED`, `OBSERVATION_ONLY`, `ATTENDED_PILOT`, `AGENT_RECOVERY_ENABLED`, or `RUNTIME_VALIDATED`) and never claim runtime validation from source inspection alone.
- Unknown, dangerous, or structurally unidentified quest steps fail closed to manual control. Display text must not be the sole safety key, and an agent may never downgrade a manual gate.

## In-game settings
Use the settings search bar — tab indices shift on updates. Verify changes via `./microbot-cli varbit <id>`.

## Before touching `microbot/util/`
Read `docs/entity-guides/README.md`. Add a gotcha there when you fix an entity-assumption bug.

## Docs maintenance
- Keep root `README.md`, `docs/README.md`, and `docs/INDEX.md` short routing pages.
- Put volatile command details, API examples, endpoint lists, generated inventories, and screenshots in the narrowest owning doc.
- Prefer links to owner docs over copying the same guidance into multiple high-level files.

## Discussion scope

While discussing ideas, brainstorming, or doing high-level planning, do not start implementing. Only make code or repository changes when the user explicitly asks for implementation or explicitly agrees to proceed with it.

## Pull request target safety

- This checkout uses `origin` for `WJVDP/microbot` and `upstream` for `chsami/Microbot`. Unless the user explicitly names another repository, create pull requests against `WJVDP/microbot`, never `chsami/Microbot`.
- Do not infer the PR target from `gh repo view` alone; it may resolve to `upstream`. Resolve and display the owner/repository for every remote with `git remote -v` before creating a PR.
- Before any `gh pr create`, compare the head branch with the proposed base and verify that the PR contains only the intended commits and files (for example with `git log <base>..HEAD` and `git diff --stat <base>...HEAD`).
- If the proposed target differs from `origin`, stop and obtain explicit user confirmation naming that owner/repository before opening the PR. Never open a speculative PR and correct it afterward.
- Immediately after creation, verify the PR URL, base repository, base branch, head branch, commit count, and changed-file count. If any value is unexpected, close the PR before doing anything else and report the mistake.

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
