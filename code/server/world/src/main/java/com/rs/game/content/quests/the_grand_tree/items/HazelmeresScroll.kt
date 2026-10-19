package com.rs.game.content.quests.the_grand_tree.items

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem

@ServerStartupEvent
fun mapHazelmeresScroll() {
    onItemClick(Obj.grandtree_scroll, options = arrayOf("Read")) { (player) ->
        readHazelmeresScroll(player)
    }
    onItemOnItem(intArrayOf(Obj.grandtree_translationbook), intArrayOf(Obj.grandtree_scroll)) { event ->
        readHazelmeresScroll(event.player)
    }
}

private fun readHazelmeresScroll(player: Player) {
    player.startConversation {
        item(Obj.grandtree_scroll, "Hazelmere's message is written in the old gnome tongue.")
        if (!player.inventory.containsOneItem(Obj.grandtree_translationbook)) {
            simple("You will need the translation book to understand it.")
        } else {
            simple("You use the translation book to decipher Hazelmere's message.")
            simple("A man came to me with the King's seal.")
            simple("I gave the man Daconia rocks.")
            simple("And Daconia rocks will kill the tree!")
        }
    }
}
