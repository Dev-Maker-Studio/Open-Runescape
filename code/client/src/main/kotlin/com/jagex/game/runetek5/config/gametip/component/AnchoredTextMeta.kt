package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class AnchoredTextMeta(
    @JvmField val tipText: String,
    @JvmField val anchorX: AnchorX,
    @JvmField val anchorY: AnchorY,
    @JvmField val offsetX: Int,
    @JvmField val offsetY: Int,
    @JvmField val textAllignment: Int,
    @JvmField val textVerticalAllignment: Int,
    @JvmField val lineSpacing: Int,
    @JvmField val width: Int,
    @JvmField val height: Int,
    /**
     * The sprite & font id
     */
    @JvmField val fileId: Int,
    @JvmField val textColor: Int,
    @JvmField val textShadowColor: Int
) : ITipDetails {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.ANCHORED_TEXT
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): AnchoredTextMeta {
            val tipText = buffer.readString()
            val anchorX = AnchorX.values()[buffer.readUByte()]
            val archorY = AnchorY.values()[buffer.readUByte()]
            val offsetX = buffer.readShort()
            val offsetY = buffer.readShort()
            val textAllignment = buffer.readUByte()
            val textVerticalAllignment = buffer.readUByte()
            val lineSpacing = buffer.readUByte()
            val width = buffer.readUShort()
            val height = buffer.readUShort()
            val fileId = buffer.readBigSmart()
            val textColor = buffer.readInt()
            val textShadowColor = buffer.readInt()
            return AnchoredTextMeta(
                tipText, anchorX, archorY, offsetX, offsetY, textAllignment,
                textVerticalAllignment, lineSpacing, width, height, fileId, textColor, textShadowColor
            )
        }
    }
}
