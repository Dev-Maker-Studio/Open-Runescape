package com.rs.game.content.quests.dragonslayer

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.dialogue
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.dragonslayer.DragonSlayer.FINISHED_DIALOGUE_GUILDMASTER_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.KNOWS_ABOUT_DRAGON_BREATH_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.KNOWS_ABOUT_LOZAR_MAP_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.KNOWS_ABOUT_MELZAR_MAP_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.KNOWS_ABOUT_SHIP_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.KNOWS_ABOUT_THALZAR_MAP_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.KNOWS_MAP_EXISTS_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.NOT_STARTED
import com.rs.game.content.quests.dragonslayer.DragonSlayer.PREPARE_FOR_CRANDOR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.REPORT_TO_OZIACH
import com.rs.game.content.quests.dragonslayer.DragonSlayer.STARTED_DIALOGUE_GUILDMASTER_ATTR
import com.rs.game.content.quests.dragonslayer.DragonSlayer.TALK_TO_GUILDMASTER
import com.rs.game.content.quests.dragonslayer.DragonSlayer.TALK_TO_OZIACH
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private enum class Topic { ROUTE_TO_CRANDOR, WHERE_IS_THALZAR, WHERE_IS_LOZAR, WHERE_IS_MELZAR, BUYING_SHIP, DRAGON_BREATH }

class GuildMasterDragonSlayerD(player: Player) : Conversation(player, dialogue { guildMaster(player) })

private fun DialogueBuilder.guildMaster(player: Player) {
    val guildMaster = Npc.guildmaster
    val attribs = player.questManager.getAttribs(Quest.DRAGON_SLAYER)
    when (player.questManager.getStage(Quest.DRAGON_SLAYER)) {
        NOT_STARTED -> {
            player(HeadE.SKEPTICAL_THINKING, "Can I have a quest?")
            npc(guildMaster, HeadE.CALM_TALK, "Aha! Yes! A mighty and perilous quest fit for the most powerful champions! And, at the end of it, you will earn the right to wear the legendary rune platebody!")
            options("Start Dragon Slayer?") {
                op("Yes") {
                    player(HeadE.HAPPY_TALKING, "So, what is this quest?")
                    npc(guildMaster, HeadE.CALM_TALK, "You'll have to speak to Oziach, the maker of rune armour. He sets the quests that champions must complete in order to wear it. Oziach lives in a hut by the cliffs to the west of Edgeville. He can be a little ... odd ... sometimes, though.") {
                        player.questManager.setStage(Quest.DRAGON_SLAYER, TALK_TO_OZIACH)
                    }
                }
                op("No") {
                    player(HeadE.HAPPY_TALKING, "On second thoughts, a mighty and perilous quest fit only for the most powerful champions isn't something I want to commit to at the moment. Maybe later.")
                    npc(guildMaster, HeadE.CALM_TALK, "Very well. Return when you are ready")
                }
            }
        }
        TALK_TO_OZIACH -> {
            player(HeadE.HAPPY_TALKING, "Can I have a quest?")
            npc(guildMaster, HeadE.CALM_TALK, "You're already on a quest for me, if I recall correctly. Have you talked to Oziach yet?")
            player(HeadE.HAPPY_TALKING, "No, not yet.")
            npc(guildMaster, HeadE.CALM_TALK, "Well, he's the only one who can grant you the right to wear rune platemail. He lives in a hut, by the cliffs west of Edgeville.")
            player(HeadE.HAPPY_TALKING, "Okay I'll go and talk to him.")
        }
        TALK_TO_GUILDMASTER -> {
            if (!attribs.getB(STARTED_DIALOGUE_GUILDMASTER_ATTR)) {
                player(HeadE.HAPPY_TALKING, "I talked to Oziach ... and he gave me a quest.")
                npc(guildMaster, HeadE.CALM_TALK, "Oh? What did he tell you to do?")
                player(HeadE.HAPPY_TALKING, "Defeat the dragon of Crandor.")
                npc(guildMaster, HeadE.CALM_TALK, "The dragon of Crandor?")
                player(HeadE.HAPPY_TALKING, "Um, yes ...")
            }
            npc(guildMaster, HeadE.CALM_TALK, "Goodness, he hasn't given you an easy job, has he?")
            player(HeadE.HAPPY_TALKING, "What's so special about this dragon?")
            npc(guildMaster, HeadE.CALM_TALK, "Thirty years ago, Crandor was a thriving community with a great tradition of mages and adventurers. Many Crandorians even earned the right to be part of the Champions' Guild! One of their adventurers went too far, however.")
            npc(guildMaster, HeadE.CALM_TALK, "He descended into the volcano in the centre of Crandor and woke the dragon Elvarg. He must have fought valiantly against the dragon because they say that, to this day, she has a scar down her side.")
            npc(guildMaster, HeadE.CALM_TALK, "But the dragon still won the fight. She emerged and laid waste to the whole of Crandor with her fire breath! Some refugees managed to escape in fishing boats.")
            npc(guildMaster, HeadE.CALM_TALK, "They landed on the coast, north of Rimmington, and set up camp but the dragon followed them and burned the camp to the ground. Out of all of the people of Crandor there were only three survivors: a trio of wizards who used magic to escape.")
            npc(guildMaster, HeadE.CALM_TALK, "Their names were Thalzar, Lozar, and Melzar. If you're serious about taking on Elvarg, first you'll need to get to Crandor. The island is surrounded by dangerous reefs, so you'll need a ship capable of getting through them and a map to show you the way.")
            npc(guildMaster, HeadE.CALM_TALK, "When you reach Crandor, you'll also need some kind of protection against the dragon's breath.")
            exec {
                attribs.setB(STARTED_DIALOGUE_GUILDMASTER_ATTR, true)
                player.startConversation(mainOptions(player))
            }
        }
        PREPARE_FOR_CRANDOR -> {
            player(HeadE.HAPPY_TALKING, "About my quest to kill the dragon..")
            npc(guildMaster, HeadE.CALM_TALK, "If you're serious about taking on Elvarg, first you'll need to get to Crandor. The island is surrounded by dangerous reefs, so you'll need a ship capable of getting through them and a map to show you the way.")
            npc(guildMaster, HeadE.CALM_TALK, "When you reach Crandor, you'll also need some kind of protection against the dragon's breath.")
            exec { player.startConversation(mainOptions(player)) }
        }
        REPORT_TO_OZIACH -> {
            player(HeadE.HAPPY_TALKING, "I killed the dragon!")
            npc(guildMaster, HeadE.CALM_TALK, "Really? That's amazing! You are a true hero. There's no way that Oziach can refuse to sell you rune platemail now. You should tell him at once!")
        }
    }
}

