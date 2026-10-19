package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.sound.player.sequence.SequenceSoundPlayer

class InterfaceSequence : Sequence(true) {
    override fun playSound(seqType: SeqType, frameId: Int) {
        SequenceSoundPlayer.playInterfaceSequenceSound(seqType, frameId)
    }
}
