package com.jagex.game.runetek5.config.clanvarsettings

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

class ClanVarSettingsType {
    @JvmField var aChar7096: Char = 0.toChar()
    @JvmField var baseVar: Int = 0
    @JvmField var startBit: Int = 0
    @JvmField var endBit: Int = 0

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
        when (opcode) {
            1 -> this.aChar7096 = Utils.cp1252ToChar(buffer.readByte())
            2 -> {
                this.baseVar = buffer.readUShort()
                this.startBit = buffer.readUByte()
                this.endBit = buffer.readUByte()
            }
        }
    }
}
