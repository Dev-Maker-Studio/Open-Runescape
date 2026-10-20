package com.jagex.game.runetek5.utils

import kotlin.math.*

object MathUtils {

    @JvmField val SINE = IntArray(16384)
    @JvmField val COSINE = IntArray(16384)

    init {
        val step = 3.834951969714103E-4
        for (i in 0 until 16384) {
            SINE[i] = (16384.0 * sin(i * step)).toInt()
            COSINE[i] = (16384.0 * cos(i * step)).toInt()
        }
    }

    @JvmStatic
    fun getTheta(rotation: Int): Float {
        val maskedRotation = rotation and 0x3fff
        return (maskedRotation / 16384.0f) * 6.283185307179586f
    }

    @JvmStatic
    fun calculateSampleRate(maxDelay: Int, minDelay: Int): Int {
        val logMin = ln(minDelay.toDouble()) / ln(2.0)
        val logMax = ln(maxDelay.toDouble()) / ln(2.0)
        val randomLog = Math.random() * (logMin - logMax) + logMax
        return (2.0.pow(randomLog) + 0.5).toInt()
    }

    @JvmStatic
    fun furtestBitValue(value: Int): Int {
        var result = value ushr 1
        result = result or (result ushr 1)
        result = result or (result ushr 2)
        result = result or (result ushr 4)
        result = result or (result ushr 8)
        result = result or (result ushr 16)
        return value and result.inv()
    }

    @JvmStatic
    fun nextPowerOfTwo(value: Int): Int {
        var result = value - 1
        result = result or (result ushr 1)
        result = result or (result ushr 2)
        result = result or (result ushr 4)
        result = result or (result ushr 8)
        result = result or (result ushr 16)
        return result + 1
    }

    @JvmStatic
    fun power(base: Int, exponent: Int): Int {
        var currentBase = base
        var currentExp = exponent
        var result = 1
        while (currentExp > 1) {
            if ((currentExp and 0x1) != 0) {
                result = currentBase * result
            }
            currentBase *= currentBase
            currentExp = currentExp shr 1
        }
        return if (currentExp == 1) {
            currentBase * result
        } else {
            result
        }
    }
}
