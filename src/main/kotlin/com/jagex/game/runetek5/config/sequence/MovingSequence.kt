package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.sound.player.sequence.SequenceSoundPlayer

class MovingSequence(
    @JvmField val pathingEntity: PathingEntity
) : Sequence(false) {

    @JvmField
    var hasType: Boolean = false

    override fun playSound(seqtype: SeqType, soundIndex: Int) {
        if (!hasType || !pathingEntity.currentSequence.hasType() || pathingEntity.currentSequence.hasSpeed()) {
            SequenceSoundPlayer.playSequenceSound(seqtype, soundIndex, pathingEntity)
        }
    }
}
