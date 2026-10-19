package com.rs.game.content.miniquests.witches_potion

import com.rs.engine.miniquest.Miniquest
import com.rs.game.model.entity.npc.NPC
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc

@ServerStartupEvent
fun mapWitchesPotionRats() {
    instantiateNpc("Rat", "Giant rat", "Sewer rat", "Zombie rat", "Dungeon Rat", "Blessed giant rat") { npcId, tile -> WitchesPotionRats(npcId, tile) }
}

class WitchesPotionRats(id: Int, tile: Tile) : NPC(id, tile) {
    override fun drop() {
        super.drop()
        val killer = mostDamageReceivedSourcePlayer ?: return
        if (killer.getMiniquestStage(Miniquest.WITCHES_POTION) == WitchesPotion.NEED_INGREDIENTS) {
            sendDrop(killer, Item(300, 1))
        }
    }
}