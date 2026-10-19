package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapGloughsChest() {
    onObjectClick(Loc.grandtree_chestclosed) { (player) ->
        if (player.inventory.containsOneItem(Obj.grandtree_gloughskey))
            searchGloughsChest(player)
        else
            player.sendMessage("The chest is locked.")
    }
    onItemOnObject(arrayOf(Loc.grandtree_chestclosed), arrayOf(Obj.grandtree_gloughskey)) { (player) -> searchGloughsChest(player) }
}

private fun searchGloughsChest(player: Player) {
    if (player.grandTreeStage !in STAGE_GLOUGHS_KEY..STAGE_INVASION_PLANS) {
        player.sendMessage("The key doesn't fit.")
        return
    }
    player.startConversation {
        simple("You have found a scroll!")
        if (player.inventory.containsOneItem(Obj.grandtree_invasionplans)) {
            simple("You already have the invasion plans.")
        } else if (!player.inventory.hasFreeSlots()) {
            simple("You don't have enough space to hold the scroll.")
        } else {
            simple("You take the invasion plans from the chest.") {
                player.inventory.addItem(Obj.grandtree_invasionplans)
                player.setGrandTreeStage(STAGE_INVASION_PLANS)
            }
        }
    }
}
