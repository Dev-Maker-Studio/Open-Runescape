package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet

abstract class CutsceneAction protected constructor(buffer: Packet) {
    @JvmField
    val lengthInCycles: Int = buffer.readUShort()

    abstract fun perform()

    open fun method1593() {
    }

    open fun isFullyCached(): Boolean {
        return true
    }
}
