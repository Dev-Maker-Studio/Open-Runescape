package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

open class SpritePositioning(
    @JvmField val spriteId: Int,
    @JvmField val horizontalAnchor: AnchorX,
    @JvmField val verticalAnchor: AnchorY,
    @JvmField val spriteOffsetX: Int,
    @JvmField val spriteOffsetY: Int
) : ITipDetails {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.SPRITE
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): SpritePositioning {
            val spriteId = buffer.readBigSmart()
            val anchorX = AnchorX.values()[buffer.readUByte()]
            val anchorY = AnchorY.values()[buffer.readUByte()]
            val offsetX = buffer.readShort()
            val offsetY = buffer.readShort()
            return SpritePositioning(spriteId, anchorX, anchorY, offsetX, offsetY)
        }
    }
}
