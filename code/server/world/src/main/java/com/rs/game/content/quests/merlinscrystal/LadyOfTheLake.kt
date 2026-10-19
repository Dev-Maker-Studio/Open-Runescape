package com.rs.game.content.quests.merlinscrystal

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.achievements.AchievementDef
import com.rs.game.content.achievements.SetReward
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapLadyOfTheLake() {
    onNpcClick(Npc.npc_250) { (player) -> talkToLadyOfTheLake(player) }
}

private fun talkToLadyOfTheLake(player: Player) {
    val stage = player.questManager.getStage(Quest.MERLINS_CRYSTAL)
    val hasExcalibur = player.inventory.containsItem(Obj.excalibur)
    player.startConversation {
        npc(Npc.npc_250, HeadE.CALM_TALK, "Good day to you")
        options("Choose an option:") {
            op("Who are you?") {
                player(HeadE.HAPPY_TALKING, "Who are you?")
                npc(Npc.npc_250, HeadE.CALM_TALK, "I am the Lady of the Lake.")
            }
            if (stage == MerlinsCrystal.THE_BLACK_CANDLE)
                op("About Merlin's Crystal") {
                    player(HeadE.AMAZED, "I should probably get the black candle before the excalibur...")
                    npc(Npc.npc_250, HeadE.CALM_TALK, "What?")
                    player(HeadE.HAPPY_TALKING, "Oh nothing, just notes to myself...")
                }
            if (stage == MerlinsCrystal.OBTAINING_EXCALIBUR)
                op("I seek the sword Excalibur") {
                    player(HeadE.HAPPY_TALKING, "I seek the sword Excalibur")
                    npc(Npc.npc_250, HeadE.CALM_TALK, "Aye, I have that artefact in my possession. 'Tis very valuable, and not an artefact to be given away lightly. I would want to give it away only to the one who is worthy and good.")
                    player(HeadE.HAPPY_TALKING, "And how am I meant to prove that?")
                    npc(Npc.npc_250, HeadE.CALM_TALK, "I shall set a test for you. First I need you to travel to Port Sarim. Then go to the upstairs room of the jeweller' shop there.") {
                        player.questManager.getAttribs(Quest.MERLINS_CRYSTAL).setB("LADY_TEST", true)
                    }
                    player(HeadE.HAPPY_TALKING, "Okay, that seems easy enough.")
                }
            if (stage >= MerlinsCrystal.PERFORM_RITUAL && !hasExcalibur)
                op("I lost Excalibur...") {
                    player(HeadE.SAD, "I lost Excalibur...")
                    npc(Npc.npc_250, HeadE.CALM_TALK, "I found it!")
                    player(HeadE.AMAZED, "Really!?")
                    npc(Npc.npc_250, HeadE.FRUSTRATED, "Yes, please be worthy of it.")
                    item(Obj.excalibur, "She gives you the sword") { player.inventory.addItem(Obj.excalibur, 1) }
                }
            if (hasExcalibur && SetReward.SEERS_HEADBAND.hasRequirements(player, AchievementDef.Area.SEERS, AchievementDef.Difficulty.HARD, false))
                op("Sir Kay said you'd reward me.") {
                    player(HeadE.HAPPY_TALKING, "Sir Kay said you'd reward me.")
                    npc(Npc.npc_250, HeadE.CALM_TALK, "Indeed, I hear that you have earned his trust. You shall be rewarded handsomely!")
                    item(Obj.obj_14632, "The Lady of the Lake enhances Excalibur.") {
                        if (player.inventory.containsItem(Obj.excalibur)) {
                            player.inventory.deleteItem(Obj.excalibur, 1)
                            player.inventory.addItem(Obj.obj_14632, 1)
                        }
                    }
                }
            op("Good day.") { player(HeadE.HAPPY_TALKING, "Good day.") }
        }
    }
}
