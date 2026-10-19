package com.rs.game.content.quests.gunnars_ground.instances.npcs

import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import world.gregs.voidps.type.Tile

class DororanSitting(id: Int, tile: Tile) : NPC(id, tile) {
    override fun faceEntityTile(target: Entity?) { }
}
