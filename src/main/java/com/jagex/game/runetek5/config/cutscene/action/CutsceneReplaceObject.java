package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 01/02/2023
 *
 */
public class CutsceneReplaceObject extends CutsceneAction {

    public final int locIndex;
    public final int x;
    public final int y;
    public final int plane;
    public final int rotation;

    public CutsceneReplaceObject(Packet buffer) {
        super(buffer);
        this.locIndex = buffer.readUnsignedShort();
        int bitpackedPosition = buffer.readInt();
        this.x = bitpackedPosition >>> 16;
        this.y = bitpackedPosition & 0xffff;
        this.plane = buffer.readUnsignedByte();
        this.rotation = buffer.readUnsignedByte();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_OBJECTS[this.locIndex].replace(this.plane, this.x, this.y, this.rotation);
    }
}
