package com.jagex.game.runetek5.config.varc

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

class VarcType {

    @JvmField
    var persistenceType: Int = 1

    @JvmField
    var typeChar: Char = '\u0000'

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
            typeChar = Utils.cp1252ToChar(stream.readByte())
        } else if (opcode == 2) {
            persistenceType = 0
        }
    }
}
