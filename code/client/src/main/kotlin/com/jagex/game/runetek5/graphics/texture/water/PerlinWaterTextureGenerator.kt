package com.jagex.game.runetek5.graphics.texture.water

internal class PerlinWaterTextureGenerator {

    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        fun generateNoiseLayer(layer: Int, noiseGen: NoiseGenerator, scaleX: Float, scaleY: Float, scaleZ: Float, amplitude: Float, output: ByteArray, offset: Int) {
            val octaveSize = 16384
            val tempBuffer = FloatArray(octaveSize)

            var currentScaleX = scaleX
            var currentScaleY = scaleY
            var currentScaleZ = scaleZ
            var currentAmplitude = amplitude

            for (octave in 0 until 8) {
                var writePos = offset
                noiseGen.generateNoise3D(layer, currentScaleX / 128, currentScaleY / 128, currentScaleZ / 16, currentAmplitude * 127.0f, tempBuffer)

                for (i in 0 until octaveSize) {
                    output[writePos] = (output[writePos] + tempBuffer[i]).toInt().toByte()
                    ++writePos
                }

                currentScaleX *= 2.0f
                currentScaleY *= 2.0f
                currentScaleZ *= 2.0f
                currentAmplitude *= 0.6f
            }

            var normalizePos = offset

            for (i in 0 until octaveSize) {
                output[normalizePos] = (output[normalizePos] + 127).toByte()
                ++normalizePos
            }
        }

        @JvmStatic
        fun generatePerlinWaterTexture(noiseGen: NoiseGenerator): ByteArray {
            val textureData = ByteArray(262144)
            generatePerlinWaterTextureLayers(noiseGen, textureData, 0)
            return textureData
        }

        @JvmStatic
        fun generatePerlinWaterTextureLayers(noiseGen: NoiseGenerator, output: ByteArray, startOffset: Int) {
            var offset = startOffset
            for (layer in 0 until 16) {
                generateNoiseLayer(layer, noiseGen, 4.0f, 4.0f, 16.0f, 0.5f, output, offset)
                offset += 16384
            }
        }
    }
}
