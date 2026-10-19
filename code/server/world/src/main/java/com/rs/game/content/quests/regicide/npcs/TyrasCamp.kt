package com.rs.game.content.quests.regicide.npcs

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import world.gregs.voidps.type.Tile
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

internal const val GUARD_EATING_UNTIL = "guardEatingUntil"

@ServerStartupEvent
fun mapRegicideTyrasCamp() {
    onNpcClick(Npc.regicide_general_hining, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            player(CALM_TALK, "Hello.")
            if (player.getQuestStage(Quest.REGICIDE) >= STAGE_REPORT_TYRAS_DEATH) {
                npc(npc, CALM_TALK, "This is not a good place to be, go and prepare for the coming darkness.")
                player(CALM_TALK, "What do you mean?")
                npc(npc, CALM_TALK, "There is no great hero left to fight back the forces that face us now... Go, leave, prepare for the darkness. While you still have time!")
            } else {
                npc(npc, CALM_TALK, "Yes, is there anything I can help with? I'm General of what's left of this army and I'm rather busy.")
                player(CALM_TALK, "If you're a General can you help me meet the king?")
                npc(npc, CALM_TALK, "The king will see no one. He can trust no one!")
                player(CALM_TALK, "Is there no way I can see him?")
                npc(npc, CALM_TALK, "In the coming battle he will stand between us and the end. Then you will see him.")
                player(CALM_TALK, "Not quite what I had in mind.")
                if (player.getQuestStage(Quest.REGICIDE) == STAGE_FIND_TYRAS_CAMP)
                    exec { player.advanceRegicide(STAGE_REPORT_CAMP) }
            }
        }
    }
    onNpcClick(Npc.regicide_tyras_camp_guard, Npc.regicide_tyras_guard, Npc.regicide_tyras_camp_tent_guard, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            player(CALM_TALK, "Hello.")
            val dead = player.getQuestStage(Quest.REGICIDE) >= STAGE_REPORT_TYRAS_DEATH
            val text = when {
                npc.id == Npc.regicide_tyras_camp_tent_guard && dead -> "Good day. Sorry I can't stop to talk. General Hining is keeping a tight watch."
                npc.id == Npc.regicide_tyras_camp_tent_guard -> "Good day, I'm busy guarding King Tyras. I have no time for idle chat."
                dead -> "These are dark times, I must prepare for the coming battle."
                else -> "Sorry, can't stop to talk, you should go to General Hining or the king who's in his tent."
            }
            npc(npc, CALM_TALK, text)
        }
    }
    onObjectClick(Loc.regicide_tent_door, Loc.regicide_tent_door_for_fire, tiles = arrayOf(Tile.of(2186, 3144, 0))) { (player, obj) ->
        if (obj.x !in 2180..2190 || obj.y !in 3140..3145)
            return@onObjectClick
        player.startConversation {
            if (player.getQuestStage(Quest.REGICIDE) >= STAGE_REPORT_TYRAS_DEATH) {
                npc(Npc.regicide_tyras_camp_tent_guard, CALM_TALK, "You cannot enter! This is now General Hining's pavilion. Since the king's death he has held us together.")
                player(CALM_TALK, "You're not going home to Ardougne then?")
                npc(Npc.regicide_tyras_camp_tent_guard, CALM_TALK, "I wish I could. Just to see my wife, but if we don't stop the evil here, it will be too late!")
                player(CALM_TALK, "Evil?")
                npc(Npc.regicide_tyras_camp_tent_guard, CALM_TALK, "Soon Zamorak's fury will be released upon us.")
            } else {
                npc(Npc.regicide_tyras_camp_tent_guard, CALM_TALK, "You cannot pass! These are the king's quarters. No one enters without invitation.")
                player(CALM_TALK, "How do I get an invitation?")
                npc(Npc.regicide_tyras_camp_tent_guard, CALM_TALK, "If the king wants to see you he will ask for you.")
                player(CALM_TALK, "What's the chances of that?")
                npc(Npc.regicide_tyras_camp_tent_guard, CALM_TALK, "Absolutely none! Now let me get on with my job.")
            }
        }
    }
    onNpcClick(Npc.regicide_tyras_lazy_guard) { (player, npc) ->
        val stage = player.getQuestStage(Quest.REGICIDE)
        val eating = System.currentTimeMillis() < player.questManager.getAttribs(Quest.REGICIDE).getL(GUARD_EATING_UNTIL)
        player.startConversation {
            when {
                stage >= STAGE_REPORT_TYRAS_DEATH -> {
                    player(CALM_TALK, "All right.")
                    if (eating) {
                        npc(npc, CALM_TALK, "This rabbit is lovely. Thanks a lot...")
                    } else {
                        npc(npc, CALM_TALK, "They say the king died from a flying fire ball. Are you certain no one messed with the catapult while I was eating?")
                        player(CALM_TALK, "I was the only one here. i was watching the whole time.")
                    }
                }
                eating -> {
                    player(CALM_TALK, "Hows it going?")
                    npc(npc, CALM_TALK, "This is delicious, after a year of field rations. Give us a minute to finish it off.")
                }
                else -> {
                    player(CALM_TALK, "Good day.")
                    npc(npc, CALM_TALK, "Yeah what ya want?")
                    player(CALM_TALK, "I was wondering what you were doing out here?")
                    npc(npc, CALM_TALK, "I was just wondering what I was doing here too. I'm meant to be looking after this catapult but I can't keep my mind on the job. We've been living off field rations for almost a year now. I keep seeing rabbits in this forest. What I wouldn't give to eat one, but I'm not quick enough to kill any and I'm no good with a bow.")
                    player(CALM_TALK, "I can see how that would be distracting.")
                }
            }
        }
    }
    onItemOnNpc(Npc.regicide_tyras_lazy_guard) { (player, item, npc) ->
        val attributes = player.questManager.getAttribs(Quest.REGICIDE)
        player.startConversation {
            when {
                System.currentTimeMillis() < attributes.getL(GUARD_EATING_UNTIL) -> {
                    player(CALM_TALK, "Here, take a look at this.")
                    npc(npc, CALM_TALK, "Give us a minute to finish eating this rabbit.")
                }
                item.id != Obj.cooked_rabbit -> npc(npc, CALM_TALK, "I got no need for that.")
                else -> {
                    player(CALM_TALK, "Here, I cooked this for you.")
                    npc(npc, CALM_TALK, "You cooked me a rabbit! Thanks a lot. You don't mind watching things while I take 5 to eat it, do you?") {
                        if (player.inventory.containsItem(Obj.cooked_rabbit)) {
                            player.inventory.deleteItem(Obj.cooked_rabbit, 1)
                            attributes.setL(GUARD_EATING_UNTIL, System.currentTimeMillis() + 60000L)
                        }
                    }
                    player(CALM_TALK, "No problem.")
                }
            }
        }
    }
}
