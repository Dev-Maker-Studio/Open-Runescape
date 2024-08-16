package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

public class CutsceneEntitySpotAnimation extends AbstractCutsceneSpotAnimation {

    public final int targetIndex;

    public final int spotAnimationIndex;

    public final int adjustmentType;

    public CutsceneEntitySpotAnimation(Packet buffer) {
        super(buffer);
        this.targetIndex = buffer.readUnsignedShort();
        this.spotAnimationIndex = buffer.readUnsignedByte();
        this.adjustmentType = buffer.readUnsignedShort();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[this.targetIndex].getEntity().playSpotAnimation(super.gfxId, super.displayHeight << 16, super.rotation, this.adjustmentType, false, this.spotAnimationIndex);
    }
}
