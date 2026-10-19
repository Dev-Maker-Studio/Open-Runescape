package com.rs.game.model.entity.player

import com.rs.Settings
import com.rs.game.World
import com.rs.game.content.ItemConstants
import com.rs.game.content.Toolbelt
import com.rs.game.content.pets.Pet
import com.rs.game.content.skills.slayer.npcs.ConditionalDeath
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.content.skills.summoning.Pouch
import com.rs.game.model.entity.interactions.StandardEntityInteraction
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.item.ItemsContainer
import com.rs.net.decoders.handlers.InventoryOptionsHandler
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.IFOnIFEvent
import com.rs.plugin.events.IFOnNPCEvent
import com.rs.plugin.events.IFOnPlayerEvent
import com.rs.plugin.events.ItemAddedToInventoryEvent
import com.rs.plugin.events.ItemOnNPCEvent
import com.rs.plugin.events.ItemOnPlayerEvent
import com.rs.plugin.events.NPCInteractionDistanceEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onInterfaceOnInterface
import com.rs.plugin.kts.onInterfaceOnNPC
import com.rs.plugin.kts.onInterfaceOnPlayer
import com.rs.plugin.kts.onItemClick
import com.rs.utils.ItemConfig
import com.rs.utils.copyItem
import org.darkan.core.Logger
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Inv
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.IFEvents
import org.darkan.core.model.Item
import org.darkan.core.model.UseFlag
import world.gregs.voidps.type.Tile

class Inventory {
    private var coins: Long = 0
    val items: ItemsContainer<Item> = ItemsContainer(28, false)

    @Transient
    private lateinit var player: Player

    @Transient
    var inventoryWeight: Double = 0.0
        private set

    val freeSlots: Int get() = items.freeSlots

    val itemsContainerSize: Int get() = items.size

    val pouchCoins: Long get() = coins

    val pouchCoinsAsInt: Int get() = coins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    val coinsAsInt: Int get() = getCoins().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun setPlayer(player: Player) {
        this.player = player
    }

    fun replaceItem(itemId: Int, amount: Int, slot: Int) {
        val item = items[slot] ?: return
        item.changeId(itemId)
        item.setAmount(amount)
    }

    fun replace(item: Item, newItem: Item) {
        val current = items[item.slot] ?: throw RuntimeException("Yikes. Expected a ${item.id} but was not found.")
        if (item.id != current.id) throw RuntimeException("Yikes. Expected a ${item.id} but found ${current.id} instead.")
        items[item.slot] = newItem
    }

    fun replace(fromId: Int, toId: Int) {
        items.lookup(fromId)?.changeId(toId)
    }

    fun init() {
        items.markAllDirty()
        items.initSlots()
        sendPouchCoins()
    }

    fun unlockInventoryOptions() {
        val component = Utils.componentIdFromHash(Component.inventory.inv)
        player.packets.setIFEvents(
            IFEvents(Interface.inventory, component, 0, 27)
                .enableUseOptions(UseFlag.GROUND_ITEM, UseFlag.NPC, UseFlag.WORLD_OBJECT, UseFlag.PLAYER, UseFlag.ICOMPONENT)
                .enableRightClickOptions(0, 1, 2, 6, 7, 9)
                .setDepth(1)
                .enableUseTargetability()
        )
        player.packets.setIFEvents(IFEvents(Interface.inventory, component, 28, 55).enableDrag())
    }

    fun reset() {
        items.reset()
        init()
    }

    fun refresh(vararg slots: Int) = items.markDirty(*slots)

    fun refresh() = items.markAllDirty()

    fun processRefresh() {
        if (player.packets.sendDirtyItems(Inv.inv, items)) refreshConfigs()
    }

    fun hasMaterials(mats: Array<out Item>): Boolean = mats.all { getAmountOf(it.id) >= it.amount }

    fun addItem(itemId: Int): Boolean = addItem(itemId, 1)

    fun addItem(itemId: Int, amount: Int): Boolean {
        val item = Item(itemId, amount)
        if (!canAdd(item)) return false
        if (!items.add(item)) {
            items.add(Item(item.id, items.freeSlots))
            player.sendMessage("Not enough space in your inventory.")
            return false
        }
        return true
    }

