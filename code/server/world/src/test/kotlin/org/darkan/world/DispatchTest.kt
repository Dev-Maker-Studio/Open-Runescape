package org.darkan.world

import com.rs.engine.command.Commands
import com.rs.plugin.PluginManager
import com.rs.plugin.events.ButtonClickEvent
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.packet.IfButton
import org.darkan.core.net.prot.packet.LogoutFull
import org.darkan.core.net.prot.packet.LogoutLobby
import org.darkan.core.net.prot.packet.UpdateInvFull
import org.darkan.core.net.prot.packet.UpdateInvPartial
import org.darkan.world.harness.HarnessPlayer
import org.darkan.world.harness.WorldHarness
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the dispatch paths that were silently doing nothing on the modern world
 * server: the spawn commands' inventory update, and the logout tab's buttons.
 */
class DispatchTest {

    /** The client-side container key the inventory is transmitted under (`Inventory.processRefresh`). */
    private val INVENTORY_CONTAINER = 93

    private val loggedIn = mutableListOf<HarnessPlayer>()

    @AfterTest
    fun tearDown() {
        loggedIn.forEach { WorldHarness.logout(it) }
        loggedIn.clear()
    }

    /**
     * Logs a player in and lets the world tick once. Entity.isLocked() is `lockDelay >= tickCounter`
     * and both start at 0, so a player is nominally "locked" until its first processEntity() -
     * exactly as on a real server, where the client cannot click before then either.
     */
    private fun login(name: String, rights: Rights = Rights.OWNER): HarnessPlayer =
        WorldHarness.login(name, rights).also {
            loggedIn.add(it)
            WorldHarness.tick()
        }

    /** Everything the command system registered must be reachable at the OWNER rights walk. */
    @Test
    fun commandsAreRegistered() {
        WorldHarness.boot()
        val total = Rights.entries.sumOf { Commands.getCommands(it).size }
        assertTrue(total > 200, "expected the command registry to be populated, got $total commands")
    }

    /** `::item <id>` must put the item in the inventory and emit an inventory update for it. */
    @Test
    fun itemCommandAddsToInventoryAndSendsUpdate() {
        val harness = login("itemcmd")
        harness.clearSent()

        assertTrue(
            Commands.processCommand(harness.player, "item 4151", false, false),
            "::item was not dispatched by the command registry"
        )

        assertEquals(
            1,
            harness.player.inventory.getNumberOf(4151),
            "the item never reached the inventory container"
        )

        WorldHarness.tick()

        val slots = inventoryUpdatesFor(harness)
        assertTrue(
            slots.isNotEmpty(),
            "no inventory update was queued for container ${INVENTORY_CONTAINER}; " +
                "flushed=${harness.session.flushed.map { it::class.simpleName }}"
        )
        assertTrue(4151 in slots, "the inventory update did not carry item 4151, it carried $slots")
    }

    /** `::giveitem <player> <id> <amount>` must do the same for another player. */
    @Test
    fun giveItemCommandAddsToTargetInventory() {
        val giver = login("giver")
        val target = login("target")
        target.clearSent()

        assertTrue(
            Commands.processCommand(giver.player, "giveitem ${target.player.displayName} 995 5", false, false),
            "::giveitem was not dispatched by the command registry"
        )

        assertEquals(5, target.player.inventory.getNumberOf(995), "coins never reached the target inventory")

        WorldHarness.tick()
        assertTrue(995 in inventoryUpdatesFor(target), "no inventory update carried the coins")
    }

    /** Logout tab, "Exit to lobby" (component 6) must queue the lobby logout packet. */
    @Test
    fun logoutButtonExitsToLobby() {
        val harness = login("logoutlobby")
        harness.clearSent()

        harness.push(IfButton(opNum = 1, interfaceId = 182, componentId = 6, slotId = -1, itemId = -1))
        WorldHarness.tick()

        // `flushed`, not `sent`: Player.logout() finishes the player in the same call, and the
        // world tick's flush pass skips finished players, so a merely-queued logout never reaches
        // the client - the exact bug this asserts against.
        assertTrue(
            harness.session.flushed.any { it is LogoutLobby },
            "LogoutLobby never reached the client; events=${harness.session.events}"
        )
    }

    /** Logout tab, "Logout" (component 13) must queue the full logout packet. */
    @Test
    fun logoutButtonLogsOutFully() {
        val harness = login("logoutfull")
        harness.clearSent()

        harness.push(IfButton(opNum = 1, interfaceId = 182, componentId = 13, slotId = -1, itemId = -1))
        WorldHarness.tick()

        assertTrue(
            harness.session.flushed.any { it is LogoutFull },
            "LogoutFull never reached the client; events=${harness.session.events}"
        )
    }

    /**
     * The button dispatch itself: a `ButtonClickEvent` for interface 182 must find a registered
     * handler. If this fails, no interface in the game has working buttons.
     */
    @Test
    fun buttonClickEventHasRegisteredHandler() {
        val harness = login("buttondispatch")
        assertTrue(
            PluginManager.handle(ButtonClickEvent(harness.player, 182, 6, -1, -1, 1)),
            "PluginManager had no handler registered for ButtonClickEvent on interface 182"
        )
    }

    /** The logout tab has to actually be open, or IfButtonHandler's topOpen gate drops the click. */
    @Test
    fun logoutTabIsOpenAfterLogin() {
        val harness = login("logouttab")
        assertTrue(
            harness.player.interfaceManager.topOpen(182),
            "interface 182 is not registered as open, so every logout click is dropped"
        )
    }

    /** Item ids carried by inventory updates the client would actually have received. */
    private fun inventoryUpdatesFor(harness: HarnessPlayer): Set<Int> {
        val key = INVENTORY_CONTAINER
        val ids = mutableSetOf<Int>()
        harness.session.flushed.forEach { prot ->
            when (prot) {
                is UpdateInvFull -> if (prot.containerId == key) ids.addAll(prot.itemIds.toList())
                is UpdateInvPartial -> if (prot.containerId == key) ids.addAll(prot.updates.map { it.itemId })
                else -> {}
            }
        }
        ids.remove(-1)
        return ids
    }
}
