package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.sound.player.sequence.SequenceSoundPlayer;

public class MovingSequence extends Sequence {

    public boolean hasType;
    public final PathingEntity pathingEntity;

    public MovingSequence(PathingEntity pathingEntity) {
        super(false);
        this.pathingEntity = pathingEntity;
    }

    @Override
    public void playSound(SeqType seqtype, int soundIndex) {
        if (!this.hasType || !this.pathingEntity.currentSequence.hasType() || this.pathingEntity.currentSequence.hasSpeed()) {
            SequenceSoundPlayer.playSequenceSound(seqtype, soundIndex, this.pathingEntity);
        }
    }
}