    fun addItem(item: Item): Boolean {
        if (!canAdd(item)) return false
        if (!items.add(item)) {
            if (!item.definitions.isStackable() && item.getMetaData() != null)
                items.add(Item(item.id, items.freeSlots, item.getMetaData()))
            player.sendMessage("Not enough space in your inventory.")
            return false
        }
        return true
    }

    private fun canAdd(item: Item): Boolean {
        PluginManager.handle(ItemAddedToInventoryEvent(player, item))
        return isAddable(item)
    }

    private fun isAddable(item: Item): Boolean =
        item.id >= 0 && item.amount >= 0 && Utils.itemExists(item.id) && player.controllerManager.canAddInventoryItem(item.id, item.amount)

    fun addItem(itemId: Int, amount: Int, dropIfInvFull: Boolean): Boolean = addItem(Item(itemId, amount), dropIfInvFull)

    fun addItem(item: Item, dropIfInvFull: Boolean): Boolean {
        if (!isAddable(item)) return false
        if (!items.add(item)) {
            val numberToDrop = item.amount - items.freeSlots
            items.add(item.copyItem().withAmount(items.freeSlots))
            player.sendMessage("Not enough space in your inventory.")
            World.addGroundItem(item.withAmount(numberToDrop), Tile.of(player.tile), player, true, 60)
            return true
        }
        PluginManager.handle(ItemAddedToInventoryEvent(player, Item(item.id, 0)))
        return true
    }

    fun addItemDrop(id: Int, amount: Int): Boolean = addItem(id, amount, true)

    fun addItemDrop(id: Int): Boolean = addItem(id, 1, true)

    fun addItemDrop(item: Item): Boolean = addItem(item, true)

    fun getAmountOf(itemId: Int): Int = items.getNumberOf(itemId)

    fun deleteItem(slot: Int, item: Item) {
        if (!player.controllerManager.canDeleteInventoryItem(item.id, item.amount)) return
        items.remove(slot, item)
    }

    fun deleteItem(itemId: Int, amount: Int) {
        if (!player.controllerManager.canDeleteInventoryItem(itemId, amount)) return
        items.remove(Item(itemId, amount))
    }

    fun deleteItem(item: Item?) {
        if (item == null || !player.controllerManager.canDeleteInventoryItem(item.id, item.amount)) return
        items.remove(item)
    }

    fun removeItems(vararg list: Item?) {
        list.forEach { deleteItem(it) }
    }

    fun removeItems(list: List<Item?>): Boolean {
        list.forEach { deleteItem(it) }
        return true
    }

    fun removeItems(itemIdsToRemove: IntArray, amountsToRemove: IntArray): Boolean {
        if (itemIdsToRemove.size != amountsToRemove.size) return false
        for (i in itemIdsToRemove.indices)
            repeat(amountsToRemove[i].coerceAtLeast(0)) { deleteItem(itemIdsToRemove[i], 1) }
        return true
    }

    fun removeAllItems(vararg list: Int) {
        for (item in list)
            if (containsItem(item, 1)) deleteItem(item, Int.MAX_VALUE)
    }

    fun switchItem(fromSlot: Int, toSlot: Int) {
        val fromItem = items[fromSlot]
        items[fromSlot] = items[toSlot]
        items[toSlot] = fromItem
    }

    fun hasFreeSlots(): Boolean = items.freeSlot != -1

    fun hasFreeSlots(mats: Array<out Item?>, product: Item?): Boolean =
        hasRoomFor(product) || (mats.isNotEmpty() && !mats.all { it == null || it.definitions.isStackable() })

    fun getNumberOf(itemId: Int): Int = getNumberOf(itemId, true)

    fun getNumberOf(itemId: Int, checkToolbelt: Boolean): Int =
        items.getNumberOf(itemId) + if (checkToolbelt && Toolbelt.containsTool(player, itemId)) 1 else 0

    fun getTotalNumberOf(vararg ids: Int): Int = ids.sumOf { getNumberOf(it) }

    fun getItem(slot: Int): Item? = items[slot]

    fun getItemById(id: Int): Item? = items.lookup(id)

    fun findItemByIds(vararg ids: Int): Item? = ids.asSequence().mapNotNull { getItemById(it) }.firstOrNull()

