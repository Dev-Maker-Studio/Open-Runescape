package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject

private val RANGE_IDS = arrayOf<Any>(Loc.loc_2859, Loc.hazeelcult_stove_pot, Loc.hazeelcult_stove_poisoned)

@ServerStartupEvent
fun mapCarnilleanRange() {
    onItemOnObject(objectNamesOrIds = RANGE_IDS) { (player, _, item) ->
        if (item.id != Obj.poison) {
            player.sendMessage("That's not what Clivet told you to put in the food.")
            return@onItemOnObject
        }
        if (!player.isCultSide() || player.questManager.getStage(Quest.HAZEEL_CULT) != STAGE_BRANCH_TASK) {
            player.sendMessage("That's not what you need to put in the food.")
            return@onItemOnObject
        }
        if (!player.inventory.containsOneItem(Obj.poison)) return@onItemOnObject

        player.inventory.deleteItem(Obj.poison, 1)
        player.setSide(SIDE_CULT)
        player.vars.setVarBit(Varbit.hazeelcult_deaddog, 1)
        player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_BRANCH_PROGRESS)
        player.itemDialogue(Obj.poison, "You pour the poison into the pot.<br>The poison dissolves into the soup.<br>The chef does not seem to be paying attention.")
    }
}
