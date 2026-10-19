package com.rs.game.content.quests.watchtower

import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapWatchtowerShamans() {
    instantiateNpc(*WATCHTOWER_SHAMAN_VARBITS.keys.toTypedArray<Any>()) { id, tile -> WatchtowerShaman(id, tile) }
}

class WatchtowerShaman(id: Int, tile: Tile) : NPC(id, tile) {
    override fun checkAggressivity(): Boolean = false

    override fun canBeAttackedBy(player: Player): Boolean = false

    override fun handlePreHit(hit: Hit) {
        hit.damage = 0
    }
}
