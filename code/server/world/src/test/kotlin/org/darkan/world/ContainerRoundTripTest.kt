package org.darkan.world

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.item.ItemsContainer
import com.rs.plugin.PluginManager
import com.rs.plugin.events.PickupItemEvent
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.model.Item
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.ServerProt
import org.darkan.core.net.prot.packet.UpdateInvFull
import org.darkan.core.net.prot.packet.UpdateInvPartial
import org.darkan.world.harness.HarnessPlayer
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The item container layer, end to end, against the real cache.
 *
 * Every assertion here is a legacy behaviour: `com.rs.lib.game.Item`,
 * `com.rs.game.model.item.ItemsContainer` and the `Inventory`/`Equipment`/`Bank` containers built on
 * them, plus the ground-item round trip. Container ids are the legacy ones - 93 inventory, 94
 * equipment, 95 bank - and the packets are asserted through the session, so a container that mutates
 * without telling the client fails here.
 */
class ContainerRoundTripTest {

    private companion object {
        /** Abyssal whip: weapon slot, not stackable, no metadata. */
        const val WHIP = 4151

        /** Steel two-hander: weapon slot, `isEquipType(5)`, so it takes the shield slot too. */
        const val TWO_HANDER = 1307

        /** Wooden shield. */
        const val SHIELD = 1171

        /** Bronze arrows: ammo slot, stackable. */
        const val ARROWS = 882

        /** Coins: stackable, un-notable. */
        const val COINS = 995

        /** Iron ore: not stackable, has a note form. */
        const val ORE = 440

        const val INV_CONTAINER = 93
        const val EQUIP_CONTAINER = 94
        const val BANK_CONTAINER = 95
    }

    private val loggedIn = mutableListOf<HarnessPlayer>()

    @AfterTest
    fun tearDown() {
        // Every harness player spawns on the same tile, so a drop one test leaves behind is a drop
        // the next test finds. Clear the chunks these players stood in before logging them out.
        loggedIn.map { it.player.tile.getChunkId() }.distinct().forEach { chunkId ->
            ChunkManager.getChunk(chunkId).allGroundItems.toList().forEach { World.removeGroundItem(it) }
        }
        loggedIn.forEach { WorldHarness.logout(it) }
        loggedIn.clear()
    }

    private fun login(name: String, rights: Rights = Rights.OWNER): HarnessPlayer =
        WorldHarness.login(name, rights).also {
            loggedIn.add(it)
            WorldHarness.tick()
            it.clearSent()
        }

    /** Runs a world tick, which is where the deferred inventory refresh and the flush happen. */
    private fun tick() = WorldHarness.tick()

    /** Ticks, then hands back everything the client would actually have received. */
    private fun HarnessPlayer.settle(): List<ServerProt> {
        tick()
        return session.flushed
    }

    private fun List<ServerProt>.touches(container: Int): Boolean = any {
        (it is UpdateInvPartial && it.containerId == container) ||
            (it is UpdateInvFull && it.containerId == container)
    }

    private fun List<ServerProt>.slotsUpdated(container: Int): Set<Int> =
        filterIsInstance<UpdateInvPartial>()
            .filter { it.containerId == container }
            .flatMap { p -> p.updates.map { it.slot } }
            .toSet()

    private fun Player.fillInventory(id: Int = COINS) {
        inventory.items.clear()
        for (slot in 0 until inventory.itemsContainerSize) inventory.items.set(slot, Item(id, 1))
        inventory.refresh()
    }

    // ------------------------------------------------------------------ ItemsContainer unit behaviour

    /**
     * `ItemsContainer.add` must leave the container untouched when it returns false, or the caller's
     * "not enough space" branch silently eats items. Legacy `ItemsContainer.java:91-107`: the
     * multi-slot branch checks `freeSlots() < item.getAmount()` *before* writing anything.
     */
    @Test
    fun addReturningFalseNeverMutatesTheContainer() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        for (slot in 0 until 26) container.set(slot, Item(ORE, 1))
        val before = container.itemsCopy

        assertFalse(container.add(Item(ORE, 3)), "three non-stackables must not fit in two free slots")
        assertTrue(
            before.contentEquals(container.itemsCopy),
            "a failed add mutated the container: ${container.itemsCopy.toList()}"
        )
        assertEquals(2, container.freeSlots, "free slots changed after a failed add")

