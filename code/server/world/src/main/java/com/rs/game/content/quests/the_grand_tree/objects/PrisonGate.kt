package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.content.quests.the_grand_tree.npcs.charliePrisonEscapeDialogue
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapGrandTreePrisonGate() {
    onObjectClick(Loc.gnome_imp_grand_tree_cage_door) { (player) ->
        if (player.grandTreeStage == STAGE_SEARCH_GLOUGHS_HOME && player.grandTreeAttribs().getB(ATTR_ARRESTED))
            charliePrisonEscapeDialogue(player)
        else
            player.sendMessage("Surprise, surprise! It's locked!")
    }
}
