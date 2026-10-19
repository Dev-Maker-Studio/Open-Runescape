package com.rs.game.content.world.areas.braindeath_island

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

private val UNDERWATER_ENTRANCE = Tile.of(1055, 5001, 0)
private val ISLAND_ENTRANCE = Tile.of(2126, 5147, 0)

@ServerStartupEvent
fun mapBraindeathIsland() {
    onObjectClick(Loc.loc_9161) { (player) ->
        if (!player.hasDivingEquipment()) {
            player.sendMessage("You need to be wearing a fishbowl helmet and diving apparatus to go underwater.")
            return@onObjectClick
        }
        player.tele(UNDERWATER_ENTRANCE)
        player.controllerManager.startController(BraindeathUnderwaterController())
    }

    onObjectClick(Loc.acs_dungeon_portal_entrance) { (player) ->
        player.controllerManager.forceStop()
        player.tele(ISLAND_ENTRANCE)
    }
}

private fun Player.hasDivingEquipment() =
    equipment.containsOneItem(Obj.hundred_pirate_diving_helmet) && equipment.containsOneItem(Obj.hundred_pirate_diving_backpack)
