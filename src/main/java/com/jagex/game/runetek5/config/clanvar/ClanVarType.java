package com.jagex.game.runetek5.config.clanvar;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

public class ClanVarType {

    public char aChar4832;
    public int baseVar;
    public int startBit;
    public int endBit;

    public void decodeOpcode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.aChar4832 = Utils.cp1252ToChar(buffer.readByte());
        } else if (opcode == 3) {
            this.baseVar = buffer.readUnsignedShort();
            this.startBit = buffer.readUnsignedByte();
            this.endBit = buffer.readUnsignedByte();
        }
    }

    void decodeType(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }

            decodeOpcode(buffer, opcode);
        }
    }

}
