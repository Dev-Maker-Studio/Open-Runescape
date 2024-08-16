package com.jagex.game.runetek5.config.defaults;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.DefaultsFile;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class EquipmentDefaultsType {

    public int[] customizableObjSlots;
    public int shieldSlot = -1;
    public int weaponSlot = -1;
    public int[] hiddenAnimationShieldSlots;
    public int[] hiddenAnimationWeaponSlots;

    public EquipmentDefaultsType(Js5ResourceProvider defaultsResource) {
        byte[] data = defaultsResource.getFile(DefaultsFile.EQUIPMENT.fileId);
        decode(new Packet(data));
        if (this.customizableObjSlots == null) {
            throw new RuntimeException("");
        }
    }

    public void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            if (opcode == 1) {
                int count = buffer.readUnsignedByte();
                this.customizableObjSlots = new int[count];
                for (int i = 0; i < this.customizableObjSlots.length; i++) {
                    this.customizableObjSlots[i] = buffer.readUnsignedByte();
                }
            } else if (opcode == 3) {
                this.shieldSlot = buffer.readUnsignedByte();
            } else if (opcode == 4) {
                this.weaponSlot = buffer.readUnsignedByte();
            } else if (opcode == 5) {
                this.hiddenAnimationShieldSlots = new int[buffer.readUnsignedByte()];
                for (int i = 0; i < this.hiddenAnimationShieldSlots.length; i++) {
                    this.hiddenAnimationShieldSlots[i] = buffer.readUnsignedByte();
                }
            } else if (opcode == 6) {
                this.hiddenAnimationWeaponSlots = new int[buffer.readUnsignedByte()];
                for (int i = 0; i < this.hiddenAnimationWeaponSlots.length; i++) {
                    this.hiddenAnimationWeaponSlots[i] = buffer.readUnsignedByte();
                }
            }
        }
    }
}
