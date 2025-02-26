package com.jagex.game.runetek5.config.cutscene.action;

import java.util.Arrays;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;

public class CutsceneAnimatedMovement extends CutsceneAction {

    public final int entityIndex;

    public final int[] movementAnimations;

    public final int movementAnimationId;

    public final int seqFlag;

    public CutsceneAnimatedMovement(Packet buffer) {
        super(buffer);
        this.entityIndex = buffer.readUShort();
        this.movementAnimations = new int[MovementType.values().length];
        this.movementAnimationId = buffer.readBigSmart();
        Arrays.fill(this.movementAnimations, 0, this.movementAnimations.length, this.movementAnimationId);
        this.seqFlag = buffer.readInt();
    }

    @Override
    public void perform() {
        PathingEntity pathingEntity = CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex].getEntity();
        if (this.seqFlag == 0) {
            PlayerUpdate.animate(pathingEntity, this.movementAnimations, 0, false);
        } else {
            PlayerUpdate.animate(pathingEntity, new int[]{this.movementAnimationId}, new int[1], new int[]{this.seqFlag});
        }
    }

    @Override
    public boolean isFullyCached() {
        SeqType seqType = TypeLists.SEQ.list(this.movementAnimationId);
        return seqType.ready();
    }
}
