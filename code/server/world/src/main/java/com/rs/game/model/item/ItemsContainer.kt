package com.rs.game.model.item

import org.darkan.core.model.Item
import org.darkan.core.model.ItemSlotOwner
import java.util.BitSet

class ItemsContainer<T : Item?>(size: Int, private var alwaysStackable: Boolean) : ItemSlotOwner {
    private var data: Array<Item?> = arrayOfNulls(size)

    @Transient
    private var dirtySlots: BitSet? = null

    @Transient
    private var allDirty = false

    val size: Int get() = data.size

    val isDirty: Boolean get() = allDirty || dirtySlots?.isEmpty == false

    val isAllDirty: Boolean get() = allDirty

    override fun itemChanged(item: Item) {
        val slot = if (data.getOrNull(item.slot) === item) item.slot else data.indexOfFirst { it === item }
        if (slot == -1) {
            if (item.owner === this) item.owner = null
            return
        }
        item.slot = slot
        slotChanged(slot)
    }

    private fun slotChanged(slot: Int) {
        if (slot in data.indices) (dirtySlots ?: BitSet(data.size).also { dirtySlots = it }).set(slot)
    }

    fun markDirty(vararg slots: Int) {
        for (slot in slots) slotChanged(slot)
    }

    fun markAllDirty() {
        allDirty = true
    }

    fun dirtySlots(): IntArray = dirtySlots?.stream()?.toArray() ?: IntArray(0)

    fun clearDirty() {
        allDirty = false
        dirtySlots?.clear()
    }

    private fun place(slot: Int, item: Item?) {
        data[slot]?.let { if (it !== item && it.owner === this) it.owner = null }
        data[slot] = item
        item?.let {
            it.slot = slot
            it.owner = this
        }
        slotChanged(slot)
    }

    private fun clearSlot(slot: Int) {
        data[slot]?.let { if (it.owner === this) it.owner = null }
        data[slot] = null
        slotChanged(slot)
    }

