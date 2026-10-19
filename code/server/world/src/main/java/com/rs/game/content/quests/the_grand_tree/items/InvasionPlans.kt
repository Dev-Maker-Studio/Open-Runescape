package com.rs.game.content.quests.the_grand_tree.items

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick

@ServerStartupEvent
fun mapGrandTreeInvasionPlans() {
    onItemClick(Obj.grandtree_invasionplans, options = arrayOf("Read")) { (player) ->
        player.startConversation {
            item(Obj.grandtree_invasionplans, "Invasion")
            simple("Troops board three fleets of battleships at Karamja")
            simple("Fleet 1<br>Attacks Misthalin from the south.")
            simple("Fleet 2<br>Groups at Crandor and attacks Asgarnia from the west.")
            simple("Fleet 3<br>Sails north to attack Kandarin from south, reinforced by gnome foot soldiers leaving gnome stronghold.")
            simple("Take no prisoners!")
        }
    }
}
