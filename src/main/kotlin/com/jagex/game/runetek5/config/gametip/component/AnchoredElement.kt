package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

open class AnchoredElement internal constructor(
    @JvmField val horizontalAnchor: AnchorX,
    @JvmField val verticalAnchor: AnchorY,
    @JvmField val offsetX: Int,
    @JvmField val offsetY: Int,
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val anInt4092: Int,
    @JvmField val fileId: Int,
    @JvmField val anInt4091: Int
) : ITipDetails {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.ANIMATED_LOADING_BAR
    }
}
