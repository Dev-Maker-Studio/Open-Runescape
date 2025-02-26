package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 01/02/2023
 *
 */
public class CutsceneSetHintDetails extends CutsceneAction {

    public final int entityIndex;
    public final String text;
    public final int colorType;
    public final int duration;

    public CutsceneSetHintDetails(Packet buffer) {
        super(buffer);
        this.entityIndex = buffer.readUShort();
        this.text = buffer.readString();
        this.colorType = buffer.readInt();
        this.duration = buffer.readUShort();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex].getEntity().setHintDetails(this.text, this.colorType, 0, this.duration);
    }
}
