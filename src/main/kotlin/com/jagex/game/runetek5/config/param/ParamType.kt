package com.jagex.game.runetek5.config.param

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.UnknownOpcodes
import com.jagex.game.runetek5.utils.Utils

class ParamType {
    @JvmField var defaultInt: Int = 0
    @JvmField var autoDisable: Boolean = true
    @JvmField var typeName: String? = null
    @JvmField var typeChar: Char = 0.toChar()

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
            1 -> this.typeChar = Utils.cp1252ToChar(buffer.readByte())
            2 -> this.defaultInt = buffer.readInt()
            4 -> this.autoDisable = false
            5 -> this.typeName = buffer.readString()
            else -> UnknownOpcodes.record(opcode)
        }
    }

    fun isString(): Boolean {
        return this.typeChar == 's'
    }
}
