package com.rs.game.content.quests.rovingelves

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.content.ItemConstants.ItemDegrade
import com.rs.game.content.items.DEGRADE_CHARGES_KEY
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Item

internal const val UNSTARTED = 0
internal const val HELP = 1
internal const val FIND_SEED = 2
internal const val ENCHANTED = 3
internal const val PLANTED = 4
internal const val COMPLETE = 5
internal val ROVING = Quest.ROVING_ELVES
internal val Player.rovingStage get() = getQuestStage(ROVING)
internal val Player.roving get() = questManager.getAttribs(ROVING)

@QuestHandler(quest = Quest.ROVING_ELVES,
    startText = "Speak to Islwyn in one of the clearings east of Port Tyras or south-west of Lletya.",
    itemsText = "Glarial's pebble, Glarial's amulet, rope, a key to the Waterfall Dungeon and a spade (tool belt works).",
    combatText = "You must defeat a level 84 Moss giant without weapons, armour, runes, familiars or prayer.",
    rewardsText = "10,000 Strength XP<br>A partially charged crystal bow or crystal shield<br>Ability to buy crystal equipment and have seeds re-enchanted",
    completedStage = COMPLETE)
class RovingElves : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        UNSTARTED -> listOf("I can start this quest by talking to Islwyn in Isafdar.", "He travels with Eluned between two forest clearings.", "I must have completed Regicide and Waterfall Quest.")
        HELP -> listOf("Islwyn's grandmother Glarial cannot rest in peace.", "I offered to consecrate her new resting place under the waterfall.", "I should ask Eluned how to perform the ritual.")
        FIND_SEED -> listOf("Eluned told me to recover Glarial's consecration seed.", "I must defeat a Moss giant in Glarial's old tomb.", "I cannot take weapons, armour, runes or a familiar into the tomb.", "Then I should take the seed to Eluned to be re-enchanted.")
        ENCHANTED -> listOf("Eluned enchanted Glarial's consecration seed.", "I must plant it close to her remains in the Waterfall Dungeon.", "I will need a spade. Eluned can replace a lost enchanted seed.")
        PLANTED -> listOf("I planted the enchanted seed in Glarial's new resting place.", "I should return to Islwyn in Isafdar.")
        COMPLETE -> listOf("QUEST COMPLETE!", "Glarial and Islwyn can finally rest in peace.")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVar(Varp.varp_402, when (stage) { UNSTARTED -> 0; HELP -> 2; FIND_SEED -> 3; ENCHANTED -> 4; PLANTED -> 5; else -> 6 })
        if (stage == UNSTARTED) player.vars.setVarBit(Varbit.roving_entered_waterfall_to_plant_seed, 0)
    }

    override fun complete(player: Player) {
        val bow = player.tempAttribs.getB("rovingBowReward")
        val degrade = if (bow) ItemDegrade.CRYSTAL_BOW_NEW else ItemDegrade.CRYSTAL_SHIELD_NEW
        val item = Item(if (bow) Obj.crystal_bow_deg else Obj.crystal_shield_deg, 1).addMetaData(DEGRADE_CHARGES_KEY, degrade.defaultCharges / 5)
        player.tempAttribs.removeO<Any>("rovingBowReward")
        player.roving.setB("bowReward", bow)
        player.inventory.addItem(item)
        player.skills.addXpQuest(Skills.STRENGTH, 10000.0)
        sendQuestCompleteInterface(player, item.id)
    }
}

internal fun Player.canStartRoving() = isQuestComplete(Quest.REGICIDE) && isQuestComplete(Quest.WATERFALL_QUEST)