    fun hasRoomFor(vararg item: Item?): Boolean = hasRoomFor(deleting = null, adding = *item)

    fun hasRoomFor(deleting: Array<out Item?>?, vararg adding: Item?): Boolean {
        val freedSlots = deleting.orEmpty().count { it != null && (!it.definitions.isStackable() || getNumberOf(it.id, false) <= it.amount) }
        var neededSlots = 0
        for (item in adding) {
            if (item == null) continue
            val stackable = item.definitions.isStackable()
            if (stackable && getNumberOf(item.id) + item.amount <= 0) return false
            if (!stackable || getNumberOf(item.id, false) <= 0) neededSlots++
        }
        return freeSlots + freedSlots >= neededSlots
    }

    fun containsItem(name: String): Boolean = items.array().any { it != null && it.definitions.name.equals(name, ignoreCase = true) }

    fun containsItem(item: Item): Boolean = containsItem(item.id, item.amount, true)

    fun containsItem(id: Int): Boolean = containsItem(id, 1)

    fun containsItem(itemId: Int, amount: Int): Boolean = containsItem(itemId, amount, true)

    fun containsItem(itemId: Int, amount: Int, checkToolbelt: Boolean): Boolean =
        (checkToolbelt && Toolbelt.containsTool(player, itemId)) || items.contains(Item(itemId, amount))

    fun containsItems(vararg itemIds: Int): Boolean = itemIds.all { items.contains(Item(it, 1)) }

    fun containsItems(vararg item: Item?): Boolean = item.all { it != null && items.contains(it) }

    fun containsItems(list: List<Item>): Boolean = list.all { items.contains(it) }

    fun containsItems(itemIds: IntArray, amounts: IntArray): Boolean =
        (0 until minOf(itemIds.size, amounts.size)).all { items.contains(Item(itemIds[it], amounts[it])) }

    fun containsItemNoted(item: Item): Boolean =
        containsItem(item.id, item.amount) || containsItem(item.definitions.getCertId(), item.amount)

    fun deleteItemNoted(item: Item) {
        deleteItem(if (containsItem(item.id)) item.id else item.definitions.getCertId(), item.amount)
    }

    fun missingItems(vararg itemIds: Int): Boolean = itemIds.none { items.lookup(it) != null }

    fun containsOneItem(vararg itemIds: Int): Boolean = containsOneItem(true, *itemIds)

    fun containsOneItem(checkToolbelt: Boolean, vararg itemIds: Int): Boolean =
        itemIds.any { (checkToolbelt && Toolbelt.containsTool(player, it)) || items.containsOne(Item(it, 1)) }

    fun sendExamine(slotId: Int) {
        if (slotId >= itemsContainerSize) return
        val item = items[slotId] ?: return
        if (Settings.getConfig().isDebug) player.sendMessage("ID: ${item.id}")
        val prices = if (ItemConstants.isTradeable(item))
            " General store: ${Utils.formatTypicalInteger(item.definitions.getSellPrice())} High Alchemy: ${Utils.formatTypicalInteger(ItemDefinitions.getDefs(item.id).getHighAlchPrice())}"
        else ""
        player.packets.sendInventoryMessage(0, slotId, ItemConfig.get(item.id).getExamine(item) + prices)
        if (item.getMetaData("combatCharges") != null)
            player.sendMessage("<col=FF0000>It looks like it will last another " + Utils.ticksToTime(item.getMetaDataI("combatCharges").toDouble()))
        else if (item.getMetaData("brawlerCharges") != null)
            player.sendMessage("These gloves have " + item.getMetaDataI("brawlerCharges") + " charges left.")
    }

    fun refreshConfigs() {
        inventoryWeight = items.array().sumOf { if (it == null) 0.0 else ItemConfig.get(it.id).getWeight(false) }
        player.packets.refreshWeight(player.equipment.equipmentWeight + inventoryWeight)
    }

    fun coinPouchToInventory(amount: Int) {
        var num = amount.coerceAtMost(pouchCoinsAsInt)
        if (num <= 0) return
        if (getNumberOf(Obj.coins) + num < 0) num = Int.MAX_VALUE - getNumberOf(Obj.coins) - 1
        coins -= num
        sendPouchCoinsChange(num.toLong(), false)
        addItemDrop(Obj.coins, num)
    }

