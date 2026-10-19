package com.rs.game.content.world.areas.ranging_guild

import com.rs.game.model.entity.npc.NPC
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import world.gregs.voidps.type.Tile

class RangingGuildTowerArcher(id: Int, tile: Tile, spawned: Boolean) : NPC(id, tile, spawned) {

    init {
        lureDelay = 0
        setForceAgressive(true)
    }

    override fun blocksOtherNpcs(): Boolean = false
}

@ServerStartupEvent
fun instantiateRangingGuildTowerArchers() {
    instantiateNpc(688, 689, 690, 691) { npcId, tile ->
        RangingGuildTowerArcher(npcId, tile, false)
    }
}
