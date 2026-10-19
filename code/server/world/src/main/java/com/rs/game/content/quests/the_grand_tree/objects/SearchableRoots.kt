package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapSearchableGrandTreeRoots() {
    onObjectClick(Loc.gnome_area_imp_mine_large_root_var1, Loc.gnome_area_imp_mine_large_root_var2) { (player, obj) ->
        if (player.grandTreeStage !in STAGE_FIND_DACONIA..STAGE_DACONIA_FOUND) {
            player.sendMessage("You search the root but don't find anything.")
            return@onObjectClick
        }
        val correct = DACONIA_ROOTS[player.ensureDaconiaRoot()]
        if (!obj.tile.matches(correct)) {
            player.sendMessage("You search the root but don't find anything.")
            return@onObjectClick
        }
        player.startConversation {
            simple("You've found a Daconia rock!")
            if (player.inventory.containsOneItem(Obj.grandtree_daconiarock)) {
                simple("You already have the Daconia rock.")
            } else if (!player.inventory.hasFreeSlots()) {
                simple("You don't have enough space to hold the rock.")
            } else {
                simple("You take the Daconia rock from the root.") {
                    player.inventory.addItem(Obj.grandtree_daconiarock)
                    player.setGrandTreeStage(STAGE_DACONIA_FOUND)
                }
            }
        }
    }
}
