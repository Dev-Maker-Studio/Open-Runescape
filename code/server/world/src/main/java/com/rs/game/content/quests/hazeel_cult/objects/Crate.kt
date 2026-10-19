package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapHazeelCrate() {
    onObjectClick(Loc.carnilleancrate) { (player) ->
        if (!player.isCultSide() || player.questManager.getStage(Quest.HAZEEL_CULT) < STAGE_FIND_SCROLL) {
            player.sendMessage("You search the crate but find nothing interesting.")
            return@onObjectClick
        }
        if (player.inventory.containsOneItem(Obj.carnilleanchestkey) || player.bank.containsItem(Obj.carnilleanchestkey) ||
            player.inventory.containsOneItem(Obj.hazeel_scroll) || player.bank.containsItem(Obj.hazeel_scroll)) {
            player.sendMessage("You search the crate but find nothing else.")
            return@onObjectClick
        }
        giveItem(player, Obj.carnilleanchestkey, "You find a small key hidden at the bottom of the crate.")
    }
}
