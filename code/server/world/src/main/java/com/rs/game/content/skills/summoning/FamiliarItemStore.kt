package com.rs.game.content.skills.summoning

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc

@ServerStartupEvent
fun mapFamiliarItemStore() {
    onItemOnNpc(*Pouch.getAllNPCKeysWithInventory(), checkDistance = false) { (player, item, npc) ->
        val familiar = npc as? Familiar ?: return@onItemOnNpc
        if (familiar.owner != player) return@onItemOnNpc player.sendMessage("This isn't your familiar.")
        if (familiar.pouch.isForager) return@onItemOnNpc
        familiar.addItem(item.slot, item.amount)
    }
}
