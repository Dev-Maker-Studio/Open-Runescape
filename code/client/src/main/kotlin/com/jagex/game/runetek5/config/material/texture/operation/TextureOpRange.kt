package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpRange : TextureOperation(1, false) {

    @JvmField var rangeMultiplier: Int = 2048
    @JvmField var rangeOffset: Int = 1024
    @JvmField var rangeMax: Int = 3072

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val childOutput = getChildColorOutput(0, y)
            val redInput = childOutput[0]
            val greenInput = childOutput[1]
            val blueInput = childOutput[2]
            val redOutput = output[0]
            val greenOutput = output[1]
            val blueOutput = output[2]

            for (x in 0 until TextureRaster.currentWidth) {
                redOutput[x] = (rangeMultiplier * redInput[x] shr 12) + rangeOffset
                greenOutput[x] = (rangeMultiplier * greenInput[x] shr 12) + rangeOffset
                blueOutput[x] = (rangeMultiplier * blueInput[x] shr 12) + rangeOffset
            }
        }
        return output
    }

    override fun postDecode() {
        rangeMultiplier = rangeMax - rangeOffset
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> rangeOffset = buffer.readUShort()
            1 -> rangeMax = buffer.readUShort()
            2 -> monochrome = buffer.readUByte() == 1
        }
    }

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val childOutput = getChildMonochromeOutput(0, y)

            for (x in 0 until TextureRaster.currentWidth) {
                output[x] = (rangeMultiplier * childOutput[x] shr 12) + rangeOffset
            }
        }
        return output
    }
}
