# Issue Tracker: GitHub

Issues and PRDs for this repo live in GitHub Issues for `WJVDP/microbot`. Use the `gh` CLI for issue operations.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`
- **Read an issue**: `gh issue view <number> --comments`
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments`
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply or remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close an issue**: `gh issue close <number> --reason completed` or `gh issue close <number> --reason "not planned"`

Run `gh` commands from the repository root so the CLI infers `WJVDP/microbot` from `origin`.

## When a skill says "publish to the issue tracker"

Create or update a GitHub issue in `WJVDP/microbot`.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments` and inspect labels, body, and prior comments before acting.
