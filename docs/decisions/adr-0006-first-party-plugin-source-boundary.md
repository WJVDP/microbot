# ADR 0006: First-party Plugin Source Boundary

- Status: Accepted (2026-08-07)

## Context

First-party automation plugins change independently from the RuneLite client
fork. Keeping new plugins inside `runelite-client` makes upstream synchronization
more conflict-prone, while placing them in a separate repository would add
artifact publishing, compatibility-version management, and a multi-checkout
development workflow before a stable standalone plugin API exists.

## Decision

Keep first-party automation plugins in the same Git repository under
`microbot-plugins`, separate from the RuneLite client source tree. Add its
standard main and test Java/resource directories to the `:client` source sets.

Keep client integration, runtime infrastructure, shared APIs, caches, utilities,
state-machine infrastructure, UI, and Agent Server code under `runelite-client`.
Use the external plugin mechanism for third-party plugins or plugins that require
independent releases.

## Consequences

- Developers retain one checkout, one IDE import, and the existing `:client`
  compile, run, test, quality, and assemble commands.
- First-party plugins continue to ship in the shaded client jar.
- Upstream RuneLite synchronization is less likely to conflict with new
  automation-plugin sources.
- The source boundary is organizational rather than a binary API boundary;
  plugins may still use client-internal Microbot APIs.
- Moving first-party plugins into a separate repository remains deferred until
  a stable versioned plugin API and local external-plugin development workflow
  justify the additional release management.
