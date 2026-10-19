package com.rs.game.content.skills.runecrafting

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj

enum class RunecraftingPouch(val obj: Int, val capacity: Int, val level: Int) {
    SMALL(Obj.rcu_pouch_small, 3, 1),
    MEDIUM(Obj.rcu_pouch_medium, 6, 25),
    LARGE(Obj.rcu_pouch_large, 9, 50),
    GIANT(Obj.rcu_pouch_giant, 12, 75),
    MASSIVE(Obj.rcsiphonxp_massive_pouch_active, 18, 90);

    fun stored(player: Player) = player.pouches[ordinal]

    fun holdsPure(player: Player) = player.pouchesType[ordinal]

    fun essence(player: Player) = if (holdsPure(player)) Runecrafting.PURE_ESS else Runecrafting.RUNE_ESS

    fun take(player: Player): Int = stored(player).also { player.pouches[ordinal] = 0 }

    companion object {
        private val byObj = entries.associateBy { it.obj }

        @JvmStatic
        fun forObj(obj: Int) = byObj[obj]

        @JvmStatic
        fun carried(player: Player) = player.inventory.items.array().mapNotNull { item -> item?.let { forObj(it.id) } }
    }
}

fun Player.fillPouch(pouch: RunecraftingPouch) {
    if (skills.getLevel(Skills.RUNECRAFTING) < pouch.level) {
        sendMessage("You need a Runecrafting level of ${pouch.level} to use this pouch.", false)
        return
    }
    if (!inventory.containsOneItem(Runecrafting.RUNE_ESS) && !inventory.containsOneItem(Runecrafting.PURE_ESS)) {
        sendMessage("You don't have any essence with you.", false)
        return
    }
    val space = pouch.capacity - pouch.stored(this)
    if (space == pouch.capacity) pouchesType[pouch.ordinal] = inventory.items.getNumberOf(Runecrafting.PURE_ESS) > 0
    val essence = pouch.essence(this)
    val added = minOf(space, inventory.items.getNumberOf(essence))
    if (added <= 0) {
        sendMessage("Your pouch is full.", false)
        return
    }
    inventory.deleteItem(essence, added)
    pouches[pouch.ordinal] += added
    refreshPouches()
}

fun Player.emptyPouch(pouch: RunecraftingPouch) {
    val removed = minOf(pouch.stored(this), inventory.freeSlots)
    if (removed <= 0) {
        sendMessage("Your pouch has no essence left in it.", false)
        return
    }
    inventory.addItem(pouch.essence(this), removed)
    pouches[pouch.ordinal] -= removed
    refreshPouches()
}

fun Player.checkPouch(pouch: RunecraftingPouch) {
    val kind = if (pouch.holdsPure(this)) "pure" else "rune"
    sendMessage("This pouch has ${pouch.stored(this)} $kind essence in it.", false)
}

fun Player.fillPouchesFromBank(essence: Int) {
    val pure = essence == Runecrafting.PURE_ESS
    for (pouch in RunecraftingPouch.carried(this)) {
        if (pouch.holdsPure(this) != pure || skills.getLevel(Skills.RUNECRAFTING) < pouch.level) continue
        val banked = bank.getItem(essence)?.amount ?: 0
        val added = minOf(pouch.capacity - pouch.stored(this), banked)
        if (added <= 0) continue
        bank.removeItem(bank.getSlot(essence), added, true, false)
        pouches[pouch.ordinal] += added
        pouchesType[pouch.ordinal] = pure
        sendMessage("$added ${ItemDefinitions.getDefs(essence).name} has been placed into your ${ItemDefinitions.getDefs(pouch.obj).name.lowercase()}.")
    }
    refreshPouches()
}

fun Player.takePouchEssence(includeRuneEssence: Boolean): Int {
    val taken = RunecraftingPouch.carried(this)
        .filter { it.holdsPure(this) || includeRuneEssence }
        .sumOf { it.take(this) }
    if (taken > 0) refreshPouches()
    return taken
}

private fun Player.refreshPouches() {
    val items = inventory.items.array()
    inventory.refresh(*items.indices.filter { slot -> items[slot]?.let { RunecraftingPouch.forObj(it.id) } != null }.toIntArray())
}

private fun RunecraftingPouch.tooltip(player: Player): String {
    val stored = stored(player)
    return if (stored <= 0) "Empty" else "<obj=${essence(player)},$stored>"
}

@ServerStartupEvent
fun mapRunecraftingPouches() {
    val pouches = RunecraftingPouch.entries.map { it.obj }.toIntArray()
    ItemTooltips.register(pouches) { player, item ->
        RunecraftingPouch.forObj(item.id)?.tooltip(player)
    }

    onItemClick(*pouches.toTypedArray(), Obj.rcu_pouch_medium_degrade, Obj.rcu_pouch_large_degrade, options = arrayOf("Fill", "Empty", "Check")) { e ->
        val pouch = RunecraftingPouch.forObj(e.item.id)
        if (pouch != null) {
            when (e.option) {
                "Fill" -> e.player.fillPouch(pouch)
                "Empty" -> e.player.emptyPouch(pouch)
                "Check" -> e.player.checkPouch(pouch)
            }
        }
        e.player.stopAll(false)
    }

    onItemClick(Obj.rcsiphonxp_massive_pouch_inactive, options = arrayOf("Fill", "Empty", "Check")) { e ->
        when (e.option) {
            "Fill" -> activateMassivePouch(e.player)
            "Empty" -> e.player.sendMessage("This pouch contains no essence in it.")
            "Check" -> e.player.sendMessage("An empty pouch.")
        }
        e.player.stopAll(false)
    }
}

private fun activateMassivePouch(player: Player) {
    val active = Obj.rcsiphonxp_massive_pouch_active
    if (player.inventory.containsItem(active, 1) || player.bank.containsItem(active, 1)) {
        player.sendMessage("You already have a massive pouch.")
        return
    }
    if (!player.inventory.hasFreeSlots()) {
        player.sendMessage("Not enough space in your inventory.")
        return
    }
    player.inventory.deleteItem(Obj.rcsiphonxp_massive_pouch_inactive, 1)
    player.inventory.addItem(active, 1)
}
