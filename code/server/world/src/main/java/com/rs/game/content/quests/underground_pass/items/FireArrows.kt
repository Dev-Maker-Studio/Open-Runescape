package com.rs.game.content.quests.underground_pass.items

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnObject

private val KOFTIK_FIRE_TILE = Tile.of(2451, 9715, 0)

private val normalToUnlit = mapOf(
    Obj.bronze_arrow to Obj.unlitarrow,
    Obj.iron_arrow to Obj.iron_unlitarrow,
    Obj.steel_arrow to Obj.steel_unlitarrow,
    Obj.mithril_arrow to Obj.mithril_unlitarrow,
    Obj.adamant_arrow to Obj.adamant_unlitarrow,
    Obj.rune_arrow to Obj.rune_unlitarrow,
    Obj.dragon_arrow to Obj.dragon_unlitarrow,
)
private val unlitToLit = mapOf(
    Obj.unlitarrow to Obj.litarrow,
    Obj.iron_unlitarrow to Obj.iron_litarrow,
    Obj.steel_unlitarrow to Obj.steel_litarrow,
    Obj.mithril_unlitarrow to Obj.mithril_litarrow,
    Obj.adamant_unlitarrow to Obj.adamant_litarrow,
    Obj.rune_unlitarrow to Obj.rune_litarrow,
    Obj.dragon_unlitarrow to Obj.dragon_litarrow,
)
val litUndergroundPassArrows = intArrayOf(
    Obj.litarrow, Obj.iron_litarrow, Obj.steel_litarrow, Obj.mithril_litarrow,
    Obj.adamant_litarrow, Obj.rune_litarrow, Obj.dragon_litarrow
)

@ServerStartupEvent
fun mapUndergroundPassFireArrows() {
    val ingredients = intArrayOf(
        Obj.damp_cloth,
        Obj.tinderbox,
        *normalToUnlit.keys.toIntArray(),
        *unlitToLit.keys.toIntArray()
    )
    onItemOnItem(ingredients, ingredients) { event ->
        val cloth = event.itemMatching(Obj.damp_cloth)
        val normalArrow = event.otherThan(cloth)
        val unlitId = normalArrow?.let { normalToUnlit[it.id] }
        if (cloth != null && normalArrow != null && unlitId != null) {
            event.player.inventory.deleteItem(cloth.id, 1)
            event.player.inventory.deleteItem(normalArrow.id, 1)
            event.player.inventory.addItem(unlitId, 1)
            event.player.sendMessage("You wrap the damp cloth around an arrow.")
            return@onItemOnItem
        }

        val tinderbox = event.itemMatching(Obj.tinderbox)
        val unlitArrow = event.otherThan(tinderbox)
        val litId = unlitArrow?.let { unlitToLit[it.id] }
        if (tinderbox != null && unlitArrow != null && litId != null) {
            event.player.inventory.deleteItem(unlitArrow.id, 1)
            event.player.inventory.addItem(litId, 1)
            event.player.sendMessage("You light the cloth wrapped around the arrow.")
        }
    }

    onItemOnObject(
        arrayOf(Loc.loc_32099),
        unlitToLit.keys.map { it as Any }.toTypedArray(),
        tiles = arrayOf(KOFTIK_FIRE_TILE)
    ) { (player, _, arrow) ->
        val litId = unlitToLit[arrow.id] ?: return@onItemOnObject
        player.inventory.deleteItem(arrow.id, 1)
        player.inventory.addItem(litId, 1)
        player.sendMessage("You light the cloth wrapped around the arrow.")
    }
}

private fun com.rs.plugin.events.ItemOnItemEvent.itemMatching(id: Int): Item? = when (id) {
    item1.id -> item1
    item2.id -> item2
    else -> null
}

private fun com.rs.plugin.events.ItemOnItemEvent.otherThan(item: Item?): Item? = when (item) {
    null -> null
    item1 -> item2
    else -> item1
}
