package com.rs.game.content.quests.familycrest.dialogues

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.familycrest.GIVE_AVAN_JEWLERY
import com.rs.game.content.quests.familycrest.KILL_CHRONOZON
import com.rs.game.content.quests.familycrest.NOT_STARTED
import com.rs.game.content.quests.familycrest.QUEST_COMPLETE
import com.rs.game.content.quests.familycrest.TALK_TO_AVAN
import com.rs.game.content.quests.familycrest.TALK_TO_BOOT
import com.rs.game.content.quests.familycrest.TALK_TO_CALEB
import com.rs.game.content.quests.familycrest.TALK_TO_GEM_TRADER
import com.rs.game.content.quests.familycrest.TALK_TO_JOHNATHAN
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

@ServerStartupEvent
fun mapAvanFamilyCrestD() {
    onNpcClick(Npc.avan) { (player) -> talkToAvan(player) }
}

private fun talkToAvan(player: Player) = player.startConversation {
    val avan = Npc.avan
    when (player.questManager.getStage(Quest.FAMILY_CREST)) {
        NOT_STARTED, TALK_TO_CALEB, TALK_TO_GEM_TRADER -> {
            npc(avan, HeadE.CALM_TALK, "What? Can't you see I'm busy?")
            player(HeadE.HAPPY_TALKING, "Well, sooooorry...")
        }
        TALK_TO_AVAN -> options("Choose an option:") {
            op("Why are you lurking around a scorpion pit?") {
                player(HeadE.HAPPY_TALKING, "Why are you lurking around a scorpion pit?")
                npc(avan, HeadE.CALM_TALK, "It's a good place to find gold...")
            }
            op("I'm looking for a man... his name is Avan Fitzharmon.") {
                player(HeadE.HAPPY_TALKING, "I'm looking for a man... his name is Avan Fitzharmon.")
                npc(avan, HeadE.CALM_TALK, "Then you have found him. My name is Avan Fitzharmon.")
                player(HeadE.HAPPY_TALKING, "You have a part of your family crest. I am on a quest to retrieve all of the fragmented pieces and restore the crest.")
                npc(avan, HeadE.CALM_TALK, "Ha! I suppose one of my worthless brothers has sent you on this quest then?")
                player(HeadE.HAPPY_TALKING, "No, it was your father who has asked me to do this for him.")
                npc(avan, HeadE.CALM_TALK, "My... my father wishes this? Then that is a different matter. I will let you have my crest shard, adventurer, but you must first do something for me.")
                npc(avan, HeadE.CALM_TALK, "There is a certain lady I am trying to impress. As a man of noble birth, I can not give her just any gold trinket to show my devotion.")
                npc(avan, HeadE.CALM_TALK, "What I intend to give her, is a golden ring, embedded with the finest precious red stone available, and a necklace to match this ring")
                npc(avan, HeadE.CALM_TALK, "The problem however for me, is that not just any old gold will be suitable. I seek only the purest, the most high quality of gold - what I seek, if you will, is perfect gold.")
                npc(avan, HeadE.CALM_TALK, "None of the gold around here is even remotely suitable in terms of quality. I have searched far and wide for the perfect gold I desire")
                npc(avan, HeadE.CALM_TALK, "but have had no success so in finding it I am afraid. If you can find me my perfect gold, make a ring and necklace from it,")
                npc(avan, HeadE.CALM_TALK, "and add rubies to them, I will gladly hand over my fragment of the family crest to you.")
                player(HeadE.HAPPY_TALKING, "Can you give me any help on finding this 'perfect gold'?")
                npc(avan, HeadE.CALM_TALK, "I thought I had found a solid lead on its whereabouts when I heard of a dwarf who is an expert on gold who goes by the name of 'Boot'.")
                npc(avan, HeadE.CALM_TALK, "Unfortunately he has apparently returned to this home, somewhere in the mountains, and I have no idea how to find him.")
                player(HeadE.HAPPY_TALKING, "Well, I'll see what I can do") { player.questManager.setStage(Quest.FAMILY_CREST, TALK_TO_BOOT) }
            }
        }
        TALK_TO_BOOT -> {
            npc(avan, HeadE.CALM_TALK, "So how are you doing getting me my perfect gold jewellery?")
            player(HeadE.HAPPY_TALKING, "I'm still after that 'perfect gold'.")
            npc(avan, HeadE.CALM_TALK, "I know how you feel... for such a long time I have searched and searched for the elusive perfect gold...")
            npc(avan, HeadE.CALM_TALK, "I thought I had gotten a good lead on finding it when I heard talk of a dwarven expert on gold named Boot some time back, but unfortunately for me, he has returned to his mountain home, which I cannot find.")
        }
        GIVE_AVAN_JEWLERY -> {
            npc(avan, HeadE.CALM_TALK, "So how are you doing getting me my perfect gold jewellery?")
            if (player.inventory.containsItems(intArrayOf(Obj.obj_774, Obj.obj_773), intArrayOf(1, 1))) {
                player(HeadE.HAPPY_TALKING, "I have the ring and necklace right here.")
                simple("You show Avan the jewlery")
                npc(avan, HeadE.CALM_TALK, "These... these are exquisite! EXACTLY what I was searching for all of this time! Please, take my crest fragment!")
                simple("You exchange the jewlery for the family crest with Avan") {
                    player.inventory.removeItems(Item(Obj.obj_773, 1), Item(Obj.obj_774))
                    player.inventory.addItem(Obj.caleb_crest, 1)
                    player.questManager.setStage(Quest.FAMILY_CREST, TALK_TO_JOHNATHAN)
                }
                directionsToJohnathon(avan)
            } else {
                player(HeadE.HAPPY_TALKING, "I'm still after that 'perfect gold'.")
                npc(avan, HeadE.CALM_TALK, "I know how you feel... for such a long time I have searched and searched for the elusive perfect gold...")
            }
        }
        TALK_TO_JOHNATHAN, KILL_CHRONOZON -> {
            if (!player.inventory.containsItem(Obj.caleb_crest, 1) && !player.inventory.containsItem(Obj.family_crest)) {
                if (player.inventory.hasFreeSlots()) {
                    npc(avan, HeadE.CALM_TALK, "You lost this.")
                    simple("Avan gives you back his crest...") { player.inventory.addItem(Obj.caleb_crest, 1) }
                } else {
                    npc(avan, HeadE.CALM_TALK, "I have my crest if you had room for it")
                    player(HeadE.HAPPY_TALKING, "Okay.")
                }
            } else
                directionsToJohnathon(avan)
        }
        QUEST_COMPLETE -> {
            npc(avan, HeadE.CALM_TALK, "I have heard word from my father, thank you for helping to restore our family honour")
            if (player.inventory.containsItem(Obj.steel_gauntlets, 1)) {
                player(HeadE.HAPPY_TALKING, "Your father said that you could improve these Gauntlets in some way for me")
                npc(avan, HeadE.CALM_TALK, "Yes, I can. In my quest to find the perfect gold I learned a lot; I can make it so when you're wearing these you gain more experience when smithing gold.")
                options("Choose an option:") {
                    op("That sounds good, improve them for me") {
                        player(HeadE.HAPPY_TALKING, "That sounds good, improve them for me")
                        simple("Avan takes out a little hammer")
                        simple("He starts pounding on the gauntlets")
                        simple("Avan hands the gauntlets to you") {
                            player.inventory.removeItems(Item(Obj.steel_gauntlets, 1))
                            player.inventory.addItem(Obj.obj_776, 1)
                        }
                    }
                    op("I think I'll check my other options with your brothers") {
                        player(HeadE.HAPPY_TALKING, "I think I'll check my other options with your brothers")
                        npc(avan, HeadE.CALM_TALK, "Ok if you insist on getting help from the likes of them")
                    }
                }
            } else
                player(HeadE.HAPPY_TALKING, "You're welcome!")
        }
    }
}

private fun DialogueBuilder.directionsToJohnathon(avan: Int) {
    npc(avan, HeadE.CALM_TALK, "Now, I suppose you will be wanting to find my brother Johnathon who is in possession of the final piece of the family's crest?")
    player(HeadE.HAPPY_TALKING, "That's correct.")
    npc(avan, HeadE.CALM_TALK, "Well, the last I heard of my brother Johnathon, he was studying the magical arts, and trying to hunt some demon or other out in The Wilderness.")
    npc(avan, HeadE.CALM_TALK, "Unsurprisingly, I do not believe he is doing a particularly good job of things, and spends most of his time recovering from his injuries")
    npc(avan, HeadE.CALM_TALK, "in some tavern or other near the eastern edge of The Wilderness. You'll probably find him still there.")
    player(HeadE.HAPPY_TALKING, "Thanks Avan.")
}
