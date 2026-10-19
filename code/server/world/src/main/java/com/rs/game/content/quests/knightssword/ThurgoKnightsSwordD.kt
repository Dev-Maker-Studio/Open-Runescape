package com.rs.game.content.quests.knightssword

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.dialogue
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.knightssword.KnightsSword.FIND_DWARF
import com.rs.game.content.quests.knightssword.KnightsSword.GET_MATERIALS
import com.rs.game.content.quests.knightssword.KnightsSword.GET_PICTURE
import com.rs.game.content.quests.knightssword.KnightsSword.QUEST_COMPLETE
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val GAVE_PIE_ATTR = "gave_thurgo_pie"
private const val MADE_SWORD_ATTR = "made_sword"

class ThurgoKnightsSwordD(player: Player) : Conversation(player, dialogue { thurgo(player) })

private fun DialogueBuilder.thurgo(player: Player) {
    val thurgo = Npc.npc_604
    val attribs = player.questManager.getAttribs(Quest.KNIGHTS_SWORD)
    when (player.questManager.getStage(Quest.KNIGHTS_SWORD)) {
        FIND_DWARF -> when {
            attribs.getB(GAVE_PIE_ATTR) -> {
                player(HeadE.HAPPY_TALKING, "Can you make a special sword for me?")
                npc(thurgo, HeadE.CALM_TALK, "Well, after bringing me my favourite food I guess I should give it a go. What sort of sword is it?")
                player(HeadE.HAPPY_TALKING, "I need you to make a sword for one of Falador's knights. He had one which was passed down through five generations, but his squire has lost it.")
                player(HeadE.HAPPY_TALKING, "So we need an identical one to replace it.")
                npc(thurgo, HeadE.CALM_TALK, "A knight's sword eh? Well, I'd need to know exactly how it looked before I could make a new one.")
                npc(thurgo, HeadE.CALM_TALK, "All the Faladian knights used to have swords with unique designs according to their position. Could you bring me a picture or something?")
                player(HeadE.HAPPY_TALKING, "I'll go and ask his squire and see if I can find one.") { player.questManager.setStage(Quest.KNIGHTS_SWORD, GET_PICTURE) }
            }
            !player.inventory.containsItem(Obj.redberry_pie) -> {
                player(HeadE.HAPPY_TALKING, "Are you an Imcando dwarf? I need a special sword.")
                npc(thurgo, HeadE.CALM_TALK, "I don't talk about that sort of thing anymore. I'm getting old.")
                player(HeadE.HAPPY_TALKING, "I'll come back another time.")
            }
            else -> options("Choose an option:") {
                op("Are you an Imcando dwarf? I need a special sword.") {
                    player(HeadE.HAPPY_TALKING, "Are you an Imcando dwarf? I need a special sword.")
                    npc(thurgo, HeadE.CALM_TALK, "I don't talk about that sort of thing anymore. I'm getting old.")
                    options("Choose an option:") {
                        op("Would you like a redberry pie?") { giveRedberryPie(player, thurgo) }
                        op("I'll come back another time.") {
                            player(HeadE.HAPPY_TALKING, "I'll come back another time.")
                        }
                    }
                }
                op("Would you like a redberry pie?") { giveRedberryPie(player, thurgo) }
            }
        }
        GET_PICTURE -> {
            npc(thurgo, HeadE.CALM_TALK, "Did you get the picture of the sword?")
            if (player.inventory.containsItem(Obj.knights_portrait)) {
                player(HeadE.HAPPY_TALKING, "Yes, I got it here!")
                npc(thurgo, HeadE.CALM_TALK, "Great, let me take a look at it")
                simple("Thrugo looks at the portrait...")
                npc(thurgo, HeadE.CALM_TALK, "I can make this.")
                player(HeadE.HAPPY_TALKING, "Really? Great!")
                npc(thurgo, HeadE.CALM_TALK, "I am going to need 1 blurite ore and 2 iron bars though")
                player(HeadE.HAPPY_TALKING, "Awesome I will fetch that.")
                npc(thurgo, HeadE.CALM_TALK, "Great.")
                player(HeadE.HAPPY_TALKING, "But, where do I find blurite ore?")
                npc(thurgo, HeadE.CALM_TALK, "Oh there is some underground around here by all the ice.")
                player(HeadE.HAPPY_TALKING, "Great... I have to go underground?")
                npc(thurgo, HeadE.CALM_TALK, "Yep.") {
                    player.inventory.removeItems(Item(Obj.knights_portrait, 1))
                    player.questManager.setStage(Quest.KNIGHTS_SWORD, GET_MATERIALS)
                }
            } else
                player(HeadE.SAD, "Not yet...")
        }
        GET_MATERIALS -> {
            if (player.inventory.containsItem(Obj.faladian_sword)) {
                player(HeadE.HAPPY_TALKING, "Thank you for the sword!")
                npc(thurgo, HeadE.CALM_TALK, "No problem!")
                return
            }
            if (attribs.getB(MADE_SWORD_ATTR))
                player(HeadE.SAD, "I lost the sword! Can I have another?")
            npc(thurgo, HeadE.CALM_TALK, "Do you have the materials?")
            if (hasSwordMaterials(player)) {
                player(HeadE.HAPPY_TALKING, "Yes, I have them right here!")
                npc(thurgo, HeadE.CALM_TALK, "Great, okay I will get started...")
                simple("Thurgo makes you a blurite sword") {
                    makeSword(player)
                    attribs.setB(MADE_SWORD_ATTR, true)
                }
            } else {
                player(HeadE.HAPPY_TALKING, "No, not yet.")
                simple("You need 1 blurite ore and 2 iron bars...")
            }
        }
        QUEST_COMPLETE -> {
            player(HeadE.HAPPY_TALKING, "Can you make me another of Sir Vyvin's swords?")
            npc(thurgo, HeadE.CALM_TALK, "You want that knight's sword again? I suppose you brought me a lovely pie, so I don't mind. I'll need a blurite ore and two iron bars, like before.")
            if (hasSwordMaterials(player))
                simple("Thurgo makes you another blurite sword") { makeSword(player) }
            else
                simple("You need 1 blurite ore and 2 iron bars...")
        }
    }
}

private fun DialogueBuilder.giveRedberryPie(player: Player, thurgo: Int) {
    player(HeadE.HAPPY_TALKING, "Would you like a redberry pie?")
    simple("You see Thurgo's eyes light up.")
    npc(thurgo, HeadE.HAPPY_TALKING, "I'd never say no to a redberry pie! We Imcando dwarves love them - they're GREAT!")
    simple("You hand over the pie.")
    simple("Thurgo eats the pie.")
    simple("Thurgo pats his stomach.")
    npc(thurgo, HeadE.HAPPY_TALKING, "By Guthix! THAT was good pie! Anyone who makes pie like THAT has got to be alright!") {
        player.inventory.removeItems(Item(Obj.redberry_pie, 1))
        player.questManager.getAttribs(Quest.KNIGHTS_SWORD).setB(GAVE_PIE_ATTR, true)
    }
}

private fun hasSwordMaterials(player: Player) =
    player.inventory.containsItem(Obj.blurite_ore, 1) && player.inventory.containsItem(Obj.iron_bar, 2)

private fun makeSword(player: Player) {
    player.inventory.removeItems(Item(Obj.blurite_ore, 1), Item(Obj.iron_bar, 2))
    player.inventory.addItem(Item(Obj.faladian_sword, 1))
}
