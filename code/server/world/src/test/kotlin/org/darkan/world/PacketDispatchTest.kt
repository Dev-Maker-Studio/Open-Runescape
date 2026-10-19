package org.darkan.world

import com.rs.game.World
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.events.ItemOnObjectEvent
import com.rs.plugin.handlers.ItemOnObjectHandler
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.model.ObjectShape
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.packet.Chat
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.net.prot.packet.IfButton
import org.darkan.core.net.prot.packet.IfDragOntoIf
import org.darkan.core.net.prot.packet.IfOnObject
import org.darkan.core.net.prot.packet.MessagePublic
import org.darkan.world.harness.HarnessPlayer
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression tests for client-packet dispatch that was routing to the wrong place, so the content
 * behind it never ran.
 */
class PacketDispatchTest {

    private val loggedIn = mutableListOf<HarnessPlayer>()
    private val spawned = mutableListOf<GameObject>()

    @AfterTest
    fun tearDown() {
        loggedIn.forEach { WorldHarness.logout(it) }
        loggedIn.clear()
        spawned.forEach { World.removeObject(it) }
        spawned.clear()
    }

    private fun login(name: String): HarnessPlayer =
        WorldHarness.login(name, Rights.OWNER).also {
            loggedIn.add(it)
            WorldHarness.tick()
        }

    // ---------- Item on object ----------

    /**
     * "Use <item> on <object>" must reach [ItemOnObjectEvent]. The inventory interface was routed
     * into the familiar-special branch, which fires InterfaceOnObjectEvent instead - so every
     * use-item-on-object in the game did nothing.
     */
    @Test
    fun itemOnObjectReachesItemOnObjectEvent() {
        val harness = login("itemonobj")
        val player = harness.player
        val fired = mutableListOf<Pair<Int, Int>>()
        val objectId = 1276 // a tree; any interactable id the probe can key on

        val tile = Tile.of(player.x + 1, player.y, player.plane)
        val obj = GameObject(objectId, ObjectShape.SCENERY_INTERACT, 0, tile)
        World.spawnObject(obj)
        spawned.add(obj)

        registerItemOnObjectProbe(objectId) { e -> fired.add(e.item.id to e.objectId) }

        player.inventory.items.clear()
        player.inventory.refresh()
        player.inventory.addItem(1511, 1)
        assertEquals(1, player.inventory.getNumberOf(1511), "test setup: logs never entered the inventory")

        harness.push(IfOnObject(Interface.inventory, 0, 0, 1511, objectId, tile.x, tile.y, false))
        WorldHarness.tick()

        assertTrue(fired.isNotEmpty(), "ItemOnObjectEvent never fired for item 1511 on object $objectId")
        assertEquals(1511 to objectId, fired.first())
    }

    /** The item has to come out of the player's own slot, not out of the packet. */
    @Test
    fun itemOnObjectIgnoresAnItemThePlayerDoesNotHold() {
        val harness = login("itemonobjspoof")
        val player = harness.player
        val fired = mutableListOf<Pair<Int, Int>>()
        val objectId = 1277

        val tile = Tile.of(player.x + 1, player.y, player.plane)
        val obj = GameObject(objectId, ObjectShape.SCENERY_INTERACT, 0, tile)
        World.spawnObject(obj)
        spawned.add(obj)

        registerItemOnObjectProbe(objectId) { e -> fired.add(e.item.id to e.objectId) }

        // Slot 0 holds coins; the packet claims a dragon platebody.
        player.inventory.items.clear()
        player.inventory.refresh()
        player.inventory.addItem(995, 100)
        harness.push(IfOnObject(Interface.inventory, 0, 0, 1127, objectId, tile.x, tile.y, false))
        WorldHarness.tick()

        assertTrue(fired.isEmpty(), "an item the player does not hold was accepted: $fired")
    }

    private fun registerItemOnObjectProbe(objectId: Int, onEvent: (ItemOnObjectEvent) -> Unit) {
        val handler = ItemOnObjectHandler(arrayOf<Any>(objectId), null) { e -> onEvent(e) }
        ItemOnObjectEvent.registerMethod(ItemOnObjectEvent::class.java, handler)
    }

    // ---------- Chat channel ----------

    /**
     * Which channel a line goes to is server state set by ChatTypeHandler, not a field on the Chat
     * packet (the codec never fills that in). Reading it off the packet sent every friends-chat
     * and clan-chat line out as public chat.
     */
    @Test
    fun friendsChatLineIsNotBroadcastAsPublicChat() {
        val harness = login("fcchat")
        harness.player.chatType = 2
        harness.clearSent()

        harness.push(Chat(type = -1, color = 0, effect = 0, message = "friends only"))
        WorldHarness.tick()

        assertFalse(
            harness.sent.any { it is MessagePublic },
            "a friends-chat line was broadcast as public chat: ${harness.sent.map { it::class.simpleName }}"
        )
    }

