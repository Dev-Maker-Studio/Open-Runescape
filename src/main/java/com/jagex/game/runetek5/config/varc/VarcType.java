package com.jagex.game.runetek5.config.varc;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower
 * 
 * VARC = VarClient
 *
 */
public class VarcType {

    public int anInt4983 = 1;
    public char aChar4984;

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.aChar4984 = Utils.cp1252ToChar(stream.readByte());
        } else if (opcode == 2) {
            this.anInt4983 = 0;
        }
    }
}
