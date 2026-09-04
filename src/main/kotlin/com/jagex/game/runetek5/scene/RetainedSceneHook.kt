package com.jagex.game.runetek5.scene

/**
 * Lets a deferred renderer take over the per-frame drawing of STATIC scene entities.
 *
 * The legacy walk visits every visible entity every frame: visibility, projection,
 * sort, then a draw that re-derives the same model matrix and re-submits the same
 * geometry. For static locs, walls and decorations none of that changes between
 * frames, so a renderer implementing this hook captures their draws once per chunk
 * and resubmits them itself, and [Scene.drawTiles] leaves entities flagged
 * [SceneGraphEntity.modernRetained] out of its lists. Scene mutations report the
 * affected chunk so the capture is redone.
 */
interface RetainedSceneHook {
    /** True while static entities of [scene] should be left to [submitStatics]. */
    fun active(scene: Scene): Boolean

    /** Something on (plane, tileX, tileY) was added, removed or re-shaded. */
    fun chunkChanged(scene: Scene, plane: Int, tileX: Int, tileY: Int)

    /** Everything in [scene] changed (shading pass, clear); drop all captures. */
    fun sceneReset(scene: Scene)

    /**
     * Called by [Scene.drawTiles] after its own walk and before its draws: submit the
     * retained static entities that the walk skipped, applying the same plane/roof
     * rules the walk would have.
     */
    fun submitStatics(
        scene: Scene,
        settingBits: Array<Array<ByteArray>>?,
        lowestRenderedPlane: Int,
        roofPreference: Byte
    )
}
