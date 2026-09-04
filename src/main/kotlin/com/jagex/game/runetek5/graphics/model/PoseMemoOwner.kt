package com.jagex.game.runetek5.graphics.model

/**
 * An entity that keeps the deferred toolkit's per-pose geometry for its animated
 * model across render frames. The toolkit rebuilds that geometry from the pose
 * model's vertices; when the pose has not advanced since the last frame the owner
 * hands the previous geometry back instead, and the rebuild is skipped.
 */
interface PoseMemoOwner {
    /** Called by the toolkit after it built [geometry] for this owner's current pose. */
    fun storePoseGeometry(geometry: Any, streamKey: Long)
}
