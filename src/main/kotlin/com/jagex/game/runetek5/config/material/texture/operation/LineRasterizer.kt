package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet

class LineRasterizer(
    @JvmField val startX: Int,
    @JvmField val startY: Int,
    @JvmField val endX: Int,
    @JvmField val endY: Int,
    i_5: Int,
    i_6: Int
) : TextureShapeRasterizer(-1, i_5, i_6) {

    override fun drawStroked(i_1: Int, i_2: Int) {
        val i_4 = this.startX * i_1 shr 12
        val i_5 = this.endX * i_1 shr 12
        val i_6 = i_2 * this.startY shr 12
        val i_7 = i_2 * this.endY shr 12
        TextureShapeRasterizer.drawLine(i_4, i_6, i_5, i_7, super.strokeColor)
    }

    override fun drawFilled(i_1: Int, i_2: Int) {
    }

    override fun drawFilledAndStroked(i_1: Int, i_2: Int) {
    }

    companion object {
        @JvmStatic
        fun decode(rsbytebuffer0: Packet): LineRasterizer {
            return LineRasterizer(rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readUMedium(), rsbytebuffer0.readUByte())
        }
    }
}
