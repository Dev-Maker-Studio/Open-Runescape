package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.graphics.texture.water.WaterTextureGenerator
import com.jagex.game.runetek5.utils.ArrayUtils
import kotlin.math.pow

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpPerlinNoise : TextureOperation(0, true) {

    @JvmField
    var noiseArray: ByteArray = ByteArray(512)

    @JvmField
    var numNoiseSteps: Int = 4

    @JvmField
    var noiseAmplitude: Int = 1638

    @JvmField
    var texCoordScaleY: Int = 4

    @JvmField
    var texCoordScaleX: Int = 4

    @JvmField
    var randomSeed: Int = 0

    @JvmField
    var stepMultipliers: ShortArray? = null

    @JvmField
    var useFogEffect: Boolean = true

    @JvmField
    var amplitudeMultipliers: ShortArray? = null

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            computeNoiseStep(i_1, ints_3)
        }
        return ints_3
    }

    fun computeNoiseStep(i_1: Int, ints_2: IntArray) {
        val i_4 = TextureRaster.verticalTable!![i_1] * texCoordScaleY
        var s_5: Short
        var i_6: Int
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_16: Int
        var i_17: Int
        if (numNoiseSteps == 1) {
            s_5 = stepMultipliers!![0]
            i_6 = amplitudeMultipliers!![0].toInt() shl 12
            i_7 = i_6 * texCoordScaleY shr 12
            i_8 = i_6 * texCoordScaleX shr 12
            i_9 = i_6 * i_4 shr 12
            i_10 = i_9 shr 12
            i_11 = i_10 + 1
            if (i_11 >= i_7) {
                i_11 = 0
            }
            i_9 = i_9 and 0xfff
            i_12 = WaterTextureGenerator.SINE_WAVE[i_9]
            i_13 = noiseArray[i_10 and 0xff].toInt() and 0xff
            i_14 = noiseArray[i_11 and 0xff].toInt() and 0xff
            if (useFogEffect) {
                i_15 = 0
                while (i_15 < TextureRaster.currentWidth) {
                    i_16 = TextureRaster.horizontalTable!![i_15] * texCoordScaleX
                    i_17 = computeNoise(i_6 * i_16 shr 12, i_9, i_13, i_14, i_12, i_8)
                    i_17 = s_5 * i_17 shr 12
                    ints_2[i_15] = (i_17 shr 1) + 2048
                    i_15++
                }
            } else {
                i_15 = 0
                while (i_15 < TextureRaster.currentWidth) {
                    i_16 = TextureRaster.horizontalTable!![i_15] * texCoordScaleX
                    i_17 = computeNoise(i_6 * i_16 shr 12, i_9, i_13, i_14, i_12, i_8)
                    ints_2[i_15] = s_5 * i_17 shr 12
                    i_15++
                }
            }
        } else {
            s_5 = stepMultipliers!![0]
            if (s_5 > 8 || s_5 < -8) {
                i_6 = amplitudeMultipliers!![0].toInt() shl 12
                i_7 = i_6 * texCoordScaleY shr 12
                i_8 = i_6 * texCoordScaleX shr 12
                i_9 = i_4 * i_6 shr 12
                i_10 = i_9 shr 12
                i_11 = i_10 + 1
                if (i_11 >= i_7) {
                    i_11 = 0
                }
                i_9 = i_9 and 0xfff
                i_12 = WaterTextureGenerator.SINE_WAVE[i_9]
                i_13 = noiseArray[i_10 and 0xff].toInt() and 0xff
                i_14 = noiseArray[i_11 and 0xff].toInt() and 0xff
                i_15 = 0
                while (i_15 < TextureRaster.currentWidth) {
                    i_16 = TextureRaster.horizontalTable!![i_15] * texCoordScaleX
                    i_17 = computeNoise(i_6 * i_16 shr 12, i_9, i_13, i_14, i_12, i_8)
                    ints_2[i_15] = i_17 * s_5 shr 12
                    i_15++
                }
            }
            i_6 = 1
            while (i_6 < numNoiseSteps) {
                s_5 = stepMultipliers!![i_6]
                if (s_5 > 8 || s_5 < -8) {
                    i_7 = amplitudeMultipliers!![i_6].toInt() shl 12
                    i_8 = i_7 * texCoordScaleY shr 12
                    i_9 = i_7 * texCoordScaleX shr 12
                    i_10 = i_4 * i_7 shr 12
                    i_11 = i_10 shr 12
                    i_12 = i_11 + 1
                    if (i_12 >= i_8) {
                        i_12 = 0
                    }
                    i_10 = i_10 and 0xfff
                    i_13 = WaterTextureGenerator.SINE_WAVE[i_10]
                    i_14 = noiseArray[i_11 and 0xff].toInt() and 0xff
                    i_15 = noiseArray[i_12 and 0xff].toInt() and 0xff
                    var i_18: Int
                    if (useFogEffect && i_6 == numNoiseSteps - 1) {
                        i_16 = 0
                        while (i_16 < TextureRaster.currentWidth) {
                            i_17 = TextureRaster.horizontalTable!![i_16] * texCoordScaleX
                            i_18 = computeNoise(i_17 * i_7 shr 12, i_10, i_14, i_15, i_13, i_9)
                            i_18 = ints_2[i_16] + (s_5 * i_18 shr 12)
                            ints_2[i_16] = (i_18 shr 1) + 2048
                            i_16++
                        }
                    } else {
                        i_16 = 0
                        while (i_16 < TextureRaster.currentWidth) {
                            i_17 = TextureRaster.horizontalTable!![i_16] * texCoordScaleX
                            i_18 = computeNoise(i_7 * i_17 shr 12, i_10, i_14, i_15, i_13, i_9)
                            ints_2[i_16] += i_18 * s_5 shr 12
                            i_16++
                        }
                    }
                }
                i_6++
            }
        }
    }

    fun computeNoise(coordX: Int, coordY: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): Int {
        var posX = coordX shr 12
        var nextPosX = posX + 1
        if (nextPosX >= i_6) {
            nextPosX = 0
        }
        val coordXMasked = coordX and 0xfff
        posX = posX and 0xff
        nextPosX = nextPosX and 0xff
        val negPosX = coordXMasked - 4096
        val negPosY = coordY - 4096
        val noise1 = WaterTextureGenerator.SINE_WAVE[coordXMasked]
        var noiseType1 = noiseArray[i_3 + posX].toInt() and 0x3
        var i_14: Int
        i_14 = if (noiseType1 <= 1) {
            if (noiseType1 == 0) coordY + coordXMasked else coordY - coordXMasked
        } else {
            if (noiseType1 == 2) coordXMasked - coordY else -coordXMasked - coordY
        }
        noiseType1 = noiseArray[i_3 + nextPosX].toInt() and 0x3
        var i_15: Int
        i_15 = if (noiseType1 <= 1) {
            if (noiseType1 == 0) negPosX + coordY else coordY - negPosX
        } else {
            if (noiseType1 == 2) negPosX - coordY else -negPosX - coordY
        }
        val i_16 = i_14 + (noise1 * (i_15 - i_14) shr 12)
        noiseType1 = noiseArray[posX + i_4].toInt() and 0x3
        i_14 = if (noiseType1 <= 1) {
            if (noiseType1 == 0) negPosY + coordXMasked else negPosY - coordXMasked
        } else {
            if (noiseType1 == 2) coordXMasked - negPosY else -coordXMasked - negPosY
        }
        noiseType1 = noiseArray[nextPosX + i_4].toInt() and 0x3
        i_15 = if (noiseType1 <= 1) {
            if (noiseType1 == 0) negPosX + negPosY else negPosY - negPosX
        } else {
            if (noiseType1 == 2) negPosX - negPosY else -negPosX - negPosY
        }
        val i_17 = i_14 + (noise1 * (i_15 - i_14) shr 12)
        return i_16 + (i_5 * (i_17 - i_16) shr 12)
    }

    override fun postDecode() {
        noiseArray = ArrayUtils.generateRandomlySeededBytearray(randomSeed)
        computeNoiseStep()
        var i = numNoiseSteps - 1
        while (i >= 1) {
            val stepMultiplier = stepMultipliers!![i]
            if (stepMultiplier > 8 || stepMultiplier < -8) {
                break
            }
            --numNoiseSteps
            --i
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> {
                useFogEffect = buffer.readUByte() == 1
            }
            1 -> {
                numNoiseSteps = buffer.readUByte()
            }
            2 -> {
                noiseAmplitude = buffer.readShort()
                if (noiseAmplitude < 0) {
                    stepMultipliers = ShortArray(numNoiseSteps)
                    for (i_4 in 0 until numNoiseSteps) {
                        stepMultipliers!![i_4] = buffer.readShort().toShort()
                    }
                }
            }
            3 -> {
                texCoordScaleY = buffer.readUByte()
                texCoordScaleX = texCoordScaleY
            }
            4 -> {
                randomSeed = buffer.readUByte()
            }
            5 -> {
                texCoordScaleX = buffer.readUByte()
            }
            6 -> {
                texCoordScaleY = buffer.readUByte()
            }
        }
    }

    fun computeNoiseStep() {
        if (noiseAmplitude > 0) {
            stepMultipliers = ShortArray(numNoiseSteps)
            amplitudeMultipliers = ShortArray(numNoiseSteps)
            for (i in 0 until numNoiseSteps) {
                stepMultipliers!![i] = ((noiseAmplitude / 4096.0f).pow(i) * 4096.0).toInt().toShort()
                amplitudeMultipliers!![i] = 2.0.pow(i).toInt().toShort()
            }
        } else if (stepMultipliers != null && stepMultipliers!!.size == numNoiseSteps) {
            amplitudeMultipliers = ShortArray(numNoiseSteps)
            for (i in 0 until numNoiseSteps) {
                amplitudeMultipliers!![i] = 2.0.pow(i).toInt().toShort()
            }
        }
    }

    companion object {
        @JvmStatic
        fun generateNoiseMap(): Array<IntArray> {
            val noiseMap = Array(256) { IntArray(64) }
            val flickeringTexture = TextureOpPerlinNoise()
            flickeringTexture.useFogEffect = false
            flickeringTexture.texCoordScaleX = 4
            flickeringTexture.texCoordScaleY = 4
            flickeringTexture.numNoiseSteps = 3
            flickeringTexture.noiseAmplitude = (4096.0f * 0.4f).toInt()
            flickeringTexture.postDecode()
            TextureRaster.setDimensions(64, 256)
            for (i in 0 until 256) {
                flickeringTexture.computeNoiseStep(i, noiseMap[i])
            }
            return noiseMap
        }

        @JvmStatic
        fun generateFlickeringEffect(): IntArray {
            val flickeringEffect = IntArray(2048)
            val flickeringTexture = TextureOpPerlinNoise()
            flickeringTexture.randomSeed = 35
            flickeringTexture.useFogEffect = true
            flickeringTexture.texCoordScaleX = 8
            flickeringTexture.texCoordScaleY = 8
            flickeringTexture.numNoiseSteps = 4
            flickeringTexture.noiseAmplitude = (4096.0f * 0.4f).toInt()
            flickeringTexture.postDecode()
            TextureRaster.setDimensions(2048, 1)
            flickeringTexture.computeNoiseStep(0, flickeringEffect)
            return flickeringEffect
        }
    }
}
