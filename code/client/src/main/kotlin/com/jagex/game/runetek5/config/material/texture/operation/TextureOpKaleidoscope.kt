package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.PI
import kotlin.math.atan2

class TextureOpKaleidoscope : TextureOperation(1, false) {

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            monochrome = buffer.readUByte() == 1
        }
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]
            for (x in 0 until TextureRaster.currentWidth) {
                calculateKaleidoscopeCoordinates(x, y)
                val input = getChildColorOutput(0, mirroredY)
                outputRed[x] = input[0][mirroredX]
                outputGreen[x] = input[1][mirroredX]
                outputBlue[x] = input[2][mirroredX]
            }
        }
        return output
    }

    fun calculateKaleidoscopeCoordinates(x: Int, y: Int) {
        val horizontalPos = TextureRaster.horizontalTable!![x]
        val verticalPos = TextureRaster.verticalTable!![y]
        val angle = atan2((horizontalPos - 2048).toDouble(), (verticalPos - 2048).toDouble()).toFloat()

        if (angle >= -PI && angle <= -2.356194490192345) {
            mirroredX = x
            mirroredY = y
        } else if (angle <= -1.5707963267948966 && angle >= -2.356194490192345) {
            mirroredX = y
            mirroredY = x
        } else if (angle <= -0.7853981633974483 && angle >= -1.5707963267948966) {
            mirroredX = TextureRaster.currentWidth - y
            mirroredY = x
        } else if (angle <= 0.0f && angle >= -0.7853981633974483) {
            mirroredX = x
            mirroredY = TextureRaster.currentHeight - y
        } else if (angle >= 0.0f && angle <= 0.7853981633974483) {
            mirroredX = TextureRaster.currentWidth - x
            mirroredY = TextureRaster.currentHeight - y
        } else if (angle >= 0.7853981633974483 && angle <= 1.5707963267948966) {
            mirroredX = TextureRaster.currentWidth - y
            mirroredY = TextureRaster.currentHeight - x
        } else if (angle >= 1.5707963267948966 && angle <= 2.356194490192345) {
            mirroredX = y
            mirroredY = TextureRaster.currentHeight - x
        } else if (angle >= 2.356194490192345 && angle <= PI) {
            mirroredX = TextureRaster.currentWidth - x
            mirroredY = y
        }
        mirroredX = mirroredX and TextureRaster.rowEnd
        mirroredY = mirroredY and TextureRaster.columnEnd
    }

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            for (x in 0 until TextureRaster.currentWidth) {
                calculateKaleidoscopeCoordinates(x, y)
                val input = getChildMonochromeOutput(0, mirroredY)
                output[x] = input[mirroredX]
            }
        }
        return output
    }

    companion object {
        @JvmStatic var mirroredX: Int = 0
        @JvmStatic var mirroredY: Int = 0
    }
}
