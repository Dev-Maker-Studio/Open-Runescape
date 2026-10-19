package org.darkan.world

import com.rs.game.model.entity.player.Bank
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.ButtonClickEvent
import kotlinx.coroutines.runBlocking
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.ServerProt
import org.darkan.core.net.prot.packet.IfDragOntoIf
import org.darkan.core.net.prot.packet.UpdateInvFull
import org.darkan.core.net.prot.packet.UpdateInvPartial
import org.darkan.world.harness.HarnessPlayer
import org.darkan.world.harness.WorldHarness
import org.darkan.world.server.packet.IfDragOntoIfHandler
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Bank tabs, end to end on the real bank through the real handlers.
 *
 * The bank is nine numbered tabs plus tab 0, the main tab, stored as a ragged `Item[][]`. There is
 * no slot-to-tab mapping anywhere in the protocol: the client is told **only** each tab's size
 * (varbits 4885..4892), the viewed tab (varbit 4893) and one flat container (95) holding tabs
 * 1..n followed by tab 0, and it slices that container up by the sizes. So a tab operation is
 * correct only if all three agree afterwards - a tab size the server forgot to re-send is a tab
 * the player sees with the wrong items in it, and every slot below the mistake is drawn in the
 * wrong tab too.
 *
 * That makes the destructive cases the interesting ones. `destroyTab` renumbers every tab above
 * the one it removes, so any operation that can empty a tab has to re-send the sizes rather than
 * just the tabs it thinks it touched, and the main tab must survive being emptied or the player's
 * whole arrangement shifts by one.
 */
class BankTabTest {

    private companion object {
        /** Coins: stackable. */
        const val COINS = 995

        /** Iron ore: not stackable, has a note form. */
        const val ORE = 440

        /** Bronze arrows: stackable. */
        const val ARROWS = 882

        /** Wooden shield. */
        const val SHIELD = 1171

        /** The bank's item component, and the flat container behind it. */
        const val BANK_COMPONENT = 95
        const val BANK_CONTAINER = 95

        /** The inventory-inside-the-bank interface, whose op 6 is Deposit-All. */
        const val BANK_INVENTORY_INTERFACE = 763

        /** Tab sizes live in varbits 4885..4892, one per numbered tab. */
        const val TAB_SIZE_VARBIT = 4885

        /** The viewed tab, as `currentTab + 1`. */
        const val VIEWING_TAB_VARBIT = 4893

        /**
         * The tab header component for [tab], inverting the bank button mapping in `mapBankPlugins`
         * `tabId = 9 - ((componentId - 46) / 2)` - so tab 9 is component 46 and tab 0 is 64.
         */
        fun tabHeader(tab: Int) = 46 + 2 * (9 - tab)
    }

    private val loggedIn = mutableListOf<HarnessPlayer>()

    @AfterTest
    fun tearDown() {
        loggedIn.forEach { WorldHarness.logout(it) }
        loggedIn.clear()
    }

    private fun login(name: String, rights: Rights = Rights.OWNER): HarnessPlayer =
        WorldHarness.login(name, rights).also {
            loggedIn.add(it)
            WorldHarness.tick()
            it.clearSent()
        }

    /**
     * Logs in, empties the bank and the inventory, lays the tabs out exactly as [tabs] says and
     * opens the bank - which primes the client's container copy, so every later refresh sends a
     * real diff rather than silently comparing the new state against itself.
     *
     * The tab arrays are written directly rather than through `addItem`, because `addItem` merges
     * into the first stack of that id in *any* tab: a fixture that deliberately puts the same id in
     * two tabs (which the drag paths can produce) cannot be built with it.
     */
    private fun openBankWith(name: String, vararg tabs: List<Item>): HarnessPlayer {
        val harness = login(name)
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.inventory.items.clear()
        player.inventory.refresh()
        repeat(tabs.size - 1) { bank.createTab() }
        tabs.forEachIndexed { tab, items -> bank.bankTabs[tab] = items.toTypedArray() }
        player.bank.open()
        assertTrue(player.interfaceManager.topOpen(762), "the bank interface is not open")
        WorldHarness.tick()
        harness.clearSent()
        return harness
    }

    /** Ticks, then hands back everything the client would actually have received. */
    private fun HarnessPlayer.settle(): List<ServerProt> {
        WorldHarness.tick()
        return session.flushed
    }

