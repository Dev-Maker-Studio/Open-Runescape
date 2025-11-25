package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpTile : TextureOperation(1, false) {

    @JvmField var anInt9673: Int = 4
    @JvmField var anInt9674: Int = 4

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val tileWidth = TextureRaster.currentWidth / anInt9673
            val tileHeight = TextureRaster.currentHeight / anInt9674
            val childOutput: IntArray
            val index: Int
            if (tileHeight > 0) {
                index = y % tileHeight
                childOutput = getChildMonochromeOutput(0, index * TextureRaster.currentHeight / tileHeight)
            } else {
                childOutput = getChildMonochromeOutput(0, 0)
            }

            for (x in 0 until TextureRaster.currentWidth) {
                if (tileWidth > 0) {
                    val tileX = x % tileWidth
                    output[x] = childOutput[tileX * TextureRaster.currentWidth / tileWidth]
                } else {
                    output[x] = childOutput[0]
                }
            }
        }

        return output
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val tileWidth = TextureRaster.currentWidth / anInt9673
            val tileHeight = TextureRaster.currentHeight / anInt9674
            val childOutput: Array<IntArray>
            if (tileHeight > 0) {
                val tileY = y % tileHeight
                childOutput = getChildColorOutput(0, tileY * TextureRaster.currentHeight / tileHeight)
            } else {
                childOutput = getChildColorOutput(0, 0)
            }

            val childRed = childOutput[0]
            val childGreen = childOutput[1]
            val childBlue = childOutput[2]
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]

            for (x in 0 until TextureRaster.currentWidth) {
                val sourceX: Int
                if (tileWidth > 0) {
                    val tileX = x % tileWidth
                    sourceX = tileX * TextureRaster.currentWidth / tileWidth
                } else {
                    sourceX = 0
                }

                outputRed[x] = childRed[sourceX]
                outputGreen[x] = childGreen[sourceX]
                outputBlue[x] = childBlue[sourceX]
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> anInt9673 = buffer.readUByte()
            1 -> anInt9674 = buffer.readUByte()
        }
    }
}
