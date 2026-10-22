package com.jagex.game.runetek5.graphics.modern.dump

class CaptureProfile {
    private val started = System.nanoTime()
    private val counters = linkedMapOf<String, Long>()

    @Synchronized
    fun add(name: String, value: Long) {
        counters[name] = (counters[name] ?: 0L) + value
    }

    fun <T> measure(name: String, block: () -> T): T {
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            add(name + "Nanos", System.nanoTime() - start)
        }
    }

    @Synchronized
    fun snapshot(): Map<String, Long> = counters.toMap() + ("wallCaptureNanos" to (System.nanoTime() - started))
}

object CaptureProfiler {
    private val current = ThreadLocal<CaptureProfile>()

    fun begin(profile: CaptureProfile) = current.set(profile)
    fun end() = current.remove()
    fun <T> measure(name: String, block: () -> T): T {
        val profile = current.get()
        return if (profile == null) block() else profile.measure(name, block)
    }
}
