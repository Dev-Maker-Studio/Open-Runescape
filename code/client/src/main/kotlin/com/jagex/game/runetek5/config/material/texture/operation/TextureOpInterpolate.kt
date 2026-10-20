package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpInterpolate : TextureOperation(3, false) {

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val input0 = getChildMonochromeOutput(0, y)
            val input1 = getChildMonochromeOutput(1, y)
            val blendFactor = getChildMonochromeOutput(2, y)

            for (x in 0 until TextureRaster.currentWidth) {
                val blend = blendFactor[x]
                if (blend == 4096) {
                    output[x] = input0[x]
                } else if (blend == 0) {
                    output[x] = input1[x]
                } else {
                    output[x] = ((4096 - blend) * input1[x] + blend * input0[x]) shr 12
                }
            }
        }

        return output
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val blendFactor = getChildMonochromeOutput(2, y)
            val input0 = getChildColorOutput(0, y)
            val input1 = getChildColorOutput(1, y)
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]
            val input0Red = input0[0]
            val input0Green = input0[1]
            val input0Blue = input0[2]
            val input1Red = input1[0]
            val input1Green = input1[1]
            val input1Blue = input1[2]

            for (x in 0 until TextureRaster.currentWidth) {
                val blend = blendFactor[x]
                if (blend == 4096) {
                    outputRed[x] = input0Red[x]
                    outputGreen[x] = input0Green[x]
                    outputBlue[x] = input0Blue[x]
                } else if (blend == 0) {
                    outputRed[x] = input1Red[x]
                    outputGreen[x] = input1Green[x]
                    outputBlue[x] = input1Blue[x]
                } else {
                    val inverseBlend = 4096 - blend
                    outputRed[x] = (blend * input0Red[x] + inverseBlend * input1Red[x]) shr 12
                    outputGreen[x] = (blend * input0Green[x] + inverseBlend * input1Green[x]) shr 12
                    outputBlue[x] = (blend * input0Blue[x] + inverseBlend * input1Blue[x]) shr 12
                }
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            monochrome = buffer.readUByte() == 1
        }
    }
}
