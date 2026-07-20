package com.jagex.game.runetek5.scene.ground

/**
 * Monotonic stamp for per-plane floor visibility.
 *
 * [Scene][com.jagex.game.runetek5.scene.Scene] bumps [epoch] once per frame whenever its
 * camera-static frame cache is dirty, i.e. whenever the per-plane visible-tile sets may
 * differ from the previous frame (camera pose / viewport change, roof preference change,
 * scene content change, map rebuild).
 *
 * [GroundGL] compares the epoch (together with the camera grid cell and visible range)
 * against the stamp recorded when it last rebuilt its visible-tile list; on a match the
 * previously built visible-tile list and the per-material index buffers are reused instead
 * of being rebuilt. When in doubt Scene leaves the cache dirty, so a stale epoch can only
 * cause a redundant rebuild, never a stale draw.
 */
object FloorVisibilityClock {
    @JvmField var epoch: Long = 0L
}
