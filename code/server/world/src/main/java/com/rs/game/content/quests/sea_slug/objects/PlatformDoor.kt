package com.rs.game.content.quests.sea_slug.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.world.doors.Doors
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val SOUTH_LOWER_DOOR = Tile.of(2768, 3276, 0)
private val NORTH_LOWER_DOOR = Tile.of(2768, 3288, 0)
private val KENNITH_ROOM_DOOR = Tile.of(2767, 3285, 1)

@ServerStartupEvent
fun mapSeaSlugPlatformDoors() {
    onObjectClick(Loc.slug2_village_poordoor, tiles = arrayOf(SOUTH_LOWER_DOOR, NORTH_LOWER_DOOR, KENNITH_ROOM_DOOR) ) { (player, obj) ->
        Doors.handleDoor(player, obj)
    }
}
