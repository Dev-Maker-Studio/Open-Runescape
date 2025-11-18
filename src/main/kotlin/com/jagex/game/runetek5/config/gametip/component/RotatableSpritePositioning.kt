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
}
