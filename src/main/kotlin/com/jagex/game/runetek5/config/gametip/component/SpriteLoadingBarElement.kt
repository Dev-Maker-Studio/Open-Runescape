package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class SpriteLoadingBarElement(
    horizontalAnchor: AnchorX,
    verticalAnchor: AnchorY,
    offsetX: Int,
    offsetY: Int,
    width: Int,
    height: Int,
    textOffsetY: Int,
    fileId: Int,
    textColor: Int,
    @JvmField val backgroundColor: Int,
    @JvmField val spriteFileId: Int
) : AnchoredElement(horizontalAnchor, verticalAnchor, offsetX, offsetY, width, height, textOffsetY, fileId, textColor) {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.SPRITE_LOADING_BAR
    }
}