        // A full container refuses a single item too, and still does not mutate.
        for (slot in 26 until 28) container.set(slot, Item(ORE, 1))
        val full = container.itemsCopy
        assertFalse(container.add(Item(WHIP, 1)), "a full container accepted another item")
        assertTrue(full.contentEquals(container.itemsCopy), "a failed add into a full container mutated it")
    }

    /**
     * `add` merges stackables into the existing stack rather than taking a new slot, and refuses the
     * merge when it would overflow. Legacy `ItemsContainer.java:80-90`.
     */
    @Test
    fun addMergesStackablesAndRefusesOverflow() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        assertTrue(container.add(Item(COINS, 100)))
        assertTrue(container.add(Item(COINS, 50)))
        assertEquals(1, container.usedSlots, "coins took a second slot instead of merging")
        assertEquals(150, container.get(0)!!.amount)

        container.set(1, Item(COINS, Int.MAX_VALUE))
        // Slot 0 is found first, and 150 + MAX_VALUE overflows negative, so the merge is refused.
        container.clear()
        container.set(0, Item(COINS, Int.MAX_VALUE))
        assertFalse(container.add(Item(COINS, 10)), "an overflowing stack merge was allowed")
        assertEquals(Int.MAX_VALUE, container.get(0)!!.amount, "a refused merge changed the stack")
    }

    /**
     * A non-stackable added with amount > 1 becomes that many single items, one per slot. Legacy
     * `ItemsContainer.java:91-99`.
     */
    @Test
    fun addSpreadsNonStackablesOverOneSlotEach() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        assertTrue(container.add(Item(ORE, 5)))
        assertEquals(5, container.usedSlots, "iron ore did not spread over five slots")
        for (slot in 0 until 5) assertEquals(1, container.get(slot)!!.amount)
    }

    /**
     * `remove` takes from a partial stack in place and returns how much it actually removed, and
     * clears the slot when it takes the whole stack. Legacy `ItemsContainer.java:125-143`.
     */
    @Test
    fun removeTakesPartialStacksAndReportsTheAmountRemoved() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        container.set(0, Item(COINS, 100))

        assertEquals(40, container.remove(Item(COINS, 40)), "remove misreported the amount taken")
        assertEquals(60, container.get(0)!!.amount, "the partial stack was not left behind")

        assertEquals(60, container.remove(Item(COINS, 999)), "remove over-reported when asked for more than there was")
        assertNull(container.get(0), "an exhausted stack must leave the slot empty")

        assertEquals(0, container.remove(Item(COINS, 5)), "removing from an empty container reported a removal")
    }

    /** `remove` walks every slot holding the id, not just the first. Legacy `ItemsContainer.java:127-141`. */
    @Test
    fun removeDrainsAcrossSlots() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        container.set(0, Item(ORE, 1))
        container.set(1, Item(ORE, 1))
        container.set(2, Item(ORE, 1))

        assertEquals(2, container.remove(Item(ORE, 2)))
        assertEquals(1, container.getNumberOf(ORE), "removing two of three left the wrong amount")
    }

    /**
     * `set` stamps the slot onto the item so later `getSlot()` reads are right, `set2` deliberately
     * does not, and both ignore out-of-range slots instead of throwing. Legacy
     * `ItemsContainer.java:57-69`.
     */
    @Test
    fun setStampsTheSlotAndIgnoresOutOfRangeSlots() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        val item = Item(WHIP, 1)
        container.set(7, item)
        assertEquals(7, item.slot, "set did not stamp the slot index onto the item")

        val placed = Item(WHIP, 1)
        container.set2(9, placed)
        assertEquals(9, placed.slot, "set2 did not stamp the slot index onto the item")

        container.set(-1, Item(WHIP, 1))
        container.set(28, Item(WHIP, 1))
        assertEquals(2, container.usedSlots, "an out-of-range set wrote into the container")
        assertNull(container.get(-1))
        assertNull(container.get(28))
    }

    /** `Inventory.switchItem` swaps two slots and reports both as changed. */
    @Test
    fun switchItemSwapsTwoSlotsAndRefreshesBoth() {
        val harness = login("switcher")
        val player = harness.player
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(WHIP, 1))
        player.inventory.items.set(4, Item(COINS, 25))
        tick()
        harness.clearSent()

        player.inventory.switchItem(0, 4)

        assertEquals(COINS, player.inventory.getItem(0)?.id, "slot 0 did not receive the coins")
        assertEquals(25, player.inventory.getItem(0)?.amount)
        assertEquals(WHIP, player.inventory.getItem(4)?.id, "slot 4 did not receive the whip")
        assertEquals(0, player.inventory.getItem(0)?.slot, "the swapped item kept its old slot index")
        assertEquals(4, player.inventory.getItem(4)?.slot, "the swapped item kept its old slot index")

        val flushed = harness.settle()
        assertEquals(
            setOf(0, 4),
            flushed.slotsUpdated(INV_CONTAINER),
            "switchItem must refresh exactly the two slots it touched"
        )
    }

    /** `freeSlots`/`usedSlots`/`freeSlot` all agree, and `freeSlot` is -1 only when truly full. */
    @Test
    fun freeSlotAccountingIsConsistent() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        assertEquals(28, container.freeSlots)
        assertEquals(0, container.usedSlots)
        assertEquals(0, container.freeSlot)
        assertTrue(container.isEmpty)

        container.set(0, Item(WHIP, 1))
        assertEquals(27, container.freeSlots)
        assertEquals(27, container.freeSlots, "freeSlots() and getFreeSlots() disagree")
        assertEquals(1, container.usedSlots)
        assertEquals(1, container.freeSlot, "freeSlot did not skip the occupied slot")
        assertFalse(container.isEmpty)

        for (slot in 1 until 28) container.set(slot, Item(WHIP, 1))
        assertEquals(-1, container.freeSlot, "a full container reported a free slot")
        assertEquals(0, container.freeSlots)
    }

    /**
     * `containsOne` is id-only and ignores amount, while `contains` sums the amounts across slots.
     * Legacy `ItemsContainer.java:159-176`.
     */
    @Test
    fun containsOneIgnoresAmountAndContainsSumsAcrossSlots() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)
        container.set(0, Item(COINS, 5))

        assertTrue(container.containsOne(Item(COINS, 1_000_000)), "containsOne must ignore the amount")
        assertFalse(container.contains(Item(COINS, 6)), "contains must respect the amount")
        assertTrue(container.contains(Item(COINS, 5)))

        container.set(1, Item(COINS, 1))
        assertTrue(container.contains(Item(COINS, 6)), "contains did not sum across slots")
        assertFalse((container as ItemsContainer<Item?>).contains(null), "contains(null) must be false, not a crash")
    }

    /** `sortByItemId` orders by id and pushes the empty slots to the end. Legacy `ItemsContainer.java:385-392`. */
    @Test
    fun sortByItemIdOrdersIdsAndPushesEmptySlotsLast() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(5, false)
        container.set(0, Item(WHIP, 1))
        container.set(2, Item(COINS, 1))
        container.set(4, Item(ORE, 1))

        container.sortByItemId()

        assertEquals(listOf(ORE, COINS, WHIP), (0 until 3).map { container.get(it)!!.id })
        assertNull(container.get(3))
        assertNull(container.get(4))
    }

    // ------------------------------------------------------------------ Item metadata

    /**
     * `getMetaDataI(key)` with no default returns -1 for an absent key, so callers can tell "absent"
     * from a stored zero. Legacy `com.rs.lib.game.Item:153-155`.
     */
    @Test
    fun getMetaDataIDefaultsToMinusOneForAnAbsentKey() {
        val item = Item(WHIP, 1)
        assertEquals(-1, item.getMetaDataI("combatCharges"), "an absent metadata int must read as -1")
        item.addMetaData("combatCharges", 0)
        assertEquals(0, item.getMetaDataI("combatCharges"), "a stored zero must read back as zero")
        assertEquals(7, item.getMetaDataI("nope", 7), "the explicit default was ignored")
    }

    /**
     * `getMetaData()` hands back the live map reference, and `containsMetaData()` is "has a map at
     * all". Legacy `com.rs.lib.game.Item:110-112` and `:175-177`. `ItemsContainer.getNumberOf` and
     * `remove(preferredSlot, item)` compare those references with `==`, so collapsing an empty map to
     * null makes two unrelated items compare equal.
     */
    @Test
    fun metaDataIdentityIsPreservedForEmptyMaps() {
        val item = Item(WHIP, 1)
        assertNull(item.getMetaData(), "a fresh item must have no metadata map")
        assertFalse(item.containsMetaData())

        item.setMetaDataO<Any>("charges", 5)
        val map = assertNotNull(item.getMetaData(), "the metadata map went missing after a write")
        assertTrue(item.containsMetaData())

        item.setMetaDataO<Any>("charges", null)
        assertTrue(item.metadata!!.isEmpty(), "a null write must remove the key")
        assertTrue(item.containsMetaData(), "an emptied map is still a map; legacy checks metaData != null")
        assertSameMap(map, item.getMetaData(), "getMetaData() must return the same map instance")

        val other = Item(WHIP, 1).apply { setMetaDataO<Any>("charges", 5); setMetaDataO<Any>("charges", null) }
        assertFalse(
            item.getMetaData() === other.getMetaData(),
            "two items with separate empty maps must not share one identity"
        )
    }

    /** `setMetaDataO` returns the value it replaced, the way legacy `Item:120-130` does. */
    @Test
    fun setMetaDataOReturnsThePreviousValue() {
        val item = Item(WHIP, 1)
        assertNull(item.setMetaDataO<Any>("charges", 5), "the first write must report no previous value")
        assertEquals(5, item.setMetaDataO<Any>("charges", 9), "the replaced value was not returned")
        assertEquals(9, item.setMetaDataO<Any>("charges", null), "the removed value was not returned")
        assertNull(item.getMetaData("charges"))
    }

    /**
     * Legacy never throws on a bad amount: the two- and three-argument constructors clamp anything
     * `<= 0` up to 1 (`com.rs.lib.game.Item:59-65`), and `Equipment.setSlot` relies on it - it
     * returns `new Item(-1, -1)` as its "a plugin cancelled the equip" sentinel
     * (`Equipment.java:405` and `:414`).
     */
    @Test
    fun itemConstructionClampsNonPositiveAmountsInsteadOfThrowing() {
        assertEquals(1, Item(WHIP, 0).amount, "amount 0 must clamp to 1 the way legacy does")
        assertEquals(1, Item(-1, -1).amount, "a negative amount must clamp to 1, not throw")
        assertEquals(-1, Item(-1, -1).id, "the cancelled-equip sentinel lost its id")
        assertEquals(1, Item(ORE, 0, mutableMapOf<String, Any>("k" to 1)).amount, "the metadata constructor must clamp too")
        assertEquals(0, Item(WHIP, 0, true).amount, "the amt0 constructor must keep the exact amount")
        assertEquals(-5, Item(WHIP, -5, true).amount, "the amt0 constructor must keep the exact amount")
    }

    /** `withAmount` is legacy `setAmount`, which ignores a negative amount. Legacy `Item:75-80`. */
    @Test
    fun withAmountIgnoresNegativeAmounts() {
        val item = Item(COINS, 10)
        assertEquals(4, item.withAmount(4).amount)
        assertEquals(0, item.withAmount(0).amount, "zero is a legal amount for setAmount")
        item.withAmount(3)
        assertEquals(3, item.withAmount(-1).amount, "a negative amount must be ignored")
    }

    /**
     * An item with metadata is unique: it never merges into another stack, `getNumberOf` counts it as
     * one, and it survives every container op with the map intact. Legacy `ItemsContainer.java:81`
     * gates stacking on `item.getMetaData() == null`, and `:209-217` returns 1 for a metadata item.
     */
    @Test
    fun metadataItemsNeverStackAndSurviveContainerOps() {
        WorldHarness.boot()
        val container = ItemsContainer<Item>(28, false)

        val plain = Item(COINS, 100)
        val charged = Item(COINS, 1).apply { addMetaData("combatCharges", 30) }

        assertTrue(container.add(plain))
        assertTrue(container.add(charged))
        assertEquals(2, container.usedSlots, "a metadata item merged into a plain stack")
        assertEquals(1, container.getNumberOf(charged), "getNumberOf must report 1 for a metadata item")
        assertEquals(100, container.getNumberOf(plain), "the plain stack lost its amount")
        assertEquals(30, container.get(1)!!.getMetaDataI("combatCharges"), "the metadata was dropped by add")

        // Legacy quirk worth pinning: a *stackable* item carrying metadata skips the merge branch
        // and falls into the one-slot-per-unit branch, so a stack of more than one is refused
        // outright rather than merged (ItemsContainer.java:81 vs :91-93).
        val bigCharged = Item(COINS, 100).apply { addMetaData("combatCharges", 30) }
        assertFalse(
            container.add(bigCharged),
            "a metadata stack of 100 must be refused: it needs 100 free slots, not a merge"
        )

        // Partial removal from a metadata slot keeps the map on the remainder.
        container.set(2, Item(COINS, 100, bigCharged.metadata))
        assertEquals(40, container.remove(2, Item(COINS, 40, bigCharged.metadata)))
        assertEquals(60, container.get(2)!!.amount)
        assertEquals(30, container.get(2)!!.getMetaDataI("combatCharges"), "a partial remove dropped the metadata")

        // set moves the item without touching the map, and restamps the slot.
        val moved = container.get(2)
        container.set(2, null)
        container.set(20, moved)
        assertEquals(30, container.get(20)!!.getMetaDataI("combatCharges"))
        assertEquals(20, container.get(20)!!.slot)
    }

    /** A cloned item gets its own map, so editing the copy cannot reach back into the original. */
    @Test
    fun cloneCopiesTheMetadataMap() {
        val original = Item(WHIP, 1).apply { addMetaData("combatCharges", 30) }
        val copy = original.clone()
        copy.addMetaData("combatCharges", 5)
        assertEquals(30, original.getMetaDataI("combatCharges"), "clone shared the metadata map with the original")
        assertEquals(5, copy.getMetaDataI("combatCharges"))

        assertNull(Item(WHIP, 1).clone().getMetaData(), "cloning a plain item invented a metadata map")
    }

    // ------------------------------------------------------------------ Equip / unequip

    /** The whip goes to the weapon slot, the inventory slot is freed, and both containers are told. */
    @Test
    fun equippingAWeaponMovesItAndRefreshesBothContainers() {
        val harness = login("equipper")
        val player = harness.player
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(WHIP, 1))
        player.inventory.refresh()
        tick()
        harness.clearSent()
        val appearanceBefore = assertNotNull(player.appearance.appeareanceData, "login sent no appearance block").copyOf()

        assertTrue(Equipment.sendWear(player, 0, WHIP), "sendWear refused a plain weapon")

        assertEquals(WHIP, player.equipment.weaponId, "the whip is not in the weapon slot")
        assertEquals(1, player.equipment.getItem(Equipment.WEAPON)!!.amount)
        assertNull(player.inventory.getItem(0), "the inventory slot was not freed")

        val flushed = harness.settle()
        assertTrue(flushed.touches(INV_CONTAINER), "no inventory (93) update was flushed for the equip")
        assertTrue(flushed.touches(EQUIP_CONTAINER), "no equipment (94) update was flushed for the equip")
        assertFalse(
            appearanceBefore.contentEquals(player.appearance.appeareanceData),
            "the appearance block was not regenerated after equipping"
        )
    }

    /** Unequipping puts the item back in the inventory - it must not vanish. */
    @Test
    fun unequippingReturnsTheItemToTheInventory() {
        val harness = login("unequipper")
        val player = harness.player
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(WHIP, 1))
        player.inventory.refresh()
        assertTrue(Equipment.sendWear(player, 0, WHIP))
        tick()
        harness.clearSent()

        Equipment.remove(player, Equipment.WEAPON)

        assertEquals(-1, player.equipment.weaponId, "the weapon slot was not cleared")
        assertEquals(1, player.inventory.getNumberOf(WHIP, false), "the whip did not come back to the inventory")

        val flushed = harness.settle()
        assertTrue(flushed.touches(INV_CONTAINER), "no inventory (93) update was flushed for the unequip")
        assertTrue(flushed.touches(EQUIP_CONTAINER), "no equipment (94) update was flushed for the unequip")
    }

    /** A two-hander takes the shield slot, so the worn shield has to come back to the inventory. */
    @Test
    fun equippingATwoHanderMovesTheWornShieldToTheInventory() {
        val harness = login("twohander")
        val player = harness.player
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(TWO_HANDER, 1))
        player.inventory.refresh()
        player.equipment.setNoPluginTrigger(Equipment.SHIELD, Item(SHIELD, 1))
        tick()
        harness.clearSent()

        assertTrue(Equipment.sendWear(player, 0, TWO_HANDER), "sendWear refused the two-hander")

        assertEquals(TWO_HANDER, player.equipment.weaponId, "the two-hander is not in the weapon slot")
        assertEquals(-1, player.equipment.shieldId, "the shield slot was not emptied")
        assertEquals(1, player.inventory.getNumberOf(SHIELD, false), "the shield was destroyed instead of returned")
    }

    /**
     * With a full inventory and a shield worn, a two-hander has nowhere to put the shield, so the
     * equip is refused and nothing is lost. Legacy `Equipment.java:766-770`.
     */
    @Test
    fun equippingATwoHanderWithAFullInventoryRefusesWithoutLoss() {
        val harness = login("nospace")
        val player = harness.player
        player.fillInventory(ORE)
        player.inventory.items.set(0, Item(TWO_HANDER, 1))
        player.inventory.refresh()
        player.equipment.setNoPluginTrigger(Equipment.SHIELD, Item(SHIELD, 1))
        tick()

        val invBefore = player.inventory.items.itemsCopy.map { it?.let { i -> i.id to i.amount } }

        assertTrue(Equipment.sendWear(player, 0, TWO_HANDER), "sendWear must consume the click even when it refuses")

        assertEquals(-1, player.equipment.weaponId, "the two-hander was equipped with no room for the shield")
        assertEquals(SHIELD, player.equipment.shieldId, "the shield was taken off with nowhere to go")
        assertEquals(
            invBefore,
            player.inventory.items.itemsCopy.map { it?.let { i -> i.id to i.amount } },
            "the refused equip changed the inventory"
        )
    }

    /**
     * Ammo stacks: equipping more arrows onto arrows already worn adds to the worn stack instead of
     * replacing it. Legacy `Equipment.java:804` skips the swap-out when the ids match and the item is
     * stackable, and `:818` sums the amounts.
     */
    @Test
    fun equippingStackableAmmoMergesWithTheWornStack() {
        val harness = login("arrows")
        val player = harness.player
        assertTrue(
            ItemDefinitions.getDefs(ARROWS).isStackable(),
            "the test needs a stackable ammo item; $ARROWS is not stackable"
        )
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(ARROWS, 100))
        player.inventory.refresh()
        player.equipment.setNoPluginTrigger(Equipment.AMMO, Item(ARROWS, 50))
        tick()
        harness.clearSent()

        assertTrue(Equipment.sendWear(player, 0, ARROWS), "sendWear refused stackable ammo")

        assertEquals(ARROWS, player.equipment.ammoId, "the arrows are not in the ammo slot")
        assertEquals(150, player.equipment.getItem(Equipment.AMMO)!!.amount, "the worn arrow stack did not merge")
        assertNull(player.inventory.getItem(0), "the inventory arrows were not consumed")
        assertEquals(0, player.inventory.getNumberOf(ARROWS, false), "arrows were duplicated back into the inventory")
    }

    // ------------------------------------------------------------------ Bank

    /** A plain deposit moves the whole stack into the bank and empties the inventory slot. */
    @Test
    fun depositAndWithdrawRoundTripsAStackable() {
        val harness = login("banker")
        val player = harness.player
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(COINS, 1000))
        player.inventory.refresh()
        tick()
        harness.clearSent()

        assertTrue(player.bank.depositItem(0, Int.MAX_VALUE, true), "the deposit was refused")
        assertNull(player.inventory.getItem(0), "the coins stayed in the inventory")
        assertEquals(1000, assertNotNull(player.bank.getItem(COINS), "the coins never reached the bank").amount)

        val afterDeposit = harness.settle()
        assertTrue(afterDeposit.touches(BANK_CONTAINER), "no bank (95) update was flushed for the deposit")
        assertTrue(afterDeposit.touches(INV_CONTAINER), "no inventory (93) update was flushed for the deposit")
        harness.clearSent()

        player.bank.withdrawItem(0, 400)
        assertEquals(400, player.inventory.getNumberOf(COINS, false), "the withdrawal did not reach the inventory")
        assertEquals(600, assertNotNull(player.bank.getItem(COINS)).amount, "the bank stack was not reduced by 400")

        val afterWithdraw = harness.settle()
        assertTrue(afterWithdraw.touches(BANK_CONTAINER), "no bank (95) update was flushed for the withdrawal")
        assertTrue(afterWithdraw.touches(INV_CONTAINER), "no inventory (93) update was flushed for the withdrawal")
    }

    /** Depositing a note stores the unnoted id, so notes and singles share one bank stack. */
    @Test
    fun depositingANoteBanksTheUnnotedItem() {
        val harness = login("noter")
        val player = harness.player
        val noteId = ItemDefinitions.getDefs(ORE).getCertId()
        assertTrue(noteId != -1, "the test needs a notable item; $ORE has no note form")
        assertTrue(ItemDefinitions.getDefs(noteId).isNoted(), "$noteId is not the note form of $ORE")

        player.inventory.items.clear()
        player.inventory.items.set(0, Item(noteId, 25))
        player.inventory.refresh()
        tick()

        assertTrue(player.bank.depositItem(0, Int.MAX_VALUE, true), "the note deposit was refused")
        assertNull(player.bank.getItem(noteId), "the bank stored the note instead of the item")
        assertEquals(25, assertNotNull(player.bank.getItem(ORE), "the unnoted ore never reached the bank").amount)
        assertNull(player.inventory.getItem(0), "the notes stayed in the inventory")
    }

    /** With "withdraw as note" on, a notable item comes out as its note form. */
    @Test
    fun withdrawingAsNoteHandsBackTheNoteForm() {
        val harness = login("notewithdraw")
        val player = harness.player
        val noteId = ItemDefinitions.getDefs(ORE).getCertId()

        player.bank.addItem(Item(ORE, 25), false)
        player.inventory.items.clear()
        player.inventory.refresh()
        tick()

        player.bank.switchWithdrawNotes()
        assertTrue(player.bank.withdrawNotes, "withdraw-as-note did not turn on")
        player.bank.withdrawItem(0, 10)

        assertEquals(10, player.inventory.getNumberOf(noteId, false), "the withdrawal was not noted")
        assertEquals(0, player.inventory.getNumberOf(ORE, false), "an unnoted copy came out too")
        assertEquals(15, assertNotNull(player.bank.getItem(ORE)).amount, "the bank stack was not reduced")
    }

    /**
     * A withdrawal is clamped to the free inventory slots for a non-stackable item, and refused
     * outright when there is no room at all. Legacy `Bank.java:743-755`.
     */
    @Test
    fun withdrawIsClampedToFreeInventorySlots() {
        val harness = login("bankfull")
        val player = harness.player
        player.bank.addItem(Item(ORE, 20), false)
        player.inventory.items.clear()
        for (slot in 0 until 25) player.inventory.items.set(slot, Item(COINS, 1))
        player.inventory.refresh()
        tick()

        player.bank.withdrawItem(0, 20)
        assertEquals(3, player.inventory.getNumberOf(ORE, false), "the withdrawal was not clamped to three free slots")
        assertEquals(17, assertNotNull(player.bank.getItem(ORE)).amount, "the bank lost more than it handed over")

        // Now the inventory is full: the withdrawal must be refused with nothing taken.
        player.bank.withdrawItem(0, 5)
        assertEquals(3, player.inventory.getNumberOf(ORE, false), "items appeared with no free slot")
        assertEquals(17, assertNotNull(player.bank.getItem(ORE)).amount, "the bank was debited for a refused withdrawal")
    }

    /** A deposit of a metadata item banks exactly one and keeps the map. */
    @Test
    fun depositingAMetadataItemBanksOneAndKeepsTheMetadata() {
        val harness = login("metabank")
        val player = harness.player
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(WHIP, 1).apply { addMetaData("combatCharges", 30) })
        player.inventory.refresh()
        tick()

        assertTrue(player.bank.depositItem(0, Int.MAX_VALUE, true), "the metadata deposit was refused")
        val banked = assertNotNull(player.bank.getItem(WHIP), "the charged whip never reached the bank")
        assertEquals(1, banked.amount, "a metadata item must bank as a single")
        assertEquals(30, banked.getMetaDataI("combatCharges"), "the bank dropped the metadata")
        assertNull(player.inventory.getItem(0), "the whip stayed in the inventory")
    }

    // ------------------------------------------------------------------ Ground items

    /** Dropping and picking an item back up returns exactly what was dropped. */
    @Test
    fun dropThenPickUpReturnsTheItem() {
        val harness = login("dropper")
        val player = harness.player
        val tile = player.tile
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(COINS, 500))
        player.inventory.refresh()
        tick()
        harness.clearSent()

        player.inventory.deleteItem(0, Item(COINS, 500))
        val dropped = assertNotNull(
            World.addGroundItem(Item(COINS, 500), Tile.of(tile), player, true, 60),
            "the ground item was not created"
        )
        assertEquals(0, player.inventory.getNumberOf(COINS, false), "the dropped coins are still in the inventory")

        val onFloor = assertNotNull(
            ChunkManager.getChunk(tile.getChunkId()).getGroundItem(COINS, tile, player),
            "the ground item is not visible to its owner"
        )
        assertEquals(500, onFloor.amount)

        assertTrue(World.removeGroundItem(player, dropped, true), "the pickup was refused")
        assertEquals(500, player.inventory.getNumberOf(COINS, false), "the coins did not come back")
        assertNull(
            ChunkManager.getChunk(tile.getChunkId()).getGroundItem(COINS, tile, player),
            "the ground item survived the pickup"
        )
    }

    /** A pickup into a full inventory is refused and leaves the item on the floor. */
    @Test
    fun pickUpWithAFullInventoryLeavesTheItemOnTheFloor() {
        val harness = login("fullpickup")
        val player = harness.player
        val tile = player.tile
        val dropped = assertNotNull(
            World.addGroundItem(Item(WHIP, 1), Tile.of(tile), player, true, 60),
            "the ground item was not created"
        )
        player.fillInventory(ORE)
        tick()

        assertFalse(World.removeGroundItem(player, dropped, true), "the pickup was allowed with a full inventory")
        assertNotNull(
            ChunkManager.getChunk(tile.getChunkId()).getGroundItem(WHIP, tile, player),
            "a refused pickup deleted the ground item"
        )
        assertEquals(0, player.inventory.getNumberOf(WHIP, false), "the whip appeared in a full inventory")
    }

    /** A ground item carries its metadata through the drop and back into the inventory. */
    @Test
    fun groundItemsCarryMetadataBothWays() {
        val harness = login("metaground")
        val player = harness.player
        val tile = player.tile
        player.inventory.items.clear()
        player.inventory.refresh()
        tick()

        val dropped = assertNotNull(
            World.addGroundItem(Item(WHIP, 1).apply { addMetaData("combatCharges", 30) }, Tile.of(tile), player, true, 60),
            "the ground item was not created"
        )
        assertEquals(30, dropped.getMetaDataI("combatCharges", -1), "the ground item lost the metadata")

        assertTrue(World.removeGroundItem(player, dropped, true), "the pickup was refused")
        val back = assertNotNull(player.inventory.getItemById(WHIP), "the whip did not come back")
        assertEquals(30, back.getMetaDataI("combatCharges"), "the metadata did not survive the round trip")
    }

    /**
     * When a private drop turns public it keeps its own identity: the same instance is re-keyed, so
     * its tick counter (and with it the absolute `deleteTime` deadline) survives, and the creator is
     * still recorded for the pickup log and the ironman restriction. Legacy `Chunk.java:166-168`
     * mutated the item in place; `GroundItem.removeOwner` (legacy `GroundItem.java:111-113`) clears
     * only the visibility.
     */
    @Test
    fun revealingAPrivateDropKeepsItsTicksAndItsCreator() {
        val harness = login("revealer")
        val other = login("looter")
        val player = harness.player
        val tile = player.tile

        val dropped = assertNotNull(World.addGroundItem(Item(COINS, 9), Tile.of(tile), player, true, 60))
        assertTrue(dropped.isPrivate, "an owned invisible drop must start private")
        assertNull(
            ChunkManager.getChunk(tile.getChunkId()).getGroundItem(COINS, tile, other.player),
            "a private drop must not be visible to anyone else"
        )

        dropped.privateTime = dropped.ticks + 2
        tick()
        tick()
        tick()

        assertFalse(dropped.isPrivate, "the drop never went public")
        assertEquals("revealer", dropped.getCreatorUsername(), "the reveal erased who dropped it")
        assertEquals(player.uuid, dropped.sourceId, "the reveal erased the source id the pickup log needs")
        assertTrue(
            dropped.ticks >= 3,
            "the tick counter restarted at the reveal; deleteTime is absolute, so the drop would outlive its lifetime"
        )
        assertNotNull(
            ChunkManager.getChunk(tile.getChunkId()).getGroundItem(COINS, tile, other.player),
            "the revealed drop is not visible to other players"
        )
    }

    /** The pickup event can cancel a pickup, and then nothing moves. */
    @Test
    fun aCancelledPickupEventLeavesEverythingAlone() {
        val harness = login("cancelpickup")
        val player = harness.player
        val tile = player.tile
        player.inventory.items.clear()
        player.inventory.refresh()
        tick()

        val dropped = assertNotNull(World.addGroundItem(Item(COINS, 7), Tile.of(tile), player, true, 60))
        val event = PickupItemEvent(player, dropped, false)
        PluginManager.handle(event)
        assertFalse(event.isCancelPickup, "an unrelated plugin cancelled a plain coin pickup")
    }

    private fun assertSameMap(expected: Map<String, Any>?, actual: Map<String, Any>?, message: String) {
        assertTrue(expected === actual, message)
    }
}
