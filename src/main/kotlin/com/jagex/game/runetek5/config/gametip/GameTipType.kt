package com.jagex.game.runetek5.config.gametip

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning
import com.jagex.game.runetek5.config.gametip.component.TipsBackground
import com.jagex.game.runetek5.config.gametip.component.TipsSprite
import com.jagex.game.runetek5.config.gametip.component.decodeSpriteLoadingBar
import com.jagex.game.runetek5.config.gametip.component.decodeColorLoadingBar
import com.jagex.game.runetek5.config.gametip.component.decodeMultiSpriteLoadingBar
import com.jagex.game.runetek5.config.gametip.component.decodeAnimatedLoadingBar

class GameTipType {

    @JvmField var tipDetailsCache: Array<ITipDetails?>? = null

    fun decode(buffer: Packet) {
        this.tipDetailsCache = arrayOfNulls(buffer.readUByte())
        val renderDetails = TipRenderDetails.values()
        for (index in this.tipDetailsCache!!.indices) {
            this.tipDetailsCache!![index] = decode(buffer, renderDetails[buffer.readUByte()])
        }
    }

    fun decode(buffer: Packet, details: TipRenderDetails): ITipDetails? {
        return when (details) {
            TipRenderDetails.BACKGROUND -> TipsBackground.decode(buffer)
            TipRenderDetails.COLOR_LOADING_BAR -> decodeColorLoadingBar(buffer)
            TipRenderDetails.OUTLINED -> OutlinedNewsItem.decode(buffer)
            TipRenderDetails.ROTATED_SPRITE -> RotatableSpritePositioning.decode(buffer)
            TipRenderDetails.SPRITE -> SpritePositioning.decode(buffer)
            TipRenderDetails.SPRITE_LOADING_BAR -> decodeSpriteLoadingBar(buffer)
            TipRenderDetails.MULTI_SPRITE_LOADING_BAR -> decodeMultiSpriteLoadingBar(buffer)
            TipRenderDetails.ANCHORED_TEXT -> AnchoredTextMeta.decode(buffer)
            TipRenderDetails.TIPS_SPRITE -> TipsSprite.decode(buffer)
            TipRenderDetails.ANIMATED_LOADING_BAR -> decodeAnimatedLoadingBar(buffer)
            else -> null
        }
    }
}
