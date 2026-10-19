package com.rs.game.content.skills.crafting

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapBullseyeLantern() {
    onItemOnItem(intArrayOf(Obj.bullseye_lantern_lens), intArrayOf(Obj.bullseye_lantern_nolens)) { event ->
        val player = event.player
        if (player.skills.getLevel(Constants.CRAFTING) < 49) return@onItemOnItem player.sendMessage("You need a Crafting level of 49 to fit a lens to the lantern.")
        val lantern = event.getUsedWith(Obj.bullseye_lantern_lens)
        player.inventory.deleteItem(Obj.bullseye_lantern_lens, 1)
        player.inventory.replaceItem(Obj.bullseye_lantern_empty, 1, lantern.slot)
        player.sendMessage("You fit the lens to the lantern.")
    }
}
