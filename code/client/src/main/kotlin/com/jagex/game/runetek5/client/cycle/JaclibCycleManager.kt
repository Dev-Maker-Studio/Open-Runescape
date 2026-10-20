package com.jagex.game.runetek5.client.cycle

import jaclib.nanotime.QueryPerformanceCounter

class JaclibCycleManager : AbstractCycleManager() {

    @JvmField var lastNanotime: Long = 0L
    @JvmField var currentCycleAt: Long
    @JvmField var nextCycleAt: Long
    @JvmField var cachePos: Int = 0
    @JvmField var entryCount: Int = 1
    @JvmField val timeCache = LongArray(10)

    init {
        currentCycleAt = QueryPerformanceCounter.nanoTime()
        nextCycleAt = currentCycleAt
        if (currentCycleAt == 0L) {
            throw RuntimeException()
        }
    }

    override fun getIdleDurationMs(): Long {
        currentCycleAt += averageExecutionTime()
        return if (nextCycleAt > currentCycleAt) (nextCycleAt - currentCycleAt) / 1000000L else 0L
    }

    override fun getCurrentCycle(): Long {
        return currentCycleAt
    }

    fun averageExecutionTime(): Long {
        val currentNanotime = QueryPerformanceCounter.nanoTime()
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
}
