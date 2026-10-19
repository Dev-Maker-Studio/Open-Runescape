package com.rs.game.content.minigames.barbarianassault

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private const val GRANITE_BODY_COINS = 95_000
private val LEVEL_COSTS = intArrayOf(0, 200, 300, 400, 500)
private val POINTS_TEXT = mapOf(
    Role.ATTACKER to Component.interface_473.component_473_9, Role.DEFENDER to Component.interface_473.component_473_10,
    Role.COLLECTOR to Component.interface_473.component_473_11, Role.HEALER to Component.interface_473.component_473_12,
)
private val LEVEL_BUTTONS = mapOf(
    Component.interface_473.component_473_40 to Role.ATTACKER, Component.interface_473.component_473_47 to Role.DEFENDER,
    Component.interface_473.component_473_54 to Role.COLLECTOR, Component.interface_473.component_473_61 to Role.HEALER,
)

private class Armour(val obj: Int, val cost: Int, val queenKill: Boolean)

private val ARMOUR = mapOf(
    Component.interface_473.component_473_69 to Armour(Obj.barbassault_penance_fighter_hat, 275, true),
    Component.interface_473.component_473_70 to Armour(Obj.barbassault_penance_ranger_hat, 275, true),
    Component.interface_473.component_473_71 to Armour(Obj.barbassault_penance_runner_hat, 275, true),
    Component.interface_473.component_473_72 to Armour(Obj.barbassault_penance_healer_hat, 275, true),
    Component.interface_473.component_473_73 to Armour(Obj.barbassault_penance_fighter_torso, 375, true),
    Component.interface_473.component_473_74 to Armour(Obj.barbassault_penance_ranger_legs, 375, true),
    Component.interface_473.component_473_75 to Armour(Obj.barbassault_penance_runner_boots, 100, false),
    Component.interface_473.component_473_76 to Armour(Obj.barbassault_penance_gloves, 150, false),
    Component.interface_473.component_473_132 to Armour(Obj.trident, 390, false),
)

private val PAINTS = mapOf(
    Component.interface_473.component_473_151 to (Obj.barbassault_abyssal_whip_green to Obj.barbassault_darkbow_green),
    Component.interface_473.component_473_150 to (Obj.barbassault_abyssal_whip_white to Obj.barbassault_darkbow_white),
    Component.interface_473.component_473_149 to (Obj.barbassault_abyssal_whip_blue to Obj.barbassault_darkbow_blue),
    Component.interface_473.component_473_148 to (Obj.barbassault_abyssal_whip_yellow to Obj.barbassault_darkbow_yellow),
)
private const val PAINT_COST = 50

private class Prize(val obj: Int, val amount: Int, val noted: Boolean)

private val LOW_PRIZES = listOf(
    Prize(Obj.uncut_sapphire, 1, true), Prize(Obj.snape_grass, 2, true), Prize(Obj.naturerune, 3, false), Prize(Obj.toads_legs, 3, true),
    Prize(Obj.coal, 5, true), Prize(Obj.cosmicrune, 5, false), Prize(Obj.limpwurt_root, 2, true), Prize(Obj.plank_mahogany, 2, true),
    Prize(Obj.lawrune, 2, false), Prize(Obj.orange_tree_seed, 3, false), Prize(Obj.plank_oak, 2, true), Prize(Obj.strawberry_seed, 1, false),
)
private val MEDIUM_PRIZES = listOf(
    Prize(Obj.obj_534, 1, true), Prize(Obj.unicorn_horn, 1, true), Prize(Obj.dragonhide_green, 1, true), Prize(Obj.adamant_arrow, 13, false),
    Prize(Obj.strawberry_seed, 2, false), Prize(Obj.red_spiders_eggs, 5, true), Prize(Obj.big_bones, 2, true), Prize(Obj.raw_swordfish, 3, true),
    Prize(Obj.plank_teak, 2, true), Prize(Obj.toadflax_seed, 1, false), Prize(Obj.gold_ore, 4, true), Prize(Obj.uncut_sapphire, 2, true),
)
private val HIGH_PRIZES = listOf(
    Prize(Obj.adamantite_ore, 2, true), Prize(Obj.obj_534, 2, true), Prize(Obj.bloodrune, 7, false), Prize(Obj.curry_tree_seed, 1, false),
    Prize(Obj.emerald, 3, true), Prize(Obj.dragonhide_green, 2, true), Prize(Obj.raw_lobster, 2, true), Prize(Obj.deathrune, 5, false),
    Prize(Obj.adamant_arrow, 6, false), Prize(Obj.lawrune, 7, false), Prize(Obj.raw_shark, 2, true), Prize(Obj.rune_arrow, 7, false),
    Prize(Obj.strawberry_seed, 4, false), Prize(Obj.unicorn_horn, 2, true), Prize(Obj.obj_1149, 1, false),
)
private val GAMBLES = mapOf(
    Component.interface_473.component_473_187 to (10 to LOW_PRIZES),
    Component.interface_473.component_473_188 to (20 to MEDIUM_PRIZES),
    Component.interface_473.component_473_189 to (30 to HIGH_PRIZES),
)
private const val CHAINBODY_ODDS = 6_000

