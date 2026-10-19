package com.rs.game.content.world

import com.rs.game.World.data
import com.rs.game.content.skills.cooking.CowMilkingAction
import com.rs.game.model.entity.npc.NPC
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapCowPlugins() {
    onItemOnNpc("Cow") { (player) -> player.sendMessage("The cow doesn't want that.") }

    onNpcDeath("Cow") { data.attribs.incI("cowTrackerKills") }

    onObjectClick(31297) { (player) ->
        player.sendMessage("So far, ${data.attribs.getI("cowTrackerKills")} cows have been killed by adventurers.")
    }

    onObjectClick(8689) { (player) -> player.actionManager.setAction(CowMilkingAction()) }

    instantiateNpc("Cow") { id, tile -> Cow(id, tile) }
}

class Cow(id: Int, tile: Tile) : NPC(id, tile) {
    override fun processNPC() {
        if (Utils.random(100) == 0) forceTalk("Moo")
        super.processNPC()
    }
}