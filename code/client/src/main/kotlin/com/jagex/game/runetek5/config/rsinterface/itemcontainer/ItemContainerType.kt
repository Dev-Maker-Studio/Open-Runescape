package com.jagex.game.runetek5.config.rsinterface.itemcontainer

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.CacheableNode

class ItemContainerType : CacheableNode() {

    @JvmField var defaultItemSize: Int = 0
    @JvmField var maxItemSize: Int = 0
    @JvmField var itemIds: IntArray? = null
    @JvmField var amounts: IntArray? = null

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
        if (opcode == 2) {
            this.maxItemSize = buffer.readUShort()
        } else if (opcode == 4) {
            this.defaultItemSize = buffer.readUByte()
            this.itemIds = IntArray(this.defaultItemSize)
            this.amounts = IntArray(this.defaultItemSize)
            for (index in 0 until this.defaultItemSize) {
                this.itemIds!![index] = buffer.readUShort()
                this.amounts!![index] = buffer.readUShort()
            }
        }
    }
}
