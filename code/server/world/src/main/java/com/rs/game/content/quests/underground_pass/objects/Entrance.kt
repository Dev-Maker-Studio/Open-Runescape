package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.underground_pass.STAGE_ENTERED_PASS
import com.rs.game.content.quests.underground_pass.advanceUndergroundPass
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val SURFACE_ENTRANCE_TILE = Tile.of(2433, 3313, 0)
private val PASS_ENTRANCE_TILE = Tile.of(2496, 9714, 0)
private val SURFACE_EXIT_TILE = Tile.of(2438, 3315, 0)

@ServerStartupEvent
fun mapUndergroundPassEntrance() {
    onObjectClick(Loc.upass_caveentrance2, Loc.loc_3214) { (player, obj) ->
        when (obj.id) {
            Loc.upass_caveentrance2 -> {
                if (obj.tile != SURFACE_ENTRANCE_TILE)
                    return@onObjectClick
                if (!player.isQuestStarted(Quest.UNDERGROUND_PASS)) {
                    player.sendMessage("You should speak to King Lathas before entering this dangerous cave.")
                    return@onObjectClick
                }
                player.advanceUndergroundPass(STAGE_ENTERED_PASS)
                player.tele(PASS_ENTRANCE_TILE)
            }
            Loc.loc_3214 -> player.tele(SURFACE_EXIT_TILE)
        }
    }
}
