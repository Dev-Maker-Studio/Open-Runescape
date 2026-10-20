package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.sqrt

object TileUtils {

    @JvmStatic
    fun getCullingType(bitpacked: Int): Int {
        val type = bitpacked and 0x3f
        val rotation = bitpacked shr 6 and 0x3
        return when (type) {
            18 -> when (rotation) {
                0 -> 1
                1 -> 2
                2 -> 4
                else -> 8
            }
            19, 21 -> when (rotation) {
                0 -> 16
                1 -> 32
                2 -> 64
                else -> 128
            }
            else -> 0
        }
    }

    @JvmStatic
    fun calculateTileheight(x: Int, y: Int): Int {
        var height = perlinNoise(45365 + x, y + 91923, 4) - 128 +
                (perlinNoise(x + 10294, 37821 + y, 2) - 128 shr 1) +
                (perlinNoise(x, y, 1) - 128 shr 2)
        height = (height * 0.3).toInt() + 35
        return when {
            height < 10 -> 10
            height > 60 -> 60
            else -> height
        }
    }

    @JvmStatic
    fun perlinNoise(x: Int, y: Int, frequencyReciprocal: Int): Int {
        val adjX = x / frequencyReciprocal
        val angleX = x and frequencyReciprocal - 1
        val adjY = y / frequencyReciprocal
        val angleY = y and frequencyReciprocal - 1
        val noiseBase = randomNoiseWeighedSum(adjX, adjY)
        val noiseEast = randomNoiseWeighedSum(adjX + 1, adjY)
        val noiseSouth = randomNoiseWeighedSum(adjX, adjY + 1)
        val noiseSouthEast = randomNoiseWeighedSum(adjX + 1, adjY + 1)
        val interpolatedNoiseNorth = interpolate(noiseBase, noiseEast, angleX, frequencyReciprocal)
        val interpolatedNoiseSouth = interpolate(noiseSouth, noiseSouthEast, angleX, frequencyReciprocal)
        return interpolate(interpolatedNoiseNorth, interpolatedNoiseSouth, angleY, frequencyReciprocal)
    }

    @JvmStatic
    fun randomNoiseWeighedSum(x: Int, y: Int): Int {
        val corners = randomNoise(x - 1, y - 1) + randomNoise(1 + x, y - 1) +
                randomNoise(x - 1, 1 + y) + randomNoise(x + 1, 1 + y)
        val sides = randomNoise(x - 1, y) + randomNoise(1 + x, y) +
                randomNoise(x, y - 1) + randomNoise(x, y + 1)
        val center = randomNoise(x, y)
        return corners / 16 + sides / 8 + center / 4
    }

    @JvmStatic
    fun randomNoise(x: Int, y: Int): Int {
        var n = y * 57 + x
        n = n xor (n shl 13)
        val value = n * (n * n * 15731 + 789221) + 1376312589 and 0x7fffffff
        return value shr 19 and 0xff
    }

    @JvmStatic
    fun interpolate(a: Int, b: Int, angle: Int, frequencyReciprocal: Int): Int {
        val cosine = (65536 - MathUtils.COSINE[angle * 8192 / frequencyReciprocal]) shr 1
        return ((65536 - cosine) * a shr 16) + (cosine * b shr 16)
    }
}
