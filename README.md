# Ghost Sync Visualizer

Client-side Fabric mod for detecting confirmed client/server ghost items and ghost blocks and rendering configurable visual overlays.

## Status

Early development. Milestone 1 establishes the version-independent detection core, Docker test workflow, and Minecraft 26.2 Fabric client skeleton before packet interception or rendering is added.

## Accuracy rule

False positives are worse than false negatives. An object is never rendered as a ghost unless fresh server-authoritative evidence confirms that the client is displaying something the server says is absent.

Time alone can never turn a pending discrepancy into a confirmed ghost.

## Modules

- `core` - pure Java certainty engine with no Minecraft dependencies.
- `fabric` - client-only Minecraft/Fabric adapter. Packet hooks and rendering will be added in later milestones.

## Verify with Docker

```bash
docker compose run --rm --build test
```

This runs the core regression tests and builds the Minecraft 26.2 Fabric module using Java 25.

## Current target

- Minecraft 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Fabric Loom 1.17-SNAPSHOT
- Java 25

## Next milestone

Translate real client/server synchronization events into normalized core events for block and inventory detection, while preserving the confirmed-only rule.
