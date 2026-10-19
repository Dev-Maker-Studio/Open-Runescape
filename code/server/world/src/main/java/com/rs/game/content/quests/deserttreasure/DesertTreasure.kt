package com.rs.game.content.quests.deserttreasure

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.World
import com.rs.game.content.combat.CombatDefinitions
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

const val STAGE_UNSTARTED = 0
const val STAGE_TERRY_BALANDO = 1
const val STAGE_RETURN_TO_ASGARNIA = 2
const val STAGE_MIRROR_MIRROR = 3
const val STAGE_TALK_TO_BARTENDER = 4
const val STAGE_TALK_TO_EBLIS = 5
const val STAGE_RETURN_TO_MIRRORS = 6
const val STAGE_DIAMONDS_OF_AZZANADRA = 7
const val STAGE_IFTAH_YA_SIMSIM = 8
const val STAGE_RETURN_TO_JALDRAOCHT = 9
const val STAGE_TOMB_RAIDER = 10
const val STAGE_AZZANADRA = 11
const val STAGE_COMPLETE = 12

object DesertTreasureDiamonds {
    fun hasBloodDiamond(player: Player) = player.containsItem(Obj.fd_blood_diamond)
    fun hasIceDiamond(player: Player) = player.containsItem(Obj.fd_icediamond)
    fun hasSmokeDiamond(player: Player) = player.containsItem(Obj.fd_diamond_fire)
    fun hasShadowDiamond(player: Player) = player.containsItem(Obj.fd_dark_diamond)

    fun giveBloodDiamond(player: Player) = player.inventory.addItemDrop(Obj.fd_blood_diamond, 1)
    fun giveIceDiamond(player: Player) = player.inventory.addItemDrop(Obj.fd_icediamond, 1)
    fun giveSmokeDiamond(player: Player) = player.inventory.addItemDrop(Obj.fd_diamond_fire, 1)
    fun giveShadowDiamond(player: Player) = player.inventory.addItemDrop(Obj.fd_dark_diamond, 1)

    fun diamondCount(player: Player) = listOf(
        hasBloodDiamond(player), hasIceDiamond(player), hasSmokeDiamond(player), hasShadowDiamond(player)
    ).count { it }

    fun hasAllDiamonds(player: Player) = diamondCount(player) == 4
}

fun Player.ownsActiveNpc(npcId: Int): Boolean =
    World.NPCs.any { it is OwnedNPC && it.owner === this && it.id == npcId && !it.hasFinished() }

