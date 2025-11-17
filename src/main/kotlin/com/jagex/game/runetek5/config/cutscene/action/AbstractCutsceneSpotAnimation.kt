package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType

abstract class AbstractCutsceneSpotAnimation(buffer: Packet) : CutsceneAction(buffer) {
    @JvmField val gfxId: Int = buffer.readUShort()
    @JvmField val rotation: Int = buffer.readUShort()
    @JvmField val displayHeight: Int = buffer.readUByte()

    override fun isFullyCached(): Boolean {
        val gfxType: SpotAnimType = TypeLists.SPOT_ANIM.list(this.gfxId)
        var ready = gfxType.isModelLoaded()
        val seqType: SeqType = TypeLists.SEQ.list(gfxType.sequenceId)
        ready = ready and seqType.ready()
        return ready
    }
}
