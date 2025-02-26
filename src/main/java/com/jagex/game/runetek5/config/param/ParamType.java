package com.jagex.game.runetek5.config.param;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

public class ParamType {

    public int defaultInt;
    public boolean autoDisable = true;
    public String typeName;
    public char typeChar;

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
            this.typeChar = Utils.cp1252ToChar(buffer.readByte());
        } else if (opcode == 2) {
            this.defaultInt = buffer.readInt();
        } else if (opcode == 4) {
            this.autoDisable = false;
        } else if (opcode == 5) {
            this.typeName = buffer.readString();
        }
    }

    public boolean isString() {
        return this.typeChar == 's';
    }
}
