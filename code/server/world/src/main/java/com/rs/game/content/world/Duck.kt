package com.rs.game.content.world

import com.rs.game.model.entity.npc.NPC
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc

@ServerStartupEvent
fun mapDuckQuacks() {
    instantiateNpc("Duck", "Drake") { id, tile ->
        object: NPC(id, tile) {
            override fun processNPC() {
                super.processNPC()
                if (Utils.random(50) == 0) forceTalk("Quack!")
            }
        }
    }
}
