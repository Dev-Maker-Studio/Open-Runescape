package com.rs.game.content.quests.the_grand_tree

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills

internal const val STAGE_UNSTARTED = 0
internal const val STAGE_STARTED = 5
internal const val STAGE_HAZELMERE_MESSAGE = 10
internal const val STAGE_DACONIA_WARNING = 20
internal const val STAGE_FIND_GLOUGH = 30
internal const val STAGE_GLOUGH_BLAMES_HUMANS = 40
internal const val STAGE_FIND_CHARLIE = 50
internal const val STAGE_SEARCH_GLOUGHS_HOME = 60
internal const val STAGE_ESCAPED_PRISON = 70
internal const val STAGE_REACHED_SHIPYARD = 80
internal const val STAGE_LUMBER_ORDER = 90
internal const val STAGE_GLOUGHS_KEY = 100
internal const val STAGE_INVASION_PLANS = 110
internal const val STAGE_OPENED_TRAPDOOR = 120
internal const val STAGE_DEMON_DEFEATED = 130
internal const val STAGE_FIND_DACONIA = 140
internal const val STAGE_DACONIA_FOUND = 150
internal const val STAGE_COMPLETE = 160

@QuestHandler(
    quest = Quest.GRAND_TREE,
    startText = "Talk to King Narnode Shareen in the Grand Tree.",
    itemsText = "None.",
    combatText = "You must be able to defeat a level 172 enemy.",
    rewardsText = "5 Quest Points<br>" +
            "7,900 Agility XP<br>" +
            "18,400 Attack XP<br>" +
            "2,150 Magic XP<br>" +
            "Access to the gnome gliders, spirit trees and the Grand Tree mine.",
    completedStage = STAGE_COMPLETE
)
class GrandTree : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        STAGE_UNSTARTED -> emptyList()
        STAGE_STARTED -> listOf("King Narnode has told me that the Grand Tree is dying and has asked me to help.")
        STAGE_HAZELMERE_MESSAGE -> listOf("I have taken a bark sample from the Grand tree from King Narnode to Hazelmere.")
        STAGE_DACONIA_WARNING -> listOf("Hazelmere took the bark sample and gave me a message that I translated and passed on to King Narnode. It seems that Daconia rocks are killing the Grand tree!")
        STAGE_FIND_GLOUGH -> listOf("On instructions from the King, I found Glough to tell him about the Daconia rocks.")
        STAGE_GLOUGH_BLAMES_HUMANS -> listOf("I told Glough about the Daconia rocks and he blames it on humans.")
        STAGE_FIND_CHARLIE -> listOf("I found Charlie, the prisoner, and talked to him.")
        STAGE_SEARCH_GLOUGHS_HOME -> listOf("I spoke to Charlie and agreed to search Glough's house, which is just in front of the Grand Tree.")
        STAGE_ESCAPED_PRISON -> listOf("Glough threw me in prison but the King freed me!")
        STAGE_REACHED_SHIPYARD -> listOf("I crash landed in Karamja jungle and made my way to the shipyard.")
        STAGE_LUMBER_ORDER -> listOf("I have an invoice that the foreman had. I need to find more proof of Glough's plans.")
        STAGE_GLOUGHS_KEY -> listOf("I asked Anita, Glough's girlfriend, for the keys to Glough's chest.")
        STAGE_INVASION_PLANS -> listOf("I found invasion plans in Glough's chest!")
        STAGE_OPENED_TRAPDOOR -> listOf("I used the twigs to open a trap door in Glough's house.")
        STAGE_DEMON_DEFEATED -> listOf("I defeated the Black Demon that Glough set on me! Glough fled like the coward he really is.")
        STAGE_FIND_DACONIA -> listOf("King Narnode has asked me to search the roots for the remaining Daconia rock.")
        STAGE_DACONIA_FOUND -> listOf("King Narnode has seen the light. I found the last of the Daconia rocks and the Grand Tree is safe.")
        STAGE_COMPLETE -> listOf("———QUEST COMPLETE!———")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.saveVar(Varp.grandtree, stage)
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Skills.AGILITY, 7900.0)
        player.skills.addXpQuest(Skills.ATTACK, 18400.0)
        player.skills.addXpQuest(Skills.MAGIC, 2150.0)
        player.vars.saveVarBit(Varbit.wof_earned_spins, player.vars.getVarBit(Varbit.wof_earned_spins) + 2)
        sendQuestCompleteInterface(player, Obj.grandtree_daconiarock)
    }
}
