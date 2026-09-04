package com.jagex.game.runetek5.config.clanvar

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

class ClanVarType {
    @JvmField var typeChar: Char = 0.toChar()
    @JvmField var baseVar: Int = 0
    @JvmField var startBit: Int = 0
    @JvmField var endBit: Int = 0

    fun decodeOpcode(buffer: Packet, opcode: Int) {
        if (opcode == 1) {
            typeChar = Utils.cp1252ToChar(buffer.readByte())
        } else if (opcode == 3) {
            baseVar = buffer.readUShort()
            startBit = buffer.readUByte()
            endBit = buffer.readUByte()
        }
    }

    fun decodeType(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decodeOpcode(buffer, opcode)
        }
    }
}
