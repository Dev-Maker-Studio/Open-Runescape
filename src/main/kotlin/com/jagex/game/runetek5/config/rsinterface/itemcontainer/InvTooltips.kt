package com.jagex.game.runetek5.config.rsinterface.itemcontainer

import com.jagex.game.runetek5.buffer.Packet

object InvTooltips {
    private const val FLAG_TOOLTIPS = 0x2
    private const val FLAG_SPARSE_TOOLTIPS = 0x4
    private const val MODE_BITS = 2
    private const val MODE_MASK = 0x3
    private const val MODE_CLEAR = 1
    private const val MODE_SET = 2

    @JvmStatic
    fun slot(slotHeader: Int, flags: Int): Int =
        if ((flags and FLAG_TOOLTIPS) != 0) slotHeader ushr MODE_BITS else slotHeader

    @JvmStatic
    fun readPartial(buffer: Packet, containerId: Int, slot: Int, slotHeader: Int, flags: Int, negativeKey: Boolean) {
        val mode = if ((flags and FLAG_TOOLTIPS) != 0) slotHeader and MODE_MASK else MODE_CLEAR
        when (mode) {
            MODE_CLEAR -> ItemContainer.setTooltipInContainer(containerId, slot, null, negativeKey)
            MODE_SET -> ItemContainer.setTooltipInContainer(containerId, slot, buffer.readString(), negativeKey)
        }
    }

    @JvmStatic
    fun readFull(buffer: Packet, containerId: Int, size: Int, flags: Int, negativeKey: Boolean) {
        if ((flags and FLAG_TOOLTIPS) != 0) {
            val mask = ByteArray((size + 7) shr 3) { buffer.readByte() }
            for (slot in 0..<size) {
                if ((mask[slot shr 3].toInt() shr (slot and 7) and 0x1) != 0) {
                    ItemContainer.setTooltipInContainer(containerId, slot, buffer.readString(), negativeKey)
                }
            }
        } else if ((flags and FLAG_SPARSE_TOOLTIPS) != 0) {
            repeat(buffer.readUSmart()) {
                val slot = buffer.readUSmart()
                ItemContainer.setTooltipInContainer(containerId, slot, buffer.readString(), negativeKey)
            }
        }
    }
}
