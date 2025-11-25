package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.buffer.Packet
import kotlin.math.cos
import kotlin.math.pow

internal class Filter {

    @JvmField val numPairs = IntArray(2)
    @JvmField val pairPhase = Array(2) { Array(2) { IntArray(4) } }
    @JvmField val pairMagnitude = Array(2) { Array(2) { IntArray(4) } }
    @JvmField val unity = IntArray(2)

    fun adaptMagnitude(dir: Int, k: Int, t: Float): Float {
        var alpha = pairMagnitude[dir][0][k] + t * (pairMagnitude[dir][1][k] - pairMagnitude[dir][0][k])
        alpha *= 0.0015258789f
        return 1.0f - 10.0.pow(-alpha / 20.0).toFloat()
    }

    fun adaptPhase(dir: Int, i: Int, t: Float): Float {
        var F = pairPhase[dir][0][i] + t * (pairPhase[dir][1][i] - pairPhase[dir][0][i])
        F *= 1.2207031E-4f
        return normalize(F)
    }

    fun decode(buffer: Packet, envelope: Envelope) {
        val count = buffer.readUByte()
        numPairs[0] = count shr 4
        numPairs[1] = count and 0xf
        if (count != 0) {
            unity[0] = buffer.readUShort()
            unity[1] = buffer.readUShort()
            val migrated = buffer.readUByte()

            for (dir in 0..1) {
                for (term in 0 until numPairs[dir]) {
                    pairPhase[dir][0][term] = buffer.readUShort()
                    pairMagnitude[dir][0][term] = buffer.readUShort()
                }
            }

            for (dir in 0..1) {
                for (phase in 0 until numPairs[dir]) {
                    if (migrated and (1 shl dir * 4 shl phase) != 0) {
                        pairPhase[dir][1][phase] = buffer.readUShort()
                        pairMagnitude[dir][1][phase] = buffer.readUShort()
                    } else {
                        pairPhase[dir][1][phase] = pairPhase[dir][0][phase]
                        pairMagnitude[dir][1][phase] = pairMagnitude[dir][0][phase]
                    }
                }
            }

            if (migrated != 0 || unity[1] != unity[0]) {
                envelope.decodeShape(buffer)
            }
        } else {
            unity[1] = 0
            unity[0] = 0
        }
    }

    fun compute(dir: Int, t: Float): Int {
        if (dir == 0) {
            var a0 = unity[0] + (unity[1] - unity[0]) * t
            a0 *= 0.0030517578f
            invUnityFloat = 0.1.pow(a0 / 20.0).toFloat()
            invUnityInt = (invUnityFloat * 65536.0f).toInt()
        }

        if (numPairs[dir] == 0) {
            return 0
        }
        var f_3 = adaptMagnitude(dir, 0, t)
        COEFFICIENT_FLOAT[dir][0] = -2.0f * f_3 * cos(adaptPhase(dir, 0, t).toDouble()).toFloat()
        COEFFICIENT_FLOAT[dir][1] = f_3 * f_3

        for (pair in 1 until numPairs[dir]) {
            f_3 = adaptMagnitude(dir, pair, t)
            val f_5 = -2.0f * f_3 * cos(adaptPhase(dir, pair, t).toDouble()).toFloat()
            val f_6 = f_3 * f_3
            COEFFICIENT_FLOAT[dir][pair * 2 + 1] = COEFFICIENT_FLOAT[dir][pair * 2 - 1] * f_6
            COEFFICIENT_FLOAT[dir][pair * 2] = COEFFICIENT_FLOAT[dir][pair * 2 - 1] * f_5 + COEFFICIENT_FLOAT[dir][pair * 2 - 2] * f_6

            var i_7 = pair * 2 - 1
            while (i_7 >= 2) {
                COEFFICIENT_FLOAT[dir][i_7] += COEFFICIENT_FLOAT[dir][i_7 - 1] * f_5 + COEFFICIENT_FLOAT[dir][i_7 - 2] * f_6
                --i_7
            }

            COEFFICIENT_FLOAT[dir][1] += COEFFICIENT_FLOAT[dir][0] * f_5 + f_6
            COEFFICIENT_FLOAT[dir][0] += f_5
        }

        if (dir == 0) {
            for (pair in 0 until numPairs[0] * 2) {
                COEFFICIENT_FLOAT[0][pair] *= invUnityFloat
            }
        }

        for (pair in 0 until numPairs[dir] * 2) {
            COEFFICIENT_INT[dir][pair] = (COEFFICIENT_FLOAT[dir][pair] * 65536.0f).toInt()
        }

        return numPairs[dir] * 2
    }

    companion object {
        @JvmField val COEFFICIENT_FLOAT = Array(2) { FloatArray(8) }
        @JvmField val COEFFICIENT_INT = Array(2) { IntArray(8) }
        @JvmField var invUnityFloat: Float = 0f
        @JvmField var invUnityInt: Int = 0

        @JvmStatic
        fun normalize(f: Float): Float {
            val F = 32.703197f * 2.0.pow(f.toDouble()).toFloat()
            return F * 3.1415927f / 11025.0f
        }
    }
}
