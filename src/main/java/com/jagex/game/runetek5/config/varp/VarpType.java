package com.jagex.game.runetek5.config.varp;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower - 25/03/2023
 * 
 * (Old varp format)
 * VARP = VarPlayer
 *
 */
public class VarpType {

    public char aChar4328;
    public int anInt4327;

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
            this.aChar4328 = Utils.cp1252ToChar(buffer.readByte());
        } else if (opcode == 5) {
            this.anInt4327 = buffer.readUnsignedShort();
        }
    }
}
