package com.jagex.game.runetek5.client.cycle

class DecoupledCycleManager : AbstractCycleManager() {

    @JvmField
    var nextPulseAt: Long = System.nanoTime()

    private var pulseDuration: Long = 20_000_000L

    override fun loopcycle(preferedDuration: Long): Int {
        pulseDuration = preferedDuration
        val now = System.nanoTime()
        if (now < nextPulseAt) {
            return 0
        }
        var pulses = 0
        while (now >= nextPulseAt && pulses < MAX_PULSES_PER_FRAME) {
            nextPulseAt += preferedDuration
            pulses++
        }
        if (now - nextPulseAt > preferedDuration) {
            nextPulseAt = now + preferedDuration
        }
        return pulses
    }

    override fun pulseFraction(preferedDuration: Long): Float {
        val sincePulse = preferedDuration - (nextPulseAt - System.nanoTime())
        if (sincePulse <= 0L) {
            return 0.0f
        }
        if (sincePulse >= preferedDuration) {
            return 1.0f
        }
        return sincePulse.toFloat() / preferedDuration.toFloat()
    }

    override fun getIdleDurationMs(): Long {
        return 0L
    }

    override fun loopsInCycle(preferedDuration: Long): Int {
        val now = System.nanoTime()
        if (now < nextPulseAt) {
            return 0
        }
        val due = (now - nextPulseAt) / preferedDuration + 1L
        return due.coerceAtMost(MAX_PULSES_PER_FRAME.toLong()).toInt()
    }

    override fun getCurrentCycle(): Long {
        return System.nanoTime()
    }

    override fun reset() {
        nextPulseAt = maxOf(nextPulseAt, System.nanoTime() + pulseDuration)
    }

    companion object {
        const val MAX_PULSES_PER_FRAME: Int = 10
    }
}
