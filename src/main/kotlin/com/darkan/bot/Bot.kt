package com.darkan.bot

import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.utils.*
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.node.HashTableIterator
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode
import com.jagex.game.runetek5.entity.pathingentity.EntityHitbar
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategy
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.vardomain.VarProviders
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.pow

fun main(args: Array<String>) {
    Bot.ENABLED = true
    ScriptExecutor.loadScripts()
    Loader.main(args)
}

object Bot {

    @JvmField
    var PROD: Boolean = true
    @JvmField
    var ENABLED: Boolean = false

    // Interface IDs
    private const val BANK_INTERFACE = 762
    private const val DEPOSIT_BOX_INTERFACE = 11
    private const val INVENTORY_INTERFACE = 679

    var disableInterfaces = false
    var disableIncomingPackets = false

    val responsePlayers = mutableListOf<String>()

    @JvmField
    var inventory = ContainerWrapper(ItemContainer())
    @JvmField
    var bank = ContainerWrapper(ItemContainer())

    var username = ""
    var password = ""

    var lastAnimated = System.currentTimeMillis()
    val optionsDialogue = arrayOfNulls<String>(5)

    @JvmStatic
    fun pulse() {
        try {
            ScriptExecutor.mainLogic()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun bankIsOpen() = interfaceOpen(762)

    @JvmStatic
    fun refreshLastAnimated() {
        lastAnimated = System.currentTimeMillis()
    }

    fun getTimeSinceLastAnimation() = System.currentTimeMillis() - lastAnimated

    @JvmStatic
    fun getMyPlayerPosition() = Tile(getMyPlayerX(), getMyPlayerY(), getMyPlayer().plane.toInt())

    fun getCurrentAnimation() = getMyPlayer().currentAnimations?.get(0) ?: -1

    fun millisElapsed(startTime: Long) = System.currentTimeMillis() - startTime

    fun secondsElapsed(startTime: Long) = millisElapsed(startTime) / 1000f

    fun minutesElapsed(startTime: Long) = secondsElapsed(startTime) / 60f

    fun hoursElapsed(startTime: Long) = minutesElapsed(startTime) / 60f

    fun getXpPerHour(skillId: Int, startingXp: Int, startTime: Long) =
        ((getXp(skillId) - startingXp) / hoursElapsed(startTime)).toInt()

    fun getFormattedXpPerHour(skillId: Int, startingXp: Int, startTime: Long) =
        format(getXpPerHour(skillId, startingXp, startTime))

    fun format(number: Int) = NumberFormat.getNumberInstance(Locale.US).format(number)

    fun isAnimating() = getCurrentAnimation() != -1

    fun tradePlayer(username: String) = playerOption(username, 4)

    fun continueDialogue() = clickDialogue(1184, 18)

    fun interfaceOpen(id: Int): Boolean {
        val iterator = HashTableIterator<SubInterface>(client.OPEN_INTERFACES)
        for (inter in iterator) {
            if (inter?.subInterfaceId == id) return true
        }
        return false
    }

    fun setRunning(on: Boolean) {
        if (getVar(173) == 0 && on)
            clickButton(750, 4)
    }

    fun clickDialogueOption(option: Int) {
        clickDialogue(1188, if (option == 1) 11 else (12 + option - 1))
    }

    fun clickDialogueOption(option: String) {
        val op = optionsDialogue.indexOfFirst {
            it?.contains(option, ignoreCase = true) == true
        }
        if (op != -1) {
            clickDialogue(1188, if (op == 0) 11 else (12 + op))
        }
    }

    fun getPlayerPid(username: String): Int {
        for (i in 0 until PlayerUpdate.NUM_OUT_PLAYER_INDICES) {
            val player = PlayerUpdate.players[PlayerUpdate.OUT_PLAYER_INDICES[i]] ?: continue
            if (player.username?.equals(username, ignoreCase = true) == true) {
                return PlayerUpdate.OUT_PLAYER_INDICES[i]
            }
        }
        return -1
    }

    fun getPlayer(username: String): PlayerEntity? {
        for (i in 0 until PlayerUpdate.NUM_OUT_PLAYER_INDICES) {
            val player = PlayerUpdate.players[PlayerUpdate.OUT_PLAYER_INDICES[i]] ?: continue
            if (player.username?.equals(username, ignoreCase = true) == true) {
                return player
            }
        }
        return null
    }

    fun getClosestGroundItem(): Item? {
        return getGroundItems()
            .mapNotNull { item ->
                val distance = getDistanceTo(item.position)
                if (distance != -1) distance to item else null
            }
            .minByOrNull { it.first }
            ?.second
    }

    private inline fun <T> getClosestEntity(
        entities: List<T>,
        distanceCalculator: (T) -> Int
    ): T? {
        return entities
            .mapNotNull { entity ->
                val distance = distanceCalculator(entity)
                if (distance != -1) distance to entity else null
            }
            .minByOrNull { it.first }
            ?.second
    }

    fun getClosestObjectWithOption(option: String) =
        getClosestEntity(getNearbyObjectsWithOption(option)) { getDistanceTo(it) }

    fun getClosestObjectNoClip(id: String) =
        getClosestEntity(getObjectsNearby(id)) { Utils.distance(getMyPlayerPosition(), it) }

    fun getClosestObjectExact(id: String) =
        getClosestEntity(getObjectsNearbyExact(id)) { getDistanceTo(it) }

    fun getClosestObject(id: String) =
        getClosestEntity(getObjectsNearby(id)) { getDistanceTo(it) }

    fun getClosestObject(vararg ids: Int): WorldObject? {
        val allObjects = ids.flatMap { getObjectsNearby(it) }
        return getClosestEntity(allObjects) { getDistanceTo(it) }
    }

    fun getClosestObject(id: Int) =
        getClosestEntity(getObjectsNearby(id)) { getDistanceTo(it) }

    fun getClosestObject(id: String, option: String) =
        getClosestEntity(getObjectsNearby(id, option)) { getDistanceTo(it) }

    fun getClosestObject(id: Int, option: String) =
        getClosestEntity(getObjectsNearby(id, option)) { getDistanceTo(it) }

    operator fun Array<String?>.contains(option: String) =
        any { it?.equals(option, ignoreCase = true) == true }

    fun getObjectAt(tile: Tile) =
        getNearbyObjects().find { it.x == tile.x && it.y == tile.y }

    fun getObjectsNearby(id: Int, option: String) =
        getNearbyObjects().filter { it.id == id && it.getDefinitions().actions?.contains(option) == true }

    fun getObjectsNearby(id: String, option: String) =
        getNearbyObjects().filter {
            it.getDefinitions().name.contains(id, ignoreCase = true) &&
            it.getDefinitions().actions?.contains(option) == true
        }

    fun getObjectsNearby(id: Int) =
        getNearbyObjects().filter { it.id == id }

    fun getNearbyObjectsWithOption(option: String) =
        getNearbyObjects().filter { it.getDefinitions().actions?.contains(option) == true }

    fun getObjectsNearby(id: String) =
        getNearbyObjects().filter { it.getDefinitions().name.contains(id, ignoreCase = true) }

    fun getObjectsNearbyExact(id: String) =
        getNearbyObjects().filter { it.getDefinitions().name.equals(id, ignoreCase = true) }

    fun openClosestDepositBox(): Boolean {
        getClosestObjectWithOption("deposit")?.let {
            clickObject(it, "deposit")
            return true
        }
        getClosestObjectWithOption("bank")?.let {
            clickObject(it, "bank")
            return true
        }
        getClosestObject("bank chest")?.let {
            clickObject(it, "use")
            return true
        }
        getClosestNPC("banker")?.let {
            clickNPC(it, "bank")
            return true
        }
        return false
    }

    fun openClosestBank(): Boolean {
        if (inventory.contains(5733, 1)) {
            clickItem("rotten potato", 2)
            return true
        }
        getClosestObjectWithOption("bank")?.let {
            clickObject(it, "bank")
            return true
        }
        getClosestObject("bank chest")?.let {
            clickObject(it, "use")
            return true
        }
        getClosestNPC("banker")?.let {
            clickNPC(it, "bank")
            return true
        }
        return false
    }

    fun clickClosestObject(name: Int, action: String) =
        getClosestObject(name, action)?.let { clickObject(it, it.containsOption(action)) }

    fun clickClosestObject(name: String, action: String) =
        getClosestObject(name, action)?.let { clickObject(it, it.containsOption(action)) }

    fun clickClosestObject(name: Int, action: Int) =
        getClosestObject(name)?.let { clickObject(it, action) }

    fun clickClosestObject(name: IntArray, action: Int) =
        getClosestObject(*name)?.let { clickObject(it, action) }

    fun clickClosestObject(name: String, action: Int) =
        getClosestObject(name)?.let { clickObject(it, action) }

    fun clickClosestObject(name: Int) =
        getClosestObject(name)?.let { clickObject(it) }

    fun clickClosestObject(name: String) =
        getClosestObject(name)?.let { clickObject(it) }

    fun playerOption(username: String, option: Int) {
        val prot = when (option) {
            1 -> ClientProt.PLAYER_OP1
            4 -> ClientProt.PLAYER_OP4
            5 -> ClientProt.PLAYER_OP5
            6 -> ClientProt.PLAYER_OP6
            7 -> ClientProt.PLAYER_OP7
            9 -> ClientProt.PLAYER_OP9
            else -> null
        } ?: return

        val packet = PacketEncoder.createPacket(prot, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShort(getPlayerPid(username))
        packet.buffer.writeByteSubtract(0)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun sendText(string: String) {
        val packet = PacketEncoder.createPacket(ClientProt.RESUME_TEXTDIALOG, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeByte(string.length + 1)
        packet.buffer.writeString(string)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun addResponsePlayer(username: String) {
        if (!playerInList(username))
            responsePlayers.add(username)
    }

    fun removeResponsePlayer(username: String) = responsePlayers.remove(username)

    fun clearResponsePlayers() = responsePlayers.clear()

    fun sendItemOnObject(item: String, obj: WorldObject) {
        val itemId = inventory.getItem(inventory.getSlotByItem(item))
        sendItemOnObject(itemId, obj.id, obj.x, obj.y)
    }

    fun sendItemOnObject(item: String, id: Int) {
        getClosestObject(id)?.let { sendItemOnObject(item, it) }
    }

    fun sendItemOnObject(item: String, objectName: String) {
        getClosestObject(objectName)?.let { sendItemOnObject(item, it) }
    }

    fun sendItemOnObject(itemId: Int, objectId: Int, x: Int, y: Int) {
        val packet = PacketEncoder.createPacket(ClientProt.IF_ON_OBJECT, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShortAddLittle(x)
        packet.buffer.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
        packet.buffer.writeIntMiddle(objectId)
        packet.buffer.writeInt(44498944)
        packet.buffer.writeShortLittle(itemId)
        packet.buffer.writeShortAdd(inventory.getSlotByItem(itemId))
        packet.buffer.writeShortLittle(y)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun clickDialogue(id: Int) {
        val packet = PacketEncoder.createPacket(ClientProt.IF_CONTINUE, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeIntMiddle(id)
        packet.buffer.writeShortAddLittle(-1)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun clickSkillDialogue(option: Int) = clickDialogue(905, option + 13)

    fun clickDialogue(interfaceId: Int, componentId: Int) =
        clickDialogue(interfaceId shl 16 or componentId)

    fun playerInList(username: String) =
        responsePlayers.any { it.equals(username, ignoreCase = true) }

    fun getMyPlayer(): PlayerEntity = PlayerUpdate.MY_PLAYER!!

    fun itemOnItem(fromItem: Int, fromSlot: Int, toItem: Int, toSlot: Int) {
        interfaceOnInterface(44498944, 44498944, fromSlot, toSlot, fromItem, toItem)
    }

    fun itemOnItem(item1: String, item2: String) {
        val slot1 = inventory.getSlotByItem(item1)
        val slot2 = inventory.getSlotByItem(item2)
        itemOnItem(inventory.getItem(slot1), slot1, inventory.getItem(slot2), slot2)
    }

    fun itemOnItem(itemId: Int, item: String) {
        val slot1 = inventory.getSlotByItem(itemId)
        val slot2 = inventory.getSlotByItem(item)
        itemOnItem(itemId, slot1, inventory.getItem(slot2), slot2)
    }

    fun itemOnItem(itemId: Int, itemId2: Int) {
        itemOnItem(itemId, inventory.getSlotByItem(itemId), itemId2, inventory.getSlotByItem(itemId2))
    }

    fun alch(slot: Int) {
        interfaceOnInterface(hashFromInterface(192, 59), hashFromInterface(INVENTORY_INTERFACE, 0), -1, slot, -1, inventory.getItem(slot))
    }

    fun interfaceOnInterface(fromIf: Int, toIf: Int, fromSlot: Int, toSlot: Int, fromItem: Int, toItem: Int) {
        val packet = PacketEncoder.createPacket(ClientProt.IF_ON_IF, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShortAddLittle(toSlot)
        packet.buffer.writeShortLittle(fromSlot)
        packet.buffer.writeShortAddLittle(toItem)
        packet.buffer.writeIntLittle(toIf)
        packet.buffer.writeIntInverseMiddle(fromIf)
        packet.buffer.writeShortLittle(fromItem)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun activateQuickprayers() = clickButton(749, 4)

    private fun getDepositInterfaceInfo(option: Int): Triple<Int, Int, Int> {
        return if (interfaceOpen(DEPOSIT_BOX_INTERFACE)) {
            Triple(DEPOSIT_BOX_INTERFACE, 17, if (option == 1) 1 else 4)
        } else {
            Triple(763, 0, option)
        }
    }

    fun depositItem(itemId: Int) {
        val (ifId, compId, option) = getDepositInterfaceInfo(1)
        clickButton(ifId, compId, inventory.getSlotByItem(itemId), itemId, option)
    }

    fun depositAllOfItem(itemId: Int) {
        val (ifId, compId, option) = getDepositInterfaceInfo(6)
        clickButton(ifId, compId, inventory.getSlotByItem(itemId), itemId, option)
    }

    fun depositAllOfItem(itemName: String) {
        val (ifId, compId, option) = getDepositInterfaceInfo(6)
        clickButton(ifId, compId, inventory.getSlotByItem(itemName), inventory.getItemId(itemName), option)
    }

    fun depositAllOfItemBySlot(slot: Int) {
        val (ifId, compId, option) = getDepositInterfaceInfo(6)
        clickButton(ifId, compId, slot, inventory.getItem(slot), option)
    }

    fun depositAllExcept(vararg itemIds: Int) {
        val (ifId, compId, option) = getDepositInterfaceInfo(6)
        for (i in 0 until 28) {
            val item = inventory.getItem(i)
            if (item == -1 || item in itemIds) continue
            clickButton(ifId, compId, i, item, option)
        }
    }

    private fun withdrawItem(slot: Int, itemId: Int, option: Int) =
        clickButton(BANK_INTERFACE, 95, slot, itemId, option)

    fun withdrawOneItem(itemId: Int) = withdrawItem(bank.getSlotByItem(itemId), itemId, 1)
    fun withdraw5Item(itemId: Int) = withdrawItem(bank.getSlotByItem(itemId), itemId, 2)
    fun withdraw10Item(itemId: Int) = withdrawItem(bank.getSlotByItem(itemId), itemId, 3)
    fun withdrawCustomItem(itemId: Int) = withdrawItem(bank.getSlotByItem(itemId), itemId, 4)
    fun withdrawAllButOneItem(itemId: Int) = withdrawItem(bank.getSlotByItem(itemId), itemId, 7)
    fun withdrawAllOfItem(itemId: Int) = withdrawItem(bank.getSlotByItem(itemId), itemId, 6)

    fun withdrawAllOfItem(name: String) {
        val slot = bank.getSlotByItem(name)
        withdrawItem(slot, bank.getItem(slot), 6)
    }

    fun withdrawOneItem(name: String) {
        val slot = bank.getSlotByItem(name)
        withdrawItem(slot, bank.getItem(slot), 1)
    }

    fun withdraw5Item(name: String) {
        val slot = bank.getSlotByItem(name)
        withdrawItem(slot, bank.getItem(slot), 2)
    }

    fun withdraw10Item(name: String) {
        val slot = bank.getSlotByItem(name)
        withdrawItem(slot, bank.getItem(slot), 3)
    }

    fun withdrawCustomItem(name: String) {
        val slot = bank.getSlotByItem(name)
        withdrawItem(slot, bank.getItem(slot), 4)
    }

    fun withdrawAllButOneItem(name: String) {
        val slot = bank.getSlotByItemOfGreaterAmount(name, 1)
        withdrawItem(slot, bank.getItem(slot), 7)
    }

    fun equipItem(itemId: Int, slot: Int) = clickButton(INVENTORY_INTERFACE, 0, slot, itemId, 2)
    fun clickItem(itemId: Int, slot: Int) = clickButton(INVENTORY_INTERFACE, 0, slot, itemId, 1)
    fun clickItem(itemId: Int, slot: Int, option: Int) = clickButton(INVENTORY_INTERFACE, 0, slot, itemId, option)
    fun clickItem(itemId: Int) = clickButton(INVENTORY_INTERFACE, 0, inventory.getSlotByItem(itemId), itemId, 1)

    fun clickItem(name: String) {
        val slot = inventory.getSlotByItem(name)
        clickButton(INVENTORY_INTERFACE, 0, slot, inventory.getItem(slot), 1)
    }

    fun clickItem(name: String, option: Int) {
        val slot = inventory.getSlotByItem(name)
        clickButton(INVENTORY_INTERFACE, 0, slot, inventory.getItem(slot), option)
    }

    fun dropItem(itemId: Int, slot: Int) = clickButton(INVENTORY_INTERFACE, 0, slot, itemId, 8)

    fun bankAll() {
        if (inventory.contains(5733, 1))
            depositAllExcept(5733)
        else
            clickButton(BANK_INTERFACE, 33)
    }

    fun bankAllInDepositBox() = clickButton(DEPOSIT_BOX_INTERFACE, 18)

    fun bankAllExcept(id: Int) {
        clickButton(BANK_INTERFACE, 33)
        withdrawOneItem(id)
    }

    fun getNearbyGroundItemsWithIds(vararg ids: Int): List<Item> {
        return getGroundItems().filter { item ->
            Utils.distance(getMyPlayerPosition(), item.position) < 7 && item.id in ids
        }
    }

    fun closeInterfaces() = PacketEncoder.closeInterface()

    fun bankEquipment() = clickButton(49938469, -1, -1, 1)

    fun clickButton(interfaceId: Int, componentId: Int) =
        clickButton(interfaceId, componentId, -1, -1, 1)

    fun clickButton(interfaceId: Int, componentId: Int, clickType: Int) =
        clickButton(interfaceId, componentId, -1, -1, clickType)

    fun clickButton(interfaceId: Int, componentId: Int, slotId: Int, clickType: Int) =
        clickButton(interfaceId, componentId, slotId, -1, clickType)

    fun clickButton(interfaceId: Int, componentId: Int, slotId: Int, slotId2: Int, clickType: Int) {
        val interfaceHash = interfaceId shl 16 or componentId
        val context = client.getConnectionContext()

        val prot = when (clickType) {
            1 -> ClientProt.IF_OP1
            2 -> ClientProt.IF_OP2
            3 -> ClientProt.IF_OP3
            4 -> ClientProt.IF_OP4
            5 -> ClientProt.IF_OP5
            6 -> ClientProt.IF_OP6
            7 -> ClientProt.IF_OP7
            8 -> ClientProt.IF_OP8
            9 -> ClientProt.IF_OP9
            10 -> ClientProt.IF_OP10
            else -> return
        }

        val packet = PacketEncoder.createPacket(prot, context.isaac)
        PacketEncoder.method12590(packet, interfaceHash, slotId, slotId2)
        context.queuePacket(packet)
    }

    fun closeMainInterface() {
        // Class108.method1150(true, 1)
    }

    fun takeGroundItem(item: Item) = clickGroundItem(item, 3)
    fun takeGroundItem(itemId: Int, tile: Tile) = clickGroundItem(Item(itemId, tile), 3)

    fun clickGroundItem(item: Item, op: Int) {
        val prot = when (op) {
            1 -> ClientProt.GROUND_ITEM_OP1
            2 -> ClientProt.GROUND_ITEM_OP2
            3 -> ClientProt.GROUND_ITEM_OP3
            4 -> ClientProt.GROUND_ITEM_OP4
            5 -> ClientProt.GROUND_ITEM_OP5
            6 -> ClientProt.GROUND_ITEM_EXAMINE
            else -> return
        }

        val packet = PacketEncoder.createPacket(prot, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShortAddLittle(item.id)
        packet.buffer.writeByteInverse(if (KeyDownCommand.runKeyDown()) 1 else 0)
        packet.buffer.writeShort(item.position.y)
        packet.buffer.writeShortAdd(item.position.x)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        RouteFinder.routeToGrounditem(item.position.x, item.position.y)
    }

    fun command(message: String) = chat(";;$message", 0, 0)

    fun chat(message: String, color: Int, effect: Int) {
        val context = client.getConnectionContext()
        val packet = PacketEncoder.createPacket(ClientProt.CHAT, context.isaac)
        packet.buffer.writeByte(0)
        val startIndex = packet.buffer.currentPosition
        packet.buffer.writeByte(color)
        packet.buffer.writeByte(effect)
        PacketEncoder.chatMessageToBuffer(packet.buffer, message)
        packet.buffer.writeIndex(packet.buffer.currentPosition - startIndex)
        context.queuePacket(packet)
    }

    fun walkTo(tile: Tile) {
        val packet = PacketEncoder.walkToTile(tile.x - getBaseX(), tile.y - getBaseY(), 0)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet!!)
        RouteFinder.calculateRoute(
            tile.x - getBaseX(),
            tile.y - getBaseY(),
            true,
            RouteStrategies.forFixedTile(tile.x - getBaseX(), tile.y - getBaseY())
        )
    }

    fun getDistanceTo(strategy: RouteStrategy, alt: Boolean = false): Int {
        return Utils.findRouteDistance(
            getMyPlayer().endX.toInt(),
            getMyPlayer().endZ.toInt(), PlayerUpdate.MY_PLAYER!!.getSize(),
            strategy,
            Resource.mapRegion!!.getCollisionMap(PlayerUpdate.MY_PLAYER!!.plane.toInt()),
            alt,
            client.WALKING_STEPS_X,
            client.WALKING_STEPS_Y
        )
    }

    fun getDistanceTo(npc: NpcEntity) =
        getDistanceTo(RouteStrategies.forNpc(npc.movementBaseX[0], npc.movementBaseY[0], npc.getSize(), npc.getSize()))

    fun getDistanceTo(tile: Tile) =
        getDistanceTo(RouteStrategies.forFixedTile(tile.x - getBaseX(), tile.y - getBaseY()))

    fun inCombat(entity: PathingEntity): Boolean {
        if (entity.faceEntity != -1 && !facingMe(entity) && getHitbarValue(getFacing(entity), 0) <= 0)
            return true
        return false
    }

    fun facingMe(entity: PathingEntity) = (entity.faceEntity - 32768) == getMyPlayer().playerIndex

    fun getHitbarValue(entity: PathingEntity?, hitbarIndex: Int): Int {
        if (entity == null) return -1
        val hitbarDef = TypeLists.HITBAR!!.list(hitbarIndex)
        for (hitbar in entity.entityHitbars) {
            val bar = hitbar as? EntityHitbar ?: continue
            if (bar.hitbarType != hitbarDef) continue
            val update = bar.method12600(client.cycles)
            if (update != null) return update.toPercentage
        }
        return -1
    }

    fun getFacing(entity: PathingEntity): PathingEntity? {
        if (entity.faceEntity == -1) return null
        return when {
            entity.faceEntity < 32768 -> {
                val node = client.NPC_MAP.get(entity.faceEntity.toLong())
                node?.value as? NpcEntity
            }
            else -> PlayerUpdate.players[entity.faceEntity - 32768]
        }
    }

    fun getClosestOutOfCombatNPC(id: String) =
        getClosestEntity(getFilteredNPCs(id).filter { !inCombat(it) }) {
            Utils.distance(getMyPlayerPosition(), Tile(it.endX + getBaseX(), it.endZ + getBaseY()))
        }

    fun getClosestOutOfCombatNPCContaining(id: String) =
        getClosestEntity(getFilteredNPCsContaining(id).filter { !inCombat(it) }) { getDistanceTo(it) }

    fun getClosestOutOfCombatNPC(id: Int) =
        getClosestEntity(getFilteredNPCs(id).filter { !inCombat(it) }) { getDistanceTo(it) }

    fun getClosestNPCWithOption(option: String) =
        getClosestEntity(getFilteredNPCsByOption(option)) { getDistanceTo(it) }

    fun getClosestNPC(id: String) =
        getClosestEntity(getFilteredNPCs(id)) { getDistanceTo(it) }

    fun getClosestNPC(id: Int) =
        getClosestEntity(getFilteredNPCs(id)) { getDistanceTo(it) }

    fun getDistanceTo(obj: WorldObject): Int {
        val locShape: ObjectShapes = Identifiable.identify(ObjectShapes.entries.toTypedArray(), obj.type) as ObjectShapes? ?: return Int.MAX_VALUE
        val strategy: RouteStrategy = when {
            locShape == ObjectShapes.SCENERY_INTERACT || locShape == ObjectShapes.GROUND_INTERACT || locShape == ObjectShapes.GROUND_DECORATION -> {
                val defs = TypeLists.LOC!!.list(obj.id)
                val (sizeX, sizeY) = if (obj.rotation == 0 || obj.rotation == 2) {
                    defs.sizeX to defs.sizeY
                } else {
                    defs.sizeY to -defs.sizeX
                }
                RouteStrategies.forWallObject(
                    obj.x - getBaseX(),
                    obj.y - getBaseY(),
                    sizeX,
                    sizeY,
                    ObjectShapes.WALL_STRAIGHT,
                    0
                )
            }
            ObjectShapes.isWall(locShape.shape) -> {
                RouteStrategies.forWallObject(
                    obj.x - getBaseX(),
                    obj.y - getBaseY(),
                    0,
                    0,
                    locShape,
                    obj.rotation
                )
            }
            else -> {
                RouteStrategies.forInteractiveObject(
                    obj.x - getBaseX(),
                    obj.y - getBaseY(),
                    locShape,
                    obj.rotation
                )
            }
        }
        return getDistanceTo(strategy)
    }

    fun clickObject(obj: WorldObject?, action: String) {
        obj?.let { clickObject(it, it.containsOption(action)) }
    }

    fun clickObject(obj: WorldObject?, option: Int) {
        obj?.let { clickObject(it.id, it, option) }
    }

    fun clickObject(obj: WorldObject?) {
        obj?.let { clickObject(it.id, it, 2) }
    }

    fun clickObject(objectId: Int, tile: Tile, option: Int, walk: Boolean = true) {
        val prot = when (option) {
            1 -> ClientProt.OBJECT_OP1
            2 -> ClientProt.OBJECT_OP2
            3 -> ClientProt.OBJECT_OP3
            4 -> ClientProt.OBJECT_OP4
            5 -> ClientProt.OBJECT_OP5
            6 -> ClientProt.OBJECT_EXAMINE
            else -> return
        }

        val packet = PacketEncoder.createPacket(prot, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShort(tile.y)
        packet.buffer.writeShort(tile.x)
        packet.buffer.writeInt(objectId)
        packet.buffer.writeByteAdd(if (KeyDownCommand.runKeyDown()) 1 else 0)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun getBaseX() = Resource.mapRegion!!.getBaseCoordinates().x
    fun getBaseY() = Resource.mapRegion!!.getBaseCoordinates().y
    fun getMyPlayerX() = getMyPlayer().endX.toInt() + getBaseX()
    fun getMyPlayerY() = getMyPlayer().endZ.toInt() + getBaseY()

    fun getNpcIndexByName(name: String): Int =
        findNPC { it.definitions!!.name.equals(name, ignoreCase = true) }?.playerIndex ?: -1

    fun getPid(entity: PathingEntity) = entity.playerIndex

    private inline fun findNPC(predicate: (NpcEntity) -> Boolean): NpcEntity? {
        for (i in 0 until client.npcCount) {
            val idx = client.NPC_INDICES[i].toLong()
            val node = client.NPC_MAP.get(idx) ?: continue
            val npc = node.value as? NpcEntity ?: continue
            if (predicate(npc)) return npc
        }
        return null
    }

    fun getNPCByIndexExcluding(index: Int, exclude: Int): NpcEntity? =
        findNPC { it.playerIndex == index && it.playerIndex != exclude }

    fun getNPCByIndex(index: Int): NpcEntity? =
        findNPC { it.playerIndex == index }

    fun getNpcIndexById(id: Int): Int =
        findNPC { it.definitions!!.type == id }?.playerIndex ?: -1

    fun buy500ShopItem(slot: Int) = clickButton(1265, 20, slot, 6)

    fun clickNPC(npc: NpcEntity?, option: Int) {
        npc?.let { npcOption(it.playerIndex, option) }
    }

    fun clickNPC(npc: NpcEntity?, option: String) {
        npc?.let { npcOption(it.playerIndex, Utils.stringContainsIgnoreCase(it.definitions!!.actions, option)) }
    }

    fun clickNpc(name: String, option: Int) =
        getClosestNPC(name)?.let { clickNPC(it, option) }

    fun clickNPCWithNearestOption(name: String, optionRequired: String, action: String) {
        val nearestNPC = getNPCs().firstOrNull { npc ->
            npc.definitions!!.name.equals(name, ignoreCase = true) &&
            npc.definitions!!.actions?.any { it?.contains(optionRequired) == true } == true
        }
        clickNPC(nearestNPC, action)
    }

    fun clickObjectWithNearestOption(name: String, optionRequired: String, action: String) {
        val nearestObject = getNearbyObjects().firstOrNull { obj ->
            obj.getDefinitions().name.equals(name, ignoreCase = true) &&
            obj.getDefinitions().actions?.contains(optionRequired) == true
        }
        nearestObject?.let {
            Console.writeLine("Clicking ${it.getDefinitions().name}")
            clickObject(it, action)
        }
    }

    fun clickNpc(npcId: Int, option: Int) =
        getClosestNPC(npcId)?.let { clickNPC(it, option) }

    fun npcOption(index: Int, option: Int) {
        val prot = when (option) {
            1 -> ClientProt.NPC_OP1
            2 -> ClientProt.NPC_OP2
            3 -> ClientProt.NPC_OP3
            4 -> ClientProt.NPC_OP4
            5 -> ClientProt.NPC_OP5
            6 -> ClientProt.NPC_EXAMINE
            else -> return
        }

        val node = client.NPC_MAP.get(index.toLong()) ?: return
        val npc = node.value as? NpcEntity ?: return

        val packet = PacketEncoder.createPacket(prot, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShort(index)
        packet.buffer.writeByte(if (KeyDownCommand.runKeyDown()) 1 else 0)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        RouteFinder.calculateRoute(npc.movementBaseX[0], npc.movementBaseY[0], true, RouteStrategies.forPlayer(npc.movementBaseX[0], npc.movementBaseY[0], npc.getSize(), npc.getSize()));
    }

    fun dropAllExceptExact(vararg itemNames: String) {
        for (i in 0 until 28) {
            val itemName = TypeLists.OBJ!!.list(inventory.getItem(i)).name
            if (itemNames.any { it.equals(itemName, ignoreCase = true) }) continue
            dropItem(inventory.getItem(i), i)
            clickDialogue(77529097)
        }
    }

    fun dropAllExcept(vararg itemNames: String) {
        for (i in 0 until 28) {
            val itemName = TypeLists.OBJ!!.list(inventory.getItem(i)).name
            if (itemNames.any { itemName.contains(it, ignoreCase = true) }) continue
            dropItem(inventory.getItem(i), i)
            clickDialogue(77529097)
        }
    }

    fun dropWholeInventory() {
        for (i in 0 until 28) {
            dropItem(inventory.getItem(i), i)
            clickDialogue(77529097)
        }
    }

    fun isLoggedOut() = GameState.isLoggedOut(client.gameState)

    fun getGroundItems(): List<Item> {
        val items = mutableListOf<Item>()
        for (x in 0 until 104) {
            for (y in 0 until 104) {
                val positionHash = (getMyPlayer().plane.toLong() shl 28 or
                                   ((y + getBaseY()).toLong() shl 14) or
                                   (x + getBaseX()).toLong())
                val deque = client.GROUND_ITEMS.get(positionHash) ?: continue
                for (groundItems in deque.groundItems) {
                    val gi = groundItems as? GroundItemNode ?: continue
                    items.add(Item(gi.id, gi.amount, Tile(x + getBaseX(), y + getBaseY(), getMyPlayer().plane.toInt())))
                }
            }
        }
        return items
    }

    fun getNearbyObjects(): List<WorldObject> {
        val objects = mutableListOf<WorldObject>()
        for (x in 0 until 104) {
            for (y in 0 until 104) {
                val plane = getMyPlayer().plane.toInt()
                var obj: GameObject? = Resource.mapRegion!!.getScene()!!.getInteractiveObject(plane, x, y, client.INTERACTIVE_OBJECT_COMPARABLE) as GameObject?

                if (obj == null)
                    obj = Resource.mapRegion!!.getScene()!!.getGroundDecoration(plane, x, y) as GameObject?

                if (obj == null)
                    obj = Resource.mapRegion!!.getScene()!!.getWall(plane, x, y) as GameObject?

                if (obj == null)
                    obj = Resource.mapRegion!!.getScene()!!.getPrimaryWallDecoration(plane, x, y) as GameObject?

                obj?.let {
                    objects.add(WorldObject(
                        it.id,
                        it.getRotation(),
                        it.shape,
                        x + getBaseX(),
                        y + getBaseY(),
                        TypeLists.LOC!!.list(it.id).actions
                    ))
                }
            }
        }
        return objects
    }

    private fun joinArgs(cmd: Array<String>, startIndex: Int = 1) =
        cmd.drop(startIndex).joinToString(" ").replace("_", " ")

    @JvmStatic
    fun handleConsoleCommand(cmd: Array<String>) {
        when (cmd[0]) {
            // Movement & Position
            "walk" -> walkTo(Tile(cmd[1].toInt(), cmd[2].toInt(), 0))
            "pos" -> Console.writeLine("x: ${getMyPlayerX()} y: ${getMyPlayerY()} (${getMyPlayer().endX.toInt()}, ${getMyPlayer().endZ.toInt()})")
            "localpos" -> Console.writeLine("x: ${getMyPlayer().endX.toInt()} y: ${getMyPlayer().endZ.toInt()}")

            // Banking & Inventory
            "bankall" -> bankAll()
            "withdraw" -> withdrawOneItem(1333)
            "food" -> withdraw10Item(15272)
            "pray" -> withdraw5Item(139)
            "withdrawcustom" -> withdrawCustomItem(995)
            "empty" -> command("empty")
            "drop" -> dropWholeInventory()
            "closebank" -> closeInterfaces()
            "b" -> { command("bank"); bankAll() }

            // Trading & Players
            "trade" -> tradePlayer(cmd[1].replace("_", " "))
            "add" -> addResponsePlayer(cmd[1].replace("_", " "))
            "remove" -> removeResponsePlayer(cmd[1].replace("_", " "))
            "clear" -> clearResponsePlayers()

            // Game Commands
            "yell" -> command("yell ${joinArgs(cmd)}")
            "teleto" -> command("teleto ${joinArgs(cmd)}")
            "dicebag" -> command("dicebag ${joinArgs(cmd)}")
            "ticket" -> command("ticket ${joinArgs(cmd)}")
            "bank" -> command("bank ${joinArgs(cmd)}")
            "banticket" -> command("banticket ${joinArgs(cmd, 1)}")
            "resettask" -> command("resettask ${joinArgs(cmd)}")
            "bid" -> command("ah bid")

            // Interface & Buttons
            "clickbutton" -> clickButton(cmd[1].toInt(), cmd[2].toInt(), cmd[3].toInt(), cmd[4].toInt())
            "test" -> clickButton(374 shl 16 or 5, -1, -1, 1)
            "buy" -> repeat(20) { clickButton(82903060, -1, cmd[1].toInt(), 6) }
            "1b" -> clickButton(85983267, -1, -1, 1)

            // Items & Objects
            "itemtest" -> getGroundItems().forEach { println(it) }
            "useon" -> for (i in 0 until 14) itemOnItem(inventory.getItem(i), i, inventory.getItem(i + 14), i + 14)

            // NPCs & Objects
            "npcnext" -> { println(getClosestNPC("fishing spot")); getNPCs().forEach { println(it.definitions!!.name) } }
            "objnext" -> { println(getClosestObject("Fur stall")); println("Number found: ${getObjectsNearby("Fur stall").size}"); clickClosestObject("Fur stall") }
            "nearbyobjects" -> { Console.writeLine("Objects found: ${getObjectsNearby(cmd[1]).size}, Ids: "); getObjectsNearby(cmd[1]).forEach { Console.writeLine("ID: ${it.id}, Coords: X${it.x} Y${it.y} Z${it.z}") } }
            "objonme" -> println((Resource.mapRegion!!.getScene()!!.getInteractiveObject(getMyPlayer().plane.toInt(), getMyPlayer().endX.toInt(), getMyPlayer().endZ.toInt(), client.INTERACTIVE_OBJECT_COMPARABLE) as? GameObject)?.id)

            // Debug & Toggles
            "togglepackets" -> { disableIncomingPackets = !disableIncomingPackets; Console.writeLine("Disabled packets: $disableIncomingPackets") }
            "toggleinterfaces" -> { disableInterfaces = !disableInterfaces; Console.writeLine("Disabled interfaces: $disableInterfaces") }
            "iswalking" -> { Console.writeLine("${isWalking()}"); Console.writeLine(getMyPlayer().movementTypes.contentToString()); Console.writeLine("${getMyPlayer().movingSequence.getType()?.id}") }
            "healthpray" -> println("Health: ${getHealth()} Prayer: ${getPrayer()}")
            "curemote" -> Console.writeLine("Emote: ${getCurrentAnimation()}")

            // Other
            "string" -> sendText(cmd[1])
            "crash" -> chat("Bank sale.", -1, -1)
            "log" -> sendLogout(true)
            "chet" -> chat(joinArgs(cmd), 2, 4)

            // Complex commands that need their original form
            "objtest" -> {
                for (x in 0 until 104) {
                    for (y in 0 until 104) {
                        val plane = getMyPlayer().plane.toInt()
                        var obj: GameObject? = Resource.mapRegion!!.getScene()!!.getInteractiveObject(plane, x, y, client.INTERACTIVE_OBJECT_COMPARABLE) as? GameObject
                        if (obj == null) obj = Resource.mapRegion!!.getScene()!!.getGroundDecoration(plane, x, y) as? GameObject
                        if (obj == null) obj = Resource.mapRegion!!.getScene()!!.getWall(plane, x, y) as? GameObject
                        if (obj == null) obj = Resource.mapRegion!!.getScene()!!.getPrimaryWallDecoration(plane, x, y) as? GameObject
                        obj?.let { println("Object type: ${it.javaClass.name}") }
                    }
                }
            }

            else -> {}
        }
    }

    fun findGroundItem(id: String, distance: Int = 15): Tile? {
        return getGroundItems().firstOrNull { item ->
            Utils.distance(getMyPlayerPosition(), item.position) < distance &&
            getDistanceTo(item.position) != -1 &&
            item.getName().contains(id, ignoreCase = true)
        }?.let { Tile(it.position.x, it.position.y, it.id) }
    }

    fun findGroundItem(id: Int, distance: Int = 10): Tile? {
        return getGroundItems().firstOrNull { item ->
            Utils.distance(getMyPlayerPosition(), item.position) < distance &&
            getDistanceTo(item.position) != -1 &&
            item.id == id
        }?.let { Tile(it.position.x, it.position.y, it.id) }
    }

    fun isWalking(): Boolean {
        val animId = getMyPlayer().movingSequence.getType()?.id ?: -1
        return animId != 11975
    }

    fun getXp(skillId: Int) = client.SKILL_XP[skillId]
    fun getLevel(skillId: Int) = client.SKILL_LEVEL_VISIBLE[skillId]

    fun sendLogout(lobby: Boolean) {
        client.logout(if (lobby) LoginManager.currentlyInLobby else false)
    }

    fun getXPForLevel(level: Int): Int {
        var points = 0.0
        var output = 0
        for (lvl in 1..level) {
            points += floor(lvl + 300.0 * 2.0.pow(lvl / 7.0))
            if (lvl >= level) return output
            output = floor(points / 4).toInt()
        }
        return 0
    }

    fun getNPCs(): List<NpcEntity> {
        val npcs = mutableListOf<NpcEntity>()
        for (i in 0 until client.npcCount) {
            val npcNode = client.NPC_ARRAY[i]
            npcNode?.value?.let { npcs.add(it as NpcEntity) }
        }
        return npcs
    }

    fun getFilteredNPCs(id: Int) =
        getNPCs().filter { it.definitions!!.type == id }

    fun getFilteredNPCs(id: String) =
        getNPCs().filter { it.definitions!!.name.equals(id, ignoreCase = true) }

    fun getFilteredNPCsByOption(id: String) =
        getNPCs().filter { it.definitions!!.actions?.contains(id) == true }

    fun getFilteredNPCsContaining(id: String) =
        getNPCs().filter { it.definitions!!.name.contains(id, ignoreCase = true) }

    fun getClosestNPCNoClip(id: String) =
        getClosestEntity(getFilteredNPCs(id)) {
            Utils.distance(getMyPlayerPosition(), Tile(it.endX.toInt() + getBaseX(), it.endZ.toInt() + getBaseY()))
        }

    fun getClosestNPCNoClipContaining(id: String) =
        getClosestEntity(getFilteredNPCsContaining(id)) {
            Utils.distance(getMyPlayerPosition(), Tile(it.endX.toInt() + getBaseX(), it.endZ.toInt() + getBaseY()))
        }

    fun getClosestNPCNoClip(id: Int) =
        getClosestEntity(getFilteredNPCs(id)) {
            Utils.distance(getMyPlayerPosition(), Tile(it.endX.toInt() + getBaseX(), it.endZ.toInt() + getBaseY()))
        }

    private fun formatTimeUnit(value: Long) = if (value < 10) "0$value" else "$value"

    fun getTimeToLevel(skillId: Int, startXP: Int, startTime: Long, level: Int): String {
        val xpPh = getXpPerHour(skillId, startXP, startTime)
        val xpPs = xpPh / 3600f

        val secondsToLevel = (getXPToLevel(skillId, level) / xpPs).toInt()
        val minutesToLevel = secondsToLevel / 60
        val hoursToLevel = minutesToLevel / 60
        val formatMinutesToLevel = minutesToLevel - (hoursToLevel * 60)
        val formatSecondsToLevel = secondsToLevel - (minutesToLevel * 60)

        return "${formatTimeUnit(hoursToLevel.toLong())}:${formatTimeUnit(formatMinutesToLevel.toLong())}:${formatTimeUnit(formatSecondsToLevel.toLong())}"
    }

    fun getXPToLevel(skillId: Int, level: Int): Int {
        if (level == 126) return 200000000 - getXp(skillId)
        return getXPForLevel(level) - getXp(skillId)
    }

    fun getXPToNextLevel(skillId: Int) = getXPForLevel(getLevel(skillId) + 1) - getXp(skillId)

    fun getScriptTime(currTime: Long, startTime: Long): String {
        val ms = currTime - startTime
        val totalSecs = ms / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs / 60) % 60
        val secs = totalSecs % 60

        return "${formatTimeUnit(hours)}:${formatTimeUnit(mins)}:${formatTimeUnit(secs)}"
    }

    fun getLootPerHour(itemAmount: Int, startingAmount: Int, startTime: Long) =
        ((itemAmount - startingAmount) / hoursElapsed(startTime)).toInt()

    fun getFormattedLootPerHour(itemAmount: Int, startingAmount: Int, startTime: Long) =
        format(getLootPerHour(itemAmount, startingAmount, startTime))

    fun myPlayerInCombat() = inCombat(getMyPlayer())

    fun getHealth() = VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(7198).toDouble()
    fun getHealthPercent() = (getHealth() / (getLevel(Skill.CONSTITUTION.ordinal) * 10)) * 100.0
    fun usingSpec() = VarProviders.PLAYER_VAR_PROVIDER.getVarValue(301) == 1
    fun specPercent() = VarProviders.PLAYER_VAR_PROVIDER.getVarValue(300) / 10.0
    fun getPrayer() = VarProviders.PLAYER_VAR_PROVIDER.getVarValue(2382).toDouble()
    fun getVar(varId: Int) = VarProviders.PLAYER_VAR_PROVIDER.getVarValue(varId)
    fun getVarBit(varBit: Int) = VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(varBit)

    fun hasntAnimatedFor(time: Int) = getTimeSinceLastAnimation() > time

    fun dropAll(vararg ids: Int) {
        for (i in 0 until 28) {
            if (inventory.getItem(i) in ids) {
                dropItem(inventory.getItem(i), i)
            }
        }
    }

    fun sendClick() {
        val packet = PacketEncoder.createPacket(ClientProt.MOUSE_CLICK, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeIntLittle(Utils.random(0, 400) or (Utils.random(0, 400) shl 16))
        packet.buffer.writeShort(5 or (10 shl 0))
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    fun loginLobby(un: String, pw: String) = LoginManager.lobbyLogin(un, pw)
    fun loginWorld() = LoginManager.worldLogin(StaticMethods.aString99, StaticMethods.aString102)
}
