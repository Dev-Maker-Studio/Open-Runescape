package com.rs.game.content.world.areas.dungeons

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private val TEARS_TUNNEL_MOUTH = Tile.of(3219, 9532, 2)
private val SWAMP_TUNNEL_MOUTH = Tile.of(3226, 9542, 0)

@ServerStartupEvent
fun mapLumbridgeSwampDungeon() {
    onObjectClick(Loc.di2_goblin_cave_wall_cave_down_entrance) { (player) ->
        player.tele(Tile.of(3219, 9532, 2))
        player.musicsManager.unlockMusic(Midi.midi_397)
        player.musicsManager.playSpecificAmbientSong(Midi.midi_397, true)
    }
    onObjectClick(Loc.tog_cave_up, tiles = arrayOf(Tile.of(3218, 9533, 2)), checkDistance = false) { (player) ->
        if (player.isLocked || player.plane != TEARS_TUNNEL_MOUTH.plane) return@onObjectClick
        player.stopAll()
        if (player.tile.within(TEARS_TUNNEL_MOUTH, 2)) player.leaveTearsCavern()
        else player.setRouteEvent(RouteEvent(TEARS_TUNNEL_MOUTH, Runnable { player.leaveTearsCavern() }))
    }
    onObjectClick(Loc.swamp_cave_steppingstone_a, Loc.swamp_cave_steppingstone_b, checkDistance = false) { (player, obj) ->
        if (player.isLocked || player.plane != obj.plane || player.getLongestDelta(obj.tile) > 3) return@onObjectClick
        val destination = if (obj.id == Loc.swamp_cave_steppingstone_a)
            Tile.of(if (player.x < 3206) 3208 else 3204, 9572, 0)
        else Tile.of(3221, if (player.y < 9555) 9556 else 9553, 0)
        val running = player.run
        player.stopAll()
        player.lock(7)
        player.run = true
        player.setNextFaceTile(obj.tile)
        player.schedule {
            try {
                player.forceMove(destination, Seq.seq_1995, 0, 120, false)
                player.lock(6)
                wait(4)
                player.anim(Seq.human_jump_hurdle)
                wait(1)
            } finally { player.run = running; player.unlock() }
        }
    }
}

private fun Player.leaveTearsCavern() {
    if (isLocked || !tile.within(TEARS_TUNNEL_MOUTH, 2)) return
    useStairs(-1, SWAMP_TUNNEL_MOUTH)
    schedule {
        wait(3)
        if (!hasFinished() && tile.within(SWAMP_TUNNEL_MOUTH, 2))
            controllerManager.startController(UndergroundDungeonController(false, true))
    }
}
