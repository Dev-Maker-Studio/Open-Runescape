package com.jagex.game.runetek5.graphics.texture.water

import java.util.Random

/**
 * Implements 3D Perlin noise generation for water textures
 * Uses gradient vectors and permutation table for pseudorandom noise
 */
internal class PerlinNoiseGenerator : NoiseGenerator() {

    @JvmField val permutationTable: IntArray = IntArray(512)

    init {
        val random = Random(419684)

        var i: Int
        for (i in 0 until 256) {
            this.permutationTable[i + 256] = i
            this.permutationTable[i] = this.permutationTable[i + 256]
        }

        for (i in 0 until 256) {
            val randomIndex = random.nextInt() and 0xff
            val temp = this.permutationTable[randomIndex]
            this.permutationTable[randomIndex + 256] = this.permutationTable[i]
            this.permutationTable[randomIndex] = this.permutationTable[randomIndex + 256]
            this.permutationTable[i + 256] = temp
            this.permutationTable[i] = this.permutationTable[i + 256]
        }
    }

    override fun method4634(layer: Int, scaleX: Float, scaleY: Float, scaleZ: Float, amplitude: Float, output: FloatArray) {
        var outputIndex = 0
        var xMask = (128 * scaleX - 1.0f).toInt()
        xMask = xMask and 0xff
        var yMask = (128 * scaleY - 1.0f).toInt()
        yMask = yMask and 0xff
        var zMask = (16 * scaleZ - 1.0f).toInt()
        zMask = zMask and 0xff
        val layerZ = layer * scaleZ
        var z0 = layerZ.toInt()
        var z1 = z0 + 1
        val zFrac = layerZ - z0
        val zInvFrac = 1.0f - zFrac
        val zFade = fade(zFrac)
        z0 = z0 and zMask
        z1 = z1 and zMask
        val hash0 = this.permutationTable[z0]
        val hash1 = this.permutationTable[z1]

        for (y in 0 until 128) {
            val yPos = y * scaleY
            var y0 = yPos.toInt()
            var y1 = y0 + 1
            val yFrac = yPos - y0
            val yInvFrac = 1.0f - yFrac
            val yFade = fade(yFrac)
            y0 = y0 and yMask
            y1 = y1 and yMask
            val hash00 = this.permutationTable[y0 + hash0]
            val hash01 = this.permutationTable[y1 + hash0]
            val hash10 = this.permutationTable[y0 + hash1]
            val hash11 = this.permutationTable[y1 + hash1]

            for (x in 0 until 128) {
                val xPos = x * scaleX
                var x0 = xPos.toInt()
                var x1 = x0 + 1
                val xFrac = xPos - x0
                val xInvFrac = 1.0f - xFrac
                val xFade = fade(xFrac)
                x0 = x0 and xMask
                x1 = x1 and xMask
                output[outputIndex++] = amplitude * lerp(lerp(lerp(gradientDot(this.permutationTable[x0 + hash00] and 0x7, xInvFrac, yInvFrac, zInvFrac), gradientDot(this.permutationTable[x1 + hash00] and 0x7, xFrac, yInvFrac, zInvFrac), xFade), lerp(gradientDot(this.permutationTable[x0 + hash01] and 0x7, xInvFrac, yFrac, zInvFrac), gradientDot(this.permutationTable[x1 + hash01] and 0x7, xFrac, yFrac, zInvFrac), xFade), yFade), lerp(lerp(gradientDot(this.permutationTable[x0 + hash10] and 0x7, xInvFrac, yInvFrac, zFrac), gradientDot(this.permutationTable[x1 + hash10] and 0x7, xFrac, yInvFrac, zFrac), xFade), lerp(gradientDot(this.permutationTable[x0 + hash11] and 0x7, xInvFrac, yFrac, zFrac), gradientDot(this.permutationTable[x1 + hash11] and 0x7, xFrac, yFrac, zFrac), xFade), yFade), zFade)
            }
        }
    }

    companion object {
        /** 8 gradient vectors for Perlin noise */
        @JvmField val GRADIENT_VECTORS: Array<FloatArray> = arrayOf(floatArrayOf(-0.333333f, -0.333333f, -0.333333f), floatArrayOf(0.333333f, -0.333333f, -0.333333f), floatArrayOf(-0.333333f, 0.333333f, -0.333333f), floatArrayOf(0.333333f, 0.333333f, -0.333333f), floatArrayOf(-0.333333f, -0.333333f, 0.333333f), floatArrayOf(0.333333f, -0.333333f, 0.333333f), floatArrayOf(-0.333333f, 0.333333f, 0.333333f), floatArrayOf(0.333333f, 0.333333f, 0.333333f))

        /** Linear interpolation between two values */
        @JvmStatic
        fun lerp(a: Float, b: Float, t: Float): Float {
            return a + (b - a) * t
        }

        /** Computes dot product with gradient vector */
        @JvmStatic
        fun gradientDot(gradientIndex: Int, x: Float, y: Float, z: Float): Float {
            val gradient = GRADIENT_VECTORS[gradientIndex]
            return gradient[0] * x + gradient[1] * y + gradient[2] * z
        }

        /** Smoothstep/fade function: 6t⁵ - 15t⁴ + 10t³ */
        @JvmStatic
        fun fade(t: Float): Float {
            return t * t * t * (10.0f + (t * 6.0f - 15.0f) * t)
        }
    }
}
