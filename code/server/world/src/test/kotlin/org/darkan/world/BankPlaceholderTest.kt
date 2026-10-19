package org.darkan.world

import com.rs.game.content.Skillcapes
import com.rs.game.model.entity.player.Bank
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.ButtonClickEvent
import com.rs.plugin.events.InputIntegerEvent
import kotlinx.coroutines.runBlocking
import org.darkan.core.Constants
import org.darkan.core.JsonFileManager
import org.darkan.core.Utils
import org.darkan.core.cache.VarBitDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Inv
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Item
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.ServerProt
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.net.prot.packet.IfDragOntoIf
import org.darkan.core.net.prot.packet.UpdateInvFull
import org.darkan.core.net.prot.packet.UpdateInvPartial
import org.darkan.core.net.prot.packet.VarpLarge
import org.darkan.core.net.prot.packet.VarpSmall
import org.darkan.world.harness.HarnessPlayer
import org.darkan.world.harness.WorldHarness
import org.darkan.world.server.packet.IfDragOntoIfHandler
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BankPlaceholderTest {

    private companion object {
        const val FILLER_BASE = 30000
        const val XP_99 = 13_034_431.0
        val BANK_SLOTS = Component.interface_762.component_762_95
        val TOGGLE_BUTTON = Component.interface_762.placeholder_toggle_button
        val RELEASE_ALL_BUTTON = Component.interface_762.placeholder_release_button
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

    private fun HarnessPlayer.settle(): List<ServerProt> {
        WorldHarness.tick()
        return session.flushed
    }

    private fun List<ServerProt>.touches(container: Int): Boolean = any {
        (it is UpdateInvPartial && it.containerId == container) ||
            (it is UpdateInvFull && it.containerId == container)
    }

    private fun List<ServerProt>.slotUpdates(container: Int, slot: Int): List<Pair<Int, Int>> =
        filterIsInstance<UpdateInvPartial>()
            .filter { it.containerId == container }
            .flatMap { p -> p.updates.filter { it.slot == slot }.map { it.itemId to it.amount } }

    private fun List<ServerProt>.varpValues(id: Int): List<Int> = mapNotNull {
        when {
            it is VarpSmall && it.id == id -> it.value
            it is VarpLarge && it.id == id -> it.value
            else -> null
        }
    }

    private fun Player.clickBank(component: Int, slot: Int, op: Int) {
        PluginManager.handle(ButtonClickEvent(this, Interface.interface_762, Utils.componentIdFromHash(component), slot, -1, op))
    }

    private var Player.placeholders: Boolean
        get() = vars.getVarBit(Varbit.bank_placeholders) == 1
        set(value) = vars.saveVarBit(Varbit.bank_placeholders, if (value) 1 else 0)

    private fun Player.givePlaceholder(id: Int, tab: Int = 0, combatCharges: Int? = null) {
        val was = placeholders
        placeholders = true
        bank.addItem(Item(id, 1).apply { if (combatCharges != null) addMetaData("combatCharges", combatCharges) }, tab, false)
        bank.removeItem(bank.getItemSlot(id), 1, false, false)
        placeholders = was
    }

    private fun Bank.primeClientCopy() {
        sendItems()
        refreshItems()
    }

    @Test
    fun withdrawingTheLastOfAnItemLeavesAPlaceholder() {
        val harness = login("phwithdraw")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.placeholders = true
        bank.addItem(Item(Obj.iron_ore, 20), false)
        bank.addItem(Item(Obj.coins, 1000), false)
        player.inventory.items.clear()
        player.inventory.refresh()
        bank.primeClientCopy()
        WorldHarness.tick()
        harness.clearSent()

        player.clickBank(BANK_SLOTS, 0, 6)

        assertEquals(20, player.inventory.getNumberOf(Obj.iron_ore, false), "the withdrawal never reached the inventory")
        assertEquals(2, bank.bankTabs[0].size, "the tab shrank instead of keeping a placeholder")
        val entry = bank.bankTabs[0][0]
        assertEquals(Obj.iron_ore, entry.id)
        assertEquals(0, entry.amount)
        assertTrue(Bank.isPlaceholder(entry))
        assertEquals(2, bank.bankSize, "the placeholder stopped using a bank slot")

        val flushed = harness.settle()
        assertEquals(Obj.iron_ore to 0, flushed.slotUpdates(Inv.bank, 0).last(), "the client was not told to draw the empty slot")
    }

    @Test
    fun withdrawingTheLastOfAnItemWithTheToggleOffReleasesTheSlot() {
        val harness = login("phtoggleoff")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        assertFalse(player.placeholders, "placeholders must default to off")
        bank.addItem(Item(Obj.iron_ore, 20), false)
        bank.addItem(Item(Obj.coins, 1000), false)
        player.inventory.items.clear()
        player.inventory.refresh()

        player.clickBank(BANK_SLOTS, 0, 6)

        assertEquals(1, bank.bankTabs[0].size, "the released slot is still there")
        assertEquals(Obj.coins, bank.bankTabs[0][0].id)
        assertFalse(bank.hasPlaceholders())
    }

    @Test
    fun theToggleButtonSavesTheVarbitAndKeepsExistingPlaceholders() {
        val harness = login("phkeep")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)
        bank.addItem(Item(Obj.coins, 1000), false)
        player.inventory.items.clear()
        player.inventory.refresh()
        WorldHarness.tick()
        harness.clearSent()

        player.clickBank(TOGGLE_BUTTON, -1, 1)
        assertTrue(player.placeholders, "the button did not turn the setting on")
        assertTrue(player.vars.vars.containsKey(Varp.varp_1248), "the setting is not in the saved vars")
        val bit = 1 shl VarBitDefinitions.getDefs(Varbit.bank_placeholders).startBit
        assertTrue(harness.settle().varpValues(Varp.varp_1248).any { it and bit != 0 }, "the client was not sent the varp with the bit set")

        player.clickBank(TOGGLE_BUTTON, -1, 1)
        assertFalse(player.placeholders, "the button did not turn the setting off")
        assertEquals(1, bank.countPlaceholders(), "turning the setting off deleted an existing placeholder")

        player.clickBank(BANK_SLOTS, bank.getRealSlotIndexOf(Obj.coins), 6)
        assertEquals(1, bank.countPlaceholders(), "a placeholder was created with the setting off")
        assertNull(bank.getItem(Obj.coins), "the coins were not withdrawn")
    }

    @Test
    fun everyWithdrawOpOnAPlaceholderIsANoOp() {
        val harness = login("phnowithdraw")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)
        bank.addItem(Item(Obj.coins, 1000), false)
        player.inventory.items.clear()
        player.inventory.refresh()
        bank.lastX = 7
        bank.primeClientCopy()
        WorldHarness.tick()
        harness.clearSent()

        val placeholder = bank.bankTabs[0][0]
        for (op in listOf(1, 2, 3, 4, 6, 7)) {
            player.clickBank(BANK_SLOTS, 0, op)
            assertSame(placeholder, bank.bankTabs[0][0], "op $op replaced the placeholder entry")
            assertEquals(0, bank.bankTabs[0][0].amount, "op $op gave the placeholder an amount")
            assertEquals(0, player.inventory.getNumberOf(Obj.iron_ore, false), "op $op withdrew from a placeholder")
        }

        player.clickBank(BANK_SLOTS, 0, 5)
        val enterAmount = assertNotNull(player.tempAttribs.getO<InputIntegerEvent>("pluginInteger"), "Withdraw-X did not ask for an amount")
        enterAmount.run(1000)
        assertSame(placeholder, bank.bankTabs[0][0], "Withdraw-X replaced the placeholder entry")
        assertEquals(0, player.inventory.getNumberOf(Obj.iron_ore, false), "Withdraw-X withdrew from a placeholder")

        assertEquals(2, bank.bankTabs[0].size, "the bank changed shape")
        assertEquals(1000, assertNotNull(bank.getItem(Obj.coins)).amount, "the neighbouring stack was touched")
        assertTrue(player.inventory.items.array().all { it == null }, "something reached the inventory")
        assertFalse(harness.settle().touches(Inv.inv), "an inventory update was sent for a refused withdrawal")
    }

    @Test
    fun withdrawItemDelRefusesAPlaceholder() {
        val harness = login("phservant")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)

        assertFalse(bank.containsItem(Obj.iron_ore, 1))
        bank.withdrawItemDel(Obj.iron_ore, 1)

        assertEquals(1, bank.countPlaceholders(), "the placeholder was consumed")
        assertEquals(0, bank.bankTabs[0][0].amount, "the placeholder gained an amount")
    }

    @Test
    fun depositingOntoAPlaceholderRefillsItsSlot() {
        val harness = login("phdeposit")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)
        bank.addItem(Item(Obj.coins, 1000), false)
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(Obj.iron_ore, 25))
        player.inventory.refresh()

        assertTrue(bank.depositItem(0, Int.MAX_VALUE, true), "the deposit was refused")

        assertEquals(2, bank.bankTabs[0].size, "the deposit appended instead of filling the placeholder")
        assertEquals(Obj.iron_ore, bank.bankTabs[0][0].id)
        assertEquals(25, bank.bankTabs[0][0].amount)
        assertNull(player.inventory.getItem(0), "the ore stayed in the inventory")
        assertFalse(bank.hasPlaceholders(), "the placeholder survived its own refill")
    }

    @Test
    fun depositingAMetadataItemReplacesTheMetadataPlaceholder() {
        val harness = login("phmeta")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.abyssal_whip, 0, combatCharges = 30)
        player.inventory.items.clear()
        player.inventory.items.set(0, Item(Obj.abyssal_whip, 1).apply { addMetaData("combatCharges", 12) })
        player.inventory.refresh()

        assertTrue(bank.depositItem(0, Int.MAX_VALUE, true), "the deposit was refused")

        assertEquals(1, bank.bankTabs[0].size, "the charged whip did not take the placeholder's slot")
        assertEquals(1, bank.bankTabs[0][0].amount, "a metadata item must bank as a single")
        assertEquals(12, bank.bankTabs[0][0].getMetaDataI("combatCharges"), "the deposited charges did not replace the placeholder's")
    }

    @Test
    fun aMetadataItemLeavesAPlaceholder() {
        val harness = login("phmetaout")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.placeholders = true
        bank.addItem(Item(Obj.abyssal_whip, 1).apply { addMetaData("combatCharges", 30) }, false)
        bank.addItem(Item(Obj.coins, 1000), false)
        player.inventory.items.clear()
        player.inventory.refresh()

        player.clickBank(BANK_SLOTS, 0, 6)

        assertEquals(2, bank.bankTabs[0].size, "a charged item left no placeholder behind")
        val placeholder = bank.bankTabs[0][0]
        assertTrue(Bank.isPlaceholder(placeholder), "the charged item's slot is not a placeholder")
        assertEquals(30, placeholder.getMetaDataI("combatCharges"), "the placeholder lost the metadata contents")
        assertEquals(30, assertNotNull(player.inventory.getItem(0)).getMetaDataI("combatCharges"), "the whip lost its charges")
    }

    @Test
    fun releaseFreesAPlaceholderAndSparesARealItem() {
        val harness = login("phrelease")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)
        bank.addItem(Item(Obj.coins, 1000), false)

        player.clickBank(BANK_SLOTS, 1, 9)
        assertEquals(2, bank.bankTabs[0].size, "Release removed a real item")
        assertEquals(1000, assertNotNull(bank.getItem(Obj.coins)).amount, "Release changed a real stack")

        player.clickBank(BANK_SLOTS, 0, 9)
        assertEquals(1, bank.bankTabs[0].size, "Release did not free the placeholder's slot")
        assertEquals(Obj.coins, bank.bankTabs[0][0].id, "Release freed the wrong slot")
        assertFalse(bank.hasPlaceholders())
    }

    @Test
    fun releaseAllClearsEveryPlaceholderAndCollapsesEmptiedTabs() {
        val harness = login("phreleaseall")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        bank.createTab()
        bank.createTab()
        bank.addItem(Item(Obj.coins, 1000), 0, false)
        player.givePlaceholder(Obj.iron_ore, 0)
        player.givePlaceholder(Obj.abyssal_whip, 1)
        bank.addItem(Item(Obj.bronze_arrow, 50), 2, false)
        player.givePlaceholder(Obj.wooden_shield, 2)
        assertEquals(3, bank.countPlaceholders(), "the fixture is wrong")

        player.clickBank(RELEASE_ALL_BUTTON, -1, 1)
        assertNotNull(player.conversation, "release-all did not ask for confirmation").process(1)
        assertEquals(3, bank.countPlaceholders(), "answering No released placeholders anyway")

        player.clickBank(RELEASE_ALL_BUTTON, -1, 1)
        assertNotNull(player.conversation, "release-all did not ask for confirmation").process(0)

        assertFalse(bank.hasPlaceholders(), "placeholders survived release-all")
        assertEquals(2, bank.bankTabs.size, "the tab left holding only a placeholder was not destroyed")
        assertEquals(listOf(Obj.coins to 1000), bank.bankTabs[0].map { it.id to it.amount })
        assertEquals(listOf(Obj.bronze_arrow to 50), bank.bankTabs[1].map { it.id to it.amount })
        assertEquals(2, bank.bankSize)
    }

    @Test
    fun aPlaceholderSwapsAndInsertsLikeAnItem() {
        val harness = login("phdrag")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)
        bank.addItem(Item(Obj.coins, 1000), false)
        bank.addItem(Item(Obj.bronze_arrow, 50), false)

        bank.switchItem(0, 2, BANK_SLOTS)
        assertEquals(listOf(Obj.bronze_arrow, Obj.coins, Obj.iron_ore), bank.bankTabs[0].map { it.id }, "the swap moved the wrong entries")
        assertEquals(listOf(50, 1000, 0), bank.bankTabs[0].map { it.amount }, "the swap changed an amount")

        bank.switchInsertItems()
        bank.switchItem(2, 0, BANK_SLOTS)
        assertEquals(listOf(Obj.iron_ore, Obj.bronze_arrow, Obj.coins), bank.bankTabs[0].map { it.id }, "the insert moved the wrong entries")
        assertEquals(listOf(0, 50, 1000), bank.bankTabs[0].map { it.amount }, "the insert changed an amount")
    }

    @Test
    fun theDragHandlerRejectsAnOutOfRangeBankSlot() {
        val harness = login("phdragbad")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        bank.createTab()
        bank.addItem(Item(Obj.coins, 1000), 1, false)
        bank.addItem(Item(Obj.bronze_arrow, 50), 0, false)
        bank.open()
        assertTrue(player.interfaceManager.topOpen(Interface.interface_762), "the bank interface is not open")

        val slots = Utils.componentIdFromHash(BANK_SLOTS)
        val handler = IfDragOntoIfHandler()
        runBlocking {
            handler.handle(player, IfDragOntoIf(Interface.interface_762, slots, -1, -1, Interface.interface_762, slots, 0, -1))
            handler.handle(player, IfDragOntoIf(Interface.interface_762, slots, 0, -1, Interface.interface_762, slots, Bank.MAX_BANK_SIZE, -1))
        }

        assertEquals(listOf(Obj.coins), bank.bankTabs[1].map { it.id }, "an out-of-range drag moved something")
        assertEquals(listOf(Obj.bronze_arrow), bank.bankTabs[0].map { it.id }, "an out-of-range drag moved something")

        runBlocking { handler.handle(player, IfDragOntoIf(Interface.interface_762, slots, 0, -1, Interface.interface_762, slots, 1, -1)) }
        assertEquals(listOf(Obj.bronze_arrow), bank.bankTabs[1].map { it.id }, "a valid drag did not swap the entries")
        assertEquals(listOf(Obj.coins), bank.bankTabs[0].map { it.id }, "a valid drag did not swap the entries")
    }

    @Test
    fun aPlaceholderIsNotTheItem() {
        val harness = login("phlookup")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.givePlaceholder(Obj.iron_ore)
        bank.addItem(Item(Obj.coins, 1000), false)

        assertFalse(bank.containsItem(Obj.iron_ore))
        assertFalse(bank.containsItem(Obj.iron_ore, 1))
        assertNull(bank.getItem(Obj.iron_ore))
        assertNull(bank.getItemSlot(Obj.iron_ore))
        assertNull(bank.getSlot(Obj.iron_ore))
        assertNotNull(bank.getPlaceholderSlot(Obj.iron_ore))
        assertEquals(2, bank.bankSize, "a placeholder must still use a bank slot")

        bank.deleteItem(Obj.iron_ore, 5)
        assertEquals(0, bank.bankTabs[0][0].amount, "deleteItem changed a placeholder")
    }

    @Test
    fun theDungItemSweepReleasesAPlaceholderAndConvertsRealItems() {
        val harness = login("phdung")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        bank.addItem(Item(Obj.rand_stomp_blue_power_crystal, 3), false)
        player.givePlaceholder(Obj.rand_stomp_green_power_crystal)
        bank.addItem(Item(Obj.bronze_arrow, 50), false)
        bank.refreshItems()

        player.removeDungItems()

        assertFalse(bank.hasPlaceholders(), "the dung placeholder was left in the bank")
        assertEquals(2, bank.bankSize, "the placeholder's slot was not freed")
        assertEquals(listOf(Obj.coins to 3, Obj.bronze_arrow to 50), bank.bankTabs[0].map { it.id to it.amount })
    }

    @Test
    fun theSkillcapeTrimSkipsAPlaceholder() {
        val harness = login("phcape")
        val player = harness.player
        val bank = player.bank
        player.skills.setXp(Constants.ATTACK, XP_99)
        player.skills.setXp(Constants.STRENGTH, XP_99)
        assertTrue(player.skills.checkMulti99s(), "the fixture needs two 99s")
        bank.clear()
        bank.addItem(Item(Skillcapes.Attack.untrimmed, 1), false)
        player.givePlaceholder(Skillcapes.Mining.untrimmed)

        Skillcapes.trimCapes(player)

        assertEquals(Skillcapes.Attack.trimmed, bank.bankTabs[0][0].id, "the real cape was not trimmed")
        assertEquals(Skillcapes.Mining.untrimmed, bank.bankTabs[0][1].id, "the placeholder's id was rewritten")
        assertEquals(0, bank.bankTabs[0][1].amount, "the placeholder gained an amount")
    }

    @Test
    fun placeholdersCountTowardsTheBankLimit() {
        val harness = login("phcapacity")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        for (i in 0 until Bank.MAX_BANK_SIZE - 1) bank.addItem(Item(FILLER_BASE + i, 1), false)
        player.givePlaceholder(Obj.iron_ore)
        assertEquals(Bank.MAX_BANK_SIZE, bank.bankSize, "the fixture did not fill the bank")

        player.inventory.items.clear()
        player.inventory.items.set(0, Item(Obj.bronze_arrow, 10))
        player.inventory.refresh()
        harness.clearSent()

        assertFalse(bank.depositItem(0, Int.MAX_VALUE, true), "a full bank accepted a new item")
        assertEquals(Bank.MAX_BANK_SIZE, bank.bankSize, "the bank grew past its limit")
        assertEquals(10, player.inventory.getNumberOf(Obj.bronze_arrow, false), "the refused deposit ate the items")
        assertTrue(harness.settle().any { it is GameMessage && it.message == "Not enough space in your bank." }, "the refusal was silent")

        player.inventory.items.clear()
        player.inventory.items.set(0, Item(Obj.iron_ore, 5))
        player.inventory.refresh()
        assertTrue(bank.depositItem(0, Int.MAX_VALUE, true), "a deposit into an existing placeholder was refused")
        assertEquals(Bank.MAX_BANK_SIZE, bank.bankSize, "filling a placeholder changed the bank size")
        assertEquals(5, assertNotNull(bank.getItem(Obj.iron_ore)).amount, "the deposit did not land in the placeholder")
    }

    @Test
    fun gsonRoundTripKeepsPlaceholders() {
        val harness = login("phsave")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        bank.createTab()
        bank.addItem(Item(Obj.coins, 1000), 0, false)
        player.givePlaceholder(Obj.iron_ore, 0)
        bank.addItem(Item(Obj.abyssal_whip, 1).apply { addMetaData("combatCharges", 30) }, 1, false)

        val loaded = assertNotNull(JsonFileManager.fromJSONString(JsonFileManager.toJson(bank), Bank::class.java), "the bank did not survive the round trip")

        assertEquals(2, loaded.bankTabs.size, "a tab was lost")
        assertEquals(listOf(Obj.coins to 1000, Obj.iron_ore to 0), loaded.bankTabs[0].map { it.id to it.amount })
        assertEquals(30, loaded.bankTabs[1][0].getMetaDataI("combatCharges"), "the metadata was lost")
    }

    @Test
    fun loadingNormalisesANegativeAmountToAPlaceholder() {
        val harness = login("phnegative")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        bank.addItem(Item(Obj.iron_ore, 5), false)
        bank.bankTabs[0][0].amount = -3

        bank.setPlayer(player)

        assertEquals(0, bank.bankTabs[0][0].amount, "a negative amount was left in the bank")
        assertTrue(Bank.isPlaceholder(bank.bankTabs[0][0]))
    }

    @Test
    fun aMetadataItemFillsTheMetadataPlaceholderNotThePlainOne() {
        val harness = login("phmetafill")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.placeholders = true
        bank.addItem(Item(Obj.abyssal_whip, 1), false)
        bank.addItem(Item(Obj.abyssal_whip, 1).apply { addMetaData("combatCharges", 30) }, false)
        bank.removeItem(intArrayOf(0, 0), 1, false, false)
        bank.removeItem(intArrayOf(0, 1), 1, false, false)

        bank.addItem(Item(Obj.abyssal_whip, 1).apply { addMetaData("combatCharges", 12) }, false)

        assertTrue(Bank.isPlaceholder(bank.bankTabs[0][0]), "the metadata item filled the plain placeholder")
        assertEquals(12, bank.bankTabs[0][1].getMetaDataI("combatCharges"), "the metadata placeholder was not filled")
        assertEquals(2, bank.bankSize, "a new slot was used instead of the placeholder")
    }

    @Test
    fun aPlainItemDoesNotFillAMetadataPlaceholder() {
        val harness = login("phmetaplain")
        val player = harness.player
        val bank = player.bank
        bank.clear()
        player.placeholders = true
        bank.addItem(Item(Obj.abyssal_whip, 1).apply { addMetaData("combatCharges", 30) }, false)
        bank.removeItem(bank.getItemSlot(Obj.abyssal_whip), 1, false, false)

        bank.addItem(Item(Obj.abyssal_whip, 1), false)

        assertTrue(Bank.isPlaceholder(bank.bankTabs[0][0]), "a plain item consumed the metadata placeholder")
        assertEquals(2, bank.bankSize, "the plain item did not take its own slot")
    }

    private fun Bank.getRealSlotIndexOf(id: Int): Int {
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
}
