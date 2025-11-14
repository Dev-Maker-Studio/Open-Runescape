package com.jagex.game.runetek5.config.varc

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

/**
 * @author Brainpower
 *
 * VARC = VarClient
 */
class VarcType {

    @JvmField
    var anInt4983: Int = 1

    @JvmField
    var aChar4984: Char = '\u0000'

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun decode(stream: Packet, opcode: Int) {
        if (opcode == 1) {
            aChar4984 = Utils.cp1252ToChar(stream.readByte())
        } else if (opcode == 2) {
            anInt4983 = 0
        }
    }
}
