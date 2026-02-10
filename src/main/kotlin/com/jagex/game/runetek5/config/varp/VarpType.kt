package com.jagex.game.runetek5.config.varp

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

/**
 * @author Brainpower - 25/03/2023
 *
 * (Old varp format)
 * VARP = VarPlayer
 */
class VarpType {
    @JvmField var aChar4328: Char = 0.toChar()
    @JvmField var clientCode: Int = 0

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun decode(buffer: Packet, opcode: Int) {
        if (opcode == 1) {
            aChar4328 = Utils.cp1252ToChar(buffer.readByte())
        } else if (opcode == 5) {
            clientCode = buffer.readUShort()
        }
    }
}
