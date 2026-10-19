package com.jagex.game.runetek5.config.skybox

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.Identifiable

class SkyboxType {

    @JvmField var materialId: Int = -1
    @JvmField var defaultSunIndex: Int = -1
    @JvmField var backgroundMode: BackgroundMode
    @JvmField var archiveId: Int
    @JvmField var sunDefinitionIds: IntArray? = null

    init {
        this.backgroundMode = BackgroundMode.SINGLE_COLOR
        this.archiveId = -1
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

    fun decode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> this.materialId = buffer.readUShort()
            2 -> {
                this.sunDefinitionIds = IntArray(buffer.readUByte())
                for (index in this.sunDefinitionIds!!.indices) {
                    this.sunDefinitionIds!![index] = buffer.readUShort()
                }
            }
            3 -> this.defaultSunIndex = buffer.readUByte()
            4 -> this.backgroundMode = Identifiable.identify(BackgroundMode.values(), buffer.readUByte()) as BackgroundMode
            5 -> this.archiveId = buffer.readBigSmart()
        }
    }
}
