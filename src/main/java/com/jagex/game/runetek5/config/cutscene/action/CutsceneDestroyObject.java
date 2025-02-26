package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 06/04/2022
 *
 */
public class CutsceneDestroyObject extends CutsceneAction {

    public final int cutsceneObjectPtr;

    public CutsceneDestroyObject(Packet buffer) {
        super(buffer);
        this.cutsceneObjectPtr = buffer.readUShort();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_OBJECTS[this.cutsceneObjectPtr].destroy();
    }

}