    fun shift() {
        val items = data.filterNotNull()
        data = arrayOfNulls(data.size)
        items.forEachIndexed { index, item -> place(index, item) }
        markAllDirty()
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(slot: Int): T? = if (slot in data.indices) data[slot] as T? else null

    operator fun set(slot: Int, item: T?) {
        if (slot !in data.indices) return
        if (item == null) clearSlot(slot) else place(slot, item)
    }

    fun set2(slot: Int, item: Item?) {
        if (slot !in data.indices) return
        place(slot, item)
    }

    fun forceAdd(item: T): Boolean {
        val index = freeSlot()
        if (index == -1) return false
        place(index, item)
        return true
    }

    fun add(item: T): Boolean {
        item ?: return false
        if ((alwaysStackable || item.definitions.isStackable() || item.definitions.isNoted()) && item.getMetaData() == null) {
            for (i in data.indices) {
                val existing = data[i] ?: continue
                if (existing.id == item.id) {
                    if (existing.amount + item.amount <= 0) return false
                    place(i, Item(existing.id, existing.amount + item.amount, item.getMetaData()))
                    return true
                }
            }
        } else if (item.amount > 1) {
            if (freeSlots() < item.amount) return false
            repeat(item.amount) { place(freeSlot(), Item(item.id, 1)) }
            return true
        }
        val index = freeSlot()
        if (index == -1) return false
        place(index, item)
        return true
    }

    fun getThisItemSlot(itemId: Int): Int = data.indexOfFirst { it != null && it.id == itemId }

    fun freeSlots(): Int = data.count { it == null }

    fun remove(item: T): Int {
        item ?: return 0
        var removed = 0
        var toRemove = item.amount
        for (i in data.indices) {
            val existing = data[i] ?: continue
            if (existing.id != item.id) continue
            val amount = existing.amount
            if (amount > toRemove) {
                removed += toRemove
                place(i, Item(existing.id, amount - toRemove))
                return removed
            }
            removed += amount
            toRemove -= amount
            clearSlot(i)
        }
        return removed
    }

    fun removeAll(item: T) {
        item ?: return
        removeAll(item.id)
    }

    fun removeAll(item: Int) {
        for (i in data.indices) if (data[i]?.id == item) clearSlot(i)
    }

    fun containsOne(item: T): Boolean = item != null && data.any { it != null && it.id == item.id }

    fun contains(item: T): Boolean {
        item ?: return false
        return data.sumOf { if (it != null && it.id == item.id) it.amount else 0 } >= item.amount
    }

    fun freeSlot(): Int = data.indexOfFirst { it == null }

    fun clear() {
        for (i in data.indices) if (data[i] != null) clearSlot(i)
        markAllDirty()
    }

    val freeSlots: Int get() = freeSlots()

    val usedSlots: Int get() = data.count { it != null }

    fun getNumberOf(item: Item): Int {
        if (item.getMetaData() != null) return 1
        return data.sumOf { if (it != null && it.id == item.id && it.getMetaData() === item.getMetaData()) it.amount else 0 }
    }

    fun getNumberOf(item: Int): Int = data.sumOf { if (it != null && it.id == item) it.amount else 0 }

    fun array(): Array<Item?> = data

    val itemsCopy: Array<Item?> get() = data.copyOf()

    fun asList(): MutableList<Item> = data.filterNotNullTo(ArrayList())

    fun asItemContainer(): ItemsContainer<Item> = ItemsContainer<Item>(data.size, alwaysStackable).also { data.copyInto(it.data) }

    val freeSlot: Int get() = freeSlot()

    fun getThisItemSlot(item: T): Int {
        item ?: return freeSlot()
        val slot = getThisItemSlot(item.id)
        return if (slot != -1) slot else freeSlot()
    }

    fun lookup(id: Int): Item? = data.firstOrNull { it != null && it.id == id }

    fun lookupSlot(id: Int): Int = getThisItemSlot(id)

    fun reset() {
        data = arrayOfNulls(data.size)
        markAllDirty()
    }

    fun remove(preferredSlot: Int, item: Item): Int {
        var removed = 0
        var toRemove = item.amount
        val preferred = data[preferredSlot]
        if (preferred != null && preferred.id == item.id && preferred.getMetaData() === item.getMetaData()) {
            val amount = preferred.amount
            if (amount > toRemove) {
                set2(preferredSlot, Item(preferred.id, amount - toRemove, preferred.getMetaData()))
                return toRemove
            }
            removed += amount
            toRemove -= amount
            clearSlot(preferredSlot)
        }
        for (i in data.indices) {
            val existing = data[i] ?: continue
            if (existing.id != item.id || item.getMetaData() !== existing.getMetaData()) continue
            val amount = existing.amount
            if (amount > toRemove) {
                removed += toRemove
                set2(i, Item(existing.id, amount - toRemove, existing.getMetaData()))
                return removed
            }
            removed += amount
            toRemove -= amount
            clearSlot(i)
        }
        return removed
    }

    fun addAll(container: ItemsContainer<T>) {
        for (i in 0 until container.size) container[i]?.let { add(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun addAll(container: Array<out Item?>) {
        for (item in container) if (item != null) add(item as T)
    }

    fun hasSpaceFor(container: ItemsContainer<T>): Boolean {
        for (i in 0 until container.size) {
            val item = container[i] ?: continue
            if (!hasSpaceForItem(item)) return false
        }
        return true
    }

    private fun hasSpaceForItem(item: Item): Boolean {
        if (alwaysStackable || item.definitions.isStackable() || item.definitions.isNoted()) {
            if (data.any { it != null && it.id == item.id }) return true
        } else if (item.amount > 1) {
            return freeSlots() >= item.amount
        }
        return freeSlot() != -1
    }

    fun toArray(): Array<Item?> = data

    fun initSlots() {
        for (i in data.indices) data[i]?.let {
            it.slot = i
            it.owner = this
        }
    }

    fun addAll(list: List<T>) {
        for (item in list) add(item)
    }

    val isEmpty: Boolean get() = data.all { it == null }

    fun sortByItemId(): ItemsContainer<T> {
        data.sortWith(compareBy { it?.id ?: Int.MAX_VALUE })
        initSlots()
        markAllDirty()
        return this
    }

    val itemsNoNull: Array<Item> get() = data.filterNotNull().toTypedArray()
}
