---
name: bindless-material-table-lifecycle
description: Audit 2026-09-13 of the bindless per-draw material path (texture lifetime, handle table, revision cache) — material textures are never deleted (finalizer regression), which masks a latent stale-handle hole in the revision scheme
metadata:
  type: project
---

Material textures (`OpenGl2DTexture` via `OpenGLTextureCache`) are never deleted on the modern path:
`LRUCache.put/clean` only unlink or demote to a SoftReference, and `OpenGlTexture` lost the legacy
finalizer (upstream `Class137.method2363` = `finalize { dispose(); }`). So `bindlessHandles` entries are
immortal and `materialWordsRevision` only ever bumps on handle creation.

**Why it matters:** the revision scheme has a hole that this leak hides. `RetainedStaticScene` fills
`chunk.transMatWords` during the scene walk, i.e. under the `matEntryStamp` of the previous frame's
`drawTranslucent` pass (`beginMaterialPass` runs only in `endFrame`/`drawTranslucent`), and
`releaseTextureHandle` bumps the revision without clearing the memo. The moment material textures start
being deleted again (e.g. restoring the finalizer), a chunk refilled after a delete gets the dead handle
memoised with the new revision = persistent garbage on translucent templates (canopies).

**How to apply:** any fix that reintroduces material texture deletion must also (a) invalidate the
`resolveMaterial` memo on release (or `beginMaterialPass` before the walk) and (b) check
`glMakeTextureHandleResidentARB` errors in `textureHandle64`. Also note bindless-cached block/chunk
words never call `lookupTextureHandle`, so the LRU touch at ModernGLToolkit `lookupTextureHandle`
does not keep in-use materials recent (they get demoted after `textureRetainCycles`).
Verified OK on 2026-09-13: SSBO word/command alignment in all three bindless paths, `uDrawBase`+`gl_DrawID`
indexing, toolkit round trip creates a fresh toolkit/batcher/texture cache, streaming translucent append
nulls `translucentTpl[i]`.
