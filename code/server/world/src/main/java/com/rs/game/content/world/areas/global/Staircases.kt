package com.rs.game.content.world.areas.global

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapStaircases() {
    onObjectClick(Loc.fai_varrock_stairs_taller, Loc.fai_varrock_woodenstairs_castle, Loc.fai_falador_castle_stairs) { (player, obj) ->
        player.useStairs(-1, player.tile.ahead(obj.rotation, 4, 1), 1, 1)
    }
    onObjectClick(Loc.fai_varrock_stairs_top, Loc.fai_varrock_bank_stairs, Loc.fai_falador_castle_stairstop_gloffset) { (player, obj) ->
        val destination = if (obj.tile.isAt(3189, 3432)) player.transform(2, 6400, 0) else player.tile.ahead(obj.rotation, -4, -1)
        player.useStairs(-1, destination, 1, 1)
    }
    onObjectClick(Loc.fai_alkh_large_stairs_lower_click) { (player, obj) ->
        player.useStairs(-1, obj.tile.ahead(obj.rotation, 3, 1), 1, 1)
    }
    onObjectClick(Loc.fai_alkh_large_stairs_upper) { (player, obj) ->
        player.useStairs(-1, obj.tile.ahead(obj.rotation, -4, -1), 1, 1)
    }
    onObjectClick("Staircase", "Stairs") { (player, _, option) ->
        when (option.lowercase()) {
            "climb-up" -> if (player.plane < 3) player.useStairs(-1, player.transform(0, 0, 1), 0, 1)
            "climb-down" -> if (player.plane > 0) player.useStairs(-1, player.transform(0, 0, -1), 0, 1)
            "climb" -> if (player.plane in 1..2) player.promptUpDown("Go up the stairs.", player.transform(0, 0, 1), "Go down the stairs.", player.transform(0, 0, -1))
        }
    }
}

private fun Tile.ahead(rotation: Int, distance: Int, planeOffset: Int): Tile = when (rotation and 0x3) {
    0 -> transform(0, distance, planeOffset)
    1 -> transform(distance, 0, planeOffset)
    2 -> transform(0, -distance, planeOffset)
    else -> transform(-distance, 0, planeOffset)
}
