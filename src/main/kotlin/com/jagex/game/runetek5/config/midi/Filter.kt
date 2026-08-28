package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.buffer.Packet
import kotlin.math.cos
import kotlin.math.pow

internal class Filter {

    @JvmField val numPairs = IntArray(2)
    @JvmField val pairPhase = Array(2) { Array(2) { IntArray(4) } }
    @JvmField val pairMagnitude = Array(2) { Array(2) { IntArray(4) } }
    @JvmField val unity = IntArray(2)

    fun adaptMagnitude(dir: Int, pair: Int, t: Float): Float {
        var attenuationDb = pairMagnitude[dir][0][pair] + t * (pairMagnitude[dir][1][pair] - pairMagnitude[dir][0][pair])
        attenuationDb *= 0.0015258789f
        return 1.0f - 10.0.pow(-attenuationDb / 20.0).toFloat()
    }

    fun adaptPhase(dir: Int, pair: Int, t: Float): Float {
        var octaves = pairPhase[dir][0][pair] + t * (pairPhase[dir][1][pair] - pairPhase[dir][0][pair])
        octaves *= 1.2207031E-4f
        return normalize(octaves)
    }

    fun decode(buffer: Packet, envelope: Envelope) {
        val count = buffer.readUByte()
        numPairs[0] = count shr 4
        numPairs[1] = count and 0xf
        if (count != 0) {
            unity[0] = buffer.readUShort()
            unity[1] = buffer.readUShort()
            val varyingMask = buffer.readUByte()

            for (dir in 0..1) {
                for (pair in 0 until numPairs[dir]) {
                    pairPhase[dir][0][pair] = buffer.readUShort()
                    pairMagnitude[dir][0][pair] = buffer.readUShort()
                }
            }

            for (dir in 0..1) {
                for (pair in 0 until numPairs[dir]) {
                    if (varyingMask and (1 shl dir * 4 shl pair) != 0) {
                        pairPhase[dir][1][pair] = buffer.readUShort()
                        pairMagnitude[dir][1][pair] = buffer.readUShort()
                    } else {
                        pairPhase[dir][1][pair] = pairPhase[dir][0][pair]
                        pairMagnitude[dir][1][pair] = pairMagnitude[dir][0][pair]
                    }
                }
            }

            if (varyingMask != 0 || unity[1] != unity[0]) {
                envelope.decodeShape(buffer)
            }
        } else {
            unity[1] = 0
            unity[0] = 0
        }
    }

    fun compute(dir: Int, t: Float): Int {
        if (dir == 0) {
            var unityDb = unity[0] + (unity[1] - unity[0]) * t
            unityDb *= 0.0030517578f
            invUnityFloat = 0.1.pow(unityDb / 20.0).toFloat()
            invUnityInt = (invUnityFloat * 65536.0f).toInt()
        }

        if (numPairs[dir] == 0) {
            return 0
        }
        var magnitude = adaptMagnitude(dir, 0, t)
        COEFFICIENT_FLOAT[dir][0] = -2.0f * magnitude * cos(adaptPhase(dir, 0, t).toDouble()).toFloat()
        COEFFICIENT_FLOAT[dir][1] = magnitude * magnitude

        for (pair in 1 until numPairs[dir]) {
            magnitude = adaptMagnitude(dir, pair, t)
            val coeff1 = -2.0f * magnitude * cos(adaptPhase(dir, pair, t).toDouble()).toFloat()
            val coeff2 = magnitude * magnitude
            COEFFICIENT_FLOAT[dir][pair * 2 + 1] = COEFFICIENT_FLOAT[dir][pair * 2 - 1] * coeff2
            COEFFICIENT_FLOAT[dir][pair * 2] = COEFFICIENT_FLOAT[dir][pair * 2 - 1] * coeff1 + COEFFICIENT_FLOAT[dir][pair * 2 - 2] * coeff2

            var i = pair * 2 - 1
            while (i >= 2) {
                COEFFICIENT_FLOAT[dir][i] += COEFFICIENT_FLOAT[dir][i - 1] * coeff1 + COEFFICIENT_FLOAT[dir][i - 2] * coeff2
                --i
            }

            COEFFICIENT_FLOAT[dir][1] += COEFFICIENT_FLOAT[dir][0] * coeff1 + coeff2
            COEFFICIENT_FLOAT[dir][0] += coeff1
        }

        if (dir == 0) {
            for (i in 0 until numPairs[0] * 2) {
                COEFFICIENT_FLOAT[0][i] *= invUnityFloat
            }
        }

        for (i in 0 until numPairs[dir] * 2) {
            COEFFICIENT_INT[dir][i] = (COEFFICIENT_FLOAT[dir][i] * 65536.0f).toInt()
        }

        return numPairs[dir] * 2
    }

    companion object {
        @JvmField val COEFFICIENT_FLOAT = Array(2) { FloatArray(8) }
        @JvmField val COEFFICIENT_INT = Array(2) { IntArray(8) }
        @JvmField var invUnityFloat: Float = 0f
        @JvmField var invUnityInt: Int = 0

        @JvmStatic
        fun normalize(octaves: Float): Float {
            val frequency = 32.703197f * 2.0.pow(octaves.toDouble()).toFloat()
            return frequency * 3.1415927f / 11025.0f
        }
    }
}
