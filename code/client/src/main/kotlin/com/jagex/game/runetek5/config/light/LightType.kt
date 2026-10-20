package com.jagex.game.runetek5.config.light

import com.jagex.game.runetek5.buffer.Packet

class LightType {

    @JvmField var effect: Int = 0
    @JvmField var surrounding: Int = 0
    @JvmField var ticker: Int = 2048
    @JvmField var duration: Int = 2048

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
            1 -> this.effect = buffer.readUByte()
            2 -> this.duration = buffer.readUShort()
            3 -> this.ticker = buffer.readUShort()
            4 -> this.surrounding = buffer.readShort()
        }
    }
}
