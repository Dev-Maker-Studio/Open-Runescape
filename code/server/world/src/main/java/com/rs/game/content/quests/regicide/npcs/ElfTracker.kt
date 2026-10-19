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
fun mapRegicideElfTracker() {
    onNpcClick(Npc.regicide_old_camp_tracker, options = arrayOf("Talk-to")) { (player, npc) ->
        val stage = player.getQuestStage(Quest.REGICIDE)
        player.startConversation {
            when {
                stage < STAGE_MEET_TRACKER -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "Human! you must be one of Tyras's men. I have no time for brigands or outlaws.")
                }
                stage in STAGE_MEET_TRACKER..STAGE_GET_PENDANT -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "Human! you must be one of Tyras's men....")
                    player(CALM_TALK, "No I'm ${player.displayName}. Lord Iorwerth said you may be able to help me.")
                    npc(npc, CALM_TALK, "And you have something to prove this?")
                    if (player.getItemWithPlayer(Obj.regicide_crystal_pendant) == null) {
                        player(CALM_TALK, "Well... err... No.")
                        npc(npc, CALM_TALK, "As I was saying.... I have no time for brigands or outlaws.") {
                            player.advanceRegicide(STAGE_GET_PENDANT)
                        }
                    } else {
                        simple("You show the tracker the crystal pendant")
                        npc(npc, CALM_TALK, "That's Lord Iorwerth's pendant. He must have a lot of faith in you. Now what is it I can help you with?")
                        player(CALM_TALK, "I need to find Tyras and kill him. Do you know where his camp is?")
                        npc(npc, CALM_TALK, "Well this was his old camp. After the battle a few days ago they moved and now we're having trouble tracking him down.")
                        player(CALM_TALK, "Can I help at all?")
                        npc(npc, CALM_TALK, "As it goes I'm not actually tracking them at the moment. This here is the best trace we've had on our renegade brethren.")
                        player(CALM_TALK, "What is?")
                        npc(npc, CALM_TALK, "Ahh I guess you can't see with those human eyes. I tell you what. I'll continue to search here on the east side. You check out the west end of the camp. Come and tell me if you find anything.") {
                            player.advanceRegicide(STAGE_FIND_TRACKS)
                        }
                    }
                }
                stage == STAGE_FIND_TRACKS -> {
                    npc(npc, CALM_TALK, "What is it I can help you with?")
                    player(CALM_TALK, "I need to find Tyras and kill him. Do you know where his camp is?")
                    npc(npc, CALM_TALK, "Well this was his old camp. After the battle a few days ago they moved and now we're having trouble tracking him down.")
                    player(CALM_TALK, "Can I help at all?")
                    npc(npc, CALM_TALK, "As it goes I'm not actually tracking them at the moment. This here is the best trace we've had on our renegade brethren.")
                    player(CALM_TALK, "What is?")
                    npc(npc, CALM_TALK, "Ahh I guess you can't see with those human eyes. I tell you what. I'll continue to search here on the east side. You check out the west end of the camp. Come and tell me if you find anything.")
                }
                stage == STAGE_REPORT_TRACKS -> {
                    player(CALM_TALK, "I've found tracks leading off to the west. But they trail off into the trees. Beyond that I am unable to follow.")
                    npc(npc, CALM_TALK, "These woods are a tangle of animal tracks and hidden paths. There's bound to be a way past. Look for things like flattened grass, broken twigs, disturbed leaf litter and such like. I'm sure now you know what you're looking for you will have no trouble following the tracks.")
                    player(CALM_TALK, "Thanks... I'll see what I can find.") {
                        player.advanceRegicide(STAGE_FOLLOW_TRACKS)
                    }
                }
                stage in STAGE_FOLLOW_TRACKS..STAGE_FIND_TYRAS_CAMP -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "How goes the hunt for that bandit camp?")
                    player(CALM_TALK, "Getting there.")
                }
                stage == STAGE_REPORT_CAMP -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "How goes the hunt for that bandit camp?")
                    player(CALM_TALK, "I found it a short distance west of here.")
                    npc(npc, CALM_TALK, "I'm sure Lord Iorwerth will be pleased to hear that.")
                }
                stage in STAGE_PREPARE_EXPLOSIVE..STAGE_DESTROY_TENT -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "How goes it?")
                    player(CALM_TALK, "Well thanks.")
                }
                else -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "I take it that the huge explosion I just heard was you?")
                    player(CALM_TALK, "Yes I've finally dealt with Tyras, he should cause you no more problems.")
                    npc(npc, CALM_TALK, "This is good news indeed, Lord Iorwerth will be very happy to be told this.")
                }
            }
            options {
                op("Bye.") { player(CALM_TALK, "Bye.") }
            }
        }
    }
}
