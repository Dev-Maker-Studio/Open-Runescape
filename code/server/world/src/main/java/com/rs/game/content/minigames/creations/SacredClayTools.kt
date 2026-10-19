package com.rs.game.content.minigames.creations

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.World
import com.rs.game.content.skills.mining.Pickaxe
import com.rs.game.content.skills.woodcutting.Hatchet
import com.rs.game.model.entity.player.Player
import com.rs.game.model.item.ItemTooltips
import com.rs.net.decoders.handlers.InventoryOptionsHandler
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.XPGainEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onXpDrop
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import kotlin.math.roundToInt

const val SACRED_CLAY_REVERT_KEY = "sacredClayRevertAt"
private const val VOLATILE_CHARGE_STEP = FULL_SACRED_CLAY_CHARGE / 10
private const val VOLATILE_TOOL_LEVEL = 40
private const val PROTO_XP_BONUS = 1.0
private const val VOLATILE_XP_BONUS = 1.2
private const val PROTO_BONUS_XP = 15_000.0
private const val VOLATILE_BONUS_XP = PROTO_BONUS_XP * 1.6
private const val TOOL_USED_OBJ = "sacredClayToolObj"
private const val TOOL_USED_TICK = "sacredClayToolTick"

enum class SacredClayTool(
    val proto: Int,
    val volatile: Int,
    val baseTool: Int?,
    val label: String,
    val skills: Set<Int>,
) {
    PICKAXE(Obj.sc_reward_pickaxe, Obj.sc_reward_volatile_pickaxe, null, "Pickaxe", setOf(Constants.MINING)),
    HATCHET(Obj.sc_reward_axe, Obj.sc_reward_volatile_axe, null, "Hatchet", setOf(Constants.WOODCUTTING)),
    HARPOON(Obj.sc_reward_harpoon, Obj.sc_reward_volatile_harpoon, Obj.harpoon, "Harpoon", setOf(Constants.FISHING)),
    BUTTERFLY_NET(Obj.sc_reward_butterfly_net, Obj.sc_reward_volatile_butterfly_net, Obj.hunting_butterfly_net, "Butterfly net", setOf(Constants.HUNTER)),
    KNIFE(Obj.sc_reward_knife, Obj.sc_reward_volatile_knife, Obj.knife, "Fletching knife", setOf(Constants.FLETCHING)),
    HAMMER(Obj.sc_reward_hammer, Obj.sc_reward_volatile_hammer, Obj.hammer, "Hammer", setOf(Constants.SMITHING, Constants.CONSTRUCTION)),
    NEEDLE(Obj.sc_reward_needle, Obj.sc_reward_volatile_needle, Obj.needle, "Needle", setOf(Constants.CRAFTING));

    val ids = setOf(proto, volatile)
    fun usableBy(player: Player, form: Int) = form == proto || skills.any { player.skills.getLevel(it) >= VOLATILE_TOOL_LEVEL }

    companion object {
        val byId = entries.flatMap { tool -> tool.ids.map { it to tool } }.toMap()
        private val byBaseTool = entries.filter { it.baseTool != null }.associateBy { it.baseTool!! }
        val volatileForms = entries.map { it.volatile }.toSet()
        val protoForms = entries.map { it.proto }.toSet()

        fun forBaseTool(baseToolId: Int) = byBaseTool[baseToolId]
    }
}

fun Player.substituteSacredClayTool(baseToolId: Int): Boolean {
    val tool = SacredClayTool.forBaseTool(baseToolId) ?: return false
    val held = heldItems(tool.ids).firstOrNull { tool.usableBy(this, it.item.id) } ?: return false
    markSacredClayToolUsed(held.item.id)
    return true
}

fun Player.markSacredClayToolUsed(itemId: Int) {
    tempAttribs.setI(TOOL_USED_OBJ, itemId)
    tempAttribs.setL(TOOL_USED_TICK, World.getServerTicks().toLong())
}

