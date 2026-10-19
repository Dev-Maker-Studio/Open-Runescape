package com.rs.game.content.quests.the_grand_tree.items

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick

@ServerStartupEvent
fun mapGrandTreeLumberOrder() {
    onItemClick(Obj.grandtree_order, options = arrayOf("Read")) { (player) ->
        player.startConversation {
            item(Obj.grandtree_order, "Karamja shipyard")
            simple("Order")
            simple("Order for 30 Karamja battleships")
            simple("Lumber required: 2000")
            simple("Troop capacity: 300")
            simple("Perhaps there's more information back at the Gnome Stronghold.")
        }
    }
}
