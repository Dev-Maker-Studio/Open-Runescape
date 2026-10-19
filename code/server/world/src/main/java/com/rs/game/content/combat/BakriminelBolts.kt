package com.rs.game.content.combat

import com.rs.game.content.Effect
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.cache.definition.data.ItemDefinition
import com.rs.plugin.annotations.ServerStartupEvent
import kotlin.math.ceil

object BakriminelBolts {
    const val BOLTS = Obj.sniper_bolts

    const val HEAD = 0
    const val TORSO = 1
    const val LEGS = 2

    private val HEAD_DURATION = Ticks.fromSeconds(30)
    private val BLEED_DURATION = Ticks.fromMinutes(1)
    private val BIND_DURATION = Ticks.fromSeconds(10)
    private const val MAX_BLEED_STACKS = 5
    private const val BLEED_DAMAGE_PER_STACK = 1
    private const val BLEED_TICK_INTERVAL = 2L

    private const val HEAD_IMPACT = Spotanim.sniper_hit_head_impact
    private const val TORSO_IMPACT = Spotanim.sniper_hit_torso_impact
    private const val LEGS_IMPACT = Spotanim.sniper_hit_leg_impact

    private const val STAFF_WEAPON_TYPE = 1
    private const val STAFF_OF_LIGHT_WEAPON_TYPE = 26

    @JvmStatic
    fun firedBy(weaponId: Int, ammoId: Int): Boolean =
        ammoId == BOLTS && RangedWeapon.forId(weaponId)?.ammos?.contains(AmmoType.BAKRIMINEL_BOLTS) == true

    private fun impact(part: Int): Int = when (part) {
        HEAD -> HEAD_IMPACT
        TORSO -> TORSO_IMPACT
        else -> LEGS_IMPACT
    }

    @JvmStatic
    @ServerStartupEvent
    fun registerBakriminelBoltEffects() {
        onHitLanded { player, target, weaponId, attackStyle, hit ->
            if (attackStyle != null && firedBy(weaponId, player.equipment.ammoId)) onHit(player, target, attackStyle, hit)
        }
        onCombatFormulaAdjust { entity, _, _, combatStyle ->
            if (combatStyle != CombatStyle.MAGIC) return@onCombatFormulaAdjust CombatMod()
            val player = entity as? Player ?: return@onCombatFormulaAdjust CombatMod()
            CombatMod(accuracy = magicAccuracyMultiplier(player))
        }
        onCombatDelayAdjust { player, style ->
            if (style == CombatStyle.MAGIC && player.hasEffect(Effect.BAKRIMINEL_MAGIC_SLOW)) 1 else 0
        }
    }

    fun onHit(attacker: Player, target: Entity, attackStyle: AttackStyle, hit: Hit) {
        if (hit.damage <= 0) return
        if (target !is Player) return
        if (!target.isCanPvp || !attacker.isCanPvp) return
        val part = attackStyle.index
        if (!isMagicVulnerable(target, part)) return
        when (part) {
            HEAD -> head(target)
            TORSO -> torso(target)
            LEGS -> legs(target, hit.damage)
        }
    }

    fun isMagicVulnerable(target: Player, part: Int): Boolean {
        val slot = when (part) {
            HEAD -> Equipment.HEAD
            TORSO -> Equipment.CHEST
            else -> Equipment.LEGS
        }
        val item = target.equipment.getItem(slot) ?: return true
        return isMagicArmour(item.id) || isMagicWeapon(target.equipment.weaponId)
    }

    private fun isMagicArmour(itemId: Int): Boolean {
        val bonuses = ItemDefinitions.getDefs(itemId).getBonuses()
        val meleeAttack = maxOf(
            bonuses[ItemDefinition.STAB_ATTACK],
            bonuses[ItemDefinition.SLASH_ATTACK],
            bonuses[ItemDefinition.CRUSH_ATTACK],
        )
        return bonuses[ItemDefinition.MAGIC_ATTACK] > 0 && meleeAttack <= 0
    }

    private fun isMagicWeapon(itemId: Int): Boolean {
        val weaponType = ItemDefinitions.getDefs(itemId).getParamVal(686)
        return weaponType == STAFF_WEAPON_TYPE || weaponType == STAFF_OF_LIGHT_WEAPON_TYPE
    }

    private fun head(target: Player) {
        if (target.hasEffect(Effect.BAKRIMINEL_MAGIC_SLOW)) {
            if (Utils.random(3) == 0) {
                target.vars.setVarBit(Varbit.sniper_drained_amount, Utils.random(10, 26))
                target.sendMessage("The bakriminel bolt saps your magical accuracy.", true)
                target.spotAnim(impact(HEAD), 0, 96)
            }
            return
        }
        if (Utils.random(3) == 0) {
            target.addEffect(Effect.BAKRIMINEL_MAGIC_SLOW, HEAD_DURATION.toLong())
            target.vars.setVarBit(Varbit.sniper_is_slowed, 1)
            target.spotAnim(impact(HEAD), 0, 96)
        }
    }

    private fun torso(target: Player) {
        if (Utils.random(4) != 0) return
        val stacks = (target.vars.getVarBit(Varbit.sniper_bleed_count) + 1).coerceAtMost(MAX_BLEED_STACKS)
        target.vars.setVarBit(Varbit.sniper_bleed_count, stacks)
        target.vars.setVarBit(Varbit.sniper_bleed_damage, stacks * BLEED_DAMAGE_PER_STACK)
        if (target.hasEffect(Effect.BAKRIMINEL_BLEED))
            target.extendEffect(Effect.BAKRIMINEL_BLEED, BLEED_DURATION.toLong())
        else
            target.addEffect(Effect.BAKRIMINEL_BLEED, BLEED_DURATION.toLong())
        target.sendMessage("The bakriminel bolt rips into your flesh.", true)
        target.spotAnim(impact(TORSO), 0, 96)
    }

    private fun legs(target: Player, damage: Int) {
        if (Utils.random(3) != 0) return
        val before = target.runEnergy
        target.drainRunEnergy(ceil(damage * 0.2).coerceAtLeast(1.0))
        target.spotAnim(impact(LEGS), 0, 96)
        if (before <= 0.0 || target.runEnergy <= 0.0) {
            target.freeze(BIND_DURATION)
            target.sendMessage("The bakriminel bolt binds you to the spot.", true)
        } else {
            target.sendMessage("The bakriminel bolt drains your run energy.", true)
        }
    }

    fun bleedTick(target: Player, tickNumber: Long) {
        val damage = target.vars.getVarBit(Varbit.sniper_bleed_damage)
        if (tickNumber % BLEED_TICK_INTERVAL == 0L)
            target.applyHit(Hit(target, damage.coerceAtLeast(BLEED_DAMAGE_PER_STACK), HitLook.TRUE_DAMAGE))
    }

    fun clearBleedStacks(target: Player) {
        target.vars.setVarBit(Varbit.sniper_bleed_count, 0)
        target.vars.setVarBit(Varbit.sniper_bleed_damage, 0)
    }

    fun clearMagicSlow(target: Player) {
        target.vars.setVarBit(Varbit.sniper_is_slowed, 0)
        target.vars.setVarBit(Varbit.sniper_drained_amount, 0)
    }

    fun magicAccuracyMultiplier(target: Player): Double {
        val drain = target.vars.getVarBit(Varbit.sniper_drained_amount)
        return if (drain <= 0) 1.0 else (1.0 - drain / 100.0).coerceAtLeast(0.0)
    }
}
