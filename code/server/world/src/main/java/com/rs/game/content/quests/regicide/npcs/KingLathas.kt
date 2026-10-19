package com.rs.game.content.quests.regicide.npcs

import com.rs.game.content.quests.regicide.cutscenes.playArianwynRevelation
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj

fun regicideKingLathasDialogue(player: Player, npc: NPC) {
    val stage = player.getQuestStage(Quest.REGICIDE)
    if (stage == STAGE_RETURN_TO_LATHAS && player.inventory.containsItem(Obj.regicide_iorwerth_message)) {
        player.playArianwynRevelation()
        return
    }
    player.startConversation {
        when {
            stage == STAGE_UNSTARTED -> {
                if (!player.questManager.getAttribs(Quest.REGICIDE).getB(KINGS_MESSAGE_DELIVERED) &&
                    !player.inventory.containsItem(Obj.regicide_quest_kings_summons) &&
                    !player.bank.containsItem(Obj.regicide_quest_kings_summons)) {
                    simple("King Lathas has not summoned you yet.")
                    return@startConversation
                }
                if (!Quest.REGICIDE.meetsReqs(player, "to start this quest.")) {
                    simple("You do not meet the requirements to start Regicide.")
                    return@startConversation
                }
                questStart(Quest.REGICIDE)
                player(CALM_TALK, "I received your message my Liege. How may I serve the kingdom?")
                npc(npc, CALM_TALK, "Ahh.. ${player.displayName}. Yes... We have finally finished the work on the Well of Voyage. It is now time for us to move against my brother.")
                npc(npc, CALM_TALK, "I've managed to get a letter through to the elves that live to the west. They informed me that Tyras and his brigands are camped up in the woods to the South of their city.")
                npc(npc, CALM_TALK, "The elves have agreed to help, however they have their battles to fight. Unfortunately the men at arms I promised you are unavailable but I have faith that you'll get the job done.")
                npc(npc, CALM_TALK, "Finally and most importantly you must find an easy way to the west lands. We cannot use the well as a trade route, and certainly cannot move any bulk of troops through there.")
                npc(npc, CALM_TALK, "In short you must use the underpass to enter the west lands, kill my brother Tyras, find an easy route home and finally return back here, to report. And don't worry, you will be well rewarded.")
                exec {
                    if (player.getQuestStage(Quest.REGICIDE) == STAGE_UNSTARTED)
                        player.setQuestStage(Quest.REGICIDE, STAGE_ENTER_ISAFDAR)
                }
            }
            stage in STAGE_REPORT_TYRAS_DEATH..STAGE_DELIVER_MESSAGE -> {
                player(CALM_TALK, "My lord, Tyras is dead!")
                npc(npc, CALM_TALK, "This is grand news indeed! You'll forgive me if I'm a little apprehensive about this, but I take it you do have proof?")
                if (stage == STAGE_DELIVER_MESSAGE && player.inventory.containsItem(Obj.regicide_iorwerth_message)) {
                    player(CALM_TALK, "Yes, I have a letter sent by Lord Iorwerth.")
                    simple("You hand the king the message. He looks at the seal then opens it.")
                    npc(npc, CALM_TALK, "Yes... Good... This will do nicely. Well done, You're really proving to be quite useful. I'll send word when I next require your services. Oh and I guess you'll be wanting your reward.")
                    exec {
                        if (player.getQuestStage(Quest.REGICIDE) == STAGE_DELIVER_MESSAGE && player.inventory.containsItem(Obj.regicide_iorwerth_message)) {
                            player.inventory.deleteItem(Obj.regicide_iorwerth_message, 1)
                            player.questManager.completeQuest(Quest.REGICIDE)
                        }
                    }
                } else {
                    player(CALM_TALK, "No I have nothing to corroborate this.")
                    npc(npc, CALM_TALK, "I am sure you have done as you say, but on this matter I can afford no doubts. You must find something to confirm your story.")
                    player(CALM_TALK, "I'll see what I can do")
                }
            }
            stage == STAGE_COMPLETE -> {
                player(CALM_TALK, "Good day my lord.")
                npc(npc, CALM_TALK, "Hello ${player.displayName}. I've got nothing for you right now.")
            }
            stage in STAGE_REPORT_CAMP..STAGE_DESTROY_TENT -> {
                player(CALM_TALK, "Goodday my lord.")
                npc(npc, CALM_TALK, "Ah... ${player.displayName} how goes your exploration? Any luck finding my brother in the west lands?")
                player(CALM_TALK, "I have tracked him down, but I've yet to deal with him.")
                npc(npc, CALM_TALK, "What are you doing here then? I'm counting on you ${player.displayName}.")
                player(CALM_TALK, "Yes my lord.")
            }
            else -> {
                player(CALM_TALK, "My Liege. What must I do again?")
                npc(npc, CALM_TALK, "In short you must use the underpass to enter the west lands, kill my brother Tyras, find an easy route home and finally return back here, to report. And don't worry, you will be well rewarded.")
            }
        }
    }
}
