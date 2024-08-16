package com.jagex.game.runetek5.config.obj;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * [J]
 * @author Steven - 22/11/2023
 *
 */
public class ObjTypeCustomisation {

    public final int[] maleBody = new int[3];
    public final int[] femaleBody = new int[3];
    public final int[] maleHead = new int[2];
    public final int[] femaleHead = new int[2];
    public short[] recolDst;
    public short[] retexDst;

    public ObjTypeCustomisation(ObjType objType) {
        this.maleBody[0] = objType.maleEquip1;
        this.maleBody[1] = objType.maleEquip2;
        this.maleBody[2] = objType.maleEquip3;
        this.femaleBody[0] = objType.femaleEquip1;
        this.femaleBody[1] = objType.femaleEquip2;
        this.femaleBody[2] = objType.femaleEquip3;
        this.maleHead[0] = objType.maleHead1;
        this.maleHead[1] = objType.maleHead2;
        this.femaleHead[0] = objType.femaleHead1;
        this.femaleHead[1] = objType.femaleHead2;
        if (objType.recolorDst != null) {
            this.recolDst = new short[objType.recolorDst.length];
            System.arraycopy(objType.recolorDst, 0, this.recolDst, 0, this.recolDst.length);
        }
        if (objType.retexDst != null) {
            this.retexDst = new short[objType.retexDst.length];
            System.arraycopy(objType.retexDst, 0, this.retexDst, 0, this.retexDst.length);
        }
    }

    public static ObjTypeCustomisation decodeItemEffects(ObjType objType, Packet stream) {
        ObjTypeCustomisation customisation = new ObjTypeCustomisation(objType);
        int flags = stream.readUnsignedByte();
        boolean modifiesBody = (flags & 0x1) != 0;
        boolean modifiesHead = (flags & 0x2) != 0;
        boolean modifiesColors = (flags & 0x4) != 0;
        boolean modifiesTextures = (flags & 0x8) != 0;
        if (modifiesBody) {
            customisation.maleBody[0] = stream.readBigSmart();
            customisation.femaleBody[0] = stream.readBigSmart();
            if (objType.maleEquip2 != -1 || objType.femaleEquip2 != -1) {
                customisation.maleBody[1] = stream.readBigSmart();
                customisation.femaleBody[1] = stream.readBigSmart();
            }
            if (objType.maleEquip3 != -1 || objType.femaleEquip3 != -1) {
                customisation.maleBody[2] = stream.readBigSmart();
                customisation.femaleBody[2] = stream.readBigSmart();
            }
        }
        if (modifiesHead) {
            customisation.maleHead[0] = stream.readBigSmart();
            customisation.femaleHead[0] = stream.readBigSmart();
            if (objType.maleHead2 != -1 || objType.femaleHead2 != -1) {
                customisation.maleHead[1] = stream.readBigSmart();
                customisation.femaleHead[1] = stream.readBigSmart();
            }
        }
        if (modifiesColors) {
            int bitpackedSlots = stream.readUnsignedShort();
            int[] slots = {bitpackedSlots & 0xf, bitpackedSlots >> 4 & 0xf, bitpackedSlots >> 8 & 0xf, bitpackedSlots >> 12 & 0xf};
            for (int i = 0; i < 4; i++) {
                if (slots[i] != 15) {
                    customisation.recolDst[slots[i]] = (short) stream.readUnsignedShort();
                }
            }
        }
        if (modifiesTextures) {
            int bitpackedSlots = stream.readUnsignedByte();
            int[] slots = {bitpackedSlots & 0xf, bitpackedSlots >> 4 & 0xf};
            for (int i = 0; i < 2; i++) {
                if (slots[i] != 15) {
                    customisation.retexDst[slots[i]] = (short) stream.readUnsignedShort();
                }
            }
        }
        return customisation;
    }
}
