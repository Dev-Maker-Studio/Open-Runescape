package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 12/01/2022
 *
 */
public class CutsceneMoveEntity extends CutsceneAction {

    public final int targetIndex;
    public final int x;
    public final int y;
    public final int plane;
    public final int direction;

    public CutsceneMoveEntity(Packet buffer) {
        super(buffer);
        this.targetIndex = buffer.readUShort();
        int bitpacked = buffer.readInt();
        this.x = bitpacked >>> 16;
        this.y = bitpacked & 0xffff;
        this.plane = buffer.readUByte();
        this.direction = buffer.readSmart();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[this.targetIndex].move(this.x, this.y, this.plane, this.direction);
    }
}
