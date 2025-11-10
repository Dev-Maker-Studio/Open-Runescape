package com.darkan.bot.utils

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer

class ContainerWrapper(var container: ItemContainer) {

    fun contains(name: String, amount: Int): Boolean {
        var amtOf = 0
        for (i in container.itemIds.indices) {
            if (container.itemIds[i] != -1) {
                if (TypeLists.OBJ.list(getItem(i)).name.lowercase().contains(name)) {
                    amtOf += container.amounts[i]
                }
            }
        }
        return amtOf >= amount
    }

    fun getSlotByItem(name: String): Int {
        for (i in container.itemIds.indices) {
            if (container.itemIds[i] != -1) {
                if (TypeLists.OBJ.list(getItem(i)).name.lowercase().contains(name)) {
                    return i
                }
            }
        }
        return -1
    }

    fun getSlotByItemOfGreaterAmount(name: String, minAmount: Int): Int {
        for (i in container.itemIds.indices) {
            if (container.itemIds[i] != -1) {
                if (TypeLists.OBJ.list(getItem(i)).name.lowercase().contains(name) && container.amounts[i] > minAmount) {
                    return i
                }
            }
        }
        return -1
    }

    fun getItem(slot: Int): Int {
        if (slot < 0)
            return -1
        return container.itemIds[slot]
    }

    fun getAmount(slot: Int): Int {
        return container.amounts[slot]
    }

    fun contains(itemId: Int, amount: Int): Boolean {
        var amtOf = 0
        for (i in container.itemIds.indices) {
            if (container.itemIds[i] != -1) {
                if (getItem(i) == itemId) {
                    amtOf += container.amounts[i]
                }
            }
        }
        return amtOf >= amount
    }

    fun getItemId(name: String): Int {
        for (i in container.itemIds.indices) {
            if (container.itemIds[i] != -1) {
                val defs: ItemType = TypeLists.OBJ.list(getItem(i))
                if (!defs.name.equals(name, ignoreCase = true)) {
                    return container.itemIds[i]
                }
            }
        }
        return -1
    }

    fun getSlotByItem(itemId: Int): Int {
        for (i in container.itemIds.indices) {
            if (getItem(i) == itemId)
                return i
        }
        return -1
    }

    fun numberOf(itemId: Int): Int {
        var amount = 0
        for (i in container.itemIds.indices) {
            if (getItem(i) == itemId)
                amount += getAmount(i)
        }
        return amount
    }

    fun freeSlots(): Int {
        var j = 0
        for (i in container.itemIds.indices) {
            if (getItem(i) == -1) {
                j++
            }
        }
        return j
    }

    fun numberOf(string: String): Int {
        var amount = 0
        for (i in container.itemIds.indices) {
            if (TypeLists.OBJ.list(getItem(i)).name.lowercase().contains(string))
                amount += getAmount(i)
        }
        return amount
    }
}
