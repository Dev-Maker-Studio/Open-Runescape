package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet

class GrandExchangeSlot {
    @JvmField var itemId: Int = 0
    @JvmField var price: Int = 0
    @JvmField var amount: Int = 0
    @JvmField var currentAmount: Int = 0
    @JvmField var totalPrice: Int = 0
    @JvmField var progress: Byte = 0

    constructor()

    constructor(buffer: Packet) {
        this.progress = buffer.readByte()
        this.itemId = buffer.readUShort()
        this.price = buffer.readInt()
        this.amount = buffer.readInt()
        this.currentAmount = buffer.readInt()
        this.totalPrice = buffer.readInt()
    }

    fun method5908(): Int {
        return progress.toInt() and 0x7
    }

    fun method5909(): Int {
        return if ((progress.toInt() and 0x8) == 8) 1 else 0
    }
}
