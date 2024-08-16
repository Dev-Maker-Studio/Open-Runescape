package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;

/**
 * 
 * @author Steven - 17/08/2023
 *
 */
public class CutsceneApplyHitmark extends CutsceneAction {

    public final int entityIndex;

    public final int hitsplatId;

    public final int hitText;

    public final int soakHitsplatId;

    public final int soakText;

    public CutsceneApplyHitmark(Packet buffer) {
        super(buffer);
        this.entityIndex = buffer.readUnsignedShort();
        int flags = buffer.readUnsignedByte();
        if ((flags & 0x1) != 0) {
            this.hitsplatId = buffer.readUnsignedShort();
            this.hitText = buffer.readUnsignedShort();
        } else {
            this.hitsplatId = -1;
            this.hitText = -1;
        }
        if ((flags & 0x2) != 0) {
            this.soakHitsplatId = buffer.readUnsignedShort();
            this.soakText = buffer.readUnsignedShort();
        } else {
            this.soakHitsplatId = -1;
            this.soakText = -1;
        }
        if ((flags & 0x4) != 0) {
            int i_3 = buffer.readUnsignedShort();
            int i_4 = buffer.readUnsignedShort();
            int i_5 = i_3 * 255 / i_4;
            if (i_3 > 0 && i_5 < 1) {
                boolean bool6 = true;
            }
        }
    }

    @Override
    public void perform() {
        PathingEntity entity = CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex].getEntity();
        entity.applyHit(this.hitsplatId, this.hitText, this.soakHitsplatId, this.soakText, client.cycles, 0);
    }

}
