# Ghost Sync Visualizer

Client-side Fabric mod for detecting confirmed client/server ghost blocks and ghost items and rendering configurable visual feedback without changing gameplay state.

## Accuracy guarantee

False positives are treated as worse than false negatives. A discrepancy is never rendered as a ghost just because it persists or looks suspicious.

A ghost can be confirmed only after fresh server-authoritative evidence is paired with the client state produced by that same update. Old snapshots, timers, background scans, and manual refreshes cannot manufacture confirmation.

## Current Minecraft 26.2 implementation

- Server block updates and multi-block section updates feed the detector after vanilla applies them.
- Vanilla block prediction resolution supplies the server-known block state; ACK sequence numbers alone are never treated as proof.
- Local place/use and attack/break interactions are watched only for client prediction changes; those observations can create `PENDING` state but cannot confirm a ghost.
- Container slot/content packets and player-inventory packets provide authoritative numbered-slot evidence.
- Player-inventory-backed menu slots normalize onto one connection-scoped identity, so container and player-inventory packets cannot disagree about the same physical slot.
- The carried/cursor stack has its own menu-scoped identity. Full-container content and cursor-item packets provide authority, local carried-stack changes require newer authority, and the cursor state is invalidated with its container epoch.
- Minecraft's creative-inventory exception is preserved: cursor-item packets ignored by vanilla in that screen are not treated as authoritative evidence by the mod.
- Chunk unloads, container changes, world changes, disconnects, and detector disablement invalidate the relevant cached certainty.
- Re-enabling a detector starts from fresh certainty; pre-disable confirmed ghosts cannot silently reappear.
- Confirmed ghosts are cached separately so render paths never scan the detector or promote certainty.
- Block visuals preserve the baked resource-pack model. Original block alpha and the white model-shaped overlay are independent controls.
- Item visuals apply original-icon alpha at Minecraft 26.2's final GUI item-atlas blit, including ordinary, special, and carried item models.
- Item white overlay uses the final item-atlas alpha mask for an icon/model-shaped white silhouette independently from original-icon transparency.
- The block Performance setting only coalesces expensive terrain-transparency rebuilds; it never changes evidence or confirmation.
- Advanced Technical logging reports confirmed block/item additions and removals without changing detector behavior.
- `K` performs a safe local refresh of tracked state. It does not fake an action or server evidence.
- Mod Menu 20.0.1 is supported as an optional settings entry point.

## Verify with Docker

```bash
docker compose run --rm --build test
```

This runs core regression/fuzz/replay tests, the Fabric headless Knot/Mixin integration probe, Minecraft 26.2 contract checks, standalone GLSL validation, and the Fabric build with Java 25.

## Build an installable jar with Docker

```bash
rm -rf dist
docker compose run --rm --build package
```

The Fabric jar is written to `dist/`. Pull-request CI runs the same verification and publishes `ghost-sync-visualizer-26.2`.

## First Minecraft test checklist

1. Install Minecraft 26.2, Fabric Loader 0.19.3, and Fabric API 0.156.0+26.2. Mod Menu 20.0.1 is optional but recommended.
2. Put the CI-built Ghost Sync Visualizer jar in the instance `mods` folder. For the first run, remove unrelated rendering mods.
3. Launch Minecraft. Confirm the title screen appears and `latest.log` has no Ghost Sync Mixin, shader, or resource-loading errors.
4. Open Ghost Sync settings through Mod Menu. Confirm all controls appear, save, and survive a restart.
5. Join a vanilla-compatible server and play normally for several minutes. Ordinary blocks/items must never highlight. This is the most important false-positive test.
6. Press `K` during normal play. Nothing should highlight merely because refresh was pressed.
7. Place, break, and use blocks normally. Move items between inventory, hotbar, chests, and the carried cursor. Temporary latency/prediction must not create persistent visuals without authoritative disagreement.
8. Reproduce a known block desync where the client still displays a block after fresh server-authoritative state says it is absent. Only that block should become a ghost.
9. On that block, test white overlay at 0%, middle, and 100%. It should follow the block/model shape.
10. Test block transparency separately at 0%, middle, and 100%. It must not alter confirmation or white-overlay strength.
11. Test ghost render distance by moving away and back. Rendering should obey the configured distance without manufacturing new confirmation.
12. Unload the chunk or change dimension after a confirmed block. The old visual must disappear and must not return at reused coordinates without fresh evidence.
13. Reproduce a known inventory/container desync where the client displays an item after fresh server-authoritative state says that physical slot is empty. Only that item should highlight.
14. Test item overlay at 0%, middle, and 100%. White must follow the actual icon silhouette/transparent pixels—not draw a white slot square.
15. Test item transparency independently at 0%, middle, and 100%. Stack count, durability, and normal GUI decorations should remain usable.
16. Test a carried/cursor ghost item. The effect should follow the carried icon and disappear when authority clears it or the container closes.
17. Test player inventory slots inside normal inventory and inside container menus, plus hotbar slots. One physical slot must never produce duplicate/conflicting ghosts.
18. Close/reopen containers repeatedly. Old slot/cursor confirmations must never leak into a newly opened container.
19. Disable block detection while a block ghost is visible, then re-enable it. It must not reappear without fresh authority. Repeat for item detection.
20. Move Performance from 1 through 5 around a confirmed block. Only rebuild responsiveness should change; certainty and item behavior must remain identical.
21. Enable Technical logging. Confirm one block/item confirmation and clear each. Logs should report transitions without affecting behavior.
22. Repeat normal-play false-positive tests with realistic latency if possible. Missing a real ghost is preferable to highlighting a normal object.
23. After the clean baseline passes, repeat key visual tests with your normal resource pack and other client rendering mods.
24. For any failure, save `latest.log`, a screenshot/video, exact Minecraft/Fabric/mod versions, settings, and reproduction steps before changing code.

## Current target

- Minecraft 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Fabric Loom 1.17-SNAPSHOT
- Java 25
- Mod Menu 20.0.1 (optional)
