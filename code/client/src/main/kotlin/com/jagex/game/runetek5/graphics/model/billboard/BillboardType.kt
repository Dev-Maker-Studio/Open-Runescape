package com.jagex.game.runetek5.graphics.model.billboard

import com.jagex.game.runetek5.buffer.Packet

class BillboardType {

    @JvmField var materialId = -1

    @JvmField var size2d = 64

    @JvmField var size3d = 64

    @JvmField var shape = 2

    @JvmField var blendType = 1

    @JvmField var stationary = false

    @JvmField var hasUid = false

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
            1 -> {
                materialId = buffer.readUShort()
                if (materialId == 65535) {
                    materialId = -1
                }
            }
            2 -> {
                size2d = buffer.readUShort() + 1
                size3d = buffer.readUShort() + 1
            }
            3 -> buffer.readByte()
            4 -> shape = buffer.readUByte()
            5 -> blendType = buffer.readUByte()
            6 -> stationary = true
            7 -> hasUid = true
        }
    }
}
