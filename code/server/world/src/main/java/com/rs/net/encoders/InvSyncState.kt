package com.rs.net.encoders

import org.darkan.core.model.Item
import org.darkan.core.net.prot.dto.InvSlotUpdate
import org.darkan.core.net.prot.dto.InvTooltip

class InvSyncState {
    private class Snapshot(size: Int) {
        var ids = IntArray(size) { -1 }
        var amounts = IntArray(size)
        var tooltips = arrayOfNulls<String>(size)

        fun ensure(size: Int) {
            if (ids.size >= size) return
            val old = ids.size
            ids = ids.copyOf(size).also { it.fill(-1, old) }
            amounts = amounts.copyOf(size)
            tooltips = tooltips.copyOf(size)
        }
    }

    private val sent = HashMap<Long, Snapshot>()

    fun hasSent(key: Int, negativeKey: Boolean) = sent.containsKey(id(key, negativeKey))

    fun full(key: Int, negativeKey: Boolean, items: Array<Item?>, tooltips: Array<String?>) {
        sent[id(key, negativeKey)] = Snapshot(items.size).also { snapshot ->
            items.forEachIndexed { slot, item -> snapshot.record(slot, item, tooltips[slot]) }
        }
    }

    fun update(key: Int, negativeKey: Boolean, slot: Int, item: Item?, tooltip: String?): InvSlotUpdate {
        val snapshot = snapshot(key, negativeKey, slot)
        val previous = snapshot.tooltips[slot]
        snapshot.record(slot, item, tooltip)
        val mode = when (tooltip) {
            null -> InvTooltip.Clear
            previous -> InvTooltip.Keep
            else -> InvTooltip.Set(tooltip)
        }
        return InvSlotUpdate(slot, item?.id ?: -1, item?.amount ?: 0, mode)
    }

    fun differs(key: Int, negativeKey: Boolean, slot: Int, item: Item?, tooltip: String?): Boolean {
        val snapshot = sent[id(key, negativeKey)] ?: return true
        if (slot >= snapshot.ids.size) return item != null
        return snapshot.ids[slot] != (item?.id ?: -1) || snapshot.amounts[slot] != (item?.amount ?: 0) || snapshot.tooltips[slot] != tooltip
    }

    private fun snapshot(key: Int, negativeKey: Boolean, slot: Int) =
        sent.getOrPut(id(key, negativeKey)) { Snapshot(slot + 1) }.also { it.ensure(slot + 1) }

    private fun Snapshot.record(slot: Int, item: Item?, tooltip: String?) {
        ids[slot] = item?.id ?: -1
        amounts[slot] = item?.amount ?: 0
        tooltips[slot] = tooltip
    }

    private fun id(key: Int, negativeKey: Boolean) = key.toLong() or if (negativeKey) 1L shl 32 else 0L
}
