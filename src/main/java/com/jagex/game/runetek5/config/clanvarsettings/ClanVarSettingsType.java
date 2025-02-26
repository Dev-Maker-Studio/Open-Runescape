package com.jagex.game.runetek5.config.clanvarsettings;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

public class ClanVarSettingsType {

    public char aChar7096;
    public int baseVar;
    public int startBit;
    public int endBit;

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.aChar7096 = Utils.cp1252ToChar(buffer.readByte());
        } else if (opcode == 2) {
            this.baseVar = buffer.readUShort();
            this.startBit = buffer.readUByte();
            this.endBit = buffer.readUByte();
        }
    }
}
