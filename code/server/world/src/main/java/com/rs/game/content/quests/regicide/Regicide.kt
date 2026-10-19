package com.rs.game.content.quests.regicide

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.content.quests.regicide.npcs.scheduleRegicideMessenger
import com.rs.game.content.quests.regicide.objects.TYRAS_GUARD
import com.rs.game.model.entity.async.cancel
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp

const val STAGE_UNSTARTED = 0
const val STAGE_ENTER_ISAFDAR = 1
const val STAGE_MEET_IORWERTH = 2
const val STAGE_MEET_TRACKER = 3
const val STAGE_GET_PENDANT = 4
const val STAGE_FIND_TRACKS = 5
const val STAGE_REPORT_TRACKS = 6
const val STAGE_FOLLOW_TRACKS = 7
const val STAGE_FIND_TYRAS_CAMP = 8
const val STAGE_REPORT_CAMP = 9
const val STAGE_PREPARE_EXPLOSIVE = 10
const val STAGE_DESTROY_TENT = 11
const val STAGE_REPORT_TYRAS_DEATH = 12
const val STAGE_RETURN_TO_LATHAS = 13
const val STAGE_DELIVER_MESSAGE = 14
const val STAGE_COMPLETE = 15

@QuestHandler(
    quest = Quest.REGICIDE,
    startText = "Receive the King's message after completing Underground Pass, then speak to King Lathas in East Ardougne castle.",
    itemsText = "A bow, arrows, rope, tinderbox, spade, pestle and mortar, an empty pot, limestone, coal, four balls of wool and a cooked rabbit. Bring food and antipoison.",
    combatText = "You must defeat a level 110 Tyras guard.",
    rewardsText = "13,750 Agility XP<br>15,000 coins<br>Access through Arandar<br>Ability to buy and wield dragon halberds",
    completedStage = STAGE_COMPLETE
)
class Regicide : QuestOutline() {
    override fun updateStage(player: Player, stage: Int) {
        if (stage == STAGE_UNSTARTED) {
            player.cancel("RegicideFractionalStill")
            player.tempAttribs.removeO<Any>("regicideStillSession")
            player.tempAttribs.removeO<OwnedNPC>(TYRAS_GUARD)?.finish()
            player.questManager.getAttribs(Quest.REGICIDE).setB("stillLoaded", false)
            player.vars.saveVar(Varp.regicide_still_total, 0)
            player.vars.setVar(Varp.regicide_still_settings, 0)
            player.scheduleRegicideMessenger()
        }
        player.vars.setVarBit(Varbit.regicide_can_see_arianwyn, if (stage == STAGE_RETURN_TO_LATHAS) 1 else 0)
        player.vars.setVarBit(Varbit.regicide_iorwerth_knows_camp_found, if (stage >= STAGE_PREPARE_EXPLOSIVE) 1 else 0)
        player.vars.setVarBit(Varbit.regicide_iorwerth_knows_king_dead, if (stage >= STAGE_RETURN_TO_LATHAS) 1 else 0)
    }

    override fun getJournalLines(player: Player, stage: Int): List<String> = when (stage) {
        STAGE_UNSTARTED -> listOf("King Lathas will send for me after Underground Pass.", "I should take his message to him in East Ardougne castle.")
        STAGE_ENTER_ISAFDAR -> listOf("King Lathas has asked me to kill his brother Tyras and find an easier route to the western lands.", "I must enter the Well of Voyage in the Underground Pass.")
        STAGE_MEET_IORWERTH -> listOf("The elves told me to speak to Lord Iorwerth in the north-west of Isafdar.")
        STAGE_MEET_TRACKER -> listOf("Lord Iorwerth has offered the help of a tracker near Tyras's old camp.")
        STAGE_GET_PENDANT -> listOf("The tracker wants proof that Lord Iorwerth sent me.", "I should return to Lord Iorwerth.")
        STAGE_FIND_TRACKS -> listOf("The tracker recognises Lord Iorwerth's crystal pendant.", "I should search the western end of the old camp for tracks.")
        STAGE_REPORT_TRACKS -> listOf("The tracks lead into dense woodland.", "I should ask the tracker how to follow them.")
        STAGE_FOLLOW_TRACKS -> listOf("The tracker taught me how to follow paths through dense woodland.", "I should follow the tracks west.")
        STAGE_FIND_TYRAS_CAMP -> listOf("I defeated the guard blocking the path.", "I should continue to Tyras's camp.")
        STAGE_REPORT_CAMP -> listOf("I found Tyras's camp.", "I should tell Lord Iorwerth where it is.")
        STAGE_PREPARE_EXPLOSIVE -> listOf("Lord Iorwerth gave me the Big Book of Bangs.", "I need to learn how to prepare an explosive and find a way to deliver it.")
        STAGE_DESTROY_TENT -> listOf("I have learned how to prepare the barrel bomb.", "The catapult may let me reach Tyras's tent.")
        STAGE_REPORT_TYRAS_DEATH -> listOf("Tyras's tent has been destroyed.", "I should report to Lord Iorwerth.")
        STAGE_RETURN_TO_LATHAS -> listOf("Lord Iorwerth gave me a sealed message for King Lathas.", "His guards will let me return through Arandar.")
        STAGE_DELIVER_MESSAGE -> listOf("Arianwyn revealed the truth about my employers.", "I must deliver Iorwerth's message to King Lathas and act normally.")
        STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun complete(player: Player) {
        player.cancel("RegicideFractionalStill")
        player.tempAttribs.removeO<Any>("regicideStillSession")
        player.tempAttribs.removeO<OwnedNPC>(TYRAS_GUARD)?.finish()
        player.vars.saveVar(Varp.regicide_still_total, 0)
        player.vars.setVar(Varp.regicide_still_settings, 0)
        player.inventory.deleteItem(Obj.regicide_quest_kings_summons, player.inventory.getAmountOf(Obj.regicide_quest_kings_summons))
        player.skills.addXpQuest(Skills.AGILITY, 13750.0)
        player.inventory.addItemDrop(Obj.coins, 15000)
        sendQuestCompleteInterface(player, Obj.regicide_iorwerth_message)
    }
}

fun Player.advanceRegicide(stage: Int) {
    if (getQuestStage(Quest.REGICIDE) in STAGE_ENTER_ISAFDAR until stage)
        setQuestStage(Quest.REGICIDE, stage)
}

internal fun Player.giveRegicideItem(itemId: Int): Boolean {
    if (inventory.containsItem(itemId))
        return true
    if (!inventory.hasFreeSlots()) {
        sendMessage("You need a free inventory slot.")
        return false
    }
    return inventory.addItem(itemId, 1)
}
