package com.rs.game.content.quests.regicide.npcs

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapRegicideLordIorwerth() {
    onNpcClick(Npc.npc_1182, options = arrayOf("Talk-to")) { (player, npc) ->
        val stage = player.getQuestStage(Quest.REGICIDE)
        player.startConversation {
            when (stage) {
                STAGE_UNSTARTED, STAGE_COMPLETE -> {
                    player(CALM_TALK, "Good day.")
                    npc(npc, CALM_TALK, "Sorry, can't stop to talk. So much to do, so little time.")
                }
                STAGE_MEET_IORWERTH -> {
                    player(CALM_TALK, "Hello, I'm ${player.displayName}. Your scouts said that I should come and see you.")
                    npc(npc, CALM_TALK, "Ahh, ${player.displayName}. Welcome to Isafdar. I understand you've been sent here to rid us of that criminal Tyras. I offer my full support. Unfortunately all my troops are hunting down a group of renegade elves, but you should find one of our trackers by Tyras's old camp. He should be able to help you.")
                    player(CALM_TALK, "Thank you. I shall return, when my task is complete.") { player.advanceRegicide(STAGE_MEET_TRACKER) }
                }
                STAGE_MEET_TRACKER -> npc(npc, CALM_TALK, "The only help I can offer at this time is the use of a tracker. He can be found over the river to the south east. Good luck!")
                STAGE_GET_PENDANT -> {
                    npc(npc, CALM_TALK, "Good day, ${player.displayName}. How may I help you now?")
                    player(CALM_TALK, "Your scout refused to help, unless I take proof that you sent me.")
                    if (player.getItemWithPlayer(Obj.regicide_crystal_pendant) != null) {
                        npc(npc, CALM_TALK, "Just show him the pendant I gave you.")
                    } else {
                        npc(npc, CALM_TALK, "Bless his loyalty but curse his suspicion. Here, he will recognise this:") {
                            if (player.giveRegicideItem(Obj.regicide_crystal_pendant))
                                player.sendMessage("Lord Iorwerth gives you a crystal pendant.")
                        }
                    }
                }
                STAGE_FIND_TRACKS -> {
                    npc(npc, CALM_TALK, "${player.displayName}, how goes your search? Any luck hunting down that brigand?")
                    player(CALM_TALK, "I've found no trace of him as yet.")
                    npc(npc, CALM_TALK, "I'm sure you will find something at his old camp that can help you.")
                    player(CALM_TALK, "I'll take a look and see what I can find.")
                    npc(npc, CALM_TALK, "Good. Come find me when you've located the camp.")
                }
                STAGE_REPORT_TRACKS -> {
                    npc(npc, CALM_TALK, "${player.displayName}, how goes your search? Any luck hunting down that brigand?")
                    player(CALM_TALK, "I've found some tracks at Tyras's old camp, but they trail off in to the trees.")
                    npc(npc, CALM_TALK, "Ask my scout. He may be able to help there.")
                    player(CALM_TALK, "Thanks I'll go ask him")
                    npc(npc, CALM_TALK, "Good. Come find me when you've located the camp.")
                }
                STAGE_FOLLOW_TRACKS, STAGE_FIND_TYRAS_CAMP -> {
                    npc(npc, CALM_TALK, "${player.displayName}, how goes your search? Any luck hunting down that brigand?")
                    player(CALM_TALK, if (stage == STAGE_FOLLOW_TRACKS) "I think I just need to follow some tracks I found. Your scout showed me how." else "I have come across heavy resistance trying to follow some tracks from the old camp. I'm guessing I'm on the right path.")
                    npc(npc, CALM_TALK, "Good. Come find me when you've located the camp.")
                }
                STAGE_REPORT_CAMP -> {
                    npc(npc, CALM_TALK, "${player.displayName}! How goes your search? Any luck hunting down that brigand?")
                    player(CALM_TALK, "I've finally tracked the brute down. His camp is in a secluded bit of forest, almost directly south of here.")
                    npc(npc, CALM_TALK, "Good job. I had a plan to destroy his camp, using the knowledge found in this book but before I had perfected any formula they moved camp. Here, you may find the book useful.") {
                        if (player.giveRegicideItem(Obj.obj_3230)) {
                            player.sendMessage("Lord Iorwerth gives you a book.")
                            player.advanceRegicide(STAGE_PREPARE_EXPLOSIVE)
                        }
                    }
                }
                STAGE_PREPARE_EXPLOSIVE, STAGE_DESTROY_TENT -> {
                    if (!player.inventory.containsItem(Obj.obj_3230)) {
                        player(CALM_TALK, "I lost my copy of the Big Book o' Bangs.")
                        npc(npc, CALM_TALK, "It's lucky I had a scribe make a few copies.") {
                            if (player.giveRegicideItem(Obj.obj_3230))
                                player.sendMessage("Lord Iorwerth hands you a book.")
                        }
                        player(CALM_TALK, "Thank you.")
                    } else {
                        player(CALM_TALK, "Hello.")
                        npc(npc, CALM_TALK, "Good day, ${player.displayName}. Have you yet found a way to rid us of Tyras?")
                        player(CALM_TALK, "No but I'm working on it.")
                        npc(npc, CALM_TALK, "Good, we are running short of time.")
                    }
                }
                STAGE_REPORT_TYRAS_DEATH -> {
                    player(CALM_TALK, "Lord Iorwerth, the deed is done.")
                    npc(npc, CALM_TALK, "Good good... One of my scouts just reported that they have seen his tent in a ball of flame. I'm sure you will want to get back to your king and tell him. He will undoubtedly want some proof. Give him this letter; it verifies that the job is done. He should trust me enough in this matter.") {
                        if (player.giveRegicideItem(Obj.regicide_iorwerth_message)) {
                            player.sendMessage("Lord Iorwerth hands you a scroll.")
                            player.advanceRegicide(STAGE_RETURN_TO_LATHAS)
                        }
                    }
                    npc(npc, CALM_TALK, "You may use the pass through Arandar to return home. You'll find the start of the trail in the far north-east of the woods. I have ordered the guards there to let you pass the Arandar palisade.")
                    player(CALM_TALK, "Thank you, my lord.")
                }
                STAGE_RETURN_TO_LATHAS, STAGE_DELIVER_MESSAGE -> {
                    if (!player.inventory.containsItem(Obj.regicide_iorwerth_message)) {
                        player(CALM_TALK, "Lord Iorwerth, I have lost the message for King Lathas.")
                        npc(npc, CALM_TALK, "Here, have another copy, but don't let it fall into the wrong hands.")
                        player(CALM_TALK, "Thank you Lord Iorwerth.") {
                            if (player.giveRegicideItem(Obj.regicide_iorwerth_message))
                                player.sendMessage("Lord Iorwerth hands you a scroll.")
                        }
                    } else {
                        npc(npc, CALM_TALK, "Have you delivered the message to King Lathas yet?")
                        player(CALM_TALK, "Not yet, my lord.")
                        npc(npc, CALM_TALK, "There is no time for delay, he'll need to see that.")
                    }
                }
                else -> simple("Nothing interesting happens.")
            }
        }
    }
}
