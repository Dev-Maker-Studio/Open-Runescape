package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.model.gameobject.GameObject
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapGloughsCupboard() {
    onObjectClick(Loc.grandtree_cupboardclosed, Loc.grandtree_cupboardopen) { (player, obj, option) ->
        when (option) {
            "Open" -> {
                World.spawnObjectTemporary(GameObject(Loc.grandtree_cupboardopen, obj.type, obj.rotation, obj.tile), 100)
                return@onObjectClick
            }
            "Close" -> {
                World.spawnObjectTemporary(GameObject(Loc.grandtree_cupboardclosed, obj.type, obj.rotation, obj.tile), 100)
                return@onObjectClick
            }
        }
        if (player.grandTreeStage < STAGE_SEARCH_GLOUGHS_HOME) {
            player.sendMessage("You find nothing of interest.")
            return@onObjectClick
        }
        player.startConversation {
            simple("You've found Glough's Journal!")
            if (player.inventory.containsOneItem(Obj.grandtree_journal)) {
                simple("You already have the journal.")
            } else if (!player.inventory.hasFreeSlots()) {
                simple("You don't have enough space to hold the journal.")
            } else {
                simple("You take the journal from the cupboard.") { player.inventory.addItem(Obj.grandtree_journal) }
            }
        }
    }
}
