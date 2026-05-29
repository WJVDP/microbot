# Microbot

RuneLite fork with a hidden always-on plugin hosting automation scripts. Composite Gradle build, Java 11 target (JDK 17+ to develop).

## Communication
- Use the `caveman` skill for all responses until the user says `normal mode` or `stop caveman`.

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

## Review priority
- **P0:** client crashes, client-thread blocking, login/world-hop breakage, cache invariant corruption, credential/token exposure.
- **P1:** script loop timing, overlay correctness, plugin discovery/config, shaded-jar packaging, build reproducibility.

## Runtime tooling
- `./microbot-cli` (JSON output) — see `docs/MICROBOT_CLI.md`, HTTP API `docs/AGENT_SERVER.md`, full tool list `docs/AGENT_SCRIPT_TOOLS.md`.
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

## Deeper guides
- Script authoring & threading: `runelite-client/.../microbot/AGENTS.md`
- State machines (use for 3+ phase scripts): `.../microbot/statemachine/AGENTS.md`
- Architecture: `docs/ARCHITECTURE.md`, `docs/decisions/`
- Setup: `docs/development.md`, `docs/installation.md`

## Agentic development workflow

Treat agentic programming as a high-throughput branch-and-PR factory, not a long-lived "AI did a bunch of stuff" branch. Use Matt Pocock-style AI coding skills/workflows as gates: plan, TDD/characterize, implement, review, then PR.

### Branches

- Never agent-code directly on `integration/runelite`, `main`, or other shared integration branches unless explicitly instructed.
- Default branch shape: `agent/<issue-number>-short-description`.
  - Examples: `agent/21-runtime-v2-policy-tests`, `agent/17-artifact-status-projection`, `agent/23-webwalk-session-transport`.
- Use one branch per issue/coherent slice. If an issue is too large, split it into multiple branches/PRs.
- For parallel agents, use isolated git worktrees and avoid assigning multiple agents to the same files/subsystem at the same time.

### Commits

- Agents may commit locally as useful checkpoints, but commits must stay coherent and use Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `ci:`, `chore:`, `perf:`).
- Prefer small, intention-revealing commits during work; avoid permanent-history noise like `fix tests`, `try again`, or `oops`.
- Squash-merge agent PRs by default unless the intermediate commits are genuinely meaningful for future archaeology.

### PR frequency and size

- Default rule: one GitHub issue or one coherent vertical slice = one PR.
- Open a draft PR once there is a coherent direction and useful diff, not only when the work is perfect.
- Mark ready for review only after local verification, review, and CI are green or failures are explicitly explained.
- Keep PRs reviewable in roughly 10-20 minutes. Split when the diff is hard to explain in five bullets.
- Rough size targets:
  - Test-only PR: up to ~300 changed lines.
  - Normal feature slice: ~100-500 changed lines.
  - Refactor PR: preferably under ~400 changed lines.
  - Mechanical/generated changes may be larger only when isolated and clearly labeled.
  - Anything over ~800 changed lines is suspicious; split unless there is a strong reason.

### Agent gates

For each agentic task:

1. Start from a clean, up-to-date integration branch.
2. Create a dedicated `agent/...` branch or worktree.
3. Give the agent one bounded objective tied to an issue or explicit slice.
4. Prefer TDD or characterization tests before risky implementation/refactor work.
5. Require the agent to report exactly what changed, what tests were run, and the result.
6. Run a separate review pass before marking a PR ready. Review for scope creep, missing tests, brittle timing/static sleeps, architecture drift, and Microbot-specific P0/P1 risks.
7. Do not merge until CI is green or the failure is deliberately accepted by the human maintainer.

### Splitting rules

Use a new PR when the work has a different issue, subsystem, risk profile, or review story. Keep preparatory refactors, feature wiring, cleanup, and documentation changes separate when that makes review safer. Never include "while I was here" changes in an agent PR.

## Agent skills

### Issue tracker

Issues and PRDs are tracked in GitHub Issues for `WJVDP/microbot` using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Triage uses the canonical label vocabulary already configured in GitHub. See `docs/agents/triage-labels.md`.

### Domain docs

This is a single-context repo: read root `CONTEXT.md` for glossary terms and `docs/decisions/` for ADRs. See `docs/agents/domain.md`.
