package com.jagex.game.runetek5.graphics.modern.util

import org.lwjgl.opengl.GL33.GL_QUERY_RESULT
import org.lwjgl.opengl.GL33.GL_QUERY_RESULT_AVAILABLE
import org.lwjgl.opengl.GL33.GL_TIME_ELAPSED
import org.lwjgl.opengl.GL33.glBeginQuery
import org.lwjgl.opengl.GL33.glDeleteQueries
import org.lwjgl.opengl.GL33.glEndQuery
import org.lwjgl.opengl.GL33.glGenQueries
import org.lwjgl.opengl.GL33.glGetQueryObjecti
import org.lwjgl.opengl.GL33.glGetQueryObjectui64
import org.lwjgl.opengl.GL33.GL_TIMESTAMP
import org.lwjgl.opengl.GL33.glQueryCounter

class GpuTimer(private val passNames: Array<String>) {

    companion object {
        private const val RING = 4

        @JvmStatic
        val ENABLED: Boolean = System.getProperty("render.gputimers") != null
    }

    private val passCount = passNames.size

    private val queries = Array(passCount) { IntArray(RING) }
    private val issued = Array(passCount) { BooleanArray(RING) }

    private val accumNs = LongArray(passCount)
    private val samples = IntArray(passCount)

    private var slot = 0
    private var openPass = -1
    private var initialized = false

    val lastPassMs = DoubleArray(passCount)

    private val stampStart = IntArray(RING)
    private val stampEnd = IntArray(RING)
    private val stampIssued = BooleanArray(RING)
    private var stampSlot = 0
    private var stampOpen = false
    val frameHistoryMs = DoubleArray(6)
    private var frameHistoryCount = 0L

    fun initialize() {
        if (initialized || !ENABLED) return
        for (p in 0 until passCount) {
            for (s in 0 until RING) queries[p][s] = glGenQueries()
        }
        for (s in 0 until RING) { stampStart[s] = glGenQueries(); stampEnd[s] = glGenQueries() }
        initialized = true
    }

    fun nextFrame() {
        if (!initialized) return
        slot = (slot + 1) % RING
        for (p in 0 until passCount) {
            if (!issued[p][slot]) continue
            val q = queries[p][slot]
            if (glGetQueryObjecti(q, GL_QUERY_RESULT_AVAILABLE) == 0) continue
            val ns = glGetQueryObjectui64(q, GL_QUERY_RESULT)
            accumNs[p] += ns
            lastPassMs[p] = ns / 1e6
            samples[p]++
            issued[p][slot] = false
        }
    }

    fun frameStampStart() {
        if (!initialized || stampOpen) return
        stampSlot = (stampSlot + 1) % RING
        if (stampIssued[stampSlot]) {
            val a = stampStart[stampSlot]; val b = stampEnd[stampSlot]
            if (glGetQueryObjecti(b, GL_QUERY_RESULT_AVAILABLE) != 0 && glGetQueryObjecti(a, GL_QUERY_RESULT_AVAILABLE) != 0) {
                val ms = (glGetQueryObjectui64(b, GL_QUERY_RESULT) - glGetQueryObjectui64(a, GL_QUERY_RESULT)) / 1e6
                System.arraycopy(frameHistoryMs, 1, frameHistoryMs, 0, frameHistoryMs.size - 1)
                frameHistoryMs[frameHistoryMs.size - 1] = ms
                frameHistoryCount++
            }
            stampIssued[stampSlot] = false
        }
        glQueryCounter(stampStart[stampSlot], GL_TIMESTAMP)
        stampOpen = true
    }

    fun frameStampEnd() {
        if (!initialized || !stampOpen) return
        glQueryCounter(stampEnd[stampSlot], GL_TIMESTAMP)
        stampIssued[stampSlot] = true
        stampOpen = false
    }

    fun lastFrameString(): String {
        if (!initialized) return "gpu=off"
        val sb = StringBuilder("gpuLast[")
        for (p in 0 until passCount) { if (p > 0) sb.append(' '); sb.append(passNames[p]).append('=').append(String.format("%.2f", lastPassMs[p])) }
        sb.append("] gpuFrameHist=[")
        for (i in frameHistoryMs.indices) { if (i > 0) sb.append(','); sb.append(String.format("%.1f", frameHistoryMs[i])) }
        sb.append(']')
        return sb.toString()
    }

    fun begin(pass: Int) {
        if (!initialized || openPass >= 0 || pass >= passCount) return
        if (issued[pass][slot]) return
        glBeginQuery(GL_TIME_ELAPSED, queries[pass][slot])
        openPass = pass
    }

    fun end(pass: Int) {
        if (!initialized || openPass != pass) return
        glEndQuery(GL_TIME_ELAPSED)
        issued[pass][slot] = true
        openPass = -1
    }

    fun summary(): String {
        if (!initialized) return "gpu[off]"
        val sb = StringBuilder("gpu[")
        for (p in 0 until passCount) {
            if (p > 0) sb.append(' ')
            val ms = if (samples[p] > 0) accumNs[p] / 1e6 / samples[p] else 0.0
            sb.append(passNames[p]).append('=').append(String.format("%.2f", ms))
        }
        sb.append(']')
        return sb.toString()
    }

    fun reset() {
        java.util.Arrays.fill(accumNs, 0L)
        java.util.Arrays.fill(samples, 0)
    }

    fun destroy() {
        if (!initialized) return
        for (p in 0 until passCount) for (s in 0 until RING) glDeleteQueries(queries[p][s])
        for (s in 0 until RING) { glDeleteQueries(stampStart[s]); glDeleteQueries(stampEnd[s]) }
        initialized = false
    }
}