@ServerStartupEvent
fun mapSacredClayTools() {
    onItemClick(Obj.sc_reward_volatile_tool, Obj.sc_reward_tool, options = arrayOf("Transform", "Check")) { (player, item, option) ->
        when (option) {
            "Check" -> checkTool(player, item)
            "Transform" -> if (item.id == Obj.sc_reward_volatile_tool) transformVolatile(player, item) else chooseProtoForm(player, item)
        }
    }

    onItemClick(*SacredClayTool.byId.keys.toTypedArray(), options = arrayOf("Check", "Revert")) { (player, item, option) ->
        when (option) {
            "Check" -> checkTool(player, item)
            "Revert" -> player.heldItem(item)?.let { revertTool(player, it) }
        }
    }

    onItemOnItem(*SacredClayTool.entries.filter { it.baseTool != null }.flatMap { it.ids }.toIntArray()) { e ->
        val clayTool = if (e.item1.id in SacredClayTool.byId) e.item1 else e.item2
        val other = if (clayTool === e.item1) e.item2 else e.item1
        val tool = SacredClayTool.byId.getValue(clayTool.id)
        if (!tool.usableBy(e.player, clayTool.id)) {
            e.player.sendMessage("You need level $VOLATILE_TOOL_LEVEL in the relevant skill to use this tool.")
            return@onItemOnItem
        }
        InventoryOptionsHandler.handleItemOnItem(e.player, Item(tool.baseTool!!, 1), other, -1, other.slot)
    }

    onXpDrop { e ->
        if (e.player.isXpLocked) return@onXpDrop
        if (e.skillId in SacredClayArmour.combatSkills) applySacredClayArmourBonus(e) else applyToolBonus(e)
    }

    for (id in SacredClayTool.byId.keys + Obj.sc_reward_tool + Obj.sc_reward_volatile_tool + SacredClayArmour.morphicIds + SacredClayArmour.SHIELD.ids)
        onItemOnItem(intArrayOf(id), intArrayOf(id)) { e -> combineCharges(e.player, e.item1.slot, e.item2.slot) }
}

private fun checkTool(player: Player, item: Item) {
    player.sendSacredClayDetails(item, sacredClayToolDetails(item))
}

fun isSacredClayTool(itemId: Int) = itemId in SacredClayTool.byId || itemId == Obj.sc_reward_tool || itemId == Obj.sc_reward_volatile_tool

fun sacredClayToolDetails(item: Item): List<String> {
    val charge = item.sacredClayCharge
    val volatile = item.id == Obj.sc_reward_volatile_tool || item.id in SacredClayTool.volatileForms
    val lines = mutableListOf(
        "Xp bonus: ${ItemTooltips.value("+${((if (volatile) VOLATILE_XP_BONUS else PROTO_XP_BONUS) * 100).roundToInt()}%")}",
        "Bonus xp left: ${ItemTooltips.value(bonusXp(charge, volatile))}",
    )
    val revert = item.getMetaDataI(SACRED_CLAY_REVERT_KEY, -1)
    when {
        item.id == Obj.sc_reward_volatile_tool -> lines += "Transforms into a random tool"
        item.id == Obj.sc_reward_tool -> lines += "Transforms into any tool, free of charge"
        volatile && revert >= 0 -> lines += "Before reverting: ${ItemTooltips.value(bonusXp(charge - revert, true))}"
    }
    return lines
}

private fun bonusXp(charge: Int, volatile: Boolean) =
    (charge.coerceAtLeast(0).toDouble() / FULL_SACRED_CLAY_CHARGE * if (volatile) VOLATILE_BONUS_XP else PROTO_BONUS_XP).roundToInt()

private fun Item.startVolatileSegment() {
    addMetaData(SACRED_CLAY_REVERT_KEY, (sacredClayCharge - VOLATILE_CHARGE_STEP).coerceAtLeast(0))
}

