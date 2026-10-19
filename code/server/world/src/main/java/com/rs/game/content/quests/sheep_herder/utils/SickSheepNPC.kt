package com.rs.game.content.quests.sheep_herder.utils

import org.darkan.core.map.pathfinder.Direction
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile

class SickSheepNPC(id: Int, tile: Tile, var officialRespawnTile: Tile, dir: Direction) : NPC(id, tile) {

    init {
        faceDir(dir)
        respawnTile = officialRespawnTile
    }

    override fun faceEntityTile(target: Entity?) {
        // Stop sheep facing player
    }

    fun convertToOwnedNPC(player: Player): SickSheepOwnedNPC {
        return SickSheepOwnedNPC(player, this.id, this.tile, this.officialRespawnTile, false).apply {
            faceDir(this@SickSheepNPC.direction)
        }.also { this.finish() }
    }
}