@ServerStartupEvent
fun mapBarbarianAssaultRewards() {
    onButtonClick(Interface.interface_473) { e -> BarbarianAssaultRewards.buy(e.player, e.component) }
}

object BarbarianAssaultRewards {
    fun open(player: Player) {
        for (role in Role.entries) if (player.vars.getVarBit(role.levelVarbit) == 0) player.vars.saveVarBit(role.levelVarbit, 1)
        player.interfaceManager.sendInterface(Interface.interface_473)
        refresh(player)
    }

    private fun refresh(player: Player) = POINTS_TEXT.forEach { (role, component) -> player.packets.setIFText(component, role.points(player).toString()) }

    fun buy(player: Player, component: Int) {
        LEVEL_BUTTONS[component]?.let { return levelUp(player, it) }
        ARMOUR[component]?.let { return buyArmour(player, it) }
        PAINTS[component]?.let { return paint(player, it.first, it.second) }
        GAMBLES[component]?.let { (cost, prizes) -> return gamble(player, cost, prizes) }
        when (component) {
            Component.interface_473.component_473_133 -> upgradeTrident(player)
            Component.interface_473.component_473_177 -> claimHorn(player)
            Component.interface_473.component_473_178 -> upgradeHorn(player)
            Component.interface_473.component_473_186 -> buyGranite(player)
        }
    }

    private fun levelUp(player: Player, role: Role) {
        val level = role.level(player)
        if (level >= MAX_ROLE_LEVEL) return player.sendMessage("You have already mastered the ${role.displayName} role.")
        val cost = LEVEL_COSTS[level]
        if (role.points(player) < cost) return player.sendMessage("You need $cost ${role.displayName} Honour Points to reach level ${level + 1}.")
        role.addPoints(player, -cost)
        player.vars.saveVarBit(role.levelVarbit, level + 1)
        player.sendMessage("You are now a level ${level + 1} ${role.displayName}.")
        refresh(player)
    }

    private fun spendEveryRole(player: Player, cost: Int): Boolean {
        if (Role.entries.any { it.points(player) < cost }) {
            player.sendMessage("You need $cost Honour Points in every role for that.")
            return false
        }
        Role.entries.forEach { it.addPoints(player, -cost) }
        return true
    }

    private fun queenEntitled(player: Player): Boolean {
        if (player.vars.getVarBit(Varbit.barbassault_armour_get_granite) == 1) return true
        player.sendMessage("You need to kill the Penance Queen before you can buy that.")
        return false
    }

    private fun buyArmour(player: Player, armour: Armour) {
        if (!player.inventory.hasFreeSlots()) return player.sendMessage("You don't have enough inventory space.")
        if (armour.queenKill && !queenEntitled(player)) return
        if (!spendEveryRole(player, armour.cost)) return
        if (armour.queenKill) player.vars.saveVarBit(Varbit.barbassault_armour_get_granite, 0)
        player.inventory.addItem(armour.obj, 1)
        player.sendMessage("You buy ${Utils.addArticle(ItemDefinitions.getDefs(armour.obj).name.lowercase())}.")
        refresh(player)
    }

