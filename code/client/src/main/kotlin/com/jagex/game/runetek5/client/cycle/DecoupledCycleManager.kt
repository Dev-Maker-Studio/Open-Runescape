package com.jagex.game.runetek5.client.cycle

/**
 * Cycle manager for the decoupled logic/render loop.
 *
 * Unlike the legacy managers, [loopcycle] never sleeps. It returns the number of
 * 20ms logic pulses that are currently due (0..[MAX_PULSES_PER_FRAME]) and lets the
 * caller render freely between pulses. Frame pacing is handled separately by
 * [FrameLimiter], so the render rate is independent of the 50Hz game logic rate.
 *
 * [getCurrentCycle] returns the raw monotonic clock, so consumers measuring
 * elapsed time between redraws (e.g. Client.redraw's ms delta) see real wall time
 * rather than the legacy smoothed execution-time estimate.
 */
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
        // If we are still behind after the catch-up budget (breakpoint, GC pause,
        // region load), drop the backlog instead of spiraling.
        if (now - nextPulseAt > preferedDuration) {
            nextPulseAt = now + preferedDuration
        }
        return pulses
    }

    /**
     * Fraction of the current 20ms logic window that has elapsed, in [0, 1].
     * Used as the interpolation alpha when rendering between logic pulses.
     */
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
        // Pure query (unlike loopcycle, which consumes the pulses it reports):
        // how many pulses are currently due, without advancing nextPulseAt.
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
        /**
         * Maximum logic pulses run per render frame when catching up, matching the
         * legacy managers' catch-up bound of 10.
         */
        const val MAX_PULSES_PER_FRAME: Int = 10
    }
}
