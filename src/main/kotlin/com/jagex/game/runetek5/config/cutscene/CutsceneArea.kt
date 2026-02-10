package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

class CutsceneArea internal constructor(buffer: Packet) : Node<CutsceneArea>() {
    @JvmField val plane: Int
    @JvmField val regionX: Int
    @JvmField val regionY: Int
    @JvmField val width: Int
    @JvmField val length: Int
    @JvmField val cutscenePlane: Int
    @JvmField val chunkBaseX: Int
    @JvmField val chunkBaseY: Int
    @JvmField val rotation: Int

    init {
        val position = buffer.readInt()
        this.plane = position ushr 28
        this.regionX = position ushr 14 and 0x3fff
        this.regionY = position and 0x3fff
        this.width = buffer.readUByte()
        this.length = buffer.readUByte()
        this.cutscenePlane = buffer.readUByte()
        this.chunkBaseX = buffer.readUByte()
        this.chunkBaseY = buffer.readUByte()
        this.rotation = buffer.readUByte()
    }
}