    fun addCoins(num: Int) = addCoins(num.toLong())

    fun addCoins(num: Long) {
        if (num <= 0L) return
        val added = Utils.satAdd(coins, num) - coins
        if (added <= 0L) return
        coins += added
        sendPouchCoinsChange(added, true)
    }

    fun hasCoins(num: Int): Boolean = hasCoins(num.toLong())

    fun hasCoins(num: Long): Boolean = coins >= num || getCoins() >= num

    fun getCoins(): Long {
        val total = Utils.satAdd(coins, getNumberOf(Obj.coins).toLong())
        return if (total < 0L) coins else total
    }

    fun removeCoins(num: Int) = removeCoins(num.toLong(), false)

    fun removeCoins(num: Long) = removeCoins(num, false)

    fun removeCoins(amount: Int, pouchOnly: Boolean) = removeCoins(amount.toLong(), pouchOnly)

    fun removeCoins(amount: Long, pouchOnly: Boolean) {
        var num = amount
        if (num <= 0L) return
        if (coins >= num) {
            coins -= num
            sendPouchCoinsChange(num, false)
            return
        }
        if (pouchOnly) return
        if (coins > 0L) {
            num -= coins
            sendPouchCoinsChange(coins, false)
            coins = 0
        }
        if (num <= Int.MAX_VALUE.toLong() && getNumberOf(Obj.coins) >= num) deleteItem(Obj.coins, num.toInt())
        else {
            player.sendMessage("Error removing coins. Please report how you managed to produce this message.")
            Logger.error(Inventory::class.java, "removeCoins (${player.username})", Thread.currentThread().stackTrace.contentToString())
        }
    }

    private fun sendPouchCoinsChange(amount: Long, added: Boolean) {
        player.packets.sendRunScript(Cs2.cs2_5561, Utils.satInt(amount), if (added) 1 else 0)
        sendPouchCoins()
    }

    private fun sendPouchCoins() {
        player.packets.sendRunScript(Cs2.cs2_5560, pouchCoinsAsInt)
    }
}

private val GEYSER_TITAN_RECHARGES = mapOf(
    Obj.amulet_of_glory to Obj.amulet_of_glory_4,
    Obj.amulet_of_glory_1 to Obj.amulet_of_glory_4,
    Obj.amulet_of_glory_2 to Obj.amulet_of_glory_4,
    Obj.amulet_of_glory_3 to Obj.amulet_of_glory_4,
    Obj.trail_amulet_of_glory to Obj.trail_amulet_of_glory_4,
    Obj.trail_amulet_of_glory_1 to Obj.trail_amulet_of_glory_4,
    Obj.trail_amulet_of_glory_2 to Obj.trail_amulet_of_glory_4,
    Obj.trail_amulet_of_glory_3 to Obj.trail_amulet_of_glory_4,
    Obj.obj_2572 to Obj.obj_20659,
    Obj.obj_20653 to Obj.obj_20659,
    Obj.obj_20655 to Obj.obj_20659,
    Obj.obj_20657 to Obj.obj_20659,
)

private val INVENTORY_INTERFACES = intArrayOf(Interface.inventory)

