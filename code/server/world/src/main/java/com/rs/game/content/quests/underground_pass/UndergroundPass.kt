package com.rs.game.content.quests.underground_pass

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import org.darkan.core.gameval.gen.Varbit
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import world.gregs.voidps.type.Tile

const val STAGE_UNSTARTED = 0
const val STAGE_FIND_KOFTIK = 1
const val STAGE_ENTERED_PASS = 2
const val STAGE_CROSSED_BRIDGE = 3
const val STAGE_DESTROYED_ORBS = 4
const val STAGE_PASSED_UNICORN = 5
const val STAGE_PASSED_PALADINS = 6
const val STAGE_FOUND_DWARVES = 7
const val STAGE_PREPARED_DOLL = 8
const val STAGE_RETURN_TO_LATHAS = 9
const val STAGE_COMPLETE = 10

val IBAN_TEMPLE_EFFECT_TILES = arrayOf(
    Tile.of(2135, 4644, 1),
    Tile.of(2135, 4650, 1),
    Tile.of(2137, 4646, 1),
    Tile.of(2137, 4649, 1),
    Tile.of(2139, 4644, 1),
    Tile.of(2139, 4648, 1),
    Tile.of(2141, 4646, 1),
    Tile.of(2141, 4650, 1),
    Tile.of(2143, 4647, 1)
)

private val QUEST_ITEMS_TO_REMOVE = (Obj.swamprocks1..Obj.ibans_ashes).filter { it != Obj.klanks_gauntlets }

@QuestHandler(
    quest = Quest.UNDERGROUND_PASS,
    startText = "Talk to King Lathas in East Ardougne castle.",
    itemsText = "A bow, arrows, rope, tinderbox, spade and plenty of food. A plank and 75 coins are useful.",
    combatText = "You will need to defeat three paladins, three demons and a level 89 giant spider.",
    rewardsText = "3,000 Attack XP<br>3,000 Agility XP<br>Iban's staff<br>Klank's gauntlets<br>Access to the western lands",
    completedStage = STAGE_COMPLETE
)
class UndergroundPass : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> = when (stage) {
        STAGE_UNSTARTED -> listOf(
            "King Lathas has discovered a route through the mountains west of Ardougne.",
            "I should speak to him in East Ardougne castle.",
            "",
            "Requirements:",
            "Completed Biohazard",
            "25 Ranged"
        )

        STAGE_FIND_KOFTIK -> listOf(
            "King Lathas asked me to find a route through the Underground Pass.",
            "His scout Koftik is waiting at the cave entrance at the far west of West Ardougne."
        )

        STAGE_ENTERED_PASS -> listOf(
            "I met Koftik and entered the Underground Pass.",
            "A burning arrow may let me use the guide rope to cross the first chasm."
        )

        STAGE_CROSSED_BRIDGE -> buildList {
            add("I crossed the first chasm.")
            add("I must survive the traps and destroy the four orbs of light in the furnace.")
            add("")
            add("${struck(player, Varbit.upass_orb_1)}First orb of light")
            add("${struck(player, Varbit.upass_orb_2)}Second orb of light")
            add("${struck(player, Varbit.upass_orb_3)}Third orb of light")
            add("${struck(player, Varbit.upass_orb_4)}Fourth orb of light")
        }

        STAGE_DESTROYED_ORBS -> listOf(
            "The four orbs of light have been destroyed.",
            "I need to continue through the caves and find a way past the unicorn."
        )

        STAGE_PASSED_UNICORN -> listOf(
            "I recovered the unicorn's horn.",
            "Three paladins guard the next well. Their badges appear to be the key."
        )

        STAGE_PASSED_PALADINS -> listOf(
            "I passed the paladins and descended into the great cavern.",
            "The dwarves living below may know how Iban can be defeated."
        )

        STAGE_FOUND_DWARVES -> buildList {
            add("I learned that Iban's spirit can be destroyed through a doll made in his image.")
            add("")
            add("${struck(player, Varbit.upass_iban_blood)}Iban's blood - from Kalrag")
            add("${struck(player, Varbit.upass_iban_conscience)}Iban's conscience - his dove")
            add("${struck(player, Varbit.upass_iban_flesh)}Iban's flesh - ashes from his tomb")
            add("${struck(player, Varbit.upass_iban_shadow)}Iban's shadow - guarded by three demons")
        }

        STAGE_PREPARED_DOLL -> listOf(
            "The Doll of Iban now carries his blood, conscience, flesh and shadow.",
            "I should reach Iban's temple and cast the doll into the Well of Voyage."
        )

        STAGE_RETURN_TO_LATHAS -> listOf(
            "Iban has been destroyed and the pass is open.",
            "I should return to King Lathas in East Ardougne."
        )

        STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVarBit(Varbit.upass_started_quest, if (stage > STAGE_UNSTARTED) 1 else 0)
        player.vars.setVarBit(Varbit.wtl_upass_entrance_visible, if (player.isQuestComplete(Quest.BIOHAZARD)) 1 else 0)
        if (stage >= STAGE_FOUND_DWARVES && player.vars.getVarBit(Varbit.upass_ibantombappearance) == 0)
            player.vars.saveVarBit(Varbit.upass_ibantombappearance, 1)
    }

    override fun complete(player: Player) {
        QUEST_ITEMS_TO_REMOVE.forEach { itemId ->
            player.inventory.deleteItem(itemId, player.inventory.getAmountOf(itemId))
        }
        if (!player.inventory.containsItem(Obj.ibanstaff) && !player.bank.containsItem(Obj.ibanstaff))
            player.inventory.addItemDrop(Obj.ibanstaff, 1)
        if (!player.inventory.containsItem(Obj.klanks_gauntlets) && !player.bank.containsItem(Obj.klanks_gauntlets))
            player.inventory.addItemDrop(Obj.klanks_gauntlets, 1)
        player.skills.addXpQuest(Skills.ATTACK, 3000.0)
        player.skills.addXpQuest(Skills.AGILITY, 3000.0)
        sendQuestCompleteInterface(player, Obj.ibanstaff)
    }

    private fun struck(player: Player, varbit: Int) =
        if (player.vars.getVarBit(varbit) == 1) "<str>" else ""
}

fun Player.advanceUndergroundPass(stage: Int) {
    val current = getQuestStage(Quest.UNDERGROUND_PASS)
    if (current in STAGE_FIND_KOFTIK until stage)
        setQuestStage(Quest.UNDERGROUND_PASS, stage)
}

fun Player.updateIbanDollStage() {
    val complete = intArrayOf(
        Varbit.upass_iban_blood,
        Varbit.upass_iban_conscience,
        Varbit.upass_iban_flesh,
        Varbit.upass_iban_shadow
    ).all { vars.getVarBit(it) == 1 }
    if (complete)
        advanceUndergroundPass(STAGE_PREPARED_DOLL)
}

fun Player.hasCompletedUndergroundPassUnicorn(): Boolean =
    getQuestStage(Quest.UNDERGROUND_PASS) >= STAGE_PASSED_UNICORN ||
        questManager.getAttribs(Quest.UNDERGROUND_PASS).getB("unicornDefeated")

fun Player.giveUniqueQuestItem(itemId: Int) {
    if (!inventory.containsItem(itemId) && !bank.containsItem(itemId))
        inventory.addItemDrop(itemId, 1)
}
