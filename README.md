# Ghost Sync Visualizer

Client-side Fabric mod for detecting confirmed client/server ghost blocks and ghost items and rendering configurable visual feedback without changing gameplay state.

## Accuracy guarantee

False positives are treated as worse than false negatives. A discrepancy is never rendered as a ghost just because it persists or looks suspicious.

A ghost can be confirmed only after fresh server-authoritative evidence is paired with the client state produced by that same update. Old snapshots, timers, background scans, and manual refreshes cannot manufacture confirmation.

## Current Minecraft 26.2 implementation

- Server block updates and multi-block section updates feed the detector after vanilla applies them.
- Vanilla block prediction resolution supplies the server-known block state; ACK sequence numbers alone are never treated as proof.
- Container slot/content packets and player-inventory packets provide authoritative item evidence.
- Chunk unloads, container changes, world changes, and disconnects invalidate the relevant cached state.
- Confirmed ghosts are cached separately so render paths never scan the detector or promote certainty.
- Block visuals preserve the baked resource-pack model. Original block alpha and the white model-shaped overlay are independent controls.
- Item visuals apply original-icon alpha at Minecraft 26.2's final GUI item-atlas blit, so ordinary and special item models use the same transparency path.
- The item white-overlay pass is independent from original-icon alpha and can be refined without changing detection semantics.
- `K` performs a safe local refresh of tracked state. It does not fake an action or force the server to resend data.
- Mod Menu 20.0.1 is supported as an optional settings entry point.

## Modules

- `core` — pure Java certainty engine with no Minecraft dependencies.
- `fabric` — Minecraft 26.2/Fabric packet, lifecycle, settings, and rendering adapter.

## Verify with Docker

```bash
docker compose run --rm --build test
```

This runs the regression suite and builds the Fabric module with Java 25.

## Build an installable jar with Docker

```bash
rm -rf dist
docker compose run --rm --build package
```

The remapped Fabric jar is written to `dist/`. Pull-request CI runs the same Docker verification and publishes the jar as the `ghost-sync-visualizer-26.2` workflow artifact.

## Current target

- Minecraft 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Fabric Loom 1.17-SNAPSHOT
- Java 25
- Mod Menu 20.0.1 (optional)

## Rendering diagnostics

Minecraft 26.2 rendering internals can be inspected with the manual **Render diagnostics** GitHub Actions workflow. These probes are intentionally kept out of normal CI after the relevant API shapes have been verified.
