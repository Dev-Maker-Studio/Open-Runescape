package com.jagex.game.runetek5.config.varn;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower - 25/03/2023
 * 
 * VARN = VarNPC
 *
 */
public class VarnType {

    public char character;

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.character = Utils.cp1252ToChar(buffer.readByte());
        }
    }

    void decode(Packet buffer) {
        while (true) {
            int opcde = buffer.readUnsignedByte();
            if (opcde == 0) {
                return;
            }
            decode(buffer, opcde);
        }
    }

}