private fun mainOptions(player: Player, previous: Topic? = null): Dialogue {
    val attribs = player.questManager.getAttribs(Quest.DRAGON_SLAYER)
    if (knowsEverything(player) && !attribs.getB(FINISHED_DIALOGUE_GUILDMASTER_ATTR)) {
        attribs.setB(FINISHED_DIALOGUE_GUILDMASTER_ATTR, true)
        player.questManager.setStage(Quest.DRAGON_SLAYER, PREPARE_FOR_CRANDOR)
    }
    return dialogue {
        options("Choose an option:") {
            if (attribs.getB(KNOWS_MAP_EXISTS_ATTR)) {
                if (previous != Topic.WHERE_IS_THALZAR)
                    op("Where is Thalzar's map piece?") { exec { player.startConversation(topic(player, Topic.WHERE_IS_THALZAR)) } }
                if (previous != Topic.WHERE_IS_LOZAR)
                    op("Where is Lozar's map piece?") { exec { player.startConversation(topic(player, Topic.WHERE_IS_LOZAR)) } }
                if (previous != Topic.WHERE_IS_MELZAR)
                    op("Where is Melzar's map piece?") { exec { player.startConversation(topic(player, Topic.WHERE_IS_MELZAR)) } }
            } else
                op("How can I find the route to Crandor?") { exec { player.startConversation(topic(player, Topic.ROUTE_TO_CRANDOR)) } }
            if (previous != Topic.BUYING_SHIP)
                op("Where can I find the right ship?") { exec { player.startConversation(topic(player, Topic.BUYING_SHIP)) } }
            if (previous != Topic.DRAGON_BREATH)
                op("How can I protect myself from the dragon's breath?") { exec { player.startConversation(topic(player, Topic.DRAGON_BREATH)) } }
            if (attribs.getB(FINISHED_DIALOGUE_GUILDMASTER_ATTR))
                op("Okay, I'll get going!") { player(HeadE.HAPPY_TALKING, "Okay, I'll get going!") }
        }
    }
}