@ServerStartupEvent
fun mapInventory() {
    onButtonClick(Interface.inventory) { e ->
        val (player, _, _, slotId, slotId2, opNum) = e
        if (e.component != Component.inventory.inv) return@onButtonClick
        if (slotId > 27 || player.interfaceManager.containsInventoryInter()) return@onButtonClick
        val item = player.inventory.getItem(slotId)
        if (item == null || item.id != slotId2) {
            player.inventory.refresh(slotId)
            return@onButtonClick
        }
        when (opNum) {
            1 -> InventoryOptionsHandler.handleItemOption1(player, slotId, slotId2, item)
            2 -> InventoryOptionsHandler.handleItemOption2(player, slotId, slotId2, item)
            3 -> InventoryOptionsHandler.handleItemOption3(player, slotId, slotId2, item)
            4 -> InventoryOptionsHandler.handleItemOption4(player, slotId, slotId2, item)
            5 -> InventoryOptionsHandler.handleItemOption5(player, slotId, slotId2, item)
            7 -> InventoryOptionsHandler.handleItemOption6(player, slotId, slotId2, item)
            8 -> InventoryOptionsHandler.handleItemOption7(player, slotId, slotId2, item)
            10 -> InventoryOptionsHandler.handleItemOption8(player, slotId, slotId2, item)
        }
    }

    onInterfaceOnInterface(fromInterfaceIds = INVENTORY_INTERFACES, fromComponentIds = null, toInterfaceIds = INVENTORY_INTERFACES, toComponentIds = null) { useItemOnItem(it) }

    onInterfaceOnPlayer(checkDistance = false, fromInterfaceIds = INVENTORY_INTERFACES, fromComponentIds = null) { useItemOnPlayer(it) }

    onInterfaceOnNPC(checkDistance = false, fromInterfaceIds = INVENTORY_INTERFACES, fromComponentIds = null) { useItemOnNpc(it) }

    onItemClick(Obj.coins, options = arrayOf("Add-to-pouch")) { (player, item) ->
        if (player.inventory.getCoins() + item.amount < 0L) {
            player.sendMessage("Your pouch is full somehow. This kind of money probably deserves a ban LOL.")
            return@onItemClick
        }
        player.inventory.deleteItem(item)
        player.inventory.addCoins(item.amount)
    }
}

private fun useItemOnItem(e: IFOnIFEvent) {
    val player = e.player
    if (player.interfaceManager.containsInventoryInter()) return
    if (e.fromSlotId !in 0..27 || e.toSlotId !in 0..27 || e.fromSlotId == e.toSlotId) return
    val usedWith = player.inventory.getItem(e.toSlotId) ?: return
    val itemUsed = player.inventory.getItem(e.fromSlotId) ?: return
    if (itemUsed.id != e.fromSlotId2 || usedWith.id != e.toSlotId2) return
    player.stopAll()
    if (!InventoryOptionsHandler.handleItemOnItem(player, itemUsed, usedWith, e.fromSlotId, e.toSlotId))
        player.sendMessage("Nothing interesting happens.")
}

private fun useItemOnPlayer(e: IFOnPlayerEvent) {
    val player = e.player
    val target = e.target
    val item = player.inventory.getItem(e.slotId) ?: return
    if (!player.controllerManager.processItemOnPlayer(target, item, e.slotId)) return
    if (BotContainment.refuseExchange(player, target)) return
    player.stopAll(false)
    if (PluginManager.handle(ItemOnPlayerEvent(player, target, item, false))) return
    player.interactionManager.setInteraction(StandardEntityInteraction(target, 0) {
        if (player.inventory.containsItem(item.id, item.amount)) {
            player.faceEntityTile(target)
            PluginManager.handle(ItemOnPlayerEvent(player, target, item, true))
        }
    })
}

private fun useItemOnNpc(e: IFOnNPCEvent) {
    val player = e.player
    val target = e.target
    val item = player.inventory.getItem(e.slotId) ?: return
    if (!player.controllerManager.processItemOnNPC(target, item)) return
    player.stopAll(false)
    if (PluginManager.handle(ItemOnNPCEvent(player, target, item.withSlot(e.slotId), false))) return
    val distance = PluginManager.getObj(NPCInteractionDistanceEvent(player, target)) as Int? ?: 0
    player.interactionManager.setInteraction(StandardEntityInteraction(target, distance) {
        if (player.inventory.containsItem(item.id, item.amount)) useItemOnReachedNpc(player, target, item)
    })
}

private fun useItemOnReachedNpc(player: Player, target: NPC, item: Item) {
    player.faceEntityTile(target)
    when (target) {
        is Pet -> return player.petManager.eat(item.id, target)
        is ConditionalDeath -> {
            target.useHammer(player)
            return
        }
        is Familiar -> if (target.pouch == Pouch.GEYSER_TITAN && target.id == Npc.lore_geyser_titan && item.id in GEYSER_TITAN_RECHARGES) rechargeJewellery(player)
    }
    PluginManager.handle(ItemOnNPCEvent(player, target, item, true))
}

private fun rechargeJewellery(player: Player) {
    for (item in player.inventory.items.array())
        item?.let { GEYSER_TITAN_RECHARGES[it.id]?.let(it::changeId) }
    player.itemDialogue(Obj.amulet_of_glory_4, "Your ring of wealth and amulet of glory have all been recharged.")
}