    private fun buyGranite(player: Player) {
        when {
            !player.inventory.hasFreeSlots() -> player.sendMessage("You don't have enough inventory space.")
            !queenEntitled(player) -> return
            !player.inventory.containsItem(Obj.coins, GRANITE_BODY_COINS) -> player.sendMessage("You need ${Utils.formatNumber(GRANITE_BODY_COINS)} coins for a granite body.")
            else -> {
                player.inventory.deleteItem(Obj.coins, GRANITE_BODY_COINS)
                player.vars.saveVarBit(Varbit.barbassault_armour_get_granite, 0)
                player.inventory.addItem(Obj.granite_body, 1)
                player.sendMessage("You buy a granite body.")
            }
        }
    }

    private fun upgradeTrident(player: Player) {
        when {
            !player.inventory.containsItem(Obj.trident) -> player.sendMessage("You need a Penance trident in your inventory to upgrade.")
            player.skills.getLevelForXp(Skills.MAGIC) < 70 || player.skills.getLevelForXp(Skills.ATTACK) < 70 -> player.sendMessage("You need 70 Attack and 70 Magic to wield the master trident.")
            else -> {
                player.inventory.deleteItem(Obj.trident, 1)
                player.inventory.addItem(Obj.trident_master, 1)
                player.sendMessage("Commander Connad upgrades your trident.")
            }
        }
    }

    private fun ownsHorn(player: Player, horn: Int) = player.inventory.containsItem(horn) || player.equipment.containsOneItem(horn) || player.bank.containsItem(horn, 1)

    private fun claimHorn(player: Player) {
        when {
            ownsHorn(player, Obj.barbassault_horn) || ownsHorn(player, Obj.barbassault_master_horn) -> player.sendMessage("You already own a Penance horn.")
            !player.inventory.hasFreeSlots() -> player.sendMessage("You don't have enough inventory space.")
            else -> {
                player.inventory.addItem(Obj.barbassault_horn, 1)
                player.sendMessage("Commander Connad hands you a Penance horn.")
            }
        }
    }

    private fun upgradeHorn(player: Player) {
        when {
            !player.inventory.containsItem(Obj.barbassault_horn) -> player.sendMessage("You need a Penance horn in your inventory to upgrade it.")
            Role.entries.none { it.level(player) >= MAX_ROLE_LEVEL } -> player.sendMessage("You need to master at least one role to upgrade your horn.")
            else -> {
                player.inventory.deleteItem(Obj.barbassault_horn, 1)
                player.inventory.addItem(Obj.barbassault_master_horn, 1)
                player.sendMessage("Commander Connad upgrades your horn.")
            }
        }
    }

    private fun richestRole(player: Player, cost: Int) = Role.entries.filter { it.points(player) >= cost }.maxByOrNull { it.points(player) }

    private fun paint(player: Player, whip: Int, bow: Int) {
        val role = richestRole(player, PAINT_COST) ?: return player.sendMessage("You need $PAINT_COST Honour Points in one role for a paint.")
        val slot = player.inventory.items.array().indexOfFirst { it?.id == Obj.abyssal_whip || it?.id == Obj.darkbow }
        if (slot < 0) return player.sendMessage("You need an abyssal whip or a dark bow in your inventory to paint.")
        val target = if (player.inventory.getItem(slot)?.id == Obj.abyssal_whip) whip else bow
        role.addPoints(player, -PAINT_COST)
        player.inventory.replaceItem(target, 1, slot)
        player.sendMessage("Your weapon is painted using $PAINT_COST ${role.displayName} Honour Points.")
        refresh(player)
    }

    private fun gamble(player: Player, cost: Int, prizes: List<Prize>) {
        val role = richestRole(player, cost) ?: return player.sendMessage("You need $cost Honour Points in one role to gamble.")
        if (!player.inventory.hasFreeSlots()) return player.sendMessage("You don't have enough inventory space.")
        role.addPoints(player, -cost)
        val prize = if (prizes === HIGH_PRIZES && Utils.random(CHAINBODY_ODDS) == 0) Prize(Obj.dragon_chainbody, 1, false) else prizes.random()
        val noted = if (prize.noted) ItemDefinitions.getDefs(prize.obj).noteId.takeIf { it > 0 } ?: prize.obj else prize.obj
        player.inventory.addItemDrop(noted, prize.amount)
        player.sendMessage("You gamble $cost ${role.displayName} Honour Points and receive ${prize.amount} x ${ItemDefinitions.getDefs(prize.obj).name}.")
        refresh(player)
    }
}
