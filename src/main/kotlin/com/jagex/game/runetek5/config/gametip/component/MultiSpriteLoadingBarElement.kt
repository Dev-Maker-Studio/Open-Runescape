package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

open class MultiSpriteLoadingBarElement(
    horizontalAnchor: AnchorX,
    verticalAnchor: AnchorY,
    offX: Int,
    offY: Int,
    width: Int,
    height: Int,
    i_7: Int,
    fileId: Int,
    i_9: Int,
    @JvmField val spriteA: Int,
    @JvmField val spriteB: Int,
    @JvmField val spriteC: Int,
    @JvmField val spriteD: Int,
    @JvmField val spriteE: Int,
    @JvmField val spriteF: Int
) : AnchoredElement(horizontalAnchor, verticalAnchor, offX, offY, width, height, i_7, fileId, i_9) {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.MULTI_SPRITE_LOADING_BAR
    }
}