    /** A click on interface [inter], as `IfButtonHandler` would dispatch it. */
    private fun Player.click(inter: Int, component: Int, slot: Int, op: Int) {
        PluginManager.handle(ButtonClickEvent(this, inter, component, slot, -1, op))
    }

    /**
     * A drag of bank slot [fromSlot] onto [tab]'s header, through the real packet handler.
     *
     * The header carries no slot, so the client sends -1 - the case `Bank.switchItem` reads as
     * "this is a tab, not a slot". (The rev727 decoder read that field unsigned and delivered
     * 65535 instead, which is why this path was dead in the first place.)
     */
    private fun HarnessPlayer.dragOntoTab(fromSlot: Int, tab: Int) = runBlocking {
        IfDragOntoIfHandler().handle(
            player,
            IfDragOntoIf(762, BANK_COMPONENT, fromSlot, -1, 762, tabHeader(tab), -1, -1),
        )
    }

    /** A drag of one bank slot onto another, through the real packet handler. */
    private fun HarnessPlayer.dragOntoSlot(fromSlot: Int, toSlot: Int) = runBlocking {
        IfDragOntoIfHandler().handle(
            player,
            IfDragOntoIf(762, BANK_COMPONENT, fromSlot, -1, 762, BANK_COMPONENT, toSlot, -1),
        )
    }

    /** The tabs as (id, amount) pairs, tab 0 first, for a one-line assert on the whole bank. */
    private val Bank.layout: List<List<Pair<Int, Int>>>
        get() = bankTabs.map { tab -> tab.map { it.id to it.amount } }

    /** Every numbered tab's size as the *client* now believes it, from varbits 4885..4892. */
    private fun Player.clientTabSizes(): List<Int> = (1..8).map { vars.getVarBit(TAB_SIZE_VARBIT + it - 1) }

    /** The client's flat container: tabs 1..n, then tab 0. */
    private fun Bank.flattened(): List<Pair<Int, Int>> = generateContainer().map { it!!.id to it.amount }

    /** The (id, amount) the client was last told to draw in [slot] of the bank container. */
    private fun List<ServerProt>.slotUpdates(slot: Int): List<Pair<Int, Int>> =
        filterIsInstance<UpdateInvPartial>()
            .filter { it.containerId == BANK_CONTAINER }
            .flatMap { p -> p.updates.filter { it.slot == slot }.map { it.itemId to it.amount } }

    /** The last full bank container the client was sent, as (id, amount) pairs. */
    private fun List<ServerProt>.lastFullContainer(): List<Pair<Int, Int>>? =
        filterIsInstance<UpdateInvFull>()
            .lastOrNull { it.containerId == BANK_CONTAINER }
            ?.let { p -> p.itemIds.indices.map { p.itemIds[it] to p.amounts[it] } }

    /** The client's flat slot for the first entry of [id]; tabs 1..n come before tab 0. */
    private fun Bank.fakeSlotOf(id: Int): Int {
        var slot = 0
        for (tab in 1 until bankTabs.size) {
            for (item in bankTabs[tab]) {
                if (item.id == id) return slot
                slot++
            }
        }
        for (item in bankTabs[0]) {
            if (item.id == id) return slot
            slot++
        }
        error("no bank entry for $id")
    }

    // ------------------------------------------------------------------ creating a tab

