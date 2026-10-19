package com.rs.game.content.skills.runecrafting

import com.rs.game.content.minigames.fistofguthix.FogGloves
import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.engine.quest.Quest
import com.rs.game.content.combat.CombatDefinitions.Spellbook
import com.rs.game.content.skills.summoning.Pouch
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemEquip
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.drop.Drop
import com.rs.utils.drop.DropList
import com.rs.utils.drop.DropSet
import com.rs.utils.drop.DropTable
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varp
import world.gregs.voidps.type.Tile

// TODO: top-level and Player extension functions once RunecraftingAltar and the Runespan code are Kotlin
object Runecrafting {
    const val RUNE_ESS = Obj.blankrune
    const val PURE_ESS = Obj.blankrune_high
    const val BINDING_NECKLACE = Obj.magic_emerald_necklace

    private const val RCING_SUIT_XP_BONUS = 1.025
    private val RCING_SUIT_PIECES = listOf(
        RcingSuitPiece(Equipment.HEAD, 0.004, Obj.hlr4m_runecrafter_hat_poh, Obj.obj_21517),
        RcingSuitPiece(Equipment.CHEST, 0.008, Obj.hlr4m_runecrafter_robe_poh, Obj.obj_21516),
        RcingSuitPiece(Equipment.LEGS, 0.006, Obj.hlr4m_runecrafter_legs_poh, Obj.obj_21518),
        RcingSuitPiece(Equipment.FEET, 0.002, Obj.hlr4m_runecrafter_boots_poh, Obj.obj_21519),
    )

    private class RcingSuitPiece(val slot: Int, val bonus: Double, vararg val objs: Int)
    private const val ZMI_XP_BONUS = 2.0

    enum class RCRune(val req: Int, val xp: Double, val id: Int, val isPureEss: Boolean, vararg val multipliers: Int) {
        AIR(1, 5.0, Obj.airrune, false, 11, 2, 22, 3, 33, 4, 44, 5, 55, 6, 66, 7, 77, 8, 88, 9, 99, 10),
        MIND(1, 5.5, Obj.mindrune, false, 14, 2, 28, 3, 42, 4, 56, 5, 70, 6, 84, 7, 98, 8),
        WATER(5, 6.0, Obj.waterrune, false, 19, 2, 38, 3, 57, 4, 76, 5, 95, 6),
        EARTH(9, 6.5, Obj.earthrune, false, 26, 2, 52, 3, 78, 4),
        FIRE(14, 7.0, Obj.firerune, false, 35, 2, 70, 3),
        BODY(20, 7.5, Obj.bodyrune, false, 46, 2, 92, 3),
        COSMIC(27, 8.0, Obj.cosmicrune, true, 59, 2),
        CHAOS(35, 8.5, Obj.chaosrune, true, 74, 2),
        ASTRAL(40, 8.7, Obj.astralrune, true, 82, 2),
        NATURE(44, 9.0, Obj.naturerune, true, 91, 2),
        LAW(54, 9.5, Obj.lawrune, true),
        DEATH(65, 10.0, Obj.deathrune, true),
        BLOOD(77, 10.5, Obj.bloodrune, true),
        SOUL(90, 12.0, Obj.soulrune, true);

        fun multiplier(level: Int): Int {
            for (i in multipliers.size - 2 downTo 0 step 2)
                if (level >= multipliers[i]) return multipliers[i + 1]
            return 1
        }

        companion object {
            private val byRuneId = entries.associateBy { it.id }

            @JvmStatic
            fun forId(itemId: Int) = byRuneId[itemId]
        }
    }

