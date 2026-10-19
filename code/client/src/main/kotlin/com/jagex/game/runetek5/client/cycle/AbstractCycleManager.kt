package com.jagex.game.runetek5.client.cycle

import com.jagex.game.runetek5.tools.TimeTools

abstract class AbstractCycleManager {
    open fun loopcycle(preferedDuration: Long): Int {
        val sleepTime = getIdleDurationMs()
        if (sleepTime > 0L) {
            TimeTools.sleep(sleepTime)
        }
        return loopsInCycle(preferedDuration)
    }

    /**
     * Fraction of the current logic window that has elapsed, in [0, 1]. Managers
     * that pace the loop to exactly one pulse per iteration have no inter-pulse
     * render frames, so the default is 1 (no interpolation).
     */
    open fun pulseFraction(preferedDuration: Long): Float {
        return 1.0f
    }

    abstract fun getIdleDurationMs(): Long

    abstract fun loopsInCycle(preferedDuration: Long): Int

    abstract fun getCurrentCycle(): Long

    abstract fun reset()
}
