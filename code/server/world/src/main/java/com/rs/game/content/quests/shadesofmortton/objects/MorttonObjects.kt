package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.startConversation
import com.rs.engine.dialogue.statements.QuestStartStatement
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

@ServerStartupEvent
fun mapMorttonQuestObjects() {
    onObjectClick(Loc.mortton_signpost) { (player) ->
        player.sendMessage("You inspect the signpost more closely. You see nothing wrong with it initially, but after some time you notice teeth marks in the wood. It looks like people have tried eating the post.")
    }
    onObjectClick(Loc.shades_experimenttable) { (player) ->
        when {
            player.vars.getVarBit(Varbit.shades_table_searched) != 0 -> player.sendMessage("You search the table but find nothing.")
            player.inventory.freeSlots < 3 -> player.sendMessage("You need three free inventory spaces to take the herbs.")
            else -> {
                player.inventory.addItem(Obj.unidentified_tarromin, 2)
                player.inventory.addItem(Obj.rogues_purse, 1)
                player.vars.saveVarBit(Varbit.shades_table_searched, 1)
                player.sendMessage("You find a selection of herbs.")
            }
        }
    }
    onObjectClick(Loc.shades_experimentshelf) { (player) ->
        when {
            player.inventory.containsItem(Obj.serum_book, 1) || player.bank.containsItem(Obj.serum_book, 1) -> player.sendMessage("You find nothing of interest here.")
            !player.inventory.hasFreeSlots() -> player.sendMessage("You need a free inventory space to take the book.")
            else -> {
                player.inventory.addItem(Obj.serum_book, 1)
                player.sendMessage("You find an interesting looking book on the shelf.")
            }
        }
    }
    onItemClick(Obj.serum_book, options = arrayOf("Read")) { (player) ->
        if (player.isQuestStarted(Quest.SHADES_OF_MORTTON)) {
            player.openBook(HerbiFlaxDiary())
            return@onItemClick
        }
        player.startConversation {
            simple("~ Diary of Herbi Flax ~", "You prepare to read the book.")
            statementWithActions(QuestStartStatement(Quest.SHADES_OF_MORTTON), Runnable {
                    if (player.canStartMorttonQuest()) {
                        player.setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_READ_DIARY)
                        player.openBook(HerbiFlaxDiary())
                    } else {
                        val message = "You do not meet the requirements to start : 'Shades of Mort'ton'."
                        player.sendMessage(message)
                        player.simpleDialogue(message)
                    }
            }, Runnable {})
        }
    }
    onNpcDeath(Npc.shadeshadow_level1, Npc.shade_level1) { (killer) ->
        val player = killer as? Player ?: return@onNpcDeath
        if (player.getQuestStage(Quest.SHADES_OF_MORTTON) != STAGE_KILL_SHADES) return@onNpcDeath
        val state = player.morttonState()
        val kills = state.getI("shadeKills")
        if (kills >= 5) return@onNpcDeath
        state.setI("shadeKills", kills + 1)
        player.sendMessage(if (kills == 4) "That's all five shades!" else "That's ${arrayOf("one", "two", "three", "four")[kills]} shade${if (kills == 0) "" else "s"}!")
    }
}

private fun Player.canStartMorttonQuest(): Boolean =
    Quest.SHADES_OF_MORTTON.meetsReqs(this) &&
        skills.getLevelForXp(Constants.CRAFTING) >= 20 &&
        skills.getLevelForXp(Constants.HERBLORE) >= 15 &&
        skills.getLevelForXp(Constants.FIREMAKING) >= 5
