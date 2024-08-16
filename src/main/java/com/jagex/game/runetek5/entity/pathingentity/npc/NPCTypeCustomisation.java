package com.jagex.game.runetek5.entity.pathingentity.npc;

import com.jagex.game.runetek5.config.npc.NPCType;

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 */
public class NPCTypeCustomisation {

    public long uid;

    public final int[] modelIds;

    public short[] recolorDst;

    public short[] retextureDst;

    public NPCTypeCustomisation(long uid, int[] models, short[] recolorDst, short[] retextureDst) {
        this.uid = uid;
        this.modelIds = models;
        this.recolorDst = recolorDst;
        this.retextureDst = retextureDst;
    }

    public NPCTypeCustomisation(NPCType npcType, boolean body) {
        if (body) {
            this.modelIds = new int[npcType.modelIds.length];
            System.arraycopy(npcType.modelIds, 0, this.modelIds, 0, this.modelIds.length);
        } else {
            this.modelIds = new int[npcType.headModels.length];
            System.arraycopy(npcType.headModels, 0, this.modelIds, 0, this.modelIds.length);
        }
        if (npcType.recolorDst != null) {
            this.recolorDst = new short[npcType.recolorDst.length];
            System.arraycopy(npcType.recolorDst, 0, this.recolorDst, 0, this.recolorDst.length);
        }
        if (npcType.retextureDst != null) {
            this.retextureDst = new short[npcType.retextureDst.length];
            System.arraycopy(npcType.retextureDst, 0, this.retextureDst, 0, this.retextureDst.length);
        }
    }
}
