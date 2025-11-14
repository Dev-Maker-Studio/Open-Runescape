package com.jagex.game.runetek5.client.cycle

import com.jagex.game.runetek5.tools.TimeTools

abstract class AbstractCycleManager {
    fun loopcycle(preferedDuration: Long): Int {
        val sleepTime = getIdleDurationMs()
        if (sleepTime > 0L) {
            TimeTools.sleep(sleepTime)
        }
        return loopsInCycle(preferedDuration)
    }

    abstract fun getIdleDurationMs(): Long

    abstract fun loopsInCycle(preferedDuration: Long): Int

    abstract fun getCurrentCycle(): Long

    abstract fun reset()
}
