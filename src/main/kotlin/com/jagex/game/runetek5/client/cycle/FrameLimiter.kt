package com.jagex.game.runetek5.client.cycle

import java.util.concurrent.locks.LockSupport

/**
 * Paces the render loop to a target frame rate, independent of the 50Hz logic rate.
 *
 * Uses a hybrid park/spin wait: parks the thread while more than ~1.5ms remains in
 * the frame slot, then spin-waits the remainder for sub-millisecond accuracy (a
 * plain sleep on Linux/Windows can overshoot by a full millisecond, which at a
 * 300fps budget of 3.3ms would make the cap wildly inaccurate).
 *
 * A target of 0 disables pacing entirely (uncapped).
 */
object FrameLimiter {

    /** Target frames per second; 0 = uncapped. */
    @Volatile
    @JvmStatic
    var targetFps: Int = DEFAULT_TARGET_FPS

    private var nextFrameAt: Long = 0L

    // parkNanos on Linux/Windows typically overshoots by 50-200us; spinning the
    // final ~300us bounds cap jitter without burning meaningful CPU (~9% of a
    // core worst case at 300fps, vs ~45% with a 1.5ms margin).
    private const val PARK_MARGIN_NANOS = 300_000L

    const val DEFAULT_TARGET_FPS: Int = 300

    /**
     * Blocks until the next frame slot begins. Call once per render frame, after
     * the frame has been drawn.
     */
    @JvmStatic
    fun pace() {
        val target = targetFps
        if (target <= 0) {
            nextFrameAt = 0L
            return
        }
        val frameNanos = 1_000_000_000L / target
        val now = System.nanoTime()
        // First frame after enabling, or we fell far behind (GC pause, region
        // load): resynchronize rather than fast-forwarding through missed slots.
        if (nextFrameAt == 0L || now - nextFrameAt > frameNanos * 4) {
            nextFrameAt = now + frameNanos
            return
        }
        var remaining = nextFrameAt - System.nanoTime()
        while (remaining > PARK_MARGIN_NANOS) {
            LockSupport.parkNanos(remaining - PARK_MARGIN_NANOS)
            remaining = nextFrameAt - System.nanoTime()
        }
        while (System.nanoTime() < nextFrameAt) {
            Thread.onSpinWait()
        }
        nextFrameAt += frameNanos
    }
}
