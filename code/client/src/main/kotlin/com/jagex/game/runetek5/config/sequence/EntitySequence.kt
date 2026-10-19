package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.sound.player.sequence.SequenceSoundPlayer

open class EntitySequence(
    @JvmField val graphEntity: SceneGraphEntity
) : Sequence(false) {
    override fun playSound(seqtype: SeqType, frameId: Int) {
        SequenceSoundPlayer.playSequenceSound(seqtype, frameId, graphEntity)
    }
}
