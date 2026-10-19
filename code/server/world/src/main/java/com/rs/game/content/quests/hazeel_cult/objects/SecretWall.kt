package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.STAGE_FIND_SCROLL
import com.rs.game.content.quests.hazeel_cult.isCultSide
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val WALL_SOUTH_TILE = Tile.of(2566, 3274, 1)
private val WALL_NORTH_TILE = Tile.of(2566, 3275, 1)

@ServerStartupEvent
fun mapHazeelSecretWall() {
    onObjectClick(Loc.carnilleanbookcase) { (player) ->
        if (!player.isCultSide() || player.questManager.getStage(Quest.HAZEEL_CULT) < STAGE_FIND_SCROLL) {
            player.sendMessage("You knock on the wall. It sounds solid.")
            return@onObjectClick
        }
        player.sendMessage("You find a secret passageway.")
        val destination = if (player.y <= WALL_SOUTH_TILE.y) WALL_NORTH_TILE else WALL_SOUTH_TILE
        player.passThrough(destination)
    }
}
