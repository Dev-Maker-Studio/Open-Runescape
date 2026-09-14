---
name: ui-over-animated-model-flicker
description: darkan-bot#22 root cause — deferred (parallel) pose builds were invisible to hasPendingSceneGeometry, so on pose-change frames the UI did not flush the scene first and the animated model drew over translucent UI; verified by 16-frame HW bursts before/after
metadata:
  type: project
---

Symptom (2026-09-07 report): an ANIMATED player/NPC model (pnpc 50 KBD) under the translucent
chatbox flickers between "behind the chat" and "on top of the chat" frames; static models and
`bas 112` do not; needs text in the chat. Reproduced on the RTX 3080 (HW=1) at 33,33 with
`-Dautologin.command=tele+33+33+0;pnpc+50 -Dautologin.camera=1024,0,300`: 2 of 16 frames
showed the model over the chatbox.

**Cause:** `ModernGLToolkit.hasPendingSceneGeometry()` only asked the batchers. With
`-Drender.parallelbuild` (default on) a private pose copy whose geometry is stale is queued in
the deferred-build list (`dbCount`) and reaches the instanced batcher only at flush. On the
frame where the pose changed the walk left NOTHING in the batchers, so
`flushSceneForUiDraw()` at the first 2D call returned early; `setupTextRendering()` then
flushed the sprite batch (chatbox background) to the framebuffer, and the scene (with the
model) was flushed at frame end on top of it. Frames without a pose change went through
`animatedGeometryFor` -> `addInstance` (pending) and flushed correctly -> alternation.

**Fix:** `dbCount > 0 ||` in `hasPendingSceneGeometry()`.

**How to apply:** any new deferred submission list on the modern path must be counted as
pending scene geometry, or painter's order breaks for whichever entities use it. The
before/after A/B was done by building a reverted jar into the scratchpad (`JAR=` override on
the burst script) — worth the extra build; the "after" capture alone proves nothing.
