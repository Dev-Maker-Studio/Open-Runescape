package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Midi
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val CAVE_ENTRANCE_TILE = Tile.of(2585, 3233, 0)
private val CAVE_EXIT_TILE = Tile.of(2570, 9683, 0)
private val OVERWORLD_TILE = Tile.of(2586, 3237, 0)
private val DUNGEON_TILE = Tile.of(2571, 9682, 0)

@ServerStartupEvent
fun mapHazeelCaveEntrance() {
    onObjectClick(Loc.hazeelcultcave, tiles = arrayOf(CAVE_ENTRANCE_TILE)) { (player) ->
        player.musicsManager.playSongAndUnlock(Midi.midi_478)
        player.useStairs(DUNGEON_TILE)
    }

    onObjectClick(Loc.hazeelcultstairs, tiles = arrayOf(CAVE_EXIT_TILE)) { (player) ->
        player.useStairs(OVERWORLD_TILE)
    }
}
