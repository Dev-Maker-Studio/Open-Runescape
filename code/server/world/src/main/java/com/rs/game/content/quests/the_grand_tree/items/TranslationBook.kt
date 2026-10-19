package com.rs.game.content.quests.the_grand_tree.items

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick

@ServerStartupEvent
fun mapGrandTreeTranslationBook() {
    onItemClick(Obj.grandtree_translationbook, options = arrayOf("Read")) { (player) ->
        player.startConversation {
            item(Obj.grandtree_translationbook, "Gnome-English Translation Dictionary")
            simple("Common words in the ancient gnome language, translated into the modern tongue by Anita.")
            simple("The dictionary should let you reconstruct Hazelmere's message, one phrase at a time.")
        }
    }
}
