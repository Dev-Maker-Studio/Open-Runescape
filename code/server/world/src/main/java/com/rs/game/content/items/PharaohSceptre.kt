package com.rs.game.content.items

import com.rs.game.content.skills.magic.Magic
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

private val DESTINATIONS = mapOf(
    "Jalsavrah" to Tile.of(1968, 4428, 2),
    "Jaleustrophos" to Tile.of(3340, 2828, 0),
    "Jaldraocht" to Tile.of(3232, 2901, 0),
)

@ServerStartupEvent
fun mapPharaohSceptre() {
    onItemClick(
        Obj.ntk_jewelled_sceptre_3, Obj.ntk_jewelled_sceptre_2, Obj.ntk_jewelled_sceptre_1, Obj.ntk_jewelled_sceptre_0,
        options = arrayOf("Teleport", *DESTINATIONS.keys.toTypedArray()),
    ) { (player, item, option, equipped) ->
        if (item.id == Obj.ntk_jewelled_sceptre_0) {
            player.sendMessage("There are no charges remaining.")
            return@onItemClick
        }
        val destination = DESTINATIONS[option]
        if (destination != null) {
            teleport(player, item, destination, equipped)
            return@onItemClick
        }
        player.sendOptionDialogue("Where would you like to go?") { ops ->
            DESTINATIONS.forEach { (name, tile) -> ops.add(name) { teleport(player, item, tile, equipped) } }
            ops.add("Nowhere.")
        }
    }
}

private fun teleport(player: Player, sceptre: Item, destination: Tile, equipped: Boolean) {
    if (!Magic.sendTeleportSpell(player, Seq.atar_monastery_teleport, Seq.atar_monastery_teleport_arrive, Spotanim.atar_saradomin_teleport_spotanim, Spotanim.atar_saradomin_teleport_arrive_spotanim, 0, 0.0, destination, 3, true, TeleType.ITEM, null)) return
    sceptre.id += 2
    if (equipped) {
        player.equipment.refresh(Equipment.WEAPON)
        player.appearance.generateAppearanceData()
    } else player.inventory.refresh()
}
