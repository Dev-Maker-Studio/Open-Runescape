package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp

internal const val UNSTARTED = 0
internal const val ADVERT = 1
internal const val BODY_PARTS = 2
internal const val SEWING = 3
internal const val CONDUCTOR = 4
internal const val REVIVED = 5
internal const val TOWER = 6
internal const val TRUTH = 7
internal const val COMPLETE = 8
internal val FENKENSTRAIN = Quest.CREATURE_OF_FENKENSTRAIN
internal val Player.fenk get() = questManager.getAttribs(FENKENSTRAIN)
internal val Player.fenkStage get() = getQuestStage(FENKENSTRAIN)
internal val PARTS = listOf(Obj.fenk_head_full, Obj.fenk_torso, Obj.fenk_arms, Obj.fenk_legs)

@QuestHandler(quest = Quest.CREATURE_OF_FENKENSTRAIN,
    startText = "Read the signpost in Canifis or speak to Dr Fenkenstrain at his castle north-east of Canifis.",
    itemsText = "Ghostspeak amulet, spade, 50 coins or Telekinetic Grab runes, silver bar, 3 bronze wires, needle and 5 thread.",
    combatText = "You must defeat a level 51 Experiment. Vampyres inhabit the Haunted Woods.",
    rewardsText = "1,000 Thieving XP<br>Ring of charos<br>Access to the Experiment cave and Werewolf Agility Course",
    completedStage = COMPLETE)
class CreatureOfFenkenstrain : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        UNSTARTED -> listOf("I can read the signpost in Canifis to find a job at Fenkenstrain's Castle.")
        ADVERT -> listOf("Dr Fenkenstrain needs a braindead servant skilled in grave-digging.", "I should speak to him at his castle north-east of Canifis.")
        BODY_PARTS -> buildList {
            add("Dr Fenkenstrain wants enough dead body parts to make a complete body.")
            if (!player.fenk.getB("part${Obj.fenk_head_full}")) {
                add("I need a decapitated head and a pickled brain, which I must put inside the head.")
                add("Roavar at the Hair of the Dog pub in Canifis sells pickled brains for 50 coins.")
            }
            PARTS.forEach { if (!player.fenk.getB("part$it")) add("I still need to bring him ${when (it) { Obj.fenk_head_full -> "a head containing a brain"; Obj.fenk_torso -> "a torso"; Obj.fenk_arms -> "a pair of arms"; else -> "a pair of legs" }}.") }
            add("The gardener may know about the graves in the Haunted Woods.")
            add("The castle's upstairs bookcases may help me reach the mausoleum.")
        }
        SEWING -> listOf("Fenkenstrain has all the body parts.", "He needs ${if (player.fenk.getB("needle")) "no more needles" else "a needle"} and ${5 - player.fenk.getI("thread")} more thread to sew them together.")
        CONDUCTOR -> listOf("I must repair the lightning conductor with a silver replacement.", "The gardener may know where to find a conductor mould.", "I should extend a garden brush with three canes and bronze wires to clean the upstairs west chimney.")
        REVIVED -> listOf("Lightning has brought the creature to life.", "I should report back to Dr Fenkenstrain.")
        TOWER -> listOf("Fenkenstrain has imprisoned the creature in the tower.", "I should use the tower key and speak to the creature.")
        TRUTH -> listOf("The creature is Lord Rologarth, the castle's former owner.", "I must take the Ring of Charos from Fenkenstrain to stop his experiments.")
        COMPLETE -> listOf("QUEST COMPLETE!", "I took the Ring of Charos and stopped Fenkenstrain's experiments.")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVar(Varp.varp_399, when (stage) { ADVERT, BODY_PARTS -> 1; SEWING -> 2; CONDUCTOR -> 3; REVIVED -> 4; TOWER -> 5; TRUTH -> 7; else -> stage })
        val attributes = player.fenk
        val flags = listOf(Varbit.fenk_head to Obj.fenk_head_full, Varbit.fenk_torso to Obj.fenk_torso, Varbit.fenk_arms to Obj.fenk_arms, Varbit.fenk_legs to Obj.fenk_legs)
        flags.forEach { (flag, part) -> player.vars.setVarBit(flag, if (stage >= SEWING || attributes.getB("part$part")) 1 else 0) }
        player.vars.setVarBit(Varbit.fenk_needle, if (stage >= CONDUCTOR || attributes.getB("needle")) 1 else 0)
        player.vars.setVarBit(Varbit.fenk_threads_given, if (stage >= CONDUCTOR) 5 else attributes.getI("thread"))
        player.vars.setVarBit(Varbit.fenk_coffin, if (stage >= SEWING || attributes.getB("coffin")) 1 else 0)
        player.vars.setVarBit(Varbit.fenk_conductor_repaired, if (stage >= REVIVED) 1 else 0)
        player.vars.setVarBit(Varbit.fenk_unlocked_cavern, if (stage >= SEWING || attributes.getB("cavern")) 1 else 0)
        player.vars.setVarBit(Varbit.fenk_unlocked_tower, if (stage == COMPLETE || attributes.getB("towerUnlocked")) 1 else 0)
        player.vars.setVarBit(Varbit.fenk_unlocked_shed, if (stage >= REVIVED || attributes.getB("shed")) 1 else 0)
        player.vars.setVarBit(Varbit.fenk_read_signpost, if (stage > UNSTARTED) 1 else 0)
        refreshFenkenstrainScene(player)
    }

    override fun complete(player: Player) {
        player.inventory.addItemDrop(Obj.ring_of_charos, 1)
        player.skills.addXpQuest(Skills.THIEVING, 1000.0)
        sendQuestCompleteInterface(player, Obj.ring_of_charos)
    }
}

internal fun Player.fenkActive(minimum: Int = BODY_PARTS): Boolean {
    if (fenkStage in minimum until COMPLETE) return true
    sendMessage("You have no reason to do that at the moment.")
    return false
}

internal fun Player.fenkGive(id: Int, text: String) {
    if (containsItem(id)) simpleDialogue("You already have that item.")
    else if (!inventory.hasFreeSlots()) simpleDialogue("You need a free inventory space.")
    else {
        inventory.addItem(id, 1)
        itemDialogue(id, text)
    }
}

internal fun Player.canStartFenkenstrain(): Boolean =
    isQuestComplete(Quest.PRIEST_IN_PERIL) && isQuestStarted(Quest.RESTLESS_GHOST) &&
        skills.getLevelForXp(Skills.CRAFTING) >= 20 && skills.getLevelForXp(Skills.THIEVING) >= 25
