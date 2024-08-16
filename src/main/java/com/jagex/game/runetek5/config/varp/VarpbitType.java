package com.jagex.game.runetek5.config.varp;

import com.jagex.game.runetek5.buffer.Packet;

public class VarpbitType {

    public int configIndex;
    public int startBit;
    public int endBit;

    public void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.configIndex = buffer.readUnsignedShort();
            this.startBit = buffer.readUnsignedByte();
            this.endBit = buffer.readUnsignedByte();
        }
    }
}