    /** ...and the default channel still is public chat. */
    @Test
    fun publicChatStillReachesNearbyPlayers() {
        val harness = login("pubchat")
        harness.player.chatType = 0
        harness.clearSent()

        harness.push(Chat(type = -1, color = 0, effect = 0, message = "hello world"))
        WorldHarness.tick()

        val public = harness.sent.filterIsInstance<MessagePublic>()
        assertTrue(public.isNotEmpty(), "public chat produced no MessagePublic")
        assertEquals("hello world", public.first().message.message)
    }

    // ---------- Drag onto interface ----------

    /** Dragging one inventory slot onto another must swap them. The handler had the wrong ids. */
    @Test
    fun draggingInventorySlotsSwapsThem() {
        val harness = login("dragitems")
        val inv = harness.player.inventory
        inv.items.clear()
        inv.refresh()
        inv.addItem(1511, 1)
        inv.addItem(1521, 1)
        assertEquals(1511, inv.getItem(0)?.id, "test setup: slot 0")
        assertEquals(1521, inv.getItem(1)?.id, "test setup: slot 1")

        // The inventory tab numbers its slots from 28, so slot 1 arrives as toSlot 29.
        harness.push(IfDragOntoIf(Interface.inventory, 0, 0, 1511, Interface.inventory, 0, 29, 1521))
        WorldHarness.tick()

        assertEquals(1521, inv.getItem(0)?.id, "slots were not swapped")
        assertEquals(1511, inv.getItem(1)?.id, "slots were not swapped")
    }

    // ---------- The chosenAccountType gate ----------

    /**
     * Documents how severe `Player.processPackets`' gate is: a player whose `chosenAccountType` is
     * false has every packet dropped except IfContinue and IfButton op1, so they cannot walk,
     * click, drag or talk. That is legacy behaviour (the gamemode-selection dialogue is meant to be
     * the only thing on screen), but it means any account that reaches the world with the flag
     * unset - a migrated save, say - looks completely frozen with no error anywhere.
     */
    @Test
    fun playerWithoutAChosenAccountTypeHasPacketsDropped() {
        val harness = WorldHarness.login("nogamemode", Rights.OWNER)
        loggedIn.add(harness)
        WorldHarness.tick()
        harness.player.isChosenAccountType = false

        val inv = harness.player.inventory
        inv.items.clear()
        inv.refresh()
        inv.addItem(1511, 1)
        inv.addItem(1521, 1)
        harness.push(IfDragOntoIf(Interface.inventory, 0, 0, 1511, Interface.inventory, 0, 29, 1521))
        WorldHarness.tick()

        assertEquals(1511, inv.getItem(0)?.id, "the gate is meant to drop this packet")
    }

    // ---------- Locked-player escape hatch ----------

    /**
     * Transformation items lock the player and put their own deactivate button on screen, so a
     * locked player holding the TransformationRing attrib must still have button clicks dispatched
     * - otherwise the item traps them permanently.
     */
    @Test
    fun lockedPlayerWithTransformationRingStillGetsButtonClicks() {
        val harness = login("transformlock")
        harness.player.lock()
        harness.player.tempAttribs.setB("TransformationRing", true)
        harness.clearSent()

        harness.push(IfButton(opNum = 1, interfaceId = 182, componentId = 6, slotId = -1, itemId = -1))
        WorldHarness.tick()

        // The click reached Logout, which refused because the player is locked. Reaching the
        // refusal is the point: without the escape hatch nothing is dispatched at all.
        assertTrue(
            harness.sent.any { it is GameMessage && it.message.contains("can't log out while performing an action") },
            "the click was dropped before dispatch; sent=${harness.sent.map { it::class.simpleName }}"
        )
    }

    /** A locked player without that attrib is still gated, as before. */
    @Test
    fun lockedPlayerWithoutTransformationRingIsStillGated() {
        val harness = login("plainlock")
        harness.player.lock()
        harness.clearSent()

        harness.push(IfButton(opNum = 1, interfaceId = 182, componentId = 6, slotId = -1, itemId = -1))
        WorldHarness.tick()

        assertNull(
            harness.sent.firstOrNull { it is GameMessage && it.message.contains("can't log out") },
            "a locked player's button click was dispatched"
        )
    }
}
