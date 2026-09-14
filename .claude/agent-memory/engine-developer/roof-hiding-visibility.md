---
name: roof-hiding-visibility
description: How roof hiding reaches the modern renderer (per-tile visible set + per-record roof rule), the #3 repro recipe, and the lesson that any chunk/block-level visibility cache must still honour per-tile roof exclusion
metadata:
  type: project
---

Roof hiding is per TILE: `Scene.drawTiles` writes `tileOnScreen` per tile from
`settingBits[ownerPlane][x][y] != roofPreference`, and `GroundGL.renderFloor` turns that into
`visibleTileIndices`. Legacy `GroundMaterialGL.drawMaterialTriangles` walks exactly that set.
Statics use the same rule per record (`Scene.isHidden`, decided at the first footprint tile inside
the window); `RetainedStaticScene.recordVisible` mirrors it and was verified equal 2026-09-08.

**darkan-bot#3 (2026-09-08) root cause:** the modern ground path concatenated whole 8x8-chunk index
lists for any chunk with one visible tile, so the upper floor over a hidden roof drew as a flat
black slab (the "roof geometry clipped" report). Fix: chunk lists carry per-slice tile ids and are
filtered against the per-tile bitmap (`groundVisibleBits`). Memory note in the user's global memory
had wrongly ruled the ground out ("retained/instancing/cull/occlusion ruled out") - it never listed
the ground.

**Why:** any cache keyed coarser than a tile (chunk index lists, retained blocks) silently reintroduces
roof geometry; "a few extra border tiles are harmless" is false once roof hiding is on.

**How to apply:** when touching ground index caching or retained visibility, re-run the repro:
`HW=1 EXTRA="-Dautologin.command=tele+2894+3427+0 -Dautologin.roofs=2 -Dautologin.camera=1600,0"
tools/capture.sh 6 out.png 20` (Burthorpe bank; toolkit 1 as control, 90 s between runs for the
shared account). Black straight-edged slab top-right = regression. Reproduces headlessly too (the
ground path is not bindless-gated).
