package com.rs.game.content.world

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Obj

private val EMPTY_LAMPS = mapOf(Obj.bullseye_lantern_empty to Obj.bullseye_lantern_unlit,
    Obj.tog_sapphire_lantern_empty to Obj.tog_sapphire_lantern_unlit,
    Obj.bullseye_lantern_unlit_lunar_quest to Obj.bullseye_lantern_lit_lunar_quest)
private val STILLS = arrayOf<Any>("Lamp oil still", "Lamp still", "Fractionation still")

@ServerStartupEvent
fun mapStills() {
    onObjectClick(*STILLS) { (player, obj) -> player.fillLamp("lampOil:${obj.tile.id}") }
    onItemOnObject(STILLS, arrayOf<Any>(Obj.swamp_tar, Obj.bullseye_lantern_nolens, *EMPTY_LAMPS.keys.toTypedArray())) { event ->
        val oil = "lampOil:${event.getObject().tile.id}"
        when (event.item.id) {
            Obj.swamp_tar -> event.player.distillOil(oil)
            Obj.bullseye_lantern_nolens -> event.player.sendMessage("You need to add a lens before you can use it.")
            else -> event.player.fillLamp(oil, event.item.id)
        }
    }
}

private fun Player.distillOil(oil: String) {
    if (getI(oil, 0) != 0) { sendMessage("The still already contains enough oil to fill a lantern."); return }
    if (!inventory.containsItem(Obj.swamp_tar, 1)) return
    inventory.deleteItem(Obj.swamp_tar, 1)
    set(oil, 1)
    sendMessage("You distil the swamp tar into lamp oil. Use an empty lantern on the still to fill it.")
}

private fun Player.fillLamp(oil: String, selected: Int? = null) {
    val empty = selected ?: EMPTY_LAMPS.keys.firstOrNull { inventory.containsItem(it, 1) }
    if (empty == null || !inventory.containsItem(empty, 1)) { sendMessage("You need an empty lantern to fill with lamp oil."); return }
    if (getI(oil, 0) == 0 && !inventory.containsItem(Obj.swamp_tar, 1)) { sendMessage("You need to use swamp tar on the still to make lamp oil."); return }
    val lamp = inventory.items.lookup(empty) ?: return
    val slot = inventory.items.getThisItemSlot(lamp)
    if (getI(oil, 0) != 0) set(oil, 0) else inventory.deleteItem(Obj.swamp_tar, 1)
    inventory.replaceItem(EMPTY_LAMPS.getValue(empty), 1, slot)
    sendMessage("You fill the lantern with lamp oil.")
}
