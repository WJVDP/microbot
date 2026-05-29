# ADR 0005: Plugin Jar Entry Manifest

- Status: Accepted (2026-05-19)

## Context

Plugin Runtime V2 needs to discover plugin entry classes from jar metadata before
loading arbitrary plugin classes. RuneLite Plugin Hub jars already include a
jar-local `runelite_plugin.json` stub parsed as `PluginHubManifest.Stub`, and
that stub contains a `plugins` array with the plugin entry class names.

Microbot Hub currently stores hub and compatibility metadata in
`MicrobotPluginManifest`, including fields such as `internalName`,
`displayName`, `version`, `minClientVersion`, `sha256`, `disable`, tags, and
version metadata. It does not declare jar-local entry classes, so Microbot Hub
and sideloaded jars still depend on whole-jar scanning and
`@PluginDescriptor` filtering.

## Decision

Microbot Hub jars will reuse RuneLite's jar-local `runelite_plugin.json` format
for entry classes. The required field for Plugin Runtime V2 discovery is
`plugins`, containing fully qualified plugin class names.

Microbot-specific metadata remains outside this jar-local entry stub unless the
runtime needs jar-local data that RuneLite's format cannot represent. Examples
include Microbot permissions, capabilities, trust policy, or hub-only
marketplace metadata. Those fields should be added through Microbot's existing
remote manifest first, or through a separate Microbot-specific extension only
when the data must travel inside the jar.

## Consequences

- RuneLite-compatible plugin jars and Microbot Hub jars can share the same
  entry-class reader.
- Plugin Runtime V2 can read entry classes without whole-jar classpath scanning
  when `runelite_plugin.json` is present.
- Existing Microbot jars without `runelite_plugin.json` need a compatibility
  path during migration.
- The next implementation slice is to read `runelite_plugin.json` from
  Microbot Hub and local artifact jars during metadata discovery, populate
  `PluginArtifact.entryClasses`, and retain scanning only as a legacy fallback.