@QuestHandler(
    quest = Quest.DESERT_TREASURE,
    startText = "Talk to Asgarnia Smith, the archaeologist who is exploring the Bedabin Camp south-west of the Shantay Pass.",
    itemsText = "Ashes, blood rune, bones, charcoal, 650 coins, 6 molten glass, 12 magic logs, 6 steel bars, cake, face mask, garlic, lockpicks, ice gloves or ice arrows, silver bar, spice, climbing boots, spiked boots.",
    combatText = "Several high level enemies.",
    rewardsText = "20,000 Magic XP<br>Ancient Magicks<br>Access to the Bandit Camp lodestone<br>Ability to buy and wield the ancient staff<br>Access to the Ancient Symbol gravestone",
    completedStage = STAGE_COMPLETE
)
class DesertTreasure : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        STAGE_UNSTARTED -> listOf("I should speak to Asgarnia Smith at the Bedabin Camp south-west of Shantay Pass.")
        STAGE_TERRY_BALANDO -> listOf("The archaeologist I met in the desert seemed to think that there was some hidden treasure in the desert somewhere.", "I should take this stone tablet he gave me to Terry Balando at the Exam Centre.")
        STAGE_RETURN_TO_ASGARNIA -> listOf("I've brought the tablet to Terry and he translated it for me.", "I should return to Asgarnia Smith.")
        STAGE_MIRROR_MIRROR -> listOf("Asgarnia Smith suggested I find information with the local Bedabin nomads.", "I should head to the Bandit Camp and speak to the bartender.")
        STAGE_TALK_TO_BARTENDER -> listOf("The bartender informed me that there is an old tale about four diamonds, and that I would need to speak to someone who knows more about the desert's history.")
        STAGE_TALK_TO_EBLIS -> listOf("I spoke with Eblis who shared with me the history of the diamonds.", "He said that the diamonds were taken by warriors of Zamorak and that I will need to kill them to reclaim them.")
        STAGE_RETURN_TO_MIRRORS -> listOf("Eblis offered to assist me in locating the diamonds. To do this he requires a set of special scrying glasses.")
        STAGE_DIAMONDS_OF_AZZANADRA -> buildList {
            add("I have given Eblis the special scrying glasses and he has located the four diamonds for me.")
            add("The four diamonds are spread across these areas: Canifis, Trollheim, Pollnivneach, and Baxtorian Falls. I can search for them in any order.")
            if (DesertTreasureDiamonds.hasBloodDiamond(player)) {
                add("I have retrieved the blood diamond from Malak, near Canifis.")
            } else {
                add("I still need the blood diamond. Malak in Canifis wants me to kill Dessous, who lies beneath a grave south-east of Canifis.")
                add("Malak suggested I take a silver bar to Ruantun in the Draynor sewers for a ritual pot, have it blessed on Entrana, then return to Malak for fresh blood.")
                add("I should add crushed garlic and spice to the blessed pot of blood, then use it on Dessous' tomb to lure and weaken him.")
            }
            if (DesertTreasureDiamonds.hasIceDiamond(player)) {
                add("I have retrieved the ice diamond from the ice trolls near Trollheim.")
            } else {
                add("I still need the ice diamond. A crying troll child near Trollheim may help if I give him something sweet.")
                add("The path beyond Trollheim is dangerously cold. I will need climbing boots made into spiked boots by Dunstan in Burthorpe before climbing it.")
                add("I should follow the Ice Path, defeat Kamil, then free the troll child's parents to recover the diamond.")
            }
            if (DesertTreasureDiamonds.hasSmokeDiamond(player)) {
                add("I have retrieved the smoke diamond from Fareed, in the Smoke Dungeon.")
            } else {
                add("I still need the smoke diamond. I should enter the smokey well near Pollnivneach with protection from the smoke.")
                add("Inside the Smoke Dungeon I need to light all four standing torches before they burn out, then open the central chest for the warm key.")
                add("The warm key opens Fareed's gate. I will need to defeat him to recover the smoke diamond.")
            }
            if (DesertTreasureDiamonds.hasShadowDiamond(player)) {
                add("I have retrieved the shadow diamond from the Shadow Dungeon.")
            } else if (!player.getBool(RASOLO_DEAL_ACCEPTED)) {
                add("I still need to retrieve the shadow diamond. I should speak to Rasolo, south of Baxtorian Falls, about an invisible dungeon.")
            } else if (player.containsItem(Obj.fd_sword_cross)) {
                add("I found Rasolo's gilded cross in the Bandit Camp. I should return it to him for his ring of visibility.")
            } else if (player.containsItem(Obj.fd_ring_visibility)) {
                add("Rasolo gave me his ring of visibility. I should wear it near the invisible dungeon east of him to find Damis.")
            } else {
                add("Rasolo wants his gilded cross back before he will give me his ring of visibility. Laheeb stole it and hid it somewhere in the Bandit Camp south of Rasolo.")
            }
            if (DesertTreasureDiamonds.hasAllDiamonds(player)) add("I have all four diamonds. I should return to Eblis at the mirrors.")
        }
        STAGE_IFTAH_YA_SIMSIM -> listOf("I have all four diamonds and have met with Eblis, who wants me to sort the diamonds into their obelisks at the pyramid.")
        STAGE_RETURN_TO_JALDRAOCHT -> listOf("I have placed the diamonds in their respective obelisks and can now enter the Jaldraocht Pyramid.")
        STAGE_TOMB_RAIDER -> listOf("I've navigated the pyramid and found a central chamber.")
        STAGE_AZZANADRA -> listOf("At the heart of the pyramid I found a strange being, who gave me some powerful new magic spells.", "I can switch between my old spells and my new spells at any time by using the altar there, and can avoid the traps by using the secret passage.")
        STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Skills.MAGIC, 20000.0)
        player.combatDefinitions.setSpellbook(CombatDefinitions.Spellbook.ANCIENT)
        sendQuestCompleteInterface(player, Obj.staff_of_zaros)
    }

    override fun updateStage(player: Player, stage: Int) {
        CLIENT_PROGRESS[stage]?.let { player.vars.setVarBit(Varbit.deserttreasure, it) }
        player.vars.setVarBit(Varbit.fd_mirror_present, if (stage >= STAGE_RETURN_TO_MIRRORS) 1 else 0)
    }
}

private val CLIENT_PROGRESS = mapOf(
    STAGE_TERRY_BALANDO to 1,
    STAGE_RETURN_TO_ASGARNIA to 2,
    STAGE_MIRROR_MIRROR to 3,
    STAGE_TALK_TO_BARTENDER to 4,
    STAGE_TALK_TO_EBLIS to 5,
    STAGE_RETURN_TO_MIRRORS to 10,
    STAGE_DIAMONDS_OF_AZZANADRA to 11,
    STAGE_IFTAH_YA_SIMSIM to 12,
    STAGE_RETURN_TO_JALDRAOCHT to 12,
    STAGE_TOMB_RAIDER to 13,
    STAGE_AZZANADRA to 14,
    STAGE_COMPLETE to 15
)
