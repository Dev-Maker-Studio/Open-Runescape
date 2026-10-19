package com.rs.game.content.quests.tearsofguthix

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

internal const val UNSTARTED = 0
internal const val MAKE_BOWL = 1
internal const val MINED_STONE = 2
internal const val MADE_BOWL = 3
internal const val COMPLETE = 4
internal val TOG = Quest.TEARS_OF_GUTHIX
internal val Player.tearsStage get() = getQuestStage(TOG)
internal val Player.tears get() = questManager.getAttribs(TOG)

@QuestHandler(quest = Quest.TEARS_OF_GUTHIX,
    startText = "Speak to Juna in the cavern beneath Lumbridge Swamp.",
    itemsText = "An oil-filled bullseye lantern, a cut sapphire, a tinderbox, a pickaxe and a chisel. Tool belt tools work. A rope may be needed to enter the swamp caves.",
    combatText = "None.",
    rewardsText = "1,000 Crafting XP<br>Access to the weekly Tears of Guthix activity",
    completedStage = COMPLETE)
class TearsOfGuthix : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        UNSTARTED -> listOf("I can start this quest by speaking to Juna in the cavern", "beneath Lumbridge Swamp.", "I need 44 quest points, 20 Crafting, 20 Mining and 49 Firemaking.")
        MAKE_BOWL -> listOf("I entertained Juna with stories of my adventures.", "She will let me collect the Tears of Guthix if I make a bowl.", "I must mine the magic stone on the south side of the chasm.", "A lit sapphire lantern will attract the light creatures.")
        MINED_STONE -> listOf("I mined some magic stone from the cave across the chasm.", "I need to use a chisel to craft it into a stone bowl.", "If I lose the stone, I can mine another piece.")
        MADE_BOWL -> listOf("I made a stone bowl. I should bring it to Juna.", "I can climb down the rocks on the east of the mining platform.")
        COMPLETE -> listOf("QUEST COMPLETE!", "Juna is keeping my bowl so I can collect the tears each week.", player.tearsAvailability() ?: "I may collect the Tears of Guthix now.")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun getRewardsString(player: Player): String = super.getRewardsString(player) +
        if (player.tearsStage == COMPLETE) "<br>" + (player.tearsAvailability() ?: "You may collect the Tears of Guthix now.") else ""

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVarBit(Varbit.tog_juna_bowl, when { stage >= COMPLETE -> 2; stage >= MAKE_BOWL -> 1; else -> 0 })
        player.vars.setVarBit(Varbit.tog_juna_stories, if (stage >= MAKE_BOWL) 1 else 0)
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Constants.CRAFTING, 1000.0)
        sendQuestCompleteInterface(player, Obj.tog_bowl)
    }
}

internal fun Player.canStartTears() = questManager.questPoints >= 44 &&
    skills.getLevelForXp(Constants.CRAFTING) >= 20 && skills.getLevelForXp(Constants.MINING) >= 20 &&
    skills.getLevelForXp(Constants.FIREMAKING) >= 49 && TOG.meetsReqs(this)
