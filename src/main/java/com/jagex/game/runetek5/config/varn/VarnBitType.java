package com.jagex.game.runetek5.config.varn;

import com.jagex.game.runetek5.buffer.Packet;

public class VarnBitType {

    public int varnId;
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
            this.varnId = buffer.readUShort();
            this.startBit = buffer.readUByte();
            this.endBit = buffer.readUByte();
        }
    }
}
