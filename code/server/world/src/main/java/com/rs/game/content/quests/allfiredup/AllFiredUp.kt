package com.rs.game.content.quests.allfiredup

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants

@QuestHandler(quest = Quest.ALL_FIRED_UP,
    startText = "Talk to King Roald in the Varrock Palace Throne room.",
    completedStage = 7,
    combatText = "None",
    rewardsText =
                "5500 Firemaking XP," +
                "<br>20,000GP," +
                "<br>Access to the All Fired UP Minigame",
    itemsText = "2 sets of 20 of the same logs, 1 set of 5 of the same logs.")

class AllFiredUp : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> {
        val lines = ArrayList<String>()
        when (stage) {
            0 -> {
                lines.add("I can start this quest by speaking to King Roald")
                lines.add("in the Varrock palace.")
            }

            1 -> {
                lines.add("I spoke with Raold, and he wants me to test his beacon network.")
                lines.add("I should speak to Blaze Sharpeye near the temple.")
            }

            2 -> {
                lines.add("I spoke with Blaze, and to test the beacon I need to load it with 20 logs")
                lines.add("and light it using a tinderbox.")
            }

            3 -> lines.add("I lit the beacon with ease.")
            4 -> {
                lines.add("I spoke with Blaze, he has asked me to test another beacon.")
                lines.add("I should go speak with Squire Fyre near the Limestone Quarry.")
            }

            5 -> lines.add("I lit the second beacon with ease. I should go make sure Blaze can see it.")
            6 -> {
                lines.add("I checked in with Blaze who could see the beacon clearly.")
                lines.add("I should let Roald know the beacons are working.")
            }

            7 -> lines.add("QUEST COMPLETED")
            else -> lines.add("Invalid quest stage. Report this to an administrator.")
        }
        return lines
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Constants.FIREMAKING, 5500.0)
        player.inventory.addCoins(20000)
        sendQuestCompleteInterface(player, 13661)
    }
}