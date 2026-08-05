# Control Center UI prototype

Throwaway UI exploration for GitHub issue #27: “What should the local, single-client plugin control center look like?”

## Verdict

**Variant A — Operations desk** was selected on 2026-08-05 as the production layout direction.

The implementation should preserve its information hierarchy: an eligible-plugin rail for fast switching, a focused center workspace for lifecycle/health/state/logs, and a persistent right-side visual-check panel. The prototype remains a visual primary source only; its simulated state and interactions must not be promoted as production logic.

Run from the repository root:

```sh
./scripts/serve-control-center-prototype
```

The server binds to `0.0.0.0:4173`. Open <http://localhost:4173/?variant=A> locally, or use the host address exposed by your development environment.

The three variants deliberately disagree about hierarchy:

- `A` — Operations desk: plugin-first, dense master/detail layout.
- `B` — Signal board: health-first scan, with a state timeline and logs as the main work surface.
- `C` — Field monitor: screenshot-first focus mode with controls overlaid around the current game view.

Use the floating arrows or keyboard left/right arrows to switch. Start/stop, plugin selection, log level, capture, and refresh are local simulations. The state inspector shows the complete in-memory prototype state after every interaction. No requests leave the browser and nothing persists across reloads.

This directory is not registered with Agent Server and is not production implementation code. After a direction is selected, capture this prototype on a throwaway branch and implement the winning decisions cleanly in the real dashboard.
