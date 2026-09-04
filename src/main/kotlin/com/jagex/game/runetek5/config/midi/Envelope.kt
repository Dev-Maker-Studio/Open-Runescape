package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.buffer.Packet

/**
 * MIDI envelope for audio synthesis.
 */
internal class Envelope {
    @JvmField var form: Int = 0
    @JvmField var start: Int = 0
    @JvmField var end: Int = 0
    @JvmField var critical: Int = 0
    @JvmField var phaseIndex: Int = 0
    @JvmField var step: Int = 0
    @JvmField var amplitude: Int = 0
    @JvmField var ticks: Int = 0
    @JvmField var numPhases: Int = 2
    @JvmField var phaseDuration: IntArray = IntArray(2)
    @JvmField var phasePeak: IntArray = IntArray(2)

    init {
        phaseDuration[1] = 65535
        phasePeak[1] = 65535
    }

    fun decode(buffer: Packet) {
        form = buffer.readUByte()
        start = buffer.readInt()
        end = buffer.readInt()
        decodeShape(buffer)
    }

    fun reset() {
        critical = 0
        phaseIndex = 0
        step = 0
        amplitude = 0
        ticks = 0
    }

    fun decodeShape(buffer: Packet) {
        numPhases = buffer.readUByte()
        phaseDuration = IntArray(numPhases)
        phasePeak = IntArray(numPhases)

        for (i in 0 until numPhases) {
            phaseDuration[i] = buffer.readUShort()
            phasePeak[i] = buffer.readUShort()
        }
    }

    fun step(period: Int): Int {
        if (ticks >= critical) {
            amplitude = phasePeak[phaseIndex++] shl 15
            if (phaseIndex >= numPhases) {
                phaseIndex = numPhases - 1
            }

            critical = (phaseDuration[phaseIndex] / 65536.0 * period).toInt()
            if (critical > ticks) {
                step = ((phasePeak[phaseIndex] shl 15) - amplitude) / (critical - ticks)
            }
        }

        amplitude += step
        ticks++
        return (amplitude - step) shr 15
    }
}
