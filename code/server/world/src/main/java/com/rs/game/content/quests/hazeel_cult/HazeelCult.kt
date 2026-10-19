package com.rs.game.content.quests.hazeel_cult

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.content.quests.hazeel_cult.objects.refreshCellarWall
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin

internal const val STAGE_UNSTARTED = 0
internal const val STAGE_FIND_CLIVET = 1
internal const val STAGE_CHOOSE_SIDE = 2
internal const val STAGE_BRANCH_TASK = 3
internal const val STAGE_BRANCH_PROGRESS = 4
internal const val STAGE_ENTER_HIDEOUT = 5
internal const val STAGE_FIND_SCROLL = 6
internal const val STAGE_RETURN_SCROLL = 7
internal const val STAGE_REVIVAL = 8
internal const val STAGE_COMPLETE = 9

internal const val ATTR_SIDE = "hazeelCultSide"
internal const val SIDE_CARNILLEAN = "carnillean"
internal const val SIDE_CULT = "cult"

private const val REWARDS_UNDECIDED = "1,500 Thieving XP<br>2,000 coins<br>The Cellar Dwellers music track"
private const val REWARDS_CULT = "1,500 Thieving XP<br>2,000 coins<br>Hazeel's mark<br>The Cellar Dwellers music track"
private const val REWARDS_CARNILLEAN = "1,500 Thieving XP<br>2,005 coins<br>The Cellar Dwellers music track"
private const val COMBAT_UNDECIDED = "You may need to defeat Alomone (level 9)."
private const val COMBAT_CULT = "None."
private const val COMBAT_CARNILLEAN = "You will need to defeat Alomone (level 9)."

@QuestHandler(
    quest = Quest.HAZEEL_CULT,
    startText = "Speak to Sir Ceril Carnillean in his mansion south-west of Ardougne Castle.",
    itemsText = "None.",
    combatText = COMBAT_UNDECIDED,
    rewardsText = REWARDS_UNDECIDED,
    completedStage = STAGE_COMPLETE
)
class HazeelCult : QuestOutline() {
    private fun sideChosen(player: Player) = player.questManager.getStage(Quest.HAZEEL_CULT) >= STAGE_BRANCH_TASK

    override fun getRewardsString(player: Player): String {
        val text = when {
            !sideChosen(player) -> REWARDS_UNDECIDED
            player.isCultSide() -> REWARDS_CULT
            else -> REWARDS_CARNILLEAN
        }
        return quest.questPointRewardLine + text
    }

    override fun getCombatInformationString(player: Player): String = when {
        !sideChosen(player) -> COMBAT_UNDECIDED
        player.isCultSide() -> COMBAT_CULT
        else -> COMBAT_CARNILLEAN
    }

