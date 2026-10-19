package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet

class LineRasterizer(
    @JvmField val startX: Int,
    @JvmField val startY: Int,
    @JvmField val endX: Int,
    @JvmField val endY: Int,
    strokeColorParam: Int,
    strokeWidthParam: Int
) : TextureShapeRasterizer(-1, strokeColorParam, strokeWidthParam) {

    override fun drawStroked(width: Int, height: Int) {
        val scaledStartX = this.startX * width shr 12
        val scaledEndX = this.endX * width shr 12
        val scaledStartY = height * this.startY shr 12
        val scaledEndY = height * this.endY shr 12
        TextureShapeRasterizer.drawLine(scaledStartX, scaledStartY, scaledEndX, scaledEndY, super.strokeColor)
    }

    override fun drawFilled(width: Int, height: Int) {
    }

    override fun drawFilledAndStroked(width: Int, height: Int) {
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): LineRasterizer {
            return LineRasterizer(buffer.readShort(), buffer.readShort(), buffer.readShort(), buffer.readShort(), buffer.readUMedium(), buffer.readUByte())
        }
    }
}
