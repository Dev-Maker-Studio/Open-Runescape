package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.AttribMap

const val STAGE_UNSTARTED = 0
const val STAGE_READ_DIARY = 1
const val STAGE_KILL_SHADES = 2
const val STAGE_VISIT_ULSQUIRE = 3
const val STAGE_LEARN_ABOUT_TEMPLE = 4
const val STAGE_REBUILD_TEMPLE = 5
const val STAGE_CREMATE_SHADE = 6
const val STAGE_RETURN_TO_ULSQUIRE = 7
const val STAGE_COMPLETE = 8

fun Player.morttonState(): AttribMap = questManager.getAttribs(Quest.SHADES_OF_MORTTON)

fun Player.canRepairMorttonTemple(): Boolean =
    isQuestComplete(Quest.SHADES_OF_MORTTON) || getQuestStage(Quest.SHADES_OF_MORTTON) >= STAGE_REBUILD_TEMPLE

fun Player.learnMorttonTemple(topic: String) {
    val state = morttonState()
    state.setB(topic, true)
    if (getQuestStage(Quest.SHADES_OF_MORTTON) == STAGE_LEARN_ABOUT_TEMPLE &&
        state.getB("razmireTemple") && state.getB("ulsquireTemple") && state.getB("cremation"))
        setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_REBUILD_TEMPLE)
}

fun Player.onMorttonOilSanctified() {
    if (getQuestStage(Quest.SHADES_OF_MORTTON) == STAGE_REBUILD_TEMPLE)
        setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_CREMATE_SHADE)
}

fun Player.onMorttonShadeCremated() {
    if (getQuestStage(Quest.SHADES_OF_MORTTON) in STAGE_REBUILD_TEMPLE..STAGE_CREMATE_SHADE)
        setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_RETURN_TO_ULSQUIRE)
}

@QuestHandler(
    quest = Quest.SHADES_OF_MORTTON,
    startText = "Search the shelf in the ruined house in south-west Mort'ton and read Herbi Flax's diary.",
    itemsText = "Tarromin, ashes, vials of water, a hammer, tinderbox, logs and coins for building materials and olive oil.",
    combatText = "You must kill five Loar Shades (level 40).",
    rewardsText = "2,000 Herblore XP<br>2,000 Crafting XP<br>Ability to play the Shades of Mort'ton minigame",
    completedStage = STAGE_COMPLETE
)
class ShadesOfMortton : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> = when (stage) {
        STAGE_UNSTARTED -> listOf("I can search the shelf in the ruined house in south-west Mort'ton.", "I need level 15 Herblore, 20 Crafting and 5 Firemaking.")
        STAGE_READ_DIARY -> listOf("Herbi Flax's diary describes the affliction in Mort'ton.", "His serum combines water, tarromin and ashes.", "I should make Serum 207 and use it on Razmire or Ulsquire.")
        STAGE_KILL_SHADES -> listOf("Razmire has asked me to kill five shades.", "I have killed ${player.morttonState().getI("shadeKills")} of the five shades.", "I must bring all five Loar remains to Razmire.")
        STAGE_VISIT_ULSQUIRE -> listOf("Razmire took two remains and opened his shops.", "I should take some of the remaining shade remains to Ulsquire.")
        STAGE_LEARN_ABOUT_TEMPLE -> listOf("Ulsquire is studying the remains.", "I should ask him about cremation and the temple.", "I should also ask Razmire about the temple.")
        STAGE_REBUILD_TEMPLE -> listOf("I know how to rebuild Flamtaer temple.", "I need timber beams, limestone bricks, swamp paste and a hammer.", "I should repair the temple, light the sacred flame and sanctify olive oil.")
        STAGE_CREMATE_SHADE -> listOf("The sacred flame turned olive oil into sacred oil.", "I should use sacred oil on logs to make pyre logs.", "I need to burn shade remains on a funeral pyre.")
        STAGE_RETURN_TO_ULSQUIRE -> listOf("I have laid a shade's spirit to rest.", "I should return to Ulsquire and tell him what happened.")
        STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
        else -> emptyList()
    }

    override fun updateStage(player: Player, stage: Int) {
        if (stage == STAGE_UNSTARTED && player.getI("morttonPreviousStage", 0) > STAGE_UNSTARTED) {
            player.vars.saveVarBit(Varbit.shades_table_searched, 0)
            player.vars.saveVarBit(Varbit.shades_ulsquire_oil_given, 0)
            player.vars.saveVarBit(Varbit.player_made_perm_serum, 0)
            player.vars.saveVarBit(Varbit.razmire_perm_serum_used, 0)
            player.vars.saveVarBit(Varbit.ulsquire_perm_serum_used, 0)
            player.vars.saveVarBit(Varbit.razmire_visible, 0)
            player.vars.saveVarBit(Varbit.ulsquire_visible, 0)
            player.set("morttonLairExplained", false)
            player.set("morttonRazmireFirstCure", false)
            player.set("morttonUlsquireFirstCure", false)
            player.set("morttonDiaryBonusClaimed", false)
        }
        player.set("morttonPreviousStage", stage)
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Constants.HERBLORE, 2000.0)
        player.skills.addXpQuest(Constants.CRAFTING, 2000.0)
        sendQuestCompleteInterface(player, Obj.shade_bones1)
    }
}