private fun knowsEverything(player: Player): Boolean {
    val attribs = player.questManager.getAttribs(Quest.DRAGON_SLAYER)
    return attribs.getB(KNOWS_MAP_EXISTS_ATTR) && attribs.getB(KNOWS_ABOUT_THALZAR_MAP_ATTR) && attribs.getB(KNOWS_ABOUT_LOZAR_MAP_ATTR)
        && attribs.getB(KNOWS_ABOUT_MELZAR_MAP_ATTR) && attribs.getB(KNOWS_ABOUT_SHIP_ATTR) && attribs.getB(KNOWS_ABOUT_DRAGON_BREATH_ATTR)
}

private fun topic(player: Player, topic: Topic): Dialogue = dialogue {
    val guildMaster = Npc.guildmaster
    val attribs = player.questManager.getAttribs(Quest.DRAGON_SLAYER)
    when (topic) {
        Topic.ROUTE_TO_CRANDOR -> {
            player(HeadE.HAPPY_TALKING, "How can I find the route to Crandor?")
            npc(guildMaster, HeadE.CALM_TALK, "Only one map exists that shows the route through the reefs of Crandor. The map was split into three pieces by Melzar, Thalzar, and Lozar, the wizards who escaped from the dragon. Each of them took one piece.") {
                attribs.setB(KNOWS_MAP_EXISTS_ATTR, true)
            }
        }
        Topic.WHERE_IS_THALZAR -> {
            player(HeadE.HAPPY_TALKING, "Where is Thalzar's map piece?")
            npc(guildMaster, HeadE.CALM_TALK, "Thalzar was the most paranoid of the three wizards. He hid his map piece and took the secret of its location to his grave. I don't think you'd be able to find out where it is by ordinary means. You'll need to talk to the Oracle on Ice Mountain.") {
                attribs.setB(KNOWS_ABOUT_THALZAR_MAP_ATTR, true)
            }
        }
        Topic.WHERE_IS_LOZAR -> {
            player(HeadE.HAPPY_TALKING, "Where is Lozar's map piece?")
            npc(guildMaster, HeadE.CALM_TALK, "A few weeks ago, I'd have told you to speak to Lozar herself, in her house across the river from Lumbridge. Unfortunately, goblin raiders killed her, and stole everything. One of the goblins from the Goblin Village probably has the map piece now.") {
                attribs.setB(KNOWS_ABOUT_LOZAR_MAP_ATTR, true)
            }
        }
        Topic.WHERE_IS_MELZAR -> {
            player(HeadE.HAPPY_TALKING, "Where is Melzar's map piece?")
            npc(guildMaster, HeadE.CALM_TALK, "Melzar built a castle on the site of the Crandorian refugee camp, north of Rimmington. He's locked himself in there and no one's seen him for years. The inside of his castle is like a maze, and is populated by undead monsters.")
            npc(guildMaster, HeadE.CALM_TALK, "Maybe, if you could get all the way through the maze, you could find his piece of the map. Adventurers sometimes go in there to prove themselves, so I can give you this key to Melzar's Maze.") {
                if (!player.inventory.containsItem(Obj.melzarkey, 1))
                    player.inventory.addItem(Item(Obj.melzarkey, 1), true)
                attribs.setB(KNOWS_ABOUT_MELZAR_MAP_ATTR, true)
            }
        }
        Topic.BUYING_SHIP -> {
            player(HeadE.HAPPY_TALKING, "Where can I find the right ship?")
            npc(guildMaster, HeadE.CALM_TALK, "Even if you find the right route, only a ship made to the old crandorian design would be able to navigate through the reefs to the island. If there's still one in existence, it's probably in Port Sarim.")
            npc(guildMaster, HeadE.CALM_TALK, "Then, of course, you'll need to find a captain willing to sail to Crandor, and I'm not sure where you'd find one of them!") {
                attribs.setB(KNOWS_ABOUT_SHIP_ATTR, true)
            }
        }
        Topic.DRAGON_BREATH -> {
            player(HeadE.HAPPY_TALKING, "How can I protect myself from the dragon's breath?")
            npc(guildMaster, HeadE.CALM_TALK, "That part shouldn't be too difficult, actually, I believe the Duke of Lumbridge has a special shield in his armoury that is enchanted against dragon's breath.") {
                attribs.setB(KNOWS_ABOUT_DRAGON_BREATH_ATTR, true)
            }
        }
    }
    exec { player.startConversation(mainOptions(player, topic)) }
}