    private enum class ZMIRune(xp: Double, val id: Int, vararg val chances: Double) {
        AIR(5.0, Obj.airrune, 50.0, 15.0, 12.0, 7.0, 6.0, 5.0, 4.5, 3.0, 2.0, 1.0, 1.0),
        MIND(5.5, Obj.mindrune, 25.0, 18.0, 13.0, 8.0, 6.5, 5.5, 5.0, 3.0, 2.0, 1.0, 1.0),
        WATER(6.0, Obj.waterrune, 12.0, 21.0, 13.5, 9.0, 7.0, 6.0, 5.5, 3.0, 3.0, 2.0, 2.0),
        EARTH(6.5, Obj.earthrune, 6.0, 24.0, 14.0, 11.0, 7.5, 6.5, 6.0, 4.0, 4.0, 3.0, 3.0),
        FIRE(7.0, Obj.firerune, 3.0, 12.0, 15.0, 12.0, 8.0, 7.0, 7.0, 4.0, 5.0, 4.0, 3.0),
        BODY(7.5, Obj.bodyrune, 1.5, 6.0, 16.0, 13.0, 10.0, 7.5, 7.5, 5.0, 6.0, 5.0, 4.0),
        COSMIC(8.0, Obj.cosmicrune, 0.85, 1.75, 8.0, 20.0, 15.0, 10.0, 9.5, 7.0, 7.0, 6.0, 5.0),
        CHAOS(8.5, Obj.chaosrune, 0.6, 0.8, 4.2, 10.0, 20.0, 11.0, 10.5, 9.0, 8.0, 7.0, 6.0),
        ASTRAL(8.7, Obj.astralrune, 0.45, 0.6, 2.1, 5.0, 10.0, 15.0, 14.0, 12.0, 10.5, 10.0, 9.5),
        NATURE(9.0, Obj.naturerune, 0.3, 0.4, 1.1, 2.5, 5.0, 13.5, 15.5, 15.0, 13.5, 13.5, 13.5),
        LAW(9.5, Obj.lawrune, 0.15, 0.24, 0.55, 1.3, 2.6, 7.0, 8.0, 18.0, 14.5, 14.5, 14.5),
        DEATH(10.0, Obj.deathrune, 0.08, 0.12, 0.32, 0.6, 1.2, 3.5, 4.0, 10.0, 14.5, 16.5, 15.5),
        BLOOD(10.5, Obj.bloodrune, 0.05, 0.06, 0.15, 0.4, 0.8, 1.7, 2.0, 5.0, 6.0, 10.0, 13.0),
        SOUL(12.0, Obj.soulrune, 0.02, 0.03, 0.08, 0.2, 0.4, 0.8, 1.0, 2.0, 4.0, 6.5, 9.0);

        val xp = 1.7 * xp

        companion object {
            private const val TIERS = 11
            private val byId = entries.associateBy { it.id }
            private val tiers: List<DropList> = List(TIERS) { tier ->
                DropSet(*entries.map { DropTable(it.chances[tier], 100.0, Drop(it.id)) }.toTypedArray()).dropList
            }

            fun roll(level: Int): ZMIRune {
                val tier = Utils.clampI(if (level >= 99) TIERS - 1 else level / 10, 0, TIERS - 1)
                val drop = tiers[tier].genDrop()
                return if (drop.isEmpty()) AIR else byId.getValue(drop.first().id)
            }
        }
    }

    @JvmStatic
    fun craftTalisman(player: Player, talisman: RunecraftingTalisman) {
        player.sendOptionsDialogue("What would you like to imbue?") {
            opExec("Tiara") {
                if (!player.inventory.containsItem(Obj.tiara, 1) || !player.inventory.containsItem(talisman.talismanId, 1)) {
                    player.sendMessage("You need a tiara to do this.")
                    return@opExec
                }
                player.inventory.deleteItem(Obj.tiara, 1)
                player.inventory.deleteItem(talisman.talismanId, 1)
                player.skills.addXp(Skills.RUNECRAFTING, talisman.tiaraExp)
                player.inventory.addItem(talisman.tiaraId, 1)
            }
            opExec("Staff") {
                if (!player.inventory.containsItem(Obj.rcguild_staff_blank, 1) || !player.inventory.containsItem(talisman.talismanId, 1)) {
                    player.sendMessage("You need a runecrafting staff to do this.")
                    return@opExec
                }
                if (player.skills.getLevel(Skills.RUNECRAFTING) < talisman.staffLevelReq) {
                    player.sendMessage("You need a runecrafting level of ${talisman.staffLevelReq} to create a ${ItemDefinitions.getDefs(talisman.staffId).name}.")
                    return@opExec
                }
                player.inventory.deleteItem(Obj.rcguild_staff_blank, 1)
                player.inventory.deleteItem(talisman.talismanId, 1)
                player.skills.addXp(Skills.RUNECRAFTING, talisman.staffExp)
                player.inventory.addItem(talisman.staffId, 1)
            }
        }
    }