private fun combineCharges(player: Player, fromSlot: Int, toSlot: Int) {
    val from = player.inventory.getItem(fromSlot) ?: return
    val to = player.inventory.getItem(toSlot) ?: return
    if (fromSlot == toSlot || from.id != to.id) return
    val added = from.sacredClayCharge
    player.inventory.deleteItem(fromSlot, from)
    to.setSacredClayCharge((to.sacredClayCharge.toLong() + added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    val revert = to.getMetaDataI(SACRED_CLAY_REVERT_KEY, -1)
    if (revert >= 0) to.addMetaData(SACRED_CLAY_REVERT_KEY, (revert.toLong() + added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    player.sendMessage("You pour the charge from one ${to.name.lowercase()} into the other.")
    player.sendSacredClayDetails(to, sacredClayDetails(to))
}

private fun transformVolatile(player: Player, item: Item) {
    val held = player.heldItem(item) ?: return
    val tool = SacredClayTool.entries.random()
    item.setSacredClayCharge(item.sacredClayCharge)
    item.startVolatileSegment()
    held.changeId(tool.volatile)
    player.sendMessage("Your volatile tool transforms into a ${tool.label.lowercase()}!")
}

private fun chooseProtoForm(player: Player, item: Item) {
    player.sendOptionsDialogue("What should your proto-tool become?") {
        for (tool in SacredClayTool.entries)
            opExec(tool.label) {
                val held = player.heldItem(item) ?: return@opExec
                if (item.id != Obj.sc_reward_tool) return@opExec
                item.setSacredClayCharge(item.sacredClayCharge)
                held.changeId(tool.proto)
                player.sendMessage("Your proto-tool transforms into a ${tool.label.lowercase()}.")
            }
    }
}

private fun revertTool(player: Player, held: HeldItem) {
    val item = held.item
    if (item.id in SacredClayTool.protoForms) {
        held.replaceUnworn(Obj.sc_reward_tool)
        player.sendMessage("Your sacred clay tool reverts to a proto-tool.")
        return
    }
    val charge = item.getMetaDataI(SACRED_CLAY_REVERT_KEY, 0).coerceIn(0, item.sacredClayCharge)
    if (charge <= 0) {
        crumble(player, held)
        return
    }
    val lost = bonusXp(item.sacredClayCharge - charge, true)
    revertVolatile(held, charge)
    player.sendMessage("Your tool reverts to a volatile tool, losing ${Utils.formatNumber(lost)} bonus xp.")
}

private fun revertVolatile(held: HeldItem, charge: Int) {
    held.item.getMetaData()?.remove(SACRED_CLAY_REVERT_KEY)
    held.item.setSacredClayCharge(charge)
    held.replaceUnworn(Obj.sc_reward_volatile_tool)
}

private fun crumble(player: Player, held: HeldItem) {
    held.remove()
    player.sendMessage("Your ${held.item.name.lowercase()} crumbles to dust.")
}

private fun Player.sacredClayToolInUse(skill: Int): HeldItem? {
    val itemId = when (skill) {
        Constants.MINING -> Pickaxe.getBest(this)?.takeIf { it == Pickaxe.SACRED_CLAY || it == Pickaxe.VOLATILE_CLAY }?.itemId
        Constants.WOODCUTTING -> Hatchet.getBest(this)?.takeIf { it == Hatchet.SACRED_CLAY || it == Hatchet.VOLATILE_CLAY }?.itemId
        else -> tempAttribs.getI(TOOL_USED_OBJ, -1).takeIf { tempAttribs.getL(TOOL_USED_TICK) >= World.getServerTicks() - 1 }
    } ?: return null
    val tool = SacredClayTool.byId[itemId] ?: return null
    if (skill !in tool.skills || !tool.usableBy(this, itemId)) return null
    return heldItems(setOf(itemId)).firstOrNull()
}

private fun applyToolBonus(e: XPGainEvent) {
    val player = e.player
    val held = player.sacredClayToolInUse(e.skillId) ?: return
    val tool = SacredClayTool.byId.getValue(held.item.id)
    val volatile = held.item.id == tool.volatile
    val bonus = if (volatile) VOLATILE_XP_BONUS else PROTO_XP_BONUS
    e.multiplier += bonus
    val drain = (e.xp * bonus / (if (volatile) VOLATILE_BONUS_XP else PROTO_BONUS_XP) * FULL_SACRED_CLAY_CHARGE).roundToInt().coerceAtLeast(1)
    val charge = held.item.sacredClayCharge - drain
    if (charge <= 0) {
        crumble(player, held)
        return
    }
    held.item.setSacredClayCharge(charge)
    if (volatile && charge <= held.item.getMetaDataI(SACRED_CLAY_REVERT_KEY, 0)) {
        revertVolatile(held, charge)
        player.sendMessage("Your volatile ${tool.label.lowercase()} reverts to a volatile tool.")
    }
}

fun Item.restoreSacredClayCharge() {
    setSacredClayCharge(FULL_SACRED_CLAY_CHARGE)
    if (id in SacredClayTool.volatileForms) startVolatileSegment()
}
