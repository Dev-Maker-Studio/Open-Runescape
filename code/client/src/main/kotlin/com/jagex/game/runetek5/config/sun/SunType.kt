package com.jagex.game.runetek5.config.sun

import com.jagex.game.runetek5.buffer.Packet

class SunType {

    @JvmField var positionOffsetStep: Int = 8
    @JvmField var invertPosition: Boolean = false
    @JvmField var positionX: Int = 0
    @JvmField var positionY: Int = 0
    @JvmField var positionZ: Int = 0
    @JvmField var actionType: Int = 0
    @JvmField var materialId: Int = 0
    @JvmField var displayColor: Int = 16777215
    @JvmField var rotationX: Int = 0
    @JvmField var rotationY: Int = 0
    @JvmField var rotationZ: Int = 0

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> this.positionOffsetStep = stream.readUShort()
            2 -> this.invertPosition = true
            3 -> {
                this.positionX = stream.readShort()
                this.positionY = stream.readShort()
                this.positionZ = stream.readShort()
            }
            4 -> this.actionType = stream.readUByte()
            5 -> this.materialId = stream.readBigSmart()
            6 -> this.displayColor = stream.readUMedium()
            7 -> {
                this.rotationX = stream.readShort()
                this.rotationY = stream.readShort()
                this.rotationZ = stream.readShort()
            }
        }
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }
}
