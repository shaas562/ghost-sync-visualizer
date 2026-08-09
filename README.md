# Ghost Sync Visualizer

Client-side Fabric mod for detecting confirmed client/server ghost items and ghost blocks and rendering configurable visual overlays.

## Status

Early development. The current milestone builds the version-independent detection core, Docker test workflow, and Minecraft 26.2 Fabric integration skeleton before any rendering or packet interception is added.

## Accuracy rule

False positives are worse than false negatives. An object is never rendered as a ghost unless fresh server-authoritative evidence confirms that the client is displaying something the server says is absent.
