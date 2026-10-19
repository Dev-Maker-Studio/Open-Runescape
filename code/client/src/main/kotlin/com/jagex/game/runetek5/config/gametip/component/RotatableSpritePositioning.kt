package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class RotatableSpritePositioning(
    spriteId: Int,
    anchorX: AnchorX,
    anchorY: AnchorY,
    offsetX: Int,
    offsetY: Int,
    @JvmField val utAngle: Int
) : SpritePositioning(spriteId, anchorX, anchorY, offsetX, offsetY) {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.ROTATED_SPRITE
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): RotatableSpritePositioning {
            val spriteId = buffer.readBigSmart()
            val anchorX = AnchorX.values()[buffer.readUByte()]
            val anchorY = AnchorY.values()[buffer.readUByte()]
            val offsetX = buffer.readShort()
            val offsetY = buffer.readShort()
            val utAngle = buffer.readShort()
            return RotatableSpritePositioning(spriteId, anchorX, anchorY, offsetX, offsetY, utAngle)
        }
    }
}