    override fun getJournalLines(player: Player, stage: Int): List<String> {
        val cultSide = player.isCultSide()
        return when (stage) {
            STAGE_UNSTARTED -> listOf("I can start this quest by speaking to Sir Ceril Carnillean at his house south-west of Ardougne Castle.")
            STAGE_FIND_CLIVET -> listOf("I spoke to Sir Ceril Carnillean at his house, and agreed to help him investigate the theft of a family heirloom.")
            STAGE_CHOOSE_SIDE -> listOf("I found a member of the cult called Clivet at the entrance to the cult's hideout, south of Ardougne.")
            STAGE_BRANCH_TASK -> if (cultSide) listOf("Having decided to assist the cult in their mission to revive Hazeel, I should poison the Carnilleans' food.")
                else listOf("Clivet told me a pack of lies about the Carnilleans, then asked me to join the cult.", "Obviously, I refused.", "I am still no closer to recovering the missing armour.")
            STAGE_BRANCH_PROGRESS -> if (cultSide) listOf("I tried to poison the Carnilleans but instead only managed to kill their pet dog.", "I should report back to Clivet.")
                else listOf("I managed to enter the hideout, kill the cult leader and retrieve the armour.", "I discovered that Jones the Butler was secretly a member of the cult and a traitor.")
            STAGE_ENTER_HIDEOUT -> if (cultSide) listOf("I spoke to Clivet and he told me that I had failed, but he gave me an amulet anyway.", "I can use the correctly-set sewer valves and rafts to reach the cult hideout.")
                else listOf("I returned the armour, but Ceril didn't believe Jones was involved with the cult and responsible for the theft.", "I need undeniable evidence of Jones's treachery.")
            STAGE_FIND_SCROLL -> listOf("Alomone told me that the scroll needed to revive Hazeel is hidden in the Carnillean Mansion.", "Jones has searched high and low, so I should look for a place the family would never visit.")
            STAGE_RETURN_SCROLL -> listOf("I found the scroll needed to revive Hazeel in a secret compartment upstairs at the Carnillean Mansion.", "I should take it to Alomone in the cult hideout.")
            STAGE_REVIVAL -> listOf("I gave Alomone the scroll from the Carnillean Mansion. The revivification of Hazeel is about to begin.")
            STAGE_COMPLETE -> if (cultSide) listOf("I helped perform the revivification of Hazeel and was rewarded for returning him to his followers.", "QUEST COMPLETE!")
                else listOf("I found undeniable evidence that the Butler was involved with the cult and gave it to Ceril.", "My name was cleared and I graciously accepted the reward for all of my help.", "QUEST COMPLETE!")
            else -> listOf("Invalid quest stage. Report this to an administrator.")
        }
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVar(Varp.hazeelcultquest, stage)

        val cultSide = player.isCultSide()
        player.vars.setVarBit(Varbit.hazeelcult_deaddog, if (cultSide && stage >= STAGE_BRANCH_PROGRESS) 1 else 0)
        player.vars.setVarBit(Varbit.hazeelcult_cookingrange, if (cultSide && stage >= STAGE_BRANCH_PROGRESS) 2 else 1)
        player.vars.setVarBit(Varbit.carni_philipestate, if (cultSide && stage >= STAGE_BRANCH_PROGRESS) 2 else 1)
        player.vars.setVarBit(Varbit.varbit_11432, 1)

        if (player.questManager.getStage(Quest.CARNILLEAN_RISING) == 0) {
            player.vars.setVarBit(Varbit.carni_butlerjonesvisible, if (stage == STAGE_COMPLETE) 2 else 1)
            player.vars.setVarBit(Varbit.varbit_11428, if (stage == STAGE_COMPLETE) 1 else 0)
            player.vars.setVarBit(Varbit.varbit_11429, 0)
            player.vars.setVarBit(Varbit.carni_xeniavisible, 0)
            player.vars.setVarBit(Varbit.varbit_11431, 0)
            refreshCellarWall(player)
        }
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Skills.THIEVING, 1500.0)
        if (player.isCultSide()) sendQuestCompleteInterface(player, Obj.mark_of_hazeel, REWARDS_CULT)
        else sendQuestCompleteInterface(player, Obj.carnillean_armour, REWARDS_CARNILLEAN)
    }
}

@ServerStartupEvent
fun mapHazeelCultState() {
    onLogin { (player) ->
        val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
        if (stage > STAGE_UNSTARTED) Quest.HAZEEL_CULT.handler?.updateStage(player, stage)
    }
}

internal fun Player.isCultSide(): Boolean {
    val saved = questManager.getAttribs(Quest.HAZEEL_CULT).getO<String>(ATTR_SIDE)
    return saved == SIDE_CULT || (saved == null && vars.getVarBit(Varbit.hazeelcult_deaddog) == 1)
}

internal fun Player.setSide(side: String) {
    questManager.getAttribs(Quest.HAZEEL_CULT).setO<String>(ATTR_SIDE, side)
}

internal fun giveItem(player: Player, itemId: Int, message: String): Boolean {
    if (!player.inventory.hasFreeSlots()) {
        player.sendMessage("You need some free inventory space first.")
        return false
    }
    player.inventory.addItem(itemId)
    player.itemDialogue(itemId, message)
    return true
}