    @JvmStatic
    fun craftCombinationRune(player: Player, combination: AltarCombination): Boolean {
        if (player.skills.getLevel(Skills.RUNECRAFTING) < combination.levelReq) return false
        val pureEss = player.inventory.getNumberOf(PURE_ESS)
        if (pureEss == 0) return false
        val reagentRunes = player.inventory.getNumberOf(combination.reagentRune.id)
        if (reagentRunes == 0) return false
        if (!player.isCastMagicImbue) {
            if (!player.inventory.containsItem(combination.talisman.talismanId, 1)) return false
            player.inventory.deleteItem(combination.talisman.talismanId, 1)
        }

        var crafted = minOf(reagentRunes, pureEss)
        player.inventory.deleteItem(PURE_ESS, crafted)
        player.inventory.deleteItem(combination.reagentRune.id, crafted)

        var xp = combination.xp
        xp *= rcingSuitBonus(player)

        val runeName = ItemDefinitions.getDefs(combination.outputRuneId).name
        if (player.equipment.amuletId == BINDING_NECKLACE) {
            player.sendMessage("You bind the temple's power into ${runeName}s.")
            player.bindingNecklaceCharges--
            if (player.bindingNecklaceCharges <= 0) {
                player.equipment.deleteSlot(Equipment.NECK)
                player.sendMessage("Your binding necklace disintegrates.")
                player.bindingNecklaceCharges = 15
            }
        } else {
            player.sendMessage("You attempt to bind ${runeName}s.")
            crafted /= 2
        }
        player.inventory.addItem(combination.outputRuneId, crafted)
        player.skills.addXp(Skills.RUNECRAFTING, xp * crafted)
        return true
    }

    private fun takeFamiliarEssence(player: Player, includeRuneEssence: Boolean): Int {
        when (player.familiarPouch) {
            Pouch.ABYSSAL_PARASITE, Pouch.ABYSSAL_LURKER, Pouch.ABYSSAL_TITAN -> {
                val inventory = player.familiar.inventory
                val taken = if (includeRuneEssence) inventory.usedSlots else inventory.getNumberOf(PURE_ESS)
                if (includeRuneEssence) inventory.removeAll(RUNE_ESS)
                inventory.removeAll(PURE_ESS)
                return taken
            }
            else -> return 0
        }
    }

    @JvmStatic
    fun craftZMIAltar(player: Player) {
        var essence = player.inventory.items.getNumberOf(PURE_ESS)
        essence += player.takePouchEssence(false)
        essence += takeFamiliarEssence(player, false)
        if (essence == 0) {
            player.simpleDialogue("You don't have pure essence.")
            return
        }
        player.inventory.deleteItem(PURE_ESS, essence)
        var xp = 0.0
        repeat(essence) {
            val rune = ZMIRune.roll(player.skills.getLevel(Skills.RUNECRAFTING))
            val amount = Utils.random(1, 3)
            player.incrementCount("${ItemDefinitions.getDefs(rune.id).name} runecrafted", amount)
            player.inventory.addItem(rune.id, amount)
            xp += rune.xp
        }
        xp *= rcingSuitBonus(player)
        player.skills.addXp(Skills.RUNECRAFTING, xp * ZMI_XP_BONUS)
        playCraftAnimation(player)
        player.sendMessage("You bind the temple's power into assorted runes.")
    }

