package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails

class TipsSprite internal constructor(
    @JvmField val spriteFileId: Int
) : ITipDetails {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.aClass60601
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): TipsSprite {
            val fileId = buffer.readBigSmart()
            return TipsSprite(fileId)
        }
    }
}
