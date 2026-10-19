package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapHazeelChest() {
    onObjectClick(Loc.carnilleanshutchest, Loc.carnilleanopenchest) { (player) ->
        if (!player.isCultSide() || player.questManager.getStage(Quest.HAZEEL_CULT) < STAGE_FIND_SCROLL) {
            player.sendMessage("The chest is locked.")
            return@onObjectClick
        }
        if (player.inventory.containsOneItem(Obj.hazeel_scroll) || player.bank.containsItem(Obj.hazeel_scroll)) {
            player.sendMessage("The chest is empty.")
            return@onObjectClick
        }
        if (!player.inventory.containsOneItem(Obj.carnilleanchestkey)) {
            player.sendMessage("The chest is locked. You need a key to open it.")
            return@onObjectClick
        }
        if (!player.inventory.hasFreeSlots()) {
            player.sendMessage("You need some free inventory space first.")
            return@onObjectClick
        }

        player.inventory.deleteItem(Obj.carnilleanchestkey, 1)
        player.inventory.addItem(Obj.hazeel_scroll)
        player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_RETURN_SCROLL)
        player.itemDialogue(Obj.hazeel_scroll, "You unlock the chest and find Hazeel's scroll inside.")
    }
}
