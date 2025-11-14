package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails

class TipsBackground(
    @JvmField val color: Int
) : ITipDetails {

    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.BACKGROUND
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): TipsBackground {
            val color = buffer.readInt()
            return TipsBackground(color)
        }
    }
}