    private fun playCraftAnimation(player: Player) {
        player.spotAnim(Spotanim.runecrafting)
        player.anim(Seq.seq_791)
        player.lock(5)
    }

    @JvmStatic
    @JvmOverloads
    fun runecraft(player: Player, rune: RCRune, span: Boolean, xpMultiplier: Double = 1.0) {
        val level = player.skills.getLevel(Skills.RUNECRAFTING)
        if (level < rune.req) {
            player.simpleDialogue("You need a runecrafting level of ${rune.req} to craft this rune.")
            return
        }
        var essence = player.inventory.items.getNumberOf(PURE_ESS)
        if (!span) {
            essence += player.takePouchEssence(!rune.isPureEss)
            essence += takeFamiliarEssence(player, !rune.isPureEss)
        }
        if (essence > 0) {
            if (span) essence = 1
            player.inventory.deleteItem(PURE_ESS, essence)
        }
        if (!rune.isPureEss) {
            var runeEssence = player.inventory.items.getNumberOf(RUNE_ESS)
            if (span) runeEssence = if (essence > 0) 0 else 1
            if (runeEssence > 0) {
                player.inventory.deleteItem(RUNE_ESS, runeEssence)
                essence += runeEssence
            }
        }
        if (essence == 0) {
            player.simpleDialogue("You don't have ${if (rune.isPureEss) "pure" else "rune"} essence.")
            return
        }
        if (rune == RCRune.AIR && player.inventory.containsItem(Obj.obj_21774, 1) && level >= 72) {
            craftArmadylRunes(player, rune, essence, span)
            return
        }
        var xp = rune.xp * essence * xpMultiplier
        xp *= rcingSuitBonus(player)
        val crafted = essence * rune.multiplier(level)
        player.skills.addXp(Skills.RUNECRAFTING, xp + FogGloves.runecraftingBonus(player, rune.id, rune.xp, essence))
        if (!span) {
            playCraftAnimation(player)
            player.sendMessage("You bind the temple's power into ${ItemDefinitions.getDefs(rune.id).name.lowercase()}s.")
        }
        player.inventory.addItem(rune.id, crafted)
        player.incrementCount("${ItemDefinitions.getDefs(rune.id).name} runecrafted", crafted)
    }

    private fun craftArmadylRunes(player: Player, rune: RCRune, essence: Int, span: Boolean) {
        val shards = minOf(player.inventory.getAmountOf(Obj.obj_21774), essence)
        val crafted = shards * numberPerShard(player.skills.getLevel(Skills.RUNECRAFTING))
        player.inventory.deleteItem(Obj.obj_21774, shards)
        if (!span) {
            playCraftAnimation(player)
            player.sendMessage("You bind the temple's power into Armadyl runes.")
        }
        player.skills.addXp(Skills.RUNECRAFTING, rune.xp * essence)
        player.inventory.addItem(Obj.mah5_armadyl_rune, crafted)
        player.inventory.addItem(PURE_ESS, essence - shards - 1)
        player.incrementCount("Armadyl rune runecrafted", crafted)
    }

    @JvmStatic
    fun numberPerShard(level: Int) = when {
        level >= 99 -> 10
        level in 90..98 -> 9
        level in 77..87 -> 8
        level in 72..76 -> 7
        else -> 1
    }

    @JvmStatic
    fun hasRcingSuit(player: Player) = RCING_SUIT_PIECES.all { player.equipment.getIdInSlot(it.slot) in it.objs }

    private fun rcingSuitBonus(player: Player): Double =
        if (hasRcingSuit(player)) RCING_SUIT_XP_BONUS
        else 1.0 + RCING_SUIT_PIECES.filter { player.equipment.getIdInSlot(it.slot) in it.objs }.sumOf { it.bonus }

