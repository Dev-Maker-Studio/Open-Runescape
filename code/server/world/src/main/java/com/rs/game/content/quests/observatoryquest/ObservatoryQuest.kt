package com.rs.game.content.quests.observatoryquest

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

const val STAGE_UNSTARTED = 0
const val STAGE_GATHERING_PLANKS = 1
const val STAGE_GETTING_BRONZE = 2
const val STAGE_GETTING_MOLTEN_GLASS = 3
const val STAGE_FINDING_LENS_MOULD = 4
const val STAGE_MAKING_LENS = 5
const val STAGE_TRAVELLING_TO_OBSERVATORY = 6
const val STAGE_OBSERVING_CONSTELLATION = 7
const val STAGE_COMPLETE = 8

@QuestHandler(
    quest = Quest.OBSERVATORY_QUEST,
    startText = "Speak to the Observatory professor at the Observatory reception, south-west of Ardougne.",
    itemsText = "3 planks, a bronze bar and molten glass.",
    combatText = "You may need to defeat a goblin guard.",
    rewardsText = "2,250 Crafting XP<br>An uncut sapphire<br>A reward based on the constellation observed",
    completedStage = STAGE_COMPLETE
)
class ObservatoryQuest : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> = when (stage) {
        STAGE_UNSTARTED -> listOf("I can start this quest by speaking to the Observatory", "professor at the Observatory reception, south-west of Ardougne.")
        STAGE_GATHERING_PLANKS -> listOf("The Observatory professor needs materials to repair his telescope.", "First, he has asked me to bring him three planks of wood", "for the telescope's base.")
        STAGE_GETTING_BRONZE -> listOf("I have given the professor the three planks.", "He now needs a bronze bar for the telescope's tube.")
        STAGE_GETTING_MOLTEN_GLASS -> listOf("I have given the professor the bronze bar.", "He now needs some molten glass to make a new lens.")
        STAGE_FINDING_LENS_MOULD -> listOf("I brought the professor some molten glass, but one of the", "goblins has stolen his lens mould.", "The professor's assistant suggested I search the dungeon", "under the goblin settlement.")
        STAGE_MAKING_LENS -> listOf("I recovered the lens mould from the goblins.", "I need to use the mould with molten glass to make", "a new lens for the Observatory telescope.")
        STAGE_TRAVELLING_TO_OBSERVATORY -> listOf("I have made the new lens and given it to the professor.", "He has gone ahead to the Observatory dome.", "Since the bridge is broken, I need to travel through", "the dungeon beneath the goblin settlement to reach him.")
        STAGE_OBSERVING_CONSTELLATION -> listOf("The professor has repaired the Observatory telescope.", "He wants me to look through it and identify the", "constellation I can see.", "The star charts downstairs may help me identify it.")
        STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVarBit(Varbit.observatory_cutscene_seen, if (stage >= STAGE_OBSERVING_CONSTELLATION) 1 else 0)
        player.vars.setVarBit(Varbit.observatory_scopelooked, if (stage >= STAGE_COMPLETE || hasViewedObservatoryConstellation(player)) 1 else 0)
        val mouldTaken = stage >= STAGE_TRAVELLING_TO_OBSERVATORY || player.inventory.containsItem(Obj.lens_mould, 1) || player.bank.containsItem(Obj.lens_mould, 1) || player.inventory.containsItem(Obj.lens, 1) || player.bank.containsItem(Obj.lens, 1)
        player.vars.setVarBit(Varbit.observatory_mould_pres, if (mouldTaken) 1 else 0)
    }

    override fun complete(player: Player) {
        giveObservatoryConstellationReward(player, getOrAssignObservatoryConstellation(player))
        player.skills.addXpQuest(Skills.CRAFTING, 2250.0)
        sendQuestCompleteInterface(player, Obj.lens_mould)
    }
}
