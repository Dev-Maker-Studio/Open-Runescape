package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpHerringbone : TextureOperation(0, true) {

    @JvmField var scaleX: Int = 1
    @JvmField var scaleY: Int = 1
    @JvmField var threshold: Int = 204

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            for (x in 0 until TextureRaster.currentWidth) {
                val horizontalPos = TextureRaster.horizontalTable!![x]
                val verticalPos = TextureRaster.verticalTable!![y]
                var tileX = horizontalPos * scaleX shr 12
                var tileY = verticalPos * scaleY shr 12
                val offsetX = horizontalPos % (4096 / scaleX) * scaleX
                val offsetY = verticalPos % (4096 / scaleY) * scaleY
                if (offsetY < threshold) {
                    tileX -= tileY
                    while (tileX < 0) {
                        tileX += 4
                    }

                    while (tileX > 3) {
                        tileX -= 4
                    }

                    if (tileX != 1) {
                        output[x] = 0
                        continue
                    }

                    if (offsetX < threshold) {
                        output[x] = 0
                        continue
                    }
                }

                if (offsetX < threshold) {
                    tileX -= tileY
                    while (tileX < 0) {
                        tileX += 4
                    }

                    while (tileX > 3) {
                        tileX -= 4
                    }

                    if (tileX > 0) {
                        output[x] = 0
                        continue
                    }
                }

                output[x] = 4096
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> scaleX = buffer.readUByte()
            1 -> scaleY = buffer.readUByte()
            2 -> threshold = buffer.readUShort()
        }
    }
}
