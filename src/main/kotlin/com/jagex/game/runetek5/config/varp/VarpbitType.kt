package com.jagex.game.runetek5.config.varp

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.UnknownOpcodes

class VarpbitType {

    @JvmField
    var configIndex: Int = 0

    @JvmField
    var startBit: Int = 0

    @JvmField
    var endBit: Int = 0

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
            configIndex = buffer.readUShort()
            startBit = buffer.readUByte()
            endBit = buffer.readUByte()
        } else {
            UnknownOpcodes.record(opcode)
        }
    }
}
