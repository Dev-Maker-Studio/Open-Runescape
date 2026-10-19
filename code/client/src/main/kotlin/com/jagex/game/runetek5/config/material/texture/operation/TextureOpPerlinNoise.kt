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

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            computeNoiseStep(y, output)
        }
        return output
    }

    fun computeNoiseStep(y: Int, pixels: IntArray) {
        val scaledY = TextureRaster.verticalTable!![y] * texCoordScaleY
        var stepMult: Short
        var ampScale: Int
        var rangeY: Int
        var rangeX: Int
        var coordY: Int
        var gridY: Int
        var nextGridY: Int
        var fracY: Int
        var lerpY: Int
        var noiseRowA: Int
        var noiseRowB: Int
        var x: Int
        var scaledX: Int
        var noiseValue: Int
        if (numNoiseSteps == 1) {
            stepMult = stepMultipliers!![0]
            ampScale = amplitudeMultipliers!![0].toInt() shl 12
            rangeY = ampScale * texCoordScaleY shr 12
            rangeX = ampScale * texCoordScaleX shr 12
            coordY = ampScale * scaledY shr 12
            gridY = coordY shr 12
            nextGridY = gridY + 1
            if (nextGridY >= rangeY) {
                nextGridY = 0
            }
            coordY = coordY and 0xfff
            fracY = WaterTextureGenerator.SINE_WAVE[coordY]
            noiseRowA = noiseArray[gridY and 0xff].toInt() and 0xff
            noiseRowB = noiseArray[nextGridY and 0xff].toInt() and 0xff
            if (useFogEffect) {
                x = 0
                while (x < TextureRaster.currentWidth) {
                    scaledX = TextureRaster.horizontalTable!![x] * texCoordScaleX
                    noiseValue = computeNoise(ampScale * scaledX shr 12, coordY, noiseRowA, noiseRowB, fracY, rangeX)
                    noiseValue = stepMult * noiseValue shr 12
                    pixels[x] = (noiseValue shr 1) + 2048
                    x++
                }
            } else {
                x = 0
                while (x < TextureRaster.currentWidth) {
                    scaledX = TextureRaster.horizontalTable!![x] * texCoordScaleX
                    noiseValue = computeNoise(ampScale * scaledX shr 12, coordY, noiseRowA, noiseRowB, fracY, rangeX)
                    pixels[x] = stepMult * noiseValue shr 12
                    x++
                }
            }
        } else {
            stepMult = stepMultipliers!![0]
            if (stepMult > 8 || stepMult < -8) {
                ampScale = amplitudeMultipliers!![0].toInt() shl 12
                rangeY = ampScale * texCoordScaleY shr 12
                rangeX = ampScale * texCoordScaleX shr 12
                coordY = scaledY * ampScale shr 12
                gridY = coordY shr 12
                nextGridY = gridY + 1
                if (nextGridY >= rangeY) {
                    nextGridY = 0
                }
                coordY = coordY and 0xfff
                fracY = WaterTextureGenerator.SINE_WAVE[coordY]
                noiseRowA = noiseArray[gridY and 0xff].toInt() and 0xff
                noiseRowB = noiseArray[nextGridY and 0xff].toInt() and 0xff
                x = 0
                while (x < TextureRaster.currentWidth) {
                    scaledX = TextureRaster.horizontalTable!![x] * texCoordScaleX
                    noiseValue = computeNoise(ampScale * scaledX shr 12, coordY, noiseRowA, noiseRowB, fracY, rangeX)
                    pixels[x] = noiseValue * stepMult shr 12
                    x++
                }
            }
            ampScale = 1
            while (ampScale < numNoiseSteps) {
                stepMult = stepMultipliers!![ampScale]
                if (stepMult > 8 || stepMult < -8) {
                    rangeY = amplitudeMultipliers!![ampScale].toInt() shl 12
                    rangeX = rangeY * texCoordScaleY shr 12
                    coordY = rangeY * texCoordScaleX shr 12
                    gridY = scaledY * rangeY shr 12
                    nextGridY = gridY shr 12
                    fracY = nextGridY + 1
                    if (fracY >= rangeX) {
                        fracY = 0
                    }
                    gridY = gridY and 0xfff
                    noiseRowA = WaterTextureGenerator.SINE_WAVE[gridY]
                    noiseRowB = noiseArray[nextGridY and 0xff].toInt() and 0xff
                    x = noiseArray[fracY and 0xff].toInt() and 0xff
                    var accumulated: Int
                    if (useFogEffect && ampScale == numNoiseSteps - 1) {
                        scaledX = 0
                        while (scaledX < TextureRaster.currentWidth) {
                            noiseValue = TextureRaster.horizontalTable!![scaledX] * texCoordScaleX
                            accumulated = computeNoise(noiseValue * rangeY shr 12, gridY, noiseRowB, x, noiseRowA, coordY)
                            accumulated = pixels[scaledX] + (stepMult * accumulated shr 12)
                            pixels[scaledX] = (accumulated shr 1) + 2048
                            scaledX++
                        }
                    } else {
                        scaledX = 0
                        while (scaledX < TextureRaster.currentWidth) {
                            noiseValue = TextureRaster.horizontalTable!![scaledX] * texCoordScaleX
                            accumulated = computeNoise(rangeY * noiseValue shr 12, gridY, noiseRowB, x, noiseRowA, coordY)
                            pixels[scaledX] += accumulated * stepMult shr 12
                            scaledX++
                        }
                    }
                }
                ampScale++
            }
        }
    }

    fun computeNoise(coordX: Int, coordY: Int, noiseRowA: Int, noiseRowB: Int, lerpY: Int, rangeX: Int): Int {
        var posX = coordX shr 12
        var nextPosX = posX + 1
        if (nextPosX >= rangeX) {
            nextPosX = 0
        }
        val fracX = coordX and 0xfff
        posX = posX and 0xff
        nextPosX = nextPosX and 0xff
        val negFracX = fracX - 4096
        val negFracY = coordY - 4096
        val lerpX = WaterTextureGenerator.SINE_WAVE[fracX]
        var gradientType = noiseArray[noiseRowA + posX].toInt() and 0x3
        var gradientA: Int
        gradientA = if (gradientType <= 1) {
            if (gradientType == 0) coordY + fracX else coordY - fracX
        } else {
            if (gradientType == 2) fracX - coordY else -fracX - coordY
        }
        gradientType = noiseArray[noiseRowA + nextPosX].toInt() and 0x3
        var gradientB: Int
        gradientB = if (gradientType <= 1) {
            if (gradientType == 0) negFracX + coordY else coordY - negFracX
        } else {
            if (gradientType == 2) negFracX - coordY else -negFracX - coordY
        }
        val interpX = gradientA + (lerpX * (gradientB - gradientA) shr 12)
        gradientType = noiseArray[posX + noiseRowB].toInt() and 0x3
        gradientA = if (gradientType <= 1) {
            if (gradientType == 0) negFracY + fracX else negFracY - fracX
        } else {
            if (gradientType == 2) fracX - negFracY else -fracX - negFracY
        }
        gradientType = noiseArray[nextPosX + noiseRowB].toInt() and 0x3
        gradientB = if (gradientType <= 1) {
            if (gradientType == 0) negFracX + negFracY else negFracY - negFracX
        } else {
            if (gradientType == 2) negFracX - negFracY else -negFracX - negFracY
        }
        val interpY = gradientA + (lerpX * (gradientB - gradientA) shr 12)
        return interpX + (lerpY * (interpY - interpX) shr 12)
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
                    for (i in 0 until numNoiseSteps) {
                        stepMultipliers!![i] = buffer.readShort().toShort()
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
                stepMultipliers!![i] = ((noiseAmplitude / 4096.0f).toDouble().pow(i) * 4096.0).toInt().toShort()
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
