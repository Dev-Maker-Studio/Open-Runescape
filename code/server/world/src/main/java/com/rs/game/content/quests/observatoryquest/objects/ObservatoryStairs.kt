package com.rs.game.content.quests.observatoryquest.objects

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapObservatoryStairs() {
    onObjectClick(Loc.qip_obs_stairs1, tiles = arrayOf(Tile.of(2444, 3159, 0))) { (player) ->
        player.useStairs(-1, Tile.of(2443, 3158, 1), 0, 1)
    }
    onObjectClick(Loc.qip_obs_stairs2_down, tiles = arrayOf(Tile.of(2443, 3159, 1))) { (player) ->
        player.useStairs(-1, Tile.of(2444, 3162, 0), 0, 1)
    }

    onObjectClick(Loc.qip_obs_vstairs2, tiles = arrayOf(Tile.of(2458, 3186, 0))) { (player) ->
        player.tele(Tile.of(2355, 9394, 0))
    }

    onObjectClick(Loc.qip_obs_stairs1_dungeon, tiles = arrayOf(Tile.of(2355, 9395, 0))) { (player) ->
        player.tele(Tile.of(2458, 3185, 0))
    }

    onObjectClick(Loc.qip_obs_stairs_to_dungeon2, tiles = arrayOf(Tile.of(2438, 3164, 0))) { (player) ->
        player.tele(Tile.of(2335, 9350, 0))
    }

    onObjectClick(Loc.qip_obs_stairs1_dungeon, tiles = arrayOf(Tile.of(2335, 9351, 0))) { (player) ->
        player.tele(Tile.of(2439, 3164, 0))
    }
}
