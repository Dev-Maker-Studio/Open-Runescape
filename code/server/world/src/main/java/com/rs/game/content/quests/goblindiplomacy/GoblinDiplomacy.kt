package com.rs.game.content.quests.goblindiplomacy

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.content.items.Dye
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

private const val CRATE_SEARCHED_ATTR = "isGoblinMateCrateBlocked"

@QuestHandler(
    quest = Quest.GOBLIN_DIPLOMACY,
    startText = "Go to Goblin Village and talk to General Bentnoze or General Wartface.",
    itemsText = "Blue dye (or 2 woad leaves and 5 coins), red dye (or 3 redberries and 5 coins), and yellow dye (or 2 onions and 5 coins).",
    combatText = "None.",
    rewardsText = "200 Crafting XP<br>A gold bar",
    completedStage = 4
)
class GoblinDiplomacy : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> = when (stage) {
        0 -> listOf(
            "I can start this quest by speaking to General Bentnoze or",
            "General Wartface in the Goblin Village north of Falador."
        )
        1 -> listOf(
            "",
            "The Goblin generals need orange armour",
            "I can make orange armour by taking orange dye and using",
            "it on regular goblin mail.",
            "",
            "I can make orange dye by mixing red and yellow dye",
            "",
            "I can find goblin mail behind the generals hut",
            "",
            "Red dye is made from 3 red berries",
            "I could pay Aggie 5 coins in Draynor village for it",
            "",
            "I can find red berries by Varrock's south east mine",
            "",
            "Yellow dye can be made from 2 onions",
            "I could pay Aggie 5 coins in Draynor village for it",
            ""
        )
        2 -> listOf(
            "",
            "The Goblin generals need blue armour",
            "I can make blue armour by taking blue dye and using",
            "it on regular goblin mail.",
            "",
            "Blue dye can be made from 2 woad leaves",
            "I could pay Aggie 5 coins in Draynor village for it",
            "",
            "I can grow woad leaves from woad seeds or buy them",
            "from Wyson the gardner in Falador park"
        )
        3 -> listOf(
            "",
            "The Goblin generals need brown armour...",
            "That is just regular goblin mail...",
            ""
        )
        4 -> listOf("", "", "QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Skills.CRAFTING, 200.0)
        sendQuestCompleteInterface(player, Obj.goblin_armour)
    }
}

@ServerStartupEvent
fun mapGoblinDiplomacy() {
    onNpcClick(Npc.general_wartface_green, Npc.general_bentnoze_red) { talkToGenerals(it.player) }

    onObjectClick(Loc.goblin_ladder_bottom, Loc.goblin_ladder_top) { (player, obj) ->
        if (obj.id == Loc.goblin_ladder_bottom && player.plane == 0)
            player.ladder(Tile.of(2953, 3497, 2))
        if (obj.id == Loc.goblin_ladder_top && player.plane == 2)
            player.ladder(Tile.of(2953, 3497, 0))
    }

    onObjectClick(Loc.goblin_outpost_large_crate_armour1, Loc.goblin_outpost_large_crate_armour2, Loc.goblin_outpost_large_crate_armour3) { (player) ->
        val attribs = player.questManager.getAttribs(Quest.GOBLIN_DIPLOMACY)
        if (attribs.getB(CRATE_SEARCHED_ATTR)) {
            player.sendMessage("You search the crate but find nothing.")
            return@onObjectClick
        }
        attribs.setB(CRATE_SEARCHED_ATTR, true)
        player.inventory.addItem(Obj.goblin_armour, 1)
        player.startConversation(Dialogue().addItem(Obj.goblin_armour, "You find goblin mail."))
    }

    onItemOnItem(intArrayOf(Obj.goblin_armour), intArrayOf(Dye.ORANGE.id, Dye.BLUE.id)) { e ->
        if (e.usedWith(Dye.ORANGE.id, Obj.goblin_armour)) {
            e.player.inventory.replace(e.getUsedWith(Dye.ORANGE.id), Item(Obj.goblin_armour_orange, 1))
            e.player.inventory.deleteItem(Dye.ORANGE.id, 1)
        }
        if (e.usedWith(Dye.BLUE.id, Obj.goblin_armour)) {
            e.player.inventory.replace(e.getUsedWith(Dye.BLUE.id), Item(Obj.goblin_armour_darkblue, 1))
            e.player.inventory.deleteItem(Dye.BLUE.id, 1)
        }
    }
}

