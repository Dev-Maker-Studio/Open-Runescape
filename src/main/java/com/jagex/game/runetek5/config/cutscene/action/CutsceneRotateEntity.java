package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 06/04/2022
 *
 */
public class CutsceneRotateEntity extends CutsceneAction {

    public final int cutsceneEntityPtr;

    public final int rotation;

    public CutsceneRotateEntity(Packet buffer) {
        super(buffer);
        this.cutsceneEntityPtr = buffer.readUShort();
        this.rotation = buffer.readUShort();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[this.cutsceneEntityPtr].getEntity().turn(this.rotation, true);
    }
}
