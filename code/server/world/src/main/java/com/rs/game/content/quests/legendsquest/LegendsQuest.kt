package com.rs.game.content.quests.legendsquest

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.model.AttribMap
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile
import kotlin.math.abs

internal const val MAPPING = 1
internal const val CONTACT = 2
internal const val RESCUE = 3
internal const val BINDING = 4
internal const val SEEDS = 5
internal const val DRY_POOL = 6
internal const val SOURCE = 7
internal const val DAGGER = 8
internal const val WATER_RESTORED = 9
internal const val TOTEM = 10
internal const val DEMON_DEFEATED = 11
internal const val GIFT = 12
internal const val TRAINING = 13
internal const val COMPLETE = 14

@QuestHandler(
    quest = Quest.LEGENDS_QUEST,
    startText = "Speak to Radimus Erkle in his study at the Legends' Guild, north-east of Ardougne.",
    itemsText = "An axe, machete, charcoal, three papyrus, two gold bars, hammer, lockpick, pickaxe, seven different cut gems, soul, mind, earth and two law runes, rope, an unpowered orb and charge-orb runes. Snake weed, ardrigal and a vial of water. Bring food and antipoison.",
    combatText = "Defeat Nezikchened three times and the three guardians of the sacred water.",
    rewardsText = "7,650 XP in four chosen skills<br>Access to the Legends' Guild<br>Ability to wield the dragon sq shield<br>Ability to buy the cape of legends<br>Ability to recharge skills necklaces and combat bracelets",
    completedStage = COMPLETE
)
class LegendsQuest : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> = when (stage) {
        0 -> listOf("I can begin Legends' Quest by speaking to Radimus Erkle at the Legends' Guild.", "I need 107 quest points and must complete Heroes' Quest, Family Crest, Shilo Village, Underground Pass and Waterfall Quest.")
        MAPPING -> listOf("Radimus asked me to map the three parts of the Kharazi Jungle and bring back a token of friendship.", "I need charcoal and papyrus to draw the map using his notes.")
        CONTACT -> listOf("My map is complete. A jungle forester may be interested in it.", "I should use a bullroarer in the jungle to contact the Kharazi tribe.")
        RESCUE -> listOf("Gujuo asked me to find Ungadulu, the tribe's shaman.", "Three rocks in the north-west mark his cave. I should investigate the flames.")
        BINDING -> listOf("Ungadulu is possessed. I need sacred water in a blessed golden bowl and the Book of Binding.", "The cave contains trials of Agility, Thieving, Mining, Strength, Magic and Crafting.")
        SEEDS -> listOf("I defeated Nezikchened and freed Ungadulu.", "He can give me Yommi seeds, which need to be germinated in sacred water.")
        DRY_POOL -> listOf("The sacred pool dried up. Gujuo may know how to reach its source.", "A bravery potion made from snake weed and ardrigal will protect me from the curse of fear.")
        SOURCE -> listOf("I must search the Viyeldi caves for the sacred water's source.", "The three guardians hold pieces of a crystal. The furnace can fuse them together.")
        DAGGER -> listOf("Echned Zekin offered me a dark dagger to kill Viyeldi.", "Ungadulu may know more about this strange spirit and its weapon.")
        WATER_RESTORED -> listOf("I defeated Nezikchened a second time. I can push the boulders and restore the sacred water.", "I must plant and water a Yommi tree, then chop, trim and carve it into a totem.")
        TOTEM -> listOf("I carved a Yommi totem. I must replace a corrupted totem in the jungle.", "Nezikchened may make one final attempt to stop me.")
        DEMON_DEFEATED -> listOf("I defeated Nezikchened for the final time.", "I can now replace the corrupted totem with my Yommi totem.")
        GIFT -> listOf("Gujuo gave me a gilded totem as a token of the Kharazi tribe's friendship.", "I should take it and the completed map to Radimus Erkle.")
        TRAINING -> listOf("Radimus welcomed me into the Legends' Guild.", "I must speak to him in the main hall to receive my four training rewards.")
        COMPLETE -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        if (stage == 0) {
            clearLegendsScene(player)
            player.legends.clear()
        }
        refreshLegendsScene(player)
    }

    override fun complete(player: Player) {
        clearLegendsScene(player)
        sendQuestCompleteInterface(player, Obj.thtotempolegift)
    }
}

internal val Player.legends: AttribMap get() = questManager.getAttribs(Quest.LEGENDS_QUEST)
internal val Player.legendsStage: Int get() = getQuestStage(Quest.LEGENDS_QUEST)
internal fun Player.advanceLegends(stage: Int) {
    if (legendsStage in MAPPING until stage) setQuestStage(Quest.LEGENDS_QUEST, stage)
}
internal fun Player.inKharazi() = plane == 0 && x in 2720..2950 && y in 2880..2939
internal fun Player.inOctagram() = plane == 0 && x in 2789..2796 && y in 9325..9332
internal fun Player.legendsLevel(skill: Int, level: Int): Boolean {
    if (skills.getLevel(skill) >= level) return true
    sendMessage("You need ${Skills.SKILL_NAME[skill]} level $level to do this.")
    return false
}
internal fun Player.hasLegendsItem(id: Int) = inventory.containsItem(id) || bank.containsItem(id, 1) || equipment.weaponId == id
internal fun Player.giveLegendsItem(id: Int, amount: Int = 1): Boolean {
    if (hasLegendsItem(id)) {
        sendMessage("You already have that item.")
        return false
    }
    if (!inventory.hasFreeSlots()) {
        sendMessage("You need a free inventory slot.")
        return false
    }
    return inventory.addItem(id, amount)
}
internal fun Player.replaceLegendsItem(from: Int, to: Int): Boolean {
    val item = inventory.items.array().firstOrNull { it?.id == from } ?: return false
    item.id = to
    inventory.refresh()
    return true
}
internal fun Player.legendsDestination(tile: Tile, size: Int = 1): Tile? {
    if (World.floorAndWallsFree(tile, size)) return tile
    for (radius in 1..4) for (dx in -radius..radius) for (dy in -radius..radius) {
        if (abs(dx) != radius && abs(dy) != radius) continue
        val next = tile.transform(dx, dy, 0)
        if (World.floorAndWallsFree(next, size)) return next
    }
    sendMessage("The way is blocked. Please try again in a moment.")
    return null
}
