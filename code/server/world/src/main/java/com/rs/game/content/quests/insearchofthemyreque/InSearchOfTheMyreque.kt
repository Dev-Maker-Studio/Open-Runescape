package com.rs.game.content.quests.insearchofthemyreque

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.content.containsTool
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.gameval.gen.Varbit

internal const val UNSTARTED = 0
internal const val WEAPONS = 1
internal const val PERSUADED = 2
internal const val BOAT = 3
internal const val HIDEOUT = 4
internal const val INTRODUCTIONS = 5
internal const val HELLHOUND = 6
internal const val VICTORY = 7
internal const val EXIT = 8
internal const val COMPLETE = 9
internal fun Player.myrequeBridgeFixed(index: Int) = myrequeStage == COMPLETE || myreque.getB("bridgeFixed$index")
internal fun Player.myrequeBridgeRemaining() = BRIDGE_TILES.indices.count { !myrequeBridgeFixed(it) }
internal fun Player.myrequeBridgeBroken() = myreque.getB("bridgeBroken")
internal val MYREQUE = Quest.IN_SEARCH_OF_MYREQUE
internal val Player.myreque get() = questManager.getAttribs(MYREQUE)
internal val Player.myrequeStage get() = getQuestStage(MYREQUE)
internal val WEAPON_LIST = linkedMapOf(Obj.steel_longsword to 1, Obj.steel_sword to 2, Obj.steel_dagger to 1, Obj.steel_mace to 1, Obj.steel_warhammer to 1)
internal val MEMBERS = linkedMapOf(Npc.route_sani_piliu to "Sani", Npc.route_harold_evans to "Harold", Npc.npc_1572 to "Radigad", Npc.npc_1573 to "Polmafi", Npc.route_ivan_strom to "Ivan")
internal fun Player.hasMyrequeWeapons() = WEAPON_LIST.all { (id, amount) -> inventory.containsItem(id, amount) }
internal fun Player.hasMyrequeHammer() = inventory.containsItem(Obj.hammer, 1) || containsTool(Obj.hammer)
internal fun Player.metAllMyreque() = MEMBERS.keys.all { myreque.getB("met$it") }
internal fun Player.advanceMyreque(stage: Int) { if (myrequeStage < stage) { if (stage == COMPLETE) questManager.completeQuest(MYREQUE) else setQuestStage(MYREQUE, stage) } }

@QuestHandler(quest = Quest.IN_SEARCH_OF_MYREQUE,
    startText = "Speak to Vanstrom Klause in the Hair of the Dog tavern in Canifis.",
    itemsText = "Steel longsword, 2 steel swords, steel dagger, steel mace, steel warhammer, 6 planks, 75 steel nails, a hammer (tool belt works) and a druid pouch with at least 5 charges.",
    combatText = "You must defeat a Skeleton Hellhound. Ghasts inhabit Mort Myre.",
    rewardsText = "600 Attack XP<br>600 Defence XP<br>600 Strength XP<br>600 Constitution XP<br>600 Crafting XP<br>Access to the Myreque hideout and Canifis tunnel<br>Use of the swamp boat",
    completedStage = COMPLETE)
class InSearchOfTheMyreque : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        UNSTARTED -> listOf("I can start this quest by speaking to Vanstrom Klause", "in the Hair of the Dog tavern in Canifis.", "I need to complete Nature Spirit and have level 25 Agility.")
        WEAPONS -> listOf("Vanstrom wants me to take steel weapons to the Myreque.", "I need a longsword, two swords, a dagger, a mace and a warhammer.", "A boatman in Mort'ton may know where to find them.")
        PERSUADED -> listOf("I persuaded Cyreg Paddlehorn to let me use his boat.", "He needs three planks to repair it.", "I also need three planks, 75 steel nails and a charged druid pouch.")
        BOAT -> listOf("Cyreg has repaired his boat. I should travel to the Hollows.", "I need at least five charges in my druid pouch for the first journey.", "The bridge needs ${player.myrequeBridgeRemaining()} more planks and ${25 * (player.myrequeBridgeRemaining())} steel nails.", "I should head north and climb the unusual tree.", "Curpile guards the Myreque's hideout beyond the bridge.")
        HIDEOUT -> listOf("Curpile has unlocked the wooden doors for me.", "I should enter the tunnel, squeeze past the stalagmite", "and introduce myself to Veliaf Hurtz.")
        INTRODUCTIONS -> buildList {
            add("Veliaf wants me to introduce myself to the other Myreque.")
            MEMBERS.forEach { (id, name) -> if (!player.myreque.getB("met$id")) add("I still need to talk to $name.") }
            add("Then I should bring all six steel weapons to Veliaf.")
        }
        HELLHOUND -> listOf("Vanstrom followed me to the hideout and murdered Sani and Harold.", "I must defeat his Skeleton Hellhound to save the remaining Myreque.", "If I leave, I can return through the stalagmite to face it again.")
        VICTORY -> listOf("I defeated the Skeleton Hellhound.", "I should speak to Veliaf and ask how to leave the hideout.")
        EXIT -> listOf("Veliaf told me about a moveable wall in the main tunnel.", "I should search it and climb the ladder into Canifis.", "Then I should look for Vanstrom at the Hair of the Dog tavern.")
        COMPLETE -> listOf("QUEST COMPLETE!", "Vanstrom tricked me into revealing the Myreque's hideout.", "I saved the surviving members from his Skeleton Hellhound.")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVar(Varp.varp_387, when (stage) { UNSTARTED -> 0; WEAPONS -> 5; PERSUADED -> 10; BOAT -> 20; HIDEOUT -> 30; INTRODUCTIONS -> 50; HELLHOUND -> 75; VICTORY -> 90; EXIT -> 100; else -> 105 })
        if (stage == UNSTARTED) {
            player.myreque.removeB("bridgeBroken")
            BRIDGE_TILES.indices.forEach { player.myreque.removeB("bridgeFixed$it") }
        }
        player.vars.saveVarBit(Varbit.thsfm_vanstrom_hide, if (stage >= HELLHOUND) 1 else 0)
        refreshMyrequeScene(player)
    }

    override fun complete(player: Player) {
        listOf(Skills.ATTACK, Skills.DEFENSE, Skills.STRENGTH, Skills.HITPOINTS, Skills.CRAFTING).forEach { player.skills.addXpQuest(it, 600.0) }
        sendQuestCompleteInterface(player, Obj.steel_longsword)
    }
}
