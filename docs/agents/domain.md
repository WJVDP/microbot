# Domain Docs

This is a single-context repository for agent-skill purposes.

## Before exploring, read these

- `CONTEXT.md` at the repo root for project language and term boundaries.
- `docs/decisions/` for ADRs that touch the area you are about to work in.
- Area-specific `AGENTS.md` files when working under scoped directories such as `runelite-client/src/main/java/net/runelite/client/plugins/microbot/`.

If a domain doc does not exist for a topic, proceed with the code and existing docs. The `/grill-with-docs` skill creates or updates glossary and ADR material lazily when terms or decisions are resolved.

## Layout

```text
/
├── CONTEXT.md
├── docs/
│   └── decisions/
│       ├── adr-0001-record-architecture-decisions.md
│       └── ...
└── runelite-client/
```

## Use the glossary's vocabulary

When output names a project concept in an issue title, refactor proposal, hypothesis, test name, or implementation note, use the term as defined in `CONTEXT.md`. Do not drift to synonyms the glossary explicitly avoids.

If the concept you need is not in the glossary yet, either reconsider whether it is project-specific language or note it for `/grill-with-docs`.

## Flag ADR conflicts

If planned work contradicts an accepted decision in `docs/decisions/`, surface the contradiction explicitly before implementing or posting a final recommendation.
