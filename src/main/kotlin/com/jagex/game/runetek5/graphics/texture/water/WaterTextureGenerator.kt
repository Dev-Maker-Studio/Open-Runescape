package com.jagex.game.runetek5.graphics.texture.water

import java.util.Random
import com.jagex.game.runetek5.staticmethods.StaticMethods
import kotlin.math.pow

abstract class WaterTextureGenerator internal constructor(
    @JvmField val seed: Int,
    xScale: Int,
    yScale: Int
) {

    @JvmField val permutationTable: ShortArray = ShortArray(512)
    @JvmField var scaleX: Int = 4
    @JvmField var scaleY: Int = 4
    @JvmField var scaleZ: Int = 4
    @JvmField var octaveCount: Int = 4
    @JvmField var octaveWeights: ShortArray? = null

    init {
        scaleX = xScale
        scaleY = yScale
        scaleZ = 2
        octaveCount = 5
        initOctaveWeights()
        initPermutationTable()
    }

    abstract fun reset()

    fun generateNoise() {
        val xCoords = IntArray(128)
        val yCoords = IntArray(128)
        val zCoords = IntArray(16)

        var i: Int
        for (i in 0 until 128) {
            xCoords[i] = (i shl 12) / 128
        }

        for (i in 0 until 128) {
            yCoords[i] = (i shl 12) / 128
        }

        for (i in 0 until 16) {
            zCoords[i] = (i shl 12) / 16
        }

        clear()

        for (z in 0 until 16) {
            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    for (octave in 0 until octaveCount) {
                        val frequency = octaveWeights!![octave].toInt() shl 12
                        val wrapX = frequency * scaleX shr 12
                        val wrapY = frequency * scaleY shr 12
                        val wrapZ = frequency * scaleZ shr 12
                        var sampleX = frequency * xCoords[x] shr 12
                        var sampleY = frequency * yCoords[y] shr 12
                        var sampleZ = frequency * zCoords[z] shr 12
                        sampleX *= scaleX
                        sampleY *= scaleY
                        sampleZ *= scaleZ
                        var gridX0 = sampleX shr 12
                        var gridX1 = gridX0 + 1
                        gridX0 = gridX0 and 0xff
                        var gridY0 = sampleY shr 12
                        var gridY1 = gridY0 + 1
                        gridY0 = gridY0 and 0xff
                        var gridZ0 = sampleZ shr 12
                        var gridZ1 = gridZ0 + 1
                        gridZ0 = gridZ0 and 0xff
                        if (gridX1 >= wrapX) {
                            gridX1 = 0
                        } else {
                            gridX1 = gridX1 and 0xff
                        }

                        if (gridY1 >= wrapY) {
                            gridY1 = 0
                        } else {
                            gridY1 = gridY1 and 0xff
                        }

                        if (gridZ1 >= wrapZ) {
                            gridZ1 = 0
                        } else {
                            gridZ1 = gridZ1 and 0xff
                        }

                        sampleX = sampleX and 0xfff
                        sampleY = sampleY and 0xfff
                        sampleZ = sampleZ and 0xfff
                        val fadeX = SINE_WAVE[sampleX]
                        val fadeY = SINE_WAVE[sampleY]
                        val fadeZ = SINE_WAVE[sampleZ]
                        val farX = sampleX - 4096
                        val farY = sampleY - 4096
                        val farZ = sampleZ - 4096
                        val hashZ0 = permutationTable[gridZ0]
                        val hashZ1 = permutationTable[gridZ1]
                        val hashY0Z0 = permutationTable[gridY0 + hashZ0]
                        val hashY1Z0 = permutationTable[gridY1 + hashZ0]
                        val hashY0Z1 = permutationTable[gridY0 + hashZ1]
                        val hashY1Z1 = permutationTable[gridY1 + hashZ1]
                        var grad0 = gradientDot(permutationTable[gridX0 + hashY0Z0].toInt(), sampleX, sampleY, sampleZ)
                        var grad1 = gradientDot(permutationTable[gridX1 + hashY0Z0].toInt(), farX, sampleY, sampleZ)
                        val edgeY0Z0 = grad0 + (fadeX * (grad1 - grad0) shr 12)
                        grad0 = gradientDot(permutationTable[gridX0 + hashY1Z0].toInt(), sampleX, farY, sampleZ)
                        grad1 = gradientDot(permutationTable[gridX1 + hashY1Z0].toInt(), farX, farY, sampleZ)
                        val edgeY1Z0 = grad0 + (fadeX * (grad1 - grad0) shr 12)
                        val faceZ0 = edgeY0Z0 + (fadeY * (edgeY1Z0 - edgeY0Z0) shr 12)
                        grad0 = gradientDot(permutationTable[gridX0 + hashY0Z1].toInt(), sampleX, sampleY, farZ)
                        grad1 = gradientDot(permutationTable[gridX1 + hashY0Z1].toInt(), farX, sampleY, farZ)
                        val edgeY0Z1 = grad0 + (fadeX * (grad1 - grad0) shr 12)
                        grad0 = gradientDot(permutationTable[gridX0 + hashY1Z1].toInt(), sampleX, farY, farZ)
                        grad1 = gradientDot(permutationTable[gridX1 + hashY1Z1].toInt(), farX, farY, farZ)
                        val edgeY1Z1 = grad0 + (fadeX * (grad1 - grad0) shr 12)
                        val faceZ1 = edgeY0Z1 + (fadeY * (edgeY1Z1 - edgeY0Z1) shr 12)
                        update(octave, faceZ0 + (fadeZ * (faceZ1 - faceZ0) shr 12))
                    }

                    reset()
                }
            }
        }
    }

    fun initOctaveWeights() {
        octaveWeights = ShortArray(octaveCount)

        for (octave in 0 until octaveCount) {
            octaveWeights!![octave] = 2.0.pow(octave.toDouble()).toInt().toShort()
        }
    }

    fun initPermutationTable() {
        val random = Random(seed.toLong())

        var i: Int
        for (i in 0 until 255) {
            permutationTable[i] = i.toShort()
        }

        for (i in 0 until 255) {
            val last = 255 - i
            val swapIndex = StaticMethods.boundedRandom(random, last)
            val swapped = permutationTable[swapIndex]
            permutationTable[swapIndex] = permutationTable[last]
            permutationTable[last + 256] = swapped
            permutationTable[last] = permutationTable[last + 256]
        }
    }

    abstract fun clear()

    abstract fun update(octave: Int, noise: Int)

    companion object {
        @JvmField
        val SINE_WAVE: IntArray = IntArray(4096)

        init {
            for (i in 0 until 4096) {
                SINE_WAVE[i] = computeSCurve(i)
            }
        }

        @JvmStatic
        fun gradientDot(gradientIndex: Int, x: Int, y: Int, z: Int): Int {
            val hash = gradientIndex and 0xf
            val u = if (hash < 8) x else y
            val v = if (hash < 4) y else if (hash != 12 && hash != 14) z else x
            return (if ((hash and 0x1) == 0) u else -u) + (if ((hash and 0x2) == 0) v else -v)
        }

        @JvmStatic
        fun computeSCurve(t: Int): Int {
            val t3 = t * (t * t shr 12) shr 12
            val sixTMinus15 = t * 6 - 61440
            val poly = (t * sixTMinus15 shr 12) + 40960
            return poly * t3 shr 12
        }
    }
}
