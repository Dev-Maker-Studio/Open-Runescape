package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class ColorLoadingBarElement(
    class3561: AnchorX,
    class3532: AnchorY,
    i_3: Int,
    i_4: Int,
    i_5: Int,
    i_6: Int,
    i_7: Int,
    i_8: Int,
    i_9: Int,
    @JvmField val fillColor: Int,
    @JvmField val outlineColor: Int
) : AnchoredElement(class3561, class3532, i_3, i_4, i_5, i_6, i_7, i_8, i_9) {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.COLOR_LOADING_BAR
    }
}
