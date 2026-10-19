package com.rs.game.content.minigames.creations

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.content.ItemConstants
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.XPGainEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Bonus
import org.darkan.core.model.Item
import kotlin.math.roundToInt

private const val ARMOUR_BONUS_XP = 24_450.0
private const val SHIELD_DEFENCES = 2_000
private const val SHIELD_DRAIN_CHANCE = 2
private const val SHIELD_DOUBLE_SOAK_CHANCE = 4
private const val SOAK_THRESHOLD = 200

enum class CombatStyle(val label: String) { MELEE("Melee"), RANGED("Ranged"), MAGIC("Magic") }

enum class SacredClayArmour(val label: String, private val melee: Int, private val ranged: Int, private val magic: Int) {
    HEAD("head armour", Obj.sc_reward_helmet, Obj.sc_reward_coif, Obj.sc_reward_hat),
    BODY("body armour", Obj.sc_reward_platebody, Obj.sc_reward_leather_body, Obj.sc_reward_robetop),
    LEGS("leg armour", Obj.sc_reward_platelegs, Obj.sc_reward_chaps, Obj.sc_reward_robebottom),
    WEAPON("weapon", Obj.sc_reward_sword, Obj.sc_reward_bow, Obj.sc_reward_staff),
    SHIELD("shield", Obj.hlr4m_pk_shield_melee, Obj.hlr4m_pk_shield_ranged, Obj.hlr4m_pk_shield_magic);

    val ids = setOf(melee, ranged, magic)

    fun idFor(style: CombatStyle) = when (style) {
        CombatStyle.MELEE -> melee
        CombatStyle.RANGED -> ranged
        CombatStyle.MAGIC -> magic
    }

    fun styleOf(id: Int) = CombatStyle.entries.first { idFor(it) == id }

    companion object {
        val combatSkills = setOf(Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE, Constants.HITPOINTS, Constants.RANGE, Constants.MAGIC)
        val morphicIds = listOf(HEAD, BODY, LEGS, WEAPON).flatMap { it.ids }.toSet()
        val shieldIds = SHIELD.ids + Obj.hlr4m_pk_shield_statless

        fun forId(id: Int) = entries.firstOrNull { id in it.ids }
    }
}

@ServerStartupEvent
fun mapSacredClayEquipment() {
    onItemClick(*SacredClayArmour.morphicIds.toTypedArray(), options = arrayOf("Transform", "Check")) { (player, item, option) ->
        when (option) {
            "Check" -> checkEquipment(player, item)
            "Transform" -> chooseStyle(player, item) { style -> transformMorphic(player, item, style) }
        }
    }

    onItemClick(*SacredClayArmour.shieldIds.toTypedArray(), options = arrayOf("Morph", "Check")) { (player, item, option) ->
        when (option) {
            "Check" -> checkEquipment(player, item)
            "Morph" -> chooseStyle(player, item) { style -> morphShield(player, item, style) }
        }
    }

    ItemTooltips.metadata(SACRED_CLAY_CHARGE_KEY) { _, item, _ -> sacredClayDetails(item).joinToString("<br>") }
}

fun sacredClayDetails(item: Item): List<String> = if (isSacredClayTool(item.id)) sacredClayToolDetails(item) else sacredClayEquipmentDetails(item)

fun sacredClayEquipmentDetails(item: Item): List<String> {
    val charge = item.sacredClayCharge
    val lines = mutableListOf<String>()
    if (item.id in SacredClayArmour.morphicIds) {
        lines += "Xp bonus: ${ItemTooltips.value("+100%")} combat while worn"
        lines += "Bonus xp left: ${ItemTooltips.value((charge / FULL_SACRED_CLAY_CHARGE.toDouble() * ARMOUR_BONUS_XP).roundToInt())}"
        lines += "Form: ${ItemTooltips.value(SacredClayArmour.forId(item.id)!!.styleOf(item.id).label)}"
    } else if (item.id in SacredClayArmour.SHIELD.ids) {
        lines += "Defends: ${ItemTooltips.value(SacredClayArmour.SHIELD.styleOf(item.id).label)}"
        lines += "Hits left: about ${ItemTooltips.value(charge / (FULL_SACRED_CLAY_CHARGE / SHIELD_DEFENCES) * SHIELD_DRAIN_CHANCE)}"
    }
    return lines
}

