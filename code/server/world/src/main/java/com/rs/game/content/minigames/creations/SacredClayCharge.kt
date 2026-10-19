package com.rs.game.content.minigames.creations

import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import org.darkan.core.model.Item

const val SACRED_CLAY_CHARGE_KEY = "sacredClayCharge"
const val FULL_SACRED_CLAY_CHARGE = 1_000_000

val Item.sacredClayCharge: Int
    get() = getMetaDataI(SACRED_CLAY_CHARGE_KEY, FULL_SACRED_CLAY_CHARGE)

fun Item.setSacredClayCharge(charge: Int) {
    addMetaData(SACRED_CLAY_CHARGE_KEY, charge.coerceAtLeast(0))
}

private val TOOLTIP_TAGS = Regex("<[^>]+>")

fun Player.sendSacredClayDetails(item: Item, lines: List<String>) {
    sendMessage("Your ${item.name.lowercase()}:")
    lines.forEach { sendMessage(" - ${it.replace(TOOLTIP_TAGS, "")}") }
}

fun sacredClayItem(id: Int): Item = Item(id).addMetaData(SACRED_CLAY_CHARGE_KEY, FULL_SACRED_CLAY_CHARGE)

class HeldItem(private val player: Player, val item: Item, private val wornSlot: Int?) {
    val worn get() = wornSlot != null

    fun changeId(id: Int) {
        item.changeId(id)
        refreshWorn()
    }

    fun refreshWorn() {
        wornSlot ?: return
        player.equipment.refresh(wornSlot)
        player.appearance.generateAppearanceData()
    }

    fun remove() {
        if (wornSlot != null) {
            player.equipment.deleteSlot(wornSlot)
            player.appearance.generateAppearanceData()
        } else {
            val slot = player.inventory.items.array().indexOfFirst { it === item }
            if (slot != -1) player.inventory.deleteItem(slot, item)
        }
    }

    fun replaceUnworn(id: Int) {
        if (wornSlot == null) {
            item.changeId(id)
            return
        }
        if (!player.equipment.deleteSlot(wornSlot)) return
        player.appearance.generateAppearanceData()
        item.changeId(id)
        player.inventory.addItemDrop(item)
    }
}

fun Player.heldItems(ids: Set<Int>): List<HeldItem> {
    val held = ArrayList<HeldItem>()
    inventory.items.array().forEach { if (it != null && it.id in ids) held += HeldItem(this, it, null) }
    for (slot in 0 until Equipment.SIZE) equipment.getItem(slot)?.let { if (it.id in ids) held += HeldItem(this, it, slot) }
    return held
}

fun Player.wornItems(ids: Set<Int>): List<HeldItem> = heldItems(ids).filter { it.worn }

fun Player.heldItem(item: Item): HeldItem? {
    if (inventory.items.array().any { it === item }) return HeldItem(this, item, null)
    val slot = (0 until Equipment.SIZE).firstOrNull { equipment.getItem(it) === item } ?: return null
    return HeldItem(this, item, slot)
}
