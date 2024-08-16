package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.sound.player.sequence.SequenceSoundPlayer;

public class EntitySequence extends Sequence {

    public final SceneGraphEntity graphEntity;

    public EntitySequence(SceneGraphEntity graphEntity) {
        super(false);
        this.graphEntity = graphEntity;
    }

    @Override
    public void playSound(SeqType seqtype, int frameId) {
        SequenceSoundPlayer.playSequenceSound(seqtype, frameId, this.graphEntity);
    }

}