private fun checkEquipment(player: Player, item: Item) {
    if (item.id == Obj.hlr4m_pk_shield_statless) {
        player.sendMessage("Your sacred clay shield is inactive. The Rewards mystic can recharge it.")
        return
    }
    player.sendSacredClayDetails(item, sacredClayEquipmentDetails(item))
}

private fun chooseStyle(player: Player, item: Item, onChosen: (CombatStyle) -> Unit) {
    val armour = SacredClayArmour.forId(item.id) ?: return
    if (item.sacredClayCharge <= 0) {
        player.sendMessage("Your ${item.name.lowercase()} has no charge left to transform with. The Rewards mystic can recharge it.")
        return
    }
    val current = armour.styleOf(item.id)
    player.sendOptionsDialogue("What should it transform into?") {
        for (style in CombatStyle.entries) if (style != current) opExec(style.label) { onChosen(style) }
    }
}

private fun transformMorphic(player: Player, item: Item, style: CombatStyle) {
    val clicked = player.heldItem(item) ?: return
    val worn = player.wornItems(SacredClayArmour.morphicIds)
    val pieces = (if (clicked.worn) worn else worn + clicked).filter { it.item.sacredClayCharge > 0 }
    val targets = pieces.map { it to SacredClayArmour.forId(it.item.id)!!.idFor(style) }
    if (targets.any { (piece, id) -> piece.worn && !ItemConstants.canWear(Item(id), player) }) return
    targets.forEach { (piece, id) -> if (piece.item.id != id) piece.changeId(id) }
    player.sendMessage("Your sacred clay ${if (targets.size > 1) "equipment transforms" else "${SacredClayArmour.forId(item.id)!!.label} transforms"} into ${style.label.lowercase()} gear.")
}

private fun morphShield(player: Player, item: Item, style: CombatStyle) {
    val held = player.heldItem(item) ?: return
    val id = SacredClayArmour.SHIELD.idFor(style)
    if (held.worn && !ItemConstants.canWear(Item(id), player)) return
    held.changeId(id)
    player.sendMessage("Your sacred clay shield morphs to defend against ${style.label.lowercase()} attacks.")
}

fun applySacredClayArmourBonus(e: XPGainEvent) {
    val charged = e.player.wornItems(SacredClayArmour.morphicIds).filter { it.item.sacredClayCharge > 0 }
    if (charged.isEmpty()) return
    e.multiplier += 1.0
    val drain = (e.xp / ARMOUR_BONUS_XP * FULL_SACRED_CLAY_CHARGE / charged.size).roundToInt().coerceAtLeast(1)
    for (piece in charged) {
        val charge = piece.item.sacredClayCharge - drain
        piece.item.setSacredClayCharge(charge)
        if (charge <= 0) e.player.sendMessage("<col=FF0000>Your ${piece.item.name.lowercase()} has run out of charge.</col>")
    }
}

fun defendWithSacredClayShield(player: Player, hit: Hit) {
    if (hit.damage <= 0) return
    val shield = player.equipment.getItem(Equipment.SHIELD) ?: return
    if (shield.id !in SacredClayArmour.SHIELD.ids) return
    if (hit.damage >= SOAK_THRESHOLD && Utils.random(SHIELD_DOUBLE_SOAK_CHANCE) == 0) {
        val absorb = when (hit.look) {
            HitLook.MELEE_DAMAGE -> Bonus.ABSORB_MELEE
            HitLook.RANGE_DAMAGE -> Bonus.ABSORB_RANGE
            else -> Bonus.ABSORB_MAGIC
        }
        val soaked = hit.damage * player.combatDefinitions.getBonus(absorb) / 100
        if (soaked > 0) {
            hit.setDamage(hit.damage - soaked)
            hit.addSoaking(soaked)
        }
    }
    if (Utils.random(SHIELD_DRAIN_CHANCE) != 0) return
    val charge = shield.sacredClayCharge - FULL_SACRED_CLAY_CHARGE / SHIELD_DEFENCES
    if (charge > 0) {
        shield.setSacredClayCharge(charge)
        return
    }
    shield.getMetaData()?.remove(SACRED_CLAY_CHARGE_KEY)
    player.heldItem(shield)?.changeId(Obj.hlr4m_pk_shield_statless)
    player.sendMessage("<col=FF0000>Your sacred clay shield has run out of charge and become inactive.</col>")
}
