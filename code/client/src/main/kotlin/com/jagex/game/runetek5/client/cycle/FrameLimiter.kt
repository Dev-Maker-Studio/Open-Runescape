package com.jagex.game.runetek5.client.cycle

import java.util.concurrent.locks.LockSupport

object FrameLimiter {

    @Volatile
    @JvmStatic
    var targetFps: Int = System.getProperty("fps")?.trim()?.toIntOrNull() ?: DEFAULT_TARGET_FPS

    private var nextFrameAt: Long = 0L

    private const val PARK_MARGIN_NANOS = 300_000L

    const val DEFAULT_TARGET_FPS: Int = 300

    @JvmStatic
    fun pace() {
        val target = targetFps
        if (target <= 0) {
            nextFrameAt = 0L
            return
        }
        val frameNanos = 1_000_000_000L / target
        val now = System.nanoTime()
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
