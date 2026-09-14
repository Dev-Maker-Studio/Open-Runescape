---
name: player-bob-not-reproduced
description: darkan-bot#27 (own player bobs vertically after re-login at 2798 3528 0 on modern) could NOT be reproduced on the RTX 3080 in four variants; every CPU-side vertical input is constant; what was ruled out and what remains
metadata:
  type: project
---

Tried 2026-09-08 on HW=1, toolkit 6, all at the cliff tile (54,48 local = 2798,3528, baseY -384,
hill tilt rX=14510 rZ=2853 tY=-112): fresh-JVM re-login, default camera, same-JVM logout via
`-Dautologin.ifbutton=182:6:1` + auto re-login, and roofs=2 + `-Drender.msaa=4` + bloom. 12-30
frame bursts at 50-100 ms: the player's top row stays within 289-293 px (idle-animation
noise), no vertical alternation.

`-Drender.playerydiag` (PlayerEntity.draw, prints when any input changes) showed
baseY/prevY/projY/renderY/groundContouredOffsetY/translateY/rotateX/rotateZ/plane constant
across thousands of frames, pose-memo hit/miss alternating per 50 Hz cycle as designed, and
minY drifting -841..-832 (breathing). So the bob is not in the matrix, the interpolation, the
ground contour or the memo key.

Ruled out by reading: normal players never apply rotateX/rotateZ/translateY to the body
(only wide-model BAS do, same upstream: Player.method16133); the deferred and instanced
animated paths use the same matrix copy; PoseCopyPair slots own their vertex arrays;
copyModelData invalidates the stream geometry.

Still possible: reporter-specific state (a BAS with modelWidth/modelLength set, e.g. after
`;;bas`, would apply the tilt in place; an equipped item; a different roof/AA setting), or a
Y-only position jump (the interpolation snap test ignores Y) repeating for some reason on
their world. Next step needs the reporter's screenshots/settings or a `-Drender.playerydiag`
log from their client.
