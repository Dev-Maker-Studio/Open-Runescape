package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class OutlinedNewsItem(
    @JvmField val newsitemId: Int,
    @JvmField val horizontalAnchor: AnchorX,
    @JvmField val verticalAnchor: AnchorY,
    @JvmField val offsetX: Int,
    @JvmField val offsetY: Int,
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val textColor: Int,
    @JvmField val shadeColor: Int,
    @JvmField val backgroundColor: Int,
    @JvmField val drawOutline: Boolean
) : ITipDetails {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.OUTLINED
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): OutlinedNewsItem {
            val newsitemId = buffer.readUByte()
            val horizontalAnchor = AnchorX.values()[buffer.readUByte()]
            val verticalAnchor = AnchorY.values()[buffer.readUByte()]
            val offsetX = buffer.readShort()
            val offsetY = buffer.readShort()
            val width = buffer.readUShort()
            val height = buffer.readUShort()
            val textColor = buffer.readInt()
            val textShadeColor = buffer.readInt()
            val backgroundColor = buffer.readInt()
            val drawOutline = buffer.readUByte() == 1
            return OutlinedNewsItem(
                newsitemId, horizontalAnchor, verticalAnchor, offsetX, offsetY,
                width, height, textColor, textShadeColor, backgroundColor, drawOutline
            )
        }
    }
}
