package com.rs.game.content.minigames.creations

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private enum class ShopReward(
    val label: String,
    val selectedVarbit: Int,
    val checkbox: Int,
    val row: Int,
    val cost: Int,
    val rechargeCost: Int,
    val product: Int,
    val rechargeable: Set<Int>,
) {
    VOLATILE_TOOL("volatile tool", Varbit.hlr4m_sc_volatile, Component.interface_1128.component_1128_4, Component.interface_1128.component_1128_98, 20, 20, Obj.sc_reward_volatile_tool, SacredClayTool.volatileForms + Obj.sc_reward_volatile_tool),
    PROTO_TOOL("proto-tool", Varbit.hlr4m_sc_proto, Component.interface_1128.component_1128_106, Component.interface_1128.component_1128_128, 20, 20, Obj.sc_reward_tool, SacredClayTool.protoForms + Obj.sc_reward_tool),
    BODY("morphic body armour", Varbit.hlr4m_sc_body, Component.interface_1128.component_1128_144, Component.interface_1128.component_1128_166, 30, 20, Obj.sc_reward_platebody, SacredClayArmour.BODY.ids),
    LEGS("morphic leg armour", Varbit.hlr4m_sc_legs, Component.interface_1128.component_1128_181, Component.interface_1128.component_1128_203, 28, 20, Obj.sc_reward_platelegs, SacredClayArmour.LEGS.ids),
    HEAD("morphic head armour", Varbit.hlr4m_sc_head, Component.interface_1128.component_1128_218, Component.interface_1128.component_1128_240, 24, 20, Obj.sc_reward_helmet, SacredClayArmour.HEAD.ids),
    WEAPON("morphic weapon", Varbit.hlr4m_sc_weapon, Component.interface_1128.component_1128_255, Component.interface_1128.component_1128_277, 26, 20, Obj.sc_reward_sword, SacredClayArmour.WEAPON.ids),
    SHIELD("morphic shield", Varbit.hlr4m_sc_shield, Component.interface_1128.component_1128_292, Component.interface_1128.component_1128_314, 200, 200, Obj.hlr4m_pk_shield_melee, SacredClayArmour.shieldIds);

    companion object {
        fun forComponent(component: Int) = entries.firstOrNull { component == it.checkbox || component == it.row }
    }
}

@ServerStartupEvent
fun mapStealingCreationShop() {
    onNpcClick(Npc.sc_rewards_mystic, options = arrayOf("Exchange")) { (player) -> openStealingCreationShop(player) }

    onButtonClick(Interface.interface_1128) { e ->
        when (e.component) {
            Component.interface_1128.component_1128_45 -> purchase(e.player)
            Component.interface_1128.component_1128_330 -> recharge(e.player)
            else -> ShopReward.forComponent(e.component)?.let { toggle(e.player, it) }
        }
    }
}

fun openStealingCreationShop(player: Player) {
    player.vars.setVarBit(Varbit.sc_points, player.scPoints)
    clearSelection(player)
    player.interfaceManager.sendInterface(Interface.interface_1128)
    player.setCloseInterfacesEvent { clearSelection(player) }
}

private fun selected(player: Player) = ShopReward.entries.filter { player.vars.getVarBit(it.selectedVarbit) == 1 }

private fun toggle(player: Player, reward: ShopReward) =
    player.vars.setVarBit(reward.selectedVarbit, 1 - player.vars.getVarBit(reward.selectedVarbit))

private fun clearSelection(player: Player) = ShopReward.entries.forEach { player.vars.setVarBit(it.selectedVarbit, 0) }

private fun refreshPoints(player: Player) {
    player.vars.setVarBit(Varbit.sc_points, player.scPoints)
    player.packets.sendRunScript(Cs2.cs2_5248)
}

private fun purchase(player: Player) {
    val rewards = selected(player)
    clearSelection(player)
    if (rewards.isEmpty()) {
        player.sendMessage("Tick the rewards you would like to purchase first.")
        return
    }
    val cost = rewards.sumOf { it.cost }
    if (!canAfford(player, cost)) return
    confirm(player, "Buy ${rewards.joinToString(", ") { it.label }} for $cost points?", cost) {
        if (!canAfford(player, cost)) return@confirm
        if (player.inventory.freeSlots < rewards.size) {
            player.sendMessage("You need ${rewards.size} free inventory slots for that.")
            return@confirm
        }
        player.scPoints -= cost
        rewards.forEach { player.inventory.addItem(sacredClayItem(it.product)) }
        refreshPoints(player)
        player.sendMessage("You spend $cost reward points. You have ${player.scPoints} left.")
    }
}

private fun rechargeTarget(player: Player, reward: ShopReward) = player.heldItems(reward.rechargeable)
    .map { it to if (it.item.id == Obj.hlr4m_pk_shield_statless) 0 else it.item.sacredClayCharge }
    .filter { (_, charge) -> charge < FULL_SACRED_CLAY_CHARGE }
    .minByOrNull { (_, charge) -> charge }?.first

private fun recharge(player: Player) {
    val rewards = selected(player)
    clearSelection(player)
    if (rewards.isEmpty()) {
        player.sendMessage("Tick the rewards you would like recharged first.")
        return
    }
    val targets = rewards.mapNotNull { reward -> rechargeTarget(player, reward)?.let { reward to it } }
    rewards.filter { reward -> targets.none { it.first == reward } }.forEach { player.sendMessage("You don't have a ${it.label} that needs recharging.") }
    if (targets.isEmpty()) return
    val cost = targets.sumOf { it.first.rechargeCost }
    if (!canAfford(player, cost)) return
    confirm(player, "Recharge your ${targets.joinToString(", ") { it.first.label }} for $cost points?", cost) {
        if (!canAfford(player, cost)) return@confirm
        val held = targets.filter { (_, piece) -> player.heldItem(piece.item) != null }
        if (held.size != targets.size) return@confirm
        player.scPoints -= cost
        for ((_, piece) in held) {
            if (piece.item.id == Obj.hlr4m_pk_shield_statless) piece.changeId(Obj.hlr4m_pk_shield_melee)
            piece.item.restoreSacredClayCharge()
        }
        refreshPoints(player)
        player.sendMessage("You spend $cost reward points. You have ${player.scPoints} left.")
    }
}

private fun canAfford(player: Player, cost: Int): Boolean {
    if (player.scPoints >= cost) return true
    player.sendMessage("You need $cost reward points for that, but you only have ${player.scPoints}.")
    return false
}

private fun confirm(player: Player, question: String, cost: Int, onConfirm: () -> Unit) {
    player.sendOptionsDialogue("$question (You have ${player.scPoints}.)") {
        opExec("Yes, spend $cost points.") { onConfirm() }
        op("No.")
    }
}