    /**
     * Dropping an item on the header one past the last tab creates that tab with the item in it,
     * and all three of the client's views of the bank follow: the tab's size varbit, the flat
     * container's order (the new tab now comes before tab 0) and, on the next full send, the
     * container itself.
     */
    @Test
    fun dragOntoTheNewTabHeaderCreatesTheTab() {
        val harness = openBankWith("tabcreate", listOf(Item(COINS, 1000), Item(ORE, 20)))
        val bank = harness.player.bank
        assertEquals(1, bank.bankTabs.size, "the bank did not start as one tab")

        harness.dragOntoTab(bank.fakeSlotOf(ORE), 1)

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ORE to 20)),
            bank.layout,
            "the drag did not move the ore into a new tab 1",
        )
        assertEquals(2, bank.bankSize, "creating a tab changed the number of banked entries")
        assertEquals(
            listOf(1, 0, 0, 0, 0, 0, 0, 0),
            harness.player.clientTabSizes(),
            "the client was not told tab 1 now holds one item",
        )
        // Tab 1 comes before tab 0 in the flat container, so both slots changed meaning.
        assertEquals(listOf(ORE to 20, COINS to 1000), bank.flattened(), "the flattened order is wrong")

        val flushed = harness.settle()
        assertEquals(ORE to 20, flushed.slotUpdates(0).last(), "slot 0 was not redrawn as the ore")
        assertEquals(COINS to 1000, flushed.slotUpdates(1).last(), "slot 1 was not redrawn as the coins")

        // The same order has to survive a full send - that is what the client gets on every reopen.
        harness.clearSent()
        bank.sendItems()
        assertEquals(
            listOf(ORE to 20, COINS to 1000),
            harness.settle().lastFullContainer(),
            "the full container disagrees with the tab order",
        )
    }

    /**
     * The one-tab, one-item bank. The only entry still creates a tab: the main tab is left empty
     * rather than destroyed, so the bank can never end up with zero tabs.
     *
     * Legacy destroyed it - `removeItem`'s `(forceDestroy || bankTabs.length != 1)`
     * (`Bank.java:864`) let the forced removal through, leaving `bankTabs` empty, and `createTab`
     * then put the item straight back in tab 0 so the drag did nothing at all.
     */
    @Test
    fun theOnlyItemInTheBankStillCreatesATab() {
        val harness = openBankWith("tabonlyitem", listOf(Item(ORE, 20)))
        val bank = harness.player.bank

        harness.dragOntoTab(0, 1)

        assertEquals(listOf(emptyList<Pair<Int, Int>>(), listOf(ORE to 20)), bank.layout, "the only item did not move into tab 1")
        assertEquals(2, bank.bankTabs.size, "no tab was created")
        assertEquals(1, bank.bankSize, "the item was lost or duplicated")
        assertEquals(1, harness.player.clientTabSizes()[0], "the client was not told tab 1 exists")
        assertEquals(listOf(ORE to 20), bank.flattened(), "an empty main tab broke the flat container")
    }

    /**
     * A forced, id-based removal of the last entry of a one-tab bank leaves an empty main tab, not
     * an empty bank. `getRealSlot` and `generateContainer` both index `bankTabs[0]` unconditionally,
     * so a bank with no tabs at all is not merely odd, it throws on the next refresh.
     */
    @Test
    fun theLastRemainingTabSurvivesAForcedRemoval() {
        val harness = openBankWith("tablastremain", listOf(Item(ORE, 20)))
        val bank = harness.player.bank

        bank.removeItem(ORE) // the loyalty-shop style take-back: forceDestroy

        assertEquals(1, bank.bankTabs.size, "the bank was left with no tabs")
        assertEquals(0, bank.bankSize, "the take-back did not remove the item")
        assertNull(bank.getRealSlot(0), "getRealSlot did not treat the empty bank as empty")
        assertEquals(emptyList<Pair<Int, Int>>(), bank.flattened(), "generateContainer broke on the empty bank")
    }

    // ------------------------------------------------------------------ moving between tabs

    /** Dropping an item on an existing tab's header appends it to that tab. */
    @Test
    fun dragOntoAnExistingTabHeaderMovesTheItem() {
        val harness = openBankWith(
            "tabmove",
            listOf(Item(COINS, 1000), Item(ARROWS, 50)),
            listOf(Item(ORE, 20)),
        )
        val bank = harness.player.bank

        harness.dragOntoTab(bank.fakeSlotOf(COINS), 1)

        assertEquals(
            listOf(listOf(ARROWS to 50), listOf(ORE to 20, COINS to 1000)),
            bank.layout,
            "the coins did not move into tab 1",
        )
        assertEquals(2, harness.player.clientTabSizes()[0], "the client was not told tab 1 grew")
        assertEquals(
            listOf(ORE to 20, COINS to 1000, ARROWS to 50),
            bank.flattened(),
            "the flattened order does not put tab 1 before tab 0",
        )
    }

    /**
     * Dropping an item on the header of the tab it is already in moves it to the front of that tab
     * - by swapping it with whatever was there (legacy `Bank.java:404-406`, `421-423`), so the tab
     * keeps its size and no other tab is disturbed.
     */
    @Test
    fun dragOntoItsOwnTabHeaderMovesTheItemToTheTabStart() {
        val harness = openBankWith(
            "tabsamehdr",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20), Item(ARROWS, 50), Item(SHIELD, 1)),
        )
        val bank = harness.player.bank

        harness.dragOntoTab(bank.fakeSlotOf(SHIELD), 1)

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(SHIELD to 1, ARROWS to 50, ORE to 20)),
            bank.layout,
            "the shield did not swap to the start of its own tab",
        )
        assertEquals(3, harness.player.clientTabSizes()[0], "a same-tab move changed the tab size")
    }

    /**
     * Dragging the last item out of a tab destroys the tab, which renumbers every tab above it -
     * so **all** the sizes have to be re-sent, not just the two the move touched.
     *
     * Legacy refreshed only the source and destination tabs (`Bank.java:428-434`). With three tabs
     * and the middle one emptied, the client kept tab 2's old size and went on drawing a tab that
     * no longer existed, slicing the container one tab too far - the "dragging between tabs doesn't
     * work very well" report.
     */
    @Test
    fun draggingTheLastItemOutOfATabDestroysItAndRenumbersTheVarbits() {
        val harness = openBankWith(
            "tabdestroy",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20)),
            listOf(Item(ARROWS, 50), Item(SHIELD, 1)),
        )
        val bank = harness.player.bank
        assertEquals(listOf(1, 2, 0, 0, 0, 0, 0, 0), harness.player.clientTabSizes(), "the fixture is not set up")

        // The ore is tab 1's only entry, dropped on tab 2's header.
        harness.dragOntoTab(bank.fakeSlotOf(ORE), 2)

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ARROWS to 50, SHIELD to 1, ORE to 20)),
            bank.layout,
            "the emptied tab was not destroyed, or the ore did not follow the renumbering",
        )
        assertEquals(
            listOf(3, 0, 0, 0, 0, 0, 0, 0),
            harness.player.clientTabSizes(),
            "tab 2's size varbit is stale, so the client still draws a destroyed tab",
        )
        assertEquals(
            listOf(ARROWS to 50, SHIELD to 1, ORE to 20, COINS to 1000),
            bank.flattened(),
            "the flattened order does not match the renumbered tabs",
        )
    }

    /**
     * The main tab is never destroyed by a move out of it: emptying it leaves it empty and the
     * numbered tabs keep their numbers.
     *
     * Legacy destroyed it (`Bank.java:864` again, this time via `bankTabs.length != 1`), which
     * turned tab 1 into tab 0 behind the player's back and then merged the dragged item into it.
     */
    @Test
    fun movingTheLastItemOutOfTheMainTabKeepsTheMainTab() {
        val harness = openBankWith(
            "tabmainkeep",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20)),
        )
        val bank = harness.player.bank

        harness.dragOntoTab(bank.fakeSlotOf(COINS), 1)

        assertEquals(
            listOf(emptyList<Pair<Int, Int>>(), listOf(ORE to 20, COINS to 1000)),
            bank.layout,
            "the main tab was destroyed and tab 1 was renumbered into it",
        )
        assertEquals(2, bank.bankTabs.size, "the bank lost a tab")
        assertEquals(2, harness.player.clientTabSizes()[0], "tab 1 no longer reports two items")
    }

    /**
     * The same rule on the withdraw path: taking the last item out of the main tab while a numbered
     * tab exists must not shift that tab down into tab 0.
     */
    @Test
    fun withdrawingTheLastItemFromTheMainTabKeepsTheMainTab() {
        val harness = openBankWith(
            "tabmainwithdraw",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20)),
        )
        val player = harness.player
        val bank = player.bank

        player.click(762, BANK_COMPONENT, bank.fakeSlotOf(COINS), 6) // Withdraw-All

        assertEquals(1000, player.inventory.getNumberOf(COINS, false), "the coins never reached the inventory")
        assertEquals(listOf(emptyList<Pair<Int, Int>>(), listOf(ORE to 20)), bank.layout, "the main tab was destroyed")
        assertEquals(1, player.clientTabSizes()[0], "tab 1 was renumbered into tab 0")
    }

    // ------------------------------------------------------------------ slot-to-slot drags

    /**
     * Swap mode, across a tab boundary. A swap exchanges the two entries in place, so both tabs
     * keep their sizes and only the container changes (legacy `Bank.java:468-483`).
     */
    @Test
    fun swappingAcrossATabBoundaryExchangesTheEntriesAndKeepsTheTabSizes() {
        val harness = openBankWith(
            "tabswapcross",
            listOf(Item(COINS, 1000), Item(ARROWS, 50)),
            listOf(Item(ORE, 20), Item(SHIELD, 1)),
        )
        val player = harness.player
        val bank = player.bank
        assertFalse(bank.insertItems, "insert mode must default to off")
        assertEquals(listOf(ORE to 20, SHIELD to 1, COINS to 1000, ARROWS to 50), bank.flattened(), "bad fixture")

        harness.dragOntoSlot(0, 2) // the ore in tab 1 onto the coins in tab 0

        assertEquals(
            listOf(listOf(ORE to 20, ARROWS to 50), listOf(COINS to 1000, SHIELD to 1)),
            bank.layout,
            "the cross-tab swap did not exchange the two entries",
        )
        assertEquals(2, player.clientTabSizes()[0], "a swap changed a tab's size")
        assertEquals(4, bank.bankSize, "a swap changed the number of banked entries")
        assertEquals(
            listOf(COINS to 1000, SHIELD to 1, ORE to 20, ARROWS to 50),
            bank.flattened(),
            "the flattened container does not reflect the swap",
        )
    }

    /**
     * Insert mode, across a tab boundary: rejected outright.
     *
     * An insert shuffles one tab's array along by one, which is only defined inside a single tab -
     * legacy bails on `toRealSlot[0] != fromRealSlot[0]` (`Bank.java:450-451`). Doing anything else
     * would have to change two tab sizes, and the client is never told about it.
     */
    @Test
    fun insertingAcrossATabBoundaryIsRejected() {
        val harness = openBankWith(
            "tabinsertcross",
            listOf(Item(COINS, 1000), Item(ARROWS, 50)),
            listOf(Item(ORE, 20), Item(SHIELD, 1)),
        )
        val player = harness.player
        val bank = player.bank
        player.click(762, 15, -1, 1) // the insert-mode toggle
        assertTrue(bank.insertItems, "the button did not turn insert mode on")
        val before = bank.layout

        harness.dragOntoSlot(0, 2) // tab 1 slot 0 onto tab 0 slot 0
        harness.dragOntoSlot(2, 0) // ...and the other way round

        assertEquals(before, bank.layout, "a cross-tab insert moved something")
        assertEquals(2, player.clientTabSizes()[0], "a rejected insert changed a tab size")
    }

    /** Insert mode inside one tab shuffles that tab only, and its size is unchanged. */
    @Test
    fun insertingWithinATabShufflesThatTabOnly() {
        val harness = openBankWith(
            "tabinsertsame",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20), Item(ARROWS, 50), Item(SHIELD, 1)),
        )
        val player = harness.player
        val bank = player.bank
        player.click(762, 15, -1, 1)
        assertTrue(bank.insertItems, "the button did not turn insert mode on")

        harness.dragOntoSlot(0, 2) // the ore to the end of tab 1

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ARROWS to 50, SHIELD to 1, ORE to 20)),
            bank.layout,
            "the insert did not shuffle tab 1",
        )
        assertEquals(3, player.clientTabSizes()[0], "an insert changed the tab size")
        assertEquals(
            listOf(ARROWS to 50, SHIELD to 1, ORE to 20, COINS to 1000),
            bank.flattened(),
            "the flattened container does not reflect the insert",
        )
    }

    // ------------------------------------------------------------------ View Tab and Collapse

    /** View Tab (op 1 on a tab header) records the tab and pushes varbit 4893 as `tab + 1`. */
    @Test
    fun viewTabTracksTheViewedTabInItsVarbit() {
        val harness = openBankWith(
            "tabview",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20)),
            listOf(Item(ARROWS, 50)),
        )
        val player = harness.player
        val bank = player.bank
        assertEquals(0, bank.currentTab, "the bank did not open on the main tab")
        assertEquals(1, player.vars.getVarBit(VIEWING_TAB_VARBIT), "opening the bank did not sync the viewed tab")

        player.click(762, tabHeader(2), -1, 1)

        assertEquals(2, bank.currentTab, "View Tab did not select tab 2")
        assertEquals(3, player.vars.getVarBit(VIEWING_TAB_VARBIT), "the viewed-tab varbit is stale after View Tab")

        player.click(762, tabHeader(0), -1, 1)

        assertEquals(0, bank.currentTab, "View Tab did not select the main tab")
        assertEquals(1, player.vars.getVarBit(VIEWING_TAB_VARBIT), "the viewed-tab varbit is stale after View Tab")

        // A tab that does not exist is not selectable.
        player.click(762, tabHeader(7), -1, 1)
        assertEquals(0, bank.currentTab, "View Tab selected a tab that does not exist")
    }

    /**
     * Collapse (op 2 on a tab header) empties the tab into the main tab and destroys it. Each entry
     * has to arrive exactly once: a stack whose id the main tab already holds merges into it, and
     * nothing is duplicated or dropped.
     *
     * Legacy removed each entry by id first (`removeItem(getItemSlot(id), ...)`, `Bank.java:387-390`)
     * and `getItemSlot` finds the *first* tab holding that id - the main tab's own stack when it
     * shares the id - so the subtraction came out of the wrong stack and the re-add then merged into
     * a copy that had never been removed. Collapsing a tab that overlapped the main tab changed the
     * player's totals.
     */
    @Test
    fun collapseMergesIntoTheMainTabWithoutDuplicating() {
        val harness = openBankWith(
            "tabcollapse",
            listOf(Item(COINS, 100), Item(SHIELD, 1)),
            listOf(Item(COINS, 50), Item(ORE, 20)),
        )
        val player = harness.player
        val bank = player.bank

        player.click(762, tabHeader(1), -1, 2) // Collapse

        assertEquals(1, bank.bankTabs.size, "the collapsed tab was not destroyed")
        assertEquals(
            listOf(listOf(COINS to 150, SHIELD to 1, ORE to 20)),
            bank.layout,
            "collapse did not merge the coins once into the main tab",
        )
        assertEquals(1, bank.bankTabs[0].count { it.id == COINS }, "collapse left two coin stacks")
        assertEquals(150, assertNotNull(bank.getItem(COINS)).amount, "the collapsed coins do not add up")
        assertEquals(3, bank.bankSize, "collapse changed the number of banked entries")
        assertEquals(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            player.clientTabSizes(),
            "the destroyed tab's button was not hidden",
        )
        assertEquals(
            listOf(COINS to 150, SHIELD to 1, ORE to 20),
            harness.settle().lastFullContainer(),
            "the shorter container was not sent in full",
        )
    }

    /** Collapsing a middle tab renumbers the ones above it, and every size varbit follows. */
    @Test
    fun collapseRenumbersTheTabsAboveIt() {
        val harness = openBankWith(
            "tabcollapsemid",
            listOf(Item(COINS, 100)),
            listOf(Item(ORE, 20)),
            listOf(Item(ARROWS, 50), Item(SHIELD, 1)),
        )
        val player = harness.player
        val bank = player.bank
        player.click(762, tabHeader(2), -1, 1) // view tab 2, so the renumbering has to follow it
        assertEquals(2, bank.currentTab, "the fixture is not viewing tab 2")

        player.click(762, tabHeader(1), -1, 2) // collapse tab 1

        assertEquals(
            listOf(listOf(COINS to 100, ORE to 20), listOf(ARROWS to 50, SHIELD to 1)),
            bank.layout,
            "tab 2 was not renumbered down into tab 1",
        )
        assertEquals(
            listOf(2, 0, 0, 0, 0, 0, 0, 0),
            player.clientTabSizes(),
            "the size varbits do not match the renumbered tabs",
        )
        assertEquals(1, bank.currentTab, "the viewed tab did not follow the renumbering")
        assertEquals(2, player.vars.getVarBit(VIEWING_TAB_VARBIT), "the viewed-tab varbit did not follow")
    }

    /** Collapse is a no-op on the main tab, and on a tab that does not exist. */
    @Test
    fun collapseIgnoresTheMainTabAndMissingTabs() {
        val harness = openBankWith(
            "tabcollapsenoop",
            listOf(Item(COINS, 100)),
            listOf(Item(ORE, 20)),
        )
        val player = harness.player
        val bank = player.bank
        val before = bank.layout

        player.click(762, tabHeader(0), -1, 2)
        player.click(762, tabHeader(5), -1, 2)

        assertEquals(before, bank.layout, "collapse touched the main tab or a missing tab")
        assertEquals(2, bank.bankTabs.size, "collapse destroyed a tab it should have ignored")
    }

    // ------------------------------------------------------------------ deposits and withdrawals

    /** A deposit of a new item lands in the tab being viewed, not in the main tab. */
    @Test
    fun depositingWhileViewingATabLandsInThatTab() {
        val harness = openBankWith(
            "tabdeposit",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20)),
        )
        val player = harness.player
        val bank = player.bank
        player.inventory.items.set(0, Item(ARROWS, 50))
        player.inventory.refresh()
        player.click(762, tabHeader(1), -1, 1)
        assertEquals(1, bank.currentTab, "the fixture is not viewing tab 1")

        player.click(BANK_INVENTORY_INTERFACE, 0, 0, 6) // Deposit-All

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ORE to 20, ARROWS to 50)),
            bank.layout,
            "the deposit did not land in the viewed tab",
        )
        assertEquals(2, player.clientTabSizes()[0], "the client was not told the viewed tab grew")
        assertEquals(0, player.inventory.getNumberOf(ARROWS, false), "the arrows stayed in the inventory")
    }

    /**
     * Withdrawing everything from a numbered tab destroys it - and, because that renumbers, every
     * size varbit is re-sent. The withdraw path already refreshed all of them (`removeItem` calls
     * `refreshTabs` when it destroys a tab with `refresh` set, legacy `Bank.java:865-867`); this
     * pins that down next to the drag path, which did not.
     */
    @Test
    fun withdrawingATabsLastItemDestroysTheTab() {
        val harness = openBankWith(
            "tabwithdraw",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20)),
            listOf(Item(ARROWS, 50)),
        )
        val player = harness.player
        val bank = player.bank

        player.click(762, BANK_COMPONENT, bank.fakeSlotOf(ORE), 6) // Withdraw-All from tab 1

        assertEquals(20, player.inventory.getNumberOf(ORE, false), "the ore never reached the inventory")
        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ARROWS to 50)),
            bank.layout,
            "the emptied tab was not destroyed, or tab 2 was not renumbered",
        )
        assertEquals(
            listOf(1, 0, 0, 0, 0, 0, 0, 0),
            player.clientTabSizes(),
            "the size varbits do not match the renumbered tabs",
        )
    }

    /**
     * The placeholder rules are tab-agnostic: a placeholder keeps the slot it is in, in the tab it
     * is in, and a later deposit of that id fills it there - even when the player is viewing a
     * different tab. Without that, depositing would create a second entry for an id the bank
     * already has a slot for.
     */
    @Test
    fun placeholdersKeepTheirTabAcrossAWithdrawAndDeposit() {
        val harness = openBankWith(
            "tabplaceholder",
            listOf(Item(COINS, 1000)),
            listOf(Item(ORE, 20), Item(ARROWS, 50)),
        )
        val player = harness.player
        val bank = player.bank
        player.vars.saveVarBit(Varbit.bank_placeholders, 1)
        player.click(762, tabHeader(1), -1, 1)

        player.click(762, BANK_COMPONENT, bank.fakeSlotOf(ORE), 6) // Withdraw-All

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ORE to 0, ARROWS to 50)),
            bank.layout,
            "the withdrawal did not leave a placeholder in tab 1",
        )
        assertEquals(2, player.clientTabSizes()[0], "the placeholder stopped counting towards its tab")

        // Now view the main tab and put the ore back: it belongs to tab 1's placeholder, not here.
        player.click(762, tabHeader(0), -1, 1)
        assertEquals(0, bank.currentTab, "View Tab did not go back to the main tab")
        player.click(BANK_INVENTORY_INTERFACE, 0, 0, 6) // Deposit-All

        assertEquals(
            listOf(listOf(COINS to 1000), listOf(ORE to 20, ARROWS to 50)),
            bank.layout,
            "the deposit did not refill the placeholder in tab 1",
        )
        assertEquals(3, bank.bankSize, "the deposit created a second entry for the ore")
        assertEquals(2, player.clientTabSizes()[0], "tab 1's size changed while refilling its placeholder")
    }
}
