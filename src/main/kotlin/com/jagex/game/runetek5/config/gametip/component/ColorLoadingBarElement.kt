package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class ColorLoadingBarElement(
    horizontalAnchor: AnchorX,
    verticalAnchor: AnchorY,
    offsetX: Int,
    offsetY: Int,
    width: Int,
    height: Int,
    textOffsetY: Int,
    fileId: Int,
    textColor: Int,
    @JvmField val fillColor: Int,
    @JvmField val outlineColor: Int
) : AnchoredElement(horizontalAnchor, verticalAnchor, offsetX, offsetY, width, height, textOffsetY, fileId, textColor) {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.COLOR_LOADING_BAR
    }
}
