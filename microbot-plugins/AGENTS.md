# First-party automation plugins

This tree contains new first-party Plan plugins. Follow the repository-root
`AGENTS.md` and the script/threading guidance in
`runelite-client/src/main/java/net/runelite/client/plugins/microbot/AGENTS.md`.

- Scaffold with `./scripts/create-plan-plugin PluginName`.
- Keep the `Plan<PluginName>` Java prefix and `PluginDescriptor.Plan` badge.
- Prefer `StateMachineScript` for scripts with three or more phases.
- Put reusable runtime APIs and utilities in the owning `runelite-client`
  package rather than duplicating them here.
