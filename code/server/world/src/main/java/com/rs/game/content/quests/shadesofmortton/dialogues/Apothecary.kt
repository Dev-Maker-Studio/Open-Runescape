package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapMorttonDiaryExchange() {
    onItemOnNpc(Npc.apothecary) { (player, item) ->
        if (item.id != Obj.serum_book) return@onItemOnNpc
        if (!player.isQuestComplete(Quest.SHADES_OF_MORTTON)) {
            player.sendMessage("You should finish Shades of Mort'ton before giving away Herbi Flax's diary.")
            return@onItemOnNpc
        }
        if (player.getBool("morttonDiaryBonusClaimed")) {
            player.sendMessage("You have already received the Herblore training for Herbi Flax's diary.")
            return@onItemOnNpc
        }
        player.startConversation {
            simple("You give Herbi Flax's diary to the Apothecary, who trains you in Herblore.", extraFunctionality = Runnable {
                if (!player.getBool("morttonDiaryBonusClaimed") && player.isQuestComplete(Quest.SHADES_OF_MORTTON) && player.inventory.containsItem(Obj.serum_book, 1)) {
                    player.set("morttonDiaryBonusClaimed", true)
                    player.inventory.deleteItem(Obj.serum_book, 1)
                    player.skills.addXpQuest(Constants.HERBLORE, 335.0)
                }
            })
        }
    }
}
