package com.jagex.game.runetek5.config.light;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * < Mostly refactored >
 * @author Brainpower - 30/05/2020
 *
 */
public class LightType {

    public int effect;
    public int surrounding;
    public int ticker = 2048;
    public int duration = 2048;

    void decode(Packet buffer) {
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
            this.effect = buffer.readUnsignedByte();
        } else if (opcode == 2) {
            this.duration = buffer.readUnsignedShort();
        } else if (opcode == 3) {
            this.ticker = buffer.readUnsignedShort();
        } else if (opcode == 4) {
            this.surrounding = buffer.readShort();
        }
    }
}
