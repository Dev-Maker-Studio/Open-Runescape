package com.rs.game.content.world.areas.tree_gnome_stronghold

import com.rs.game.World
import com.rs.game.content.quests.the_grand_tree.objects.blockGrandTreeDoors
import com.rs.game.content.quests.the_grand_tree.objects.blockStrongholdGate
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile
import kotlin.math.abs
import kotlin.math.max

private const val SPIRAL_STAIRS_SEARCH_RADIUS = 3

@ServerStartupEvent
fun mapTreeGnomeStronghold() {
    onObjectClick(Loc.gnome_area_imp_stairs_bottom_var1) { e ->
        val top = findNearby(Loc.gnome_area_imp_stairs_top_var1, e.obj.tile.transform(0, 0, 1)) ?: return@onObjectClick
        e.player.useStairs(when (top.rotation) {
            0 -> top.tile.transform(1, 0)
            1 -> top.tile.transform(0, -1)
            2 -> top.tile.transform(-1, 0)
            else -> top.tile.transform(0, 1)
        })
    }

    onObjectClick(Loc.gnome_area_imp_stairs_top_var1) { e ->
        val bottom = findNearby(Loc.gnome_area_imp_stairs_bottom_var1, e.obj.tile.transform(0, 0, -1)) ?: return@onObjectClick
        e.player.useStairs(when (bottom.rotation) {
            0 -> bottom.tile.transform(1, -1)
            1 -> bottom.tile.transform(-1, 0)
            2 -> bottom.tile.transform(0, 2)
            else -> bottom.tile.transform(2, 1)
        })
    }

    onObjectClick(Loc.gnome_area_imp_platform_ladder, Loc.gnome_area_imp_platform_ladder_top) { e ->
        e.player.useStairs(if (e.player.plane == 1) Tile.of(2485, 3463, 2) else Tile.of(2483, 3463, 1))
    }

    onObjectClick(Loc.gnome_imp_treedoorl, Loc.gnome_imp_treedoorr) { e ->
        if (blockGrandTreeDoors(e.player)) return@onObjectClick
        Doors.handleInPlaceDoubleDoor(e.player, e.obj)
        e.player.resetWalkSteps()
        e.player.addWalkSteps(e.obj.x, if (e.player.y <= 3491) e.player.y + 2 else e.player.y - 2, -1, false)
    }

    onObjectClick(Loc.eyeglo_brimstails_cave_entrance) { e -> e.player.useStairs(-1, Tile.of(2408, 9812, 0), 0, 1) }

    onObjectClick(Loc.eyeglo_dungeon_exit_right, Loc.eyeglo_dungeon_exit_left) { e -> e.player.useStairs(-1, Tile.of(2402, 3419, 0), 0, 1) }

    onObjectClick(Loc.gnome_area_imp_gate) { e ->
        if (blockStrongholdGate(e.player)) return@onObjectClick
        Doors.handleInPlaceSingleDoor(e.player, e.obj)
        e.player.resetWalkSteps()
        e.player.addWalkSteps(e.obj.x, if (e.player.y > e.obj.y) e.obj.y - 1 else e.obj.y + 3, -1, false)
    }
}

private fun findNearby(id: Int, center: Tile): GameObject? {
    for (dist in 0..SPIRAL_STAIRS_SEARCH_RADIUS)
        for (dx in -dist..dist)
            for (dy in -dist..dist) {
                if (max(abs(dx), abs(dy)) != dist) continue
                World.getObjectWithId(center.transform(dx, dy), id)?.let { return it }
            }
    return null
}