private fun talkToGenerals(player: Player) = player.startConversation {
    val wartface = Npc.general_wartface_green
    val bentnoze = Npc.general_bentnoze_red
    when (player.questManager.getStage(Quest.GOBLIN_DIPLOMACY)) {
        0 -> {
            npc(wartface, HeadE.CHILD_CALM_TALK, "Green armour best")
            npc(bentnoze, HeadE.CHILD_CALM_TALK, "No no red every time")
            npc(wartface, HeadE.CHILD_ANGRY, "Go away human, we busy")
            options {
                op("Why are you arguing about the colour of your armour?") {
                    player(HeadE.CALM_TALK, "Why are you arguing about the colour of your armour?")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "We decide to celebrate goblin new century")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "By changing the colour of our armour")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "Light blue get boring after a bit")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "And we want change")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "Problem is they want different changed to us")
                }
                op("Wouldn't you prefer peace?") {
                    player(HeadE.CALM_TALK, "Wouldn't you prefer peace?")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "Yeah peace is good as long as it's peace wearing green armour")
                    npc(bentnoze, HeadE.CHILD_CALM_TALK, "But green too much like skin!")
                    npc(bentnoze, HeadE.CHILD_CALM_TALK, "Nearly make you look naked!")
                }
                op("Do you want me to pick enter an armour colour for you?") {
                    player(HeadE.CALM_TALK, "Do you want me to pick an armour colour for you?")
                    player(HeadE.CALM_TALK, "Different to either green or red")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "Hmm me dunno what that'd look like")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "You'd have to bring me some, so us could decide")
                    npc(bentnoze, HeadE.CHILD_CALM_TALK, "Yep bring us orange armour")
                    npc(wartface, HeadE.CHILD_CALM_TALK, "Yep orange might be good") { player.questManager.setStage(Quest.GOBLIN_DIPLOMACY, 1) }
                }
            }
        }
        1 -> {
            npc(wartface, HeadE.CHILD_CALM_TALK, "Green armour best")
            npc(bentnoze, HeadE.CHILD_ANGRY, "No no red every time")
            npc(wartface, HeadE.CHILD_ANGRY, "Go away human,we busy")
            npc(wartface, HeadE.CHILD_CALM_TALK, "Oh it you")
            if (player.inventory.containsItems(Item(Obj.goblin_armour_orange, 1))) {
                player(HeadE.CALM_TALK, "I have some orange armour")
                simple("You give the armour to the goblins")
                npc(wartface, HeadE.CHILD_CALM_TALK, "No I don't like that much")
                npc(bentnoze, HeadE.CHILD_CALM_TALK, "It clashes with my skin colour")
                npc(wartface, HeadE.CHILD_CALM_TALK, "Try bringing us blue armour") {
                    player.inventory.deleteItem(Item(Obj.goblin_armour_orange, 1))
                    player.questManager.setStage(Quest.GOBLIN_DIPLOMACY, 2)
                }
            } else {
                npc(wartface, HeadE.CHILD_CALM_TALK, "Have you got some orange goblin armour yet?")
                player(HeadE.CALM_TALK, "Err no")
                npc(wartface, HeadE.CHILD_CALM_TALK, "Come back when you have some")
            }
        }
        2 -> {
            npc(wartface, HeadE.CHILD_CALM_TALK, "Green armour best")
            npc(bentnoze, HeadE.CHILD_ANGRY, "No no red every time")
            npc(wartface, HeadE.CHILD_ANGRY, "Go away human,we busy")
            npc(wartface, HeadE.CHILD_CALM_TALK, "Oh it you")
            if (player.inventory.containsItems(Item(Obj.goblin_armour_darkblue, 1))) {
                player(HeadE.CALM_TALK, "I have some blue armour")
                simple("You give the armour to the goblins")
                npc(wartface, HeadE.CHILD_CALM_TALK, "No I don't like that much")
                npc(bentnoze, HeadE.CHILD_CALM_TALK, "It clashes with my skin colour")
                npc(wartface, HeadE.CHILD_CALM_TALK, "Try bringing us brown armour") {
                    player.inventory.deleteItem(Item(Obj.goblin_armour_darkblue, 1))
                    player.questManager.setStage(Quest.GOBLIN_DIPLOMACY, 3)
                }
            } else {
                npc(wartface, HeadE.CHILD_CALM_TALK, "Have you got some blue goblin armour yet?")
                player(HeadE.CALM_TALK, "Err no")
                npc(wartface, HeadE.CHILD_CALM_TALK, "Come back when you have some")
            }
        }
        3 -> {
            npc(wartface, HeadE.CHILD_CALM_TALK, "Green armour best")
            npc(bentnoze, HeadE.CHILD_ANGRY, "No no red every time")
            npc(wartface, HeadE.CHILD_ANGRY, "Go away human,we busy")
            npc(wartface, HeadE.CHILD_CALM_TALK, "Oh it you")
            if (player.inventory.containsItems(Item(Obj.goblin_armour, 1))) {
                player(HeadE.CALM_TALK, "I have some brown armour")
                simple("You give the armour to the goblins")
                npc(wartface, HeadE.CHILD_CALM_TALK, "That colour quiet nice. Me can see myself wearing this")
                npc(bentnoze, HeadE.CHILD_CALM_TALK, "It a deal then. Brown armour it is")
                npc(wartface, HeadE.CHILD_CALM_TALK, "Thank you for sorting out our argument. Take this gold bar as reward!")
                exec {
                    player.inventory.deleteItem(Item(Obj.goblin_armour, 1))
                    player.inventory.addItem(Item(Obj.gold_bar, 1), true)
                    player.questManager.completeQuest(Quest.GOBLIN_DIPLOMACY)
                }
            } else {
                player(HeadE.CALM_TALK, "Err no")
                npc(wartface, HeadE.CHILD_ANGRY, "Come back when you have some")
            }
        }
        4 -> {
            npc(wartface, HeadE.CHILD_CALM_TALK, "Now you've solved our argument we gotta think of something else to do")
            npc(bentnoze, HeadE.CHILD_CALM_TALK, "Yep, we bored now")
        }
    }
}
