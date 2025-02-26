package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 01/02/2023
 *
 */
public class CutsceneResetEntity extends CutsceneAction {

    public final int entityIndex;

    public CutsceneResetEntity(Packet buffer) {
        super(buffer);
        this.entityIndex = buffer.readUShort();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex].reset();
    }
}
