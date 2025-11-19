package com.jagex.game.runetek5.config.gametip

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning
import com.jagex.game.runetek5.config.gametip.component.TipsBackground
import com.jagex.game.runetek5.config.gametip.component.TipsSprite
import com.jagex.game.runetek5.config.gametip.component.decodeSub1
import com.jagex.game.runetek5.config.gametip.component.decodeSub2
import com.jagex.game.runetek5.config.gametip.component.decodeSub3
import com.jagex.game.runetek5.config.gametip.component.decodeSub3Sub1

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
            TipRenderDetails.aClass60602 -> decodeSub2(buffer)
            TipRenderDetails.OUTLINED -> OutlinedNewsItem.decode(buffer)
            TipRenderDetails.ROTATED_SPRITE -> RotatableSpritePositioning.decode(buffer)
            TipRenderDetails.SPRITE -> SpritePositioning.decode(buffer)
            TipRenderDetails.aClass60603 -> decodeSub1(buffer)
            TipRenderDetails.aClass60608 -> decodeSub3(buffer)
            TipRenderDetails.ANCHORED_TEXT -> AnchoredTextMeta.decode(buffer)
            TipRenderDetails.aClass60601 -> TipsSprite.decode(buffer)
            TipRenderDetails.aClass60610 -> decodeSub3Sub1(buffer)
            else -> null
        }
    }
}
