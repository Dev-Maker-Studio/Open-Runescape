package com.jagex.game.runetek5.client.cycle

/**
 * Cycle manager implementation using System.nanoTime().
 */
class SystemCycleManager : AbstractCycleManager() {

    @JvmField var lastNanotime: Long = 0L
    @JvmField var currentCycleAt: Long
    @JvmField var nextCycleAt: Long
    @JvmField var cachePos: Int = 0
    @JvmField var entryCount: Int = 1
    @JvmField val timeCache = LongArray(10)

    init {
        currentCycleAt = System.nanoTime()
        nextCycleAt = System.nanoTime()
    }

    override fun getCurrentCycle(): Long {
        return currentCycleAt
    }

    override fun getIdleDurationMs(): Long {
        currentCycleAt += averageExecutionTime()
        return if (nextCycleAt > currentCycleAt) (nextCycleAt - currentCycleAt) / 1000000L else 0L
    }

    override fun loopsInCycle(preferedDuration: Long): Int {
        if (nextCycleAt > currentCycleAt) {
            lastNanotime += nextCycleAt - currentCycleAt
            currentCycleAt += nextCycleAt - currentCycleAt
            nextCycleAt += preferedDuration
            return 1
        }

        var loops = 0
        do {
            loops++
            nextCycleAt += preferedDuration
        } while (loops < 10 && nextCycleAt < currentCycleAt)

        if (nextCycleAt < currentCycleAt) {
            nextCycleAt = currentCycleAt
        }
        return loops
    }

    override fun reset() {
        lastNanotime = 0L
        if (nextCycleAt > currentCycleAt) {
            currentCycleAt += nextCycleAt - currentCycleAt
        }
    }

    /**
     * Calculates the average execution time based on the time cache.
     *
     * @return The average execution time in nanoseconds
     */
    fun averageExecutionTime(): Long {
        val currentNanotime = System.nanoTime()
        val timedelta = currentNanotime - lastNanotime
        lastNanotime = currentNanotime

        if (timedelta > -5000000000L && timedelta < 5000000000L) {
            timeCache[cachePos] = timedelta
            cachePos = (cachePos + 1) % 10
            if (entryCount < 1) {
                entryCount++
            }
        }

        var total = 0L
        for (index in 1..entryCount) {
            total += timeCache[(cachePos - index + 10) % 10]
        }
        return total / entryCount
    }
}
