package com.jagex.game.runetek5.config.varn

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

class VarnType {

    @JvmField
    var typeChar: Char = '\u0000'

    fun decode(buffer: Packet, opcode: Int) {
        if (opcode == 1) {
            typeChar = Utils.cp1252ToChar(buffer.readByte())
        }
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcde = buffer.readUByte()
            if (opcde == 0) {
                return
            }
            decode(buffer, opcde)
        }
    }
}
