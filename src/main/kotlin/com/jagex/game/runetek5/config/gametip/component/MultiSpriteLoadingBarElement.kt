package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

open class MultiSpriteLoadingBarElement(
    horizontalAnchor: AnchorX,
    verticalAnchor: AnchorY,
    offsetX: Int,
    offsetY: Int,
    width: Int,
    height: Int,
    textOffsetY: Int,
    fileId: Int,
    textColor: Int,
    @JvmField val spriteA: Int,
    @JvmField val spriteB: Int,
    @JvmField val spriteC: Int,
    @JvmField val spriteD: Int,
    @JvmField val spriteE: Int,
    @JvmField val spriteF: Int
) : AnchoredElement(horizontalAnchor, verticalAnchor, offsetX, offsetY, width, height, textOffsetY, fileId, textColor) {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.MULTI_SPRITE_LOADING_BAR
    }
}