    @JvmStatic
    fun locate(player: Player, ruinsX: Int, ruinsY: Int) {
        val north = player.y < ruinsY
        val south = player.y > ruinsY
        val direction = when {
            player.x < ruinsX -> if (north) "north-east" else if (south) "south-east" else "east"
            player.x > ruinsX -> if (north) "north-west" else if (south) "south-west" else "west"
            north -> "north"
            south -> "south"
            else -> ""
        }
        player.sendMessage("The talisman pulls towards the $direction.", false)
    }
}

private val altarRunes = mapOf(
    Loc.loc_2478 to Runecrafting.RCRune.AIR,
    Loc.loc_2479 to Runecrafting.RCRune.MIND,
    Loc.loc_2480 to Runecrafting.RCRune.WATER,
    Loc.loc_2481 to Runecrafting.RCRune.EARTH,
    Loc.loc_2482 to Runecrafting.RCRune.FIRE,
    Loc.loc_2483 to Runecrafting.RCRune.BODY,
    Loc.loc_2484 to Runecrafting.RCRune.COSMIC,
    Loc.loc_2485 to Runecrafting.RCRune.LAW,
    Loc.loc_2486 to Runecrafting.RCRune.NATURE,
    Loc.loc_2487 to Runecrafting.RCRune.CHAOS,
    Loc.loc_2488 to Runecrafting.RCRune.DEATH,
    Loc.loc_17010 to Runecrafting.RCRune.ASTRAL,
    Loc.loc_30624 to Runecrafting.RCRune.BLOOD,
)

private const val ZMI_ALTAR = Loc.loc_26847
private const val ASTRAL_ALTAR_SPELLBOOK_OP = 2

@ServerStartupEvent
fun mapRunecrafting() {
    onItemEquip(
        Obj.tiara_air, Obj.tiara_water, Obj.tiara_body, Obj.tiara_earth, Obj.tiara_fire, Obj.tiara_cosmic, Obj.tiara_nature,
        Obj.tiara_chaos, Obj.tiara_law, Obj.tiara_death, Obj.tiara_blood, Obj.tiara_soul, Obj.tiara_astral, Obj.tiara_omni,
    ) { e -> e.player.vars.setVar(Varp.abyssal_warp, if (e.equip()) 1 else 0) }

    onObjectClick(*(altarRunes.keys + ZMI_ALTAR).toTypedArray()) { e ->
        if (e.objectId == Loc.loc_17010 && e.opNum == ASTRAL_ALTAR_SPELLBOOK_OP) {
            toggleLunarSpellbook(e.player)
            return@onObjectClick
        }
        if (e.objectId == ZMI_ALTAR) Runecrafting.craftZMIAltar(e.player)
        else altarRunes[e.objectId]?.let { Runecrafting.runecraft(e.player, it, false) }
    }

    onObjectClick(Loc.rc_zmi_dungeon_entrance, Loc.rc_zmi_dungeon_entrance_ladder) { e ->
        e.player.anim(Seq.human_reachforladder)
        val destination = if (e.objectId == Loc.rc_zmi_dungeon_entrance) Tile.of(3271, 4861, 0) else Tile.of(2452, 3232, 0)
        WorldTasks.delay(1) { e.player.tele(destination) }
    }
}

private fun toggleLunarSpellbook(player: Player) {
    player.sendOptionsDialogue("Change spellbooks?") {
        opExec("Yes, replace my spellbook.") {
            if (player.combatDefinitions.getSpellbook() != Spellbook.LUNAR) {
                if (!player.isQuestComplete(Quest.LUNAR_DIPLOMACY, "to use the Lunar Spellbook.")) return@opExec
                player.sendMessage("Lunar spells activated!")
                player.combatDefinitions.setSpellbook(Spellbook.LUNAR)
            } else {
                player.sendMessage("Your mind clears and you switch back to the normal spellbook.")
                player.combatDefinitions.setSpellbook(Spellbook.MODERN)
            }
        }
        op("Nevermind.")
    }
}
