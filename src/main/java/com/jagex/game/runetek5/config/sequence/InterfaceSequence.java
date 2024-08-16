package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.sound.player.sequence.SequenceSoundPlayer;

public class InterfaceSequence extends Sequence {

    public InterfaceSequence() {
        super(true);
    }

    @Override
    public void playSound(SeqType seqType, int frameId) {
        SequenceSoundPlayer.playInterfaceSequenceSound(seqType, frameId);
    }

}
