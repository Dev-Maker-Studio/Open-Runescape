package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import org.darkan.core.model.ObjectShape

private data class CellarWallPart(val id: Int, val tile: Tile)

private val CELLAR_WALL_PARTS = listOf(
    CellarWallPart(Loc.carni_hole_wall, Tile.of(2575, 9668, 0)),
    CellarWallPart(Loc.carni_hole_wall_left, Tile.of(2575, 9669, 0)),
    CellarWallPart(Loc.carni_hole_wall_right, Tile.of(2575, 9667, 0))
)

@ServerStartupEvent
fun mapCarnilleanCellarWall() {
    onChunkEnter { (entity) ->
        val player = entity as? Player ?: return@onChunkEnter
        if (CELLAR_WALL_PARTS.none { player.withinDistance(it.tile, 16) }) return@onChunkEnter
        player.schedule {
            // a teleport loads the region after EnterChunkEvent
            wait(1)
            refreshCellarWall(player)
        }
    }
}

internal fun refreshCellarWall(player: Player) {
    if (player.questManager.getStage(Quest.CARNILLEAN_RISING) > 0) return
    if (CELLAR_WALL_PARTS.none { player.withinDistance(it.tile, 16) }) return
    CELLAR_WALL_PARTS.forEach {
        player.packets.sendAddObject(GameObject(it.id, ObjectShape.WALL_STRAIGHT, 0, it.tile))
    }
}
