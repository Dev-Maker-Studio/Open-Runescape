package com.rs.game.content.minigames.pest

import com.rs.game.content.combat.CombatMod
import com.rs.game.content.combat.CombatStyle
import com.rs.game.content.combat.onCombatFormulaAdjust
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.gameval.gen.Obj

private const val SET_ROLES_REQUIRED = 3
private const val MELEE_BONUS = 1.1
private const val RANGED_BONUS = 1.1
private const val MAGIC_ACCURACY_BONUS = 1.3
private val MELEE_HELMS = setOf(Obj.obj_11665, Obj.dummy_game_pest_melee_helm)
private val RANGER_HELMS = setOf(Obj.obj_11664, Obj.dummy_game_pest_archer_helm)
private val MAGE_HELMS = setOf(Obj.obj_11663, Obj.dummy_game_pest_mage_helm)
private val TOPS = setOf(Obj.pest_void_knight_top, Obj.dummy_pest_void_knight_top, Obj.vkq3_reward_torso_black, Obj.vkq3_reward_torso_grey, Obj.vkq3_reward_torso_white)
private val ROBES = setOf(Obj.obj_8840, Obj.vkq3_reward_legs_black, Obj.vkq3_reward_legs_grey, Obj.vkq3_reward_legs_white)

@ServerStartupEvent
fun mapVoidKnightEquipment() {
    onCombatFormulaAdjust { attacker, _, _, style ->
        val player = attacker as? Player ?: return@onCombatFormulaAdjust CombatMod()
        when (style) {
            CombatStyle.MELEE -> if (player.wearsVoidSet(MELEE_HELMS)) CombatMod(accuracyLevel = MELEE_BONUS, strengthLevel = MELEE_BONUS) else CombatMod()
            CombatStyle.RANGE -> if (player.wearsVoidSet(RANGER_HELMS)) CombatMod(accuracyLevel = RANGED_BONUS, strengthLevel = RANGED_BONUS) else CombatMod()
            CombatStyle.MAGIC -> if (player.wearsVoidSet(MAGE_HELMS)) CombatMod(accuracyLevel = MAGIC_ACCURACY_BONUS) else CombatMod()
        }
    }
}

private fun Player.wearsVoidSet(helms: Set<Int>): Boolean {
    if (equipment.hatId !in helms) return false
    val roles = listOf(equipment.chestId in TOPS, equipment.legsId in ROBES, equipment.glovesId == Obj.pest_void_knight_gloves, equipment.shieldId == Obj.conq_reward_shield)
    return roles.count { it } >= SET_ROLES_REQUIRED
}
