package com.rs.game.model.entity.player

import com.rs.Settings
import com.rs.game.content.Potion
import com.rs.game.content.skills.cooking.Foods
import com.rs.game.content.skills.runecrafting.Runecrafting
import com.rs.game.content.skills.runecrafting.fillPouchesFromBank
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.model.entity.player.managers.InterfaceManager.Sub
import com.rs.game.model.item.ItemsContainer
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.ItemAddedToBankEvent
import com.rs.plugin.events.ItemClickEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onItemAddedToBank
import com.rs.utils.ItemConfig
import org.darkan.core.Utils
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Enum
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Inv
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.IFEvents
import org.darkan.core.model.Item
import org.darkan.core.model.UseFlag
import java.util.concurrent.ConcurrentHashMap

private val BLOCKED_BANK_ITEMS: Set<Int> = setOf(
    Obj.macro_genilamp, Obj.thosf_reward_lamp, Obj.twocats_rewardlamp, Obj.elid_genilamp, Obj.hundred_rewardlamp,
    Obj.contact_lantern, Obj.brain_blessed_lamp, Obj.atjun_easy_lamp, Obj.atjun_med_lamp, Obj.atjun_hard_lamp,
    Obj.dream_lantern, Obj.vm_reward_merlin, Obj.vm_reward_arrav, Obj.vm_reward_history, Obj.vm_reward_ghosts,
    Obj.vm_reward_digsite, Obj.obj_11679, Obj.atvar_easy_lamp, Obj.atvar_med_lamp, Obj.atvar_hard_lamp, Obj.sops_lamp,
    Obj.sops_lamp2, Obj.law_reward_lamp, Obj.tutorial2_lamp, Obj.atlum_easy_lamp, Obj.atlum_med_lamp, Obj.atlum_hard_lamp,
    Obj.lmm_xp_lamp, Obj.atfrem_lamp_easy, Obj.atfrem_lamp_medium, Obj.atfrem_lamp_hard, Obj.atfal_easy_lamp,
    Obj.atfal_med_lamp, Obj.atfal_hard_lamp, Obj.atseer_easy_lamp, Obj.atseer_med_lamp, Obj.atseer_hard_lamp,
    Obj.glomem_lamp, Obj.mah1_museum_reward_lamp, Obj.atar_easy_lamp, Obj.atar_med_lamp, Obj.atar_hard_lamp,
    Obj.atar_elite_lamp, Obj.mah4_reward_lamp, Obj.mah4_reward_lamp_combat, Obj.effi_dragonkin_lamp, Obj.effi_museum_lamp,
    Obj.love_lamp, Obj.atfal_elite_lamp1, Obj.atfal_elite_lamp2, Obj.atfal_elite_lamp3, Obj.atjun_elite_lamp1,
    Obj.atjun_elite_lamp2, Obj.atvar_elite_lamp1, Obj.atvar_elite_lamp2, Obj.atlum_elite_lamp, Obj.atseer_elite_lamp1,
    Obj.atseer_elite_lamp2, Obj.atfrem_lamp_elite1, Obj.atfrem_lamp_elite2, Obj.romcom_reward_lamp, Obj.mah5_lamp1,
    Obj.mah5_lamp2, Obj.mah5_lamp3, Obj.death_reward_lamp_main, Obj.death_reward_lamp_tier_1, Obj.death_reward_lamp_tier_2,
    Obj.death_reward_lamp_tier_3, Obj.death_reward_lamp_tier_4, Obj.death_reward_lamp_tier_5, Obj.tzhaar2_reward_lamp,
    Obj.tzhaar2_early_bird_lamp, Obj.atmo_easy_lamp, Obj.atmo_med_lamp, Obj.atmo_hard_lamp, Obj.atmo_elite_lamp1,
    Obj.atmo_elite_lamp2, Obj.early_bird_atmo_lamp, Obj.easter12_lamp, Obj.evalid_lamp, Obj.carni_lamp1, Obj.carni_lamp2,
    Obj.carni_postquestlamp, Obj.challenge_xp_lamp, Obj.obj_25119, Obj.obj_25131, Obj.obj_25132, Obj.obj_25133,
    Obj.loy_wave_reward, Obj.loy_def_reward, Obj.aura_multiskiller_reward
) + (Obj.wof_lamp_small..Obj.wof_lamp_crafting_large) + (Obj.wof_lamp_crafting_huge..Obj.wof_lamp_summoning_huge)

class Bank {
    var bankTabs: Array<Array<Item>> = arrayOf(emptyArray())
        private set
    private var bankPin: Short = 0
    var lastX = 0
    private var enteredPin: MutableMap<String, Long>? = null

    @Transient
    private lateinit var player: Player
    @Transient
    private var viewedTab = 0
    var currentTab: Int
        get() = viewedTab
        set(value) {
            if (value < 0 || value >= bankTabs.size) return
            viewedTab = value
            player.packets.sendVarc(Varc.varc_190, 1)
            refreshViewingTab()
        }
    @Transient
    private var lastContainerCopy: Array<Item?>? = null
    @Transient
    var withdrawNotes = false
        private set
    @Transient
    var insertItems = false
        private set
    @Transient
    private var sessionPin = false

    private val placeholdersEnabled get() = player.vars.getVarBit(Varbit.bank_placeholders) == 1
    val viewingOtherBank get() = player.tempAttribs.getB("viewingOtherBank") && player.interfaceManager.topOpen(Interface.interface_762)
    val bankSize get() = bankTabs.sumOf { it.size }
    val containerCopy: Array<Item?>
        get() = lastContainerCopy ?: generateContainer().also { lastContainerCopy = it }

    fun setPlayer(player: Player) {
        this.player = player
        if (bankTabs.isEmpty()) bankTabs = arrayOf(emptyArray())
        bankTabs.forEach { tab -> tab.forEach { if (it.amount < 0) it.setAmount(0) } }
    }

    private inline fun findSlot(match: (Item) -> Boolean): IntArray? {
        for (tab in bankTabs.indices) {
            val slot = bankTabs[tab].indexOfFirst(match)
            if (slot >= 0) return intArrayOf(tab, slot)
        }
        return null
    }

    fun getItemSlot(id: Int) = findSlot { it.id == id && !isPlaceholder(it) }
    fun getSlot(id: Int) = getItemSlot(id)
    fun getPlaceholderSlot(id: Int) = findSlot { it.id == id && isPlaceholder(it) }
    private fun fillablePlaceholderSlot(item: Item) = findSlot { it.id == item.id && isPlaceholder(it) && it.containsMetaData() == item.containsMetaData() }
    private fun stackSlot(item: Item) = if (item.metadata != null) null else findSlot { it.id == item.id && !isPlaceholder(it) && it.metadata == null }
    fun getItem(slot: IntArray?): Item? = slot?.let { bankTabs[it[0]][it[1]] }
    fun getItem(id: Int): Item? = getItem(getItemSlot(id))

    @JvmOverloads
    fun containsItem(id: Int, amount: Int = 1) = bankTabs.any { tab -> tab.any { it.id == id && !isPlaceholder(it) && it.amount >= amount } }
    fun hasPlaceholders() = bankTabs.any { tab -> tab.any { isPlaceholder(it) } }
    fun countPlaceholders() = bankTabs.sumOf { tab -> tab.count { isPlaceholder(it) } }
    fun hasBankSpace() = bankSize < MAX_BANK_SIZE
    fun getTabSize(slot: Int) = bankTabs.getOrNull(slot)?.size ?: 0

    fun removeItem(id: Int) {
        while (true) removeItem(findSlot { it.id == id } ?: return, Int.MAX_VALUE, true, true)
    }

    fun deleteItem(id: Int, amount: Int) {
        bankTabs.forEach { tab ->
            tab.forEach {
                if (it.id == id && !isPlaceholder(it)) {
                    it.setAmount(it.amount - amount)
                    refreshItems()
                }
            }
        }
    }

    fun convertToCoins(matches: (Int) -> Boolean) {
        for (item in bankTabs.flatMap { it.asList() }) {
            if (!matches(item.id)) continue
            if (isPlaceholder(item)) removeItem(getPlaceholderSlot(item.id), 1, false, true)
            else item.id = Obj.coins
        }
    }

    fun clear() {
        bankTabs = arrayOf(emptyArray())
    }

    fun sortCurrentTab(): Boolean {
        if (currentTab < 0 || currentTab >= bankTabs.size) return false
        bankTabs[currentTab].sortBy { it.id }
        refreshItems()
        refreshTabs()
        refreshViewingTab()
        return true
    }

    fun withdrawLastAmount(bankSlot: Int) = withdrawItem(bankSlot, lastX)

    fun withdrawItemButOne(fakeSlot: Int) {
        val item = getItem(getRealSlot(fakeSlot)) ?: return
        if (isPlaceholder(item)) return
        if (item.amount <= 1) {
            player.sendMessage("You only have one of this item in your bank")
            return
        }
        withdrawItem(fakeSlot, item.amount - 1)
    }

    fun depositLastAmount(bankSlot: Int) = depositItem(bankSlot, lastX, true)

    fun depositAllInventory(banking: Boolean) {
        if (viewingOtherBank) return
        if (MAX_BANK_SIZE - bankSize < player.inventory.items.size) {
            player.sendMessage("Not enough space in your bank.")
            return
        }
        for (i in 0 until 28) depositItem(i, Int.MAX_VALUE, false)
        refreshTabs()
        refreshItems()
    }

    fun depositAllBob(banking: Boolean) {
        if (viewingOtherBank) return
        val inventory = player.familiar?.inventory ?: return
        for (i in 0 until inventory.size) {
            val stored = inventory.get(i) ?: continue
            val item = Item(stored.id, stored.amount, stored.metadata)
            unnote(item)
            val banked = getItem(stackSlot(item))
            if (banked != null) {
                if (banked.amount + item.amount <= 0) {
                    item.setAmount(Int.MAX_VALUE - banked.amount)
                    player.sendMessage("Not enough space in your bank.")
                }
            } else if (fillablePlaceholderSlot(item) == null && !hasBankSpace()) {
                player.sendMessage("Not enough space in your bank.")
                continue
            }
            if (!addItem(item, banking)) continue
            if (item.amount >= stored.amount) inventory.set(i, null)
            else stored.setAmount(stored.amount - item.amount)
        }
    }

    fun depositAllEquipment(banking: Boolean) {
        if (viewingOtherBank) return
        for (i in 0 until Equipment.SIZE) {
            val prev = player.equipment.setSlot(i, null) ?: continue
            if (prev.id == -1) continue
            if (!addItem(prev, banking)) {
                player.sendMessage("Not enough space in your bank.")
                break
            }
        }
        player.appearance.generateAppearanceData()
    }

    fun depositPouchCoins() {
        var amount = player.inventory.pouchCoinsAsInt
        if (amount <= 0) return
        val banked = getItem(Obj.coins)?.amount ?: 0
        if (amount + banked <= 0) amount = Int.MAX_VALUE - banked
        if (amount == 0) {
            player.sendMessage("You can't deposit any coins.")
            return
        }
        player.inventory.removeCoins(amount, true)
        addItem(Item(Obj.coins, amount), true)
    }

    fun collapse(tabId: Int) {
        if (tabId == 0 || tabId >= bankTabs.size) return
        val items = bankTabs[tabId]
        destroyTab(tabId)
        items.forEach { addItem(it, 0, false) }
        refreshTabs()
        refreshItems()
    }

    fun switchItem(fromSlot: Int, toSlot: Int, toComponent: Int) {
        if (toSlot != -1) {
            if (insertItems) insert(fromSlot, toSlot) else switchItem(fromSlot, toSlot)
            return
        }
        var toTab = if (toComponent >= Component.interface_762.component_762_76) 8 - (Component.interface_762.component_762_84 - toComponent)
        else 9 - (toComponent - Component.interface_762.component_762_46) / 2
        if (toTab < 0 || toTab > 9 || toTab > bankTabs.size) return
        val fromRealSlot = getRealSlot(fromSlot) ?: return
        if (toTab == fromRealSlot[0]) {
            switchItem(fromSlot, getStartSlot(toTab))
            return
        }
        val item = getItem(fromRealSlot) ?: return
        if (toTab == bankTabs.size) {
            removeItem(fromSlot, item.amount, false, true)
            createTab()
            bankTabs[bankTabs.size - 1] = arrayOf(item)
            refreshTabs()
            refreshItems()
        } else {
            val removed = removeItem(fromSlot, item.amount, false, true)
            if (removed && fromRealSlot[0] != 0 && toTab >= fromRealSlot[0]) toTab--
            addItem(item, toTab, true)
            refreshTabs()
        }
    }

    fun insert(fromSlot: Int, toSlot: Int) {
        val from = getRealSlot(fromSlot) ?: return
        val to = getRealSlot(toSlot) ?: return
        if (getItem(from) == null || getItem(to) == null || to[0] != from[0]) return
        val tab = bankTabs[to[0]]
        if (to[1] > from[1]) for (slot in from[1] until to[1]) tab[slot] = tab[slot + 1].also { tab[slot + 1] = tab[slot] }
        else if (from[1] > to[1]) for (slot in from[1] downTo to[1] + 1) tab[slot] = tab[slot - 1].also { tab[slot - 1] = tab[slot] }
        refreshItems()
    }

    fun switchItem(fromSlot: Int, toSlot: Int) {
        val from = getRealSlot(fromSlot) ?: return
        val to = getRealSlot(toSlot) ?: return
        val fromItem = getItem(from) ?: return
        val toItem = getItem(to) ?: return
        bankTabs[from[0]][from[1]] = toItem
        bankTabs[to[0]][to[1]] = fromItem
        refreshTab(from[0])
        if (from[0] != to[0]) refreshTab(to[0])
        refreshItems()
    }

    fun openDepositBox() {
        player.tempAttribs.setB("viewingOtherBank", false)
        player.tempAttribs.setB("viewingDepositBox", true)
        player.interfaceManager.sendInterface(Interface.interface_11)
        player.interfaceManager.removeSubs(Sub.TAB_INVENTORY, Sub.TAB_EQUIPMENT)
        player.interfaceManager.openTab(Sub.TAB_FRIENDS)
        sendBoxInterItems()
        player.packets.setIFText(Component.interface_11.component_11_13, "Bank Of ${Settings.getConfig().serverName} - Deposit Box")
        player.setCloseInterfacesEvent {
            player.packets.sendRunScript(Cs2.clientscript_objdialog_close)
            player.abortDialogue()
            player.interfaceManager.sendSubDefaults(Sub.TAB_INVENTORY, Sub.TAB_EQUIPMENT)
            player.interfaceManager.openTab(Sub.TAB_INVENTORY)
            player.tempAttribs.setB("viewingDepositBox", false)
            Familiar.sendLeftClickOption(player)
        }
    }

    fun sendBoxInterItems() {
        val slots = Utils.componentIdFromHash(Component.interface_11.component_11_17)
        player.packets.sendInterSetItemsOptionsScript(Interface.interface_11, slots, Inv.inv, 6, 5, "Deposit-1", "Deposit-5", "Deposit-10", "Deposit-All", "Deposit-X", "Examine")
        player.packets.setIFRightClickOps(Interface.interface_11, slots, 0, 27, 0, 1, 2, 3, 4, 5)
    }

    fun setPin(num1: Byte, num2: Byte, num3: Byte, num4: Byte) {
        bankPin = pin(num1, num2, num3, num4)
    }

    fun pinCorrect(num1: Byte, num2: Byte, num3: Byte, num4: Byte) = bankPin == pin(num1, num2, num3, num4)

    private fun pin(num1: Byte, num2: Byte, num3: Byte, num4: Byte) = ((num1.toInt() shl 12) + (num2.toInt() shl 8) + (num3.toInt() shl 4) + num4).toShort()

    fun openPinSettings() {
        if (!checkPin()) return
        player.endConversation()
        player.tempAttribs.setB("settingPin", false)
        player.tempAttribs.setB("cancellingPin", false)
        player.tempAttribs.setB("changingPin", false)
        player.packets.sendVarc(Varc.varc_98, if (bankPin.toInt() == 0) 0 else 1)
        player.packets.sendVarcString(Varcstr.varcstr_344, "Please set a secure pin.<br>0, 0, 0, 0 will be considered as no pin.")
        player.interfaceManager.sendInterface(Interface.interface_14)
    }

    fun sendPinConfirmScreen(question: String, op1: String, op2: String) {
        player.packets.sendVarc(Varc.varc_98, -1)
        player.packets.setIFText(Component.interface_14.component_14_32, question)
        player.packets.setIFText(Component.interface_14.component_14_34, op1)
        player.packets.setIFText(Component.interface_14.component_14_36, op2)
        player.interfaceManager.sendInterface(Interface.interface_14)
    }

    fun confirmSetPin() {
        player.tempAttribs.setB("settingPin", true)
        sendPinConfirmScreen("Do you really wish to set a PIN on your bank account?", "Yes, I really want a bank PIN. I will never forget it!", "No, I might forget it!")
    }

    fun confirmCancelPin() {
        player.tempAttribs.setB("cancellingPin", true)
        sendPinConfirmScreen("Do you still want your PIN?", "Yes, I asked to cancel my PIN.", "No, I didn't ask to cancel my PIN.")
    }

    fun confirmChangePin() {
        player.tempAttribs.setB("changingPin", true)
        sendPinConfirmScreen("Do you really want to change your PIN?", "Yes, I asked to change my PIN.", "No, I didn't ask to change my PIN.")
    }

    fun openPin() {
        player.tempAttribs.setI("pinStage", 0)
        player.tempAttribs.setI("enteredPin", 0)
        player.tempAttribs.setI("prevPin", -1)
        player.packets.setIFText(Component.interface_13.component_13_27, "Bank of ${Settings.getConfig().serverName}")
        player.interfaceManager.sendInterface(Interface.interface_13)
        player.interfaceManager.sendSubSpecific(false, Interface.interface_13, Utils.componentIdFromHash(Component.interface_13.component_13_5), Interface.interface_759)
        player.packets.sendVarc(Varc.varc_98, if (bankPin.toInt() == 0) 0 else 1)
        player.vars.setVarBit(Varbit.bankpin_counter, 1, true)
        player.vars.syncVarsToClient()
        player.vars.setVarBit(Varbit.bankpin_counter, player.tempAttribs.getI("pinStage"), true)
        player.vars.syncVarsToClient()
        player.packets.sendRunScriptBlank(Cs2.bankpin_init)
    }

    internal fun enterPinDigit(componentId: Int) {
        val attribs = player.tempAttribs
        val num = componentId / 4 - 1
        attribs.setI("enteredPin", attribs.getI("enteredPin") + (num shl (3 - attribs.getI("pinStage")) * 4))
        attribs.incI("pinStage")
        when (attribs.getI("pinStage")) {
            3 -> EnumDefinitions.getEnum(Enum.enum_3554).getValues().values.forEach {
                val hash = it as Int
                player.packets.setIFHidden(Utils.interfaceIdFromHash(hash), Utils.componentIdFromHash(hash), false)
            }
            4 -> {
                player.vars.setVarBit(Varbit.bankpin_counter, 4, true)
                player.vars.syncVarsToClient()
                if (bankPin.toInt() != 0) {
                    if (attribs.getI("enteredPin") != bankPin.toInt()) player.sendMessage("That PIN was incorrect.")
                    else {
                        player.sendMessage("You have entered your PIN successfully.")
                        setEnteredPIN()
                    }
                    player.closeInterfaces()
                } else if (attribs.getI("prevPin", -1) == -1) {
                    player.packets.setIFText(Component.interface_13.component_13_26, "Please repeat that PIN again.")
                    val prev = attribs.getI("enteredPin")
                    openPin()
                    attribs.setI("prevPin", prev)
                    attribs.setI("enteredPin", 0)
                    attribs.setI("pinStage", 0)
                    player.vars.setVarBit(Varbit.bankpin_counter, 0, true)
                    player.vars.syncVarsToClient()
                } else if (attribs.getI("prevPin") != attribs.getI("enteredPin")) {
                    player.sendMessage("The PIN you entered did not match the first one.")
                    player.closeInterfaces()
                    openPinSettings()
                } else {
                    bankPin = attribs.getI("prevPin").toShort()
                    player.sendMessage("Your PIN has been set.")
                    player.closeInterfaces()
                }
            }
        }
    }

    internal fun handlePinSettings(component: Int) {
        when (component) {
            Component.interface_14.component_14_18 -> if (bankPin.toInt() == 0) confirmSetPin() else confirmChangePin()
            Component.interface_14.component_14_33 -> when {
                bankPin.toInt() == 0 -> openPin()
                player.tempAttribs.getB("changingPin") -> {
                    bankPin = 0
                    player.sendMessage("Your pin has been removed. Please set a new one.")
                    openPin()
                }
                else -> {
                    bankPin = 0
                    player.sendMessage("Your pin has been removed.")
                }
            }
            Component.interface_14.component_14_19 -> confirmCancelPin()
            else -> openPinSettings()
        }
    }

    fun open() {
        if (!checkPin()) return
        player.tempAttribs.removeB("viewingOtherBank")
        player.vars.setVar(Varp.swansong_temp, 0)
        player.vars.setVarBit(Varbit.bank_show_equipscreen, 0)
        refreshTabs()
        refreshViewingTab()
        refreshLastX()
        refreshTab(currentTab)
        player.vars.syncVarsToClient()
        player.interfaceManager.sendInterface(Interface.interface_762)
        player.interfaceManager.sendInventoryInterface(Interface.interface_763)
        player.packets.sendRunScript(Cs2.cs2_2319)
        player.packets.setIFText(Component.interface_762.component_762_47, "Bank of ${Settings.getConfig().serverName}")
        refreshOpVars()
        unlockButtons()
        sendItems()
        refreshItems()
        player.setCloseInterfacesEvent {
            player.packets.sendRunScript(Cs2.clientscript_objdialog_close)
            player.abortDialogue()
            Familiar.sendLeftClickOption(player)
        }
    }

    fun openBankOther(other: Player) {
        player.vars.setVarBit(Varbit.bank_show_equipscreen, 0)
        player.vars.syncVarsToClient()
        player.interfaceManager.sendInterface(Interface.interface_762)
        player.packets.sendRunScript(Cs2.cs2_2319)
        refreshTabs(other.bank)
        player.packets.sendItems(Inv.bank, other.bank.containerCopy)
        unlockButtons()
        player.packets.sendItems(Inv.inv, other.inventory.items)
        player.packets.sendItems(Inv.worn, other.equipment.itemsCopy)
        player.tempAttribs.setB("viewingOtherBank", true)
        player.setCloseInterfacesEvent {
            player.tempAttribs.removeB("viewingOtherBank")
            player.packets.sendRunScript(Cs2.clientscript_objdialog_close)
            player.abortDialogue()
            player.inventory.refresh()
            player.equipment.refresh()
            Familiar.sendLeftClickOption(player)
        }
    }

    fun openViewer(title: String, items: ItemsContainer<Item>) {
        player.vars.setVarBit(Varbit.bank_show_equipscreen, 0)
        player.vars.syncVarsToClient()
        player.interfaceManager.sendInterface(Interface.interface_762)
        player.tempAttribs.setB("viewingOtherBank", true)
        player.packets.sendRunScript(Cs2.cs2_2319)
        player.packets.setIFText(Component.interface_762.component_762_47, title)
        player.packets.sendItems(Inv.bank, items)
        player.packets.setIFEvents(IFEvents(Component.interface_762.component_762_95, 0, MAX_BANK_SIZE).enableRightClickOptions(0, 1, 2, 3, 4, 5, 6, 9).setDepth(2).enableDrag())
        player.vars.setVarBit(Varbit.varbit_4893, 1)
        player.vars.syncVarsToClient()
        player.setCloseInterfacesEvent {
            player.tempAttribs.removeB("viewingOtherBank")
            player.packets.sendRunScript(Cs2.clientscript_objdialog_close)
            player.abortDialogue()
        }
    }

    fun refreshLastX() = player.vars.setVar(Varp.varp_1249, lastX)

    fun createTab() {
        bankTabs += emptyArray<Item>()
    }

    fun destroyTab(slot: Int) {
        bankTabs = bankTabs.filterIndexed { index, _ -> index != slot }.toTypedArray()
        if (viewedTab != 0 && viewedTab >= slot) viewedTab--
    }

    fun withdrawItem(bankSlot: Int, quantity: Int) {
        if (viewingOtherBank || quantity < 1) return
        val banked = getItem(getRealSlot(bankSlot)) ?: return
        if (isPlaceholder(banked)) return
        val item = Item(banked.id, minOf(if (banked.metadata != null) 1 else quantity, banked.amount), banked.metadata)
        var noted = false
        val defs = item.definitions
        if (withdrawNotes) {
            if (!defs.noted && defs.noteId != -1 && item.metadata == null) {
                item.id = defs.noteId
                noted = true
            } else player.sendMessage("You cannot withdraw this item as a note.")
        }
        if ((noted || defs.isStackable()) && item.metadata == null) {
            if (player.inventory.items.containsOne(item)) {
                val invItem = player.inventory.items[player.inventory.items.getThisItemSlot(item)]!!
                if (invItem.amount + item.amount <= 0) {
                    item.setAmount(Int.MAX_VALUE - invItem.amount)
                    player.sendMessage("Not enough space in your inventory.")
                    return
                }
            } else if (!player.inventory.hasFreeSlots()) {
                player.sendMessage("Not enough space in your inventory.")
                return
            }
        } else {
            val freeSlots = player.inventory.freeSlots
            if (freeSlots == 0) {
                player.sendMessage("Not enough space in your inventory.")
                return
            }
            if (freeSlots < item.amount) {
                item.setAmount(freeSlots)
                player.sendMessage("Not enough space in your inventory.")
            }
        }
        removeItem(bankSlot, item.amount, true, false)
        player.inventory.addItem(item)
        refreshOpVars()
        if (item.id == Runecrafting.RUNE_ESS || item.id == Runecrafting.PURE_ESS) player.fillPouchesFromBank(item.id)
    }

    fun sendExamine(fakeSlot: Int) {
        val item = getItem(getRealSlot(fakeSlot)) ?: return
        player.sendMessage(ItemConfig.get(item.id).getExamine(item))
        if (item.getMetaData("combatCharges") != null) player.sendMessage("<col=FF0000>It looks like it will last another " + Utils.ticksToTime(item.getMetaDataI("combatCharges").toDouble()))
    }

    fun depositItem(invSlot: Int, quantity: Int, refresh: Boolean): Boolean {
        if (viewingOtherBank || quantity < 1 || invSlot < 0 || invSlot > 27) return false
        val invItem = player.inventory.getItem(invSlot) ?: return false
        val amount = minOf(if (invItem.metadata != null) 1 else quantity, player.inventory.items.getNumberOf(invItem))
        val item = Item(invItem.id, amount, invItem.metadata)
        val originalId = unnote(item)
        val banked = getItem(stackSlot(item))
        if (banked != null) {
            if (banked.amount + item.amount <= 0) {
                item.setAmount(Int.MAX_VALUE - banked.amount)
                player.sendMessage("Not enough space in your bank.")
            }
        } else if (fillablePlaceholderSlot(item) == null && !hasBankSpace()) {
            player.sendMessage("Not enough space in your bank.")
            return false
        }
        if (!addItem(item, refresh)) return false
        player.inventory.deleteItem(invSlot, Item(originalId, item.amount, item.metadata))
        return true
    }

    private fun unnote(item: Item): Int {
        val defs = item.definitions
        val originalId = item.id
        if (defs.noted && defs.noteId != -1) item.id = defs.noteId
        return originalId
    }

    fun addItem(item: Item?, refresh: Boolean) = addItem(item, currentTab, refresh)

    fun addItem(item: Item?, creationTab: Int, refresh: Boolean): Boolean {
        if (item == null || item.id == -1 || !player.controllerManager.canDepositItem(item)) return false
        val event = ItemAddedToBankEvent(player, item)
        PluginManager.handle(event)
        if (event.isCancelled) return false
        val placeholder = fillablePlaceholderSlot(item)
        if (placeholder != null) {
            bankTabs[placeholder[0]][placeholder[1]] = item
            if (refresh) {
                refreshTab(placeholder[0])
                refreshItems()
            }
            return true
        }
        val existing = stackSlot(item)
        if (existing == null) {
            val tab = creationTab.coerceIn(0, bankTabs.size - 1)
            bankTabs[tab] += item
            if (refresh) refreshTab(tab)
        } else {
            val stack = bankTabs[existing[0]][existing[1]]
            bankTabs[existing[0]][existing[1]] = Item(item.id, stack.amount + item.amount, true)
        }
        if (refresh) refreshItems()
        return true
    }

    fun removeItem(fakeSlot: Int, quantity: Int, refresh: Boolean, forceDestroy: Boolean) = removeItem(getRealSlot(fakeSlot), quantity, refresh, forceDestroy)

    fun removeItem(slot: IntArray?, quantity: Int, refresh: Boolean, forceDestroy: Boolean): Boolean {
        if (slot == null) return false
        val tab = bankTabs[slot[0]]
        val item = tab[slot[1]]
        var destroyed = false
        if (quantity < item.amount) {
            tab[slot[1]] = Item(item.id, item.amount - quantity, item.metadata)
        } else if (isPlaceholder(item) && !forceDestroy) {
            return false
        } else if (!forceDestroy && placeholdersEnabled) {
            tab[slot[1]] = Item(item.id, 0, true).apply { metadata = item.metadata?.toMutableMap() }
        } else if (slot[0] != 0 && tab.size == 1 && bankTabs.size != 1) {
            destroyTab(slot[0])
            if (refresh) refreshTabs()
            destroyed = true
        } else {
            bankTabs[slot[0]] = tab.filterIndexed { index, _ -> index != slot[1] }.toTypedArray()
            if (refresh) refreshTab(slot[0])
        }
        if (refresh) refreshItems()
        return destroyed
    }

    fun withdrawItemDel(id: Int, amount: Int) = removeItem(getItemSlot(id), amount, true, false)

    fun releasePlaceholder(fakeSlot: Int) {
        if (viewingOtherBank) return
        val slot = getRealSlot(fakeSlot) ?: return
        if (isPlaceholder(bankTabs[slot[0]][slot[1]])) removeItem(slot, 1, true, true)
    }

    fun promptReleaseAllPlaceholders() {
        if (!hasPlaceholders()) {
            player.sendMessage("You have no placeholders to release.")
            return
        }
        player.sendOptionDialogue("Release all placeholders?") { ops ->
            ops.add("Yes") { releaseAllPlaceholders() }
            ops.add("No")
        }
    }

    fun releaseAllPlaceholders() {
        if (!hasPlaceholders()) return
        for (tab in bankTabs.indices.reversed()) {
            val kept = bankTabs[tab].filterNot { isPlaceholder(it) }
            when {
                kept.size == bankTabs[tab].size -> continue
                kept.isEmpty() && tab != 0 -> destroyTab(tab)
                else -> bankTabs[tab] = kept.toTypedArray()
            }
        }
        refreshTabs()
        refreshItems()
    }

    fun togglePlaceholders() = player.vars.saveVarBit(Varbit.bank_placeholders, if (placeholdersEnabled) 0 else 1)

    fun getStartSlot(tabId: Int): Int {
        var slot = 0
        for (tab in 1 until (if (tabId == 0) bankTabs.size else tabId)) slot += bankTabs[tab].size
        return slot
    }

    fun getRealSlot(slot: Int): IntArray? {
        var remaining = slot
        for (tab in 1 until bankTabs.size) {
            if (remaining < bankTabs[tab].size) return intArrayOf(tab, remaining)
            remaining -= bankTabs[tab].size
        }
        return if (remaining < 0 || remaining >= bankTabs[0].size) null else intArrayOf(0, remaining)
    }

    fun refreshViewingTab() {
        player.vars.setVarBit(Varbit.varbit_4893, currentTab + 1)
        player.vars.syncVarsToClient()
    }

    fun refreshTabs() = refreshTabs(this)

    private fun refreshTabs(bank: Bank) {
        for (slot in 1 until 9) refreshTab(bank, slot)
    }

    fun refreshTab(slot: Int) = refreshTab(this, slot)

    private fun refreshTab(bank: Bank, slot: Int) {
        if (slot == 0) return
        player.vars.setVarBit(Varbit.varbit_4885 + slot - 1, bank.getTabSize(slot))
        refreshViewingTab()
    }

    fun sendItems() {
        player.packets.sendItems(Inv.bank, containerCopy)
        player.packets.sendVarc(Varc.varc_1038, 0)
        player.packets.sendVarc(Varc.varc_192, bankSize)
    }

    fun refreshItems(slots: IntArray) {
        player.packets.sendUpdateItems(Inv.bank, containerCopy, *slots)
        player.packets.sendVarc(Varc.varc_1038, 0)
        player.packets.sendVarc(Varc.varc_192, bankSize)
    }

    fun refreshItems() = refreshItems(generateContainer(), containerCopy)

    fun refreshItems(itemsAfter: Array<Item?>, itemsBefore: Array<Item?>) {
        lastContainerCopy = itemsAfter
        if (itemsBefore.size != itemsAfter.size) {
            sendItems()
            return
        }
        refreshItems(itemsAfter.indices.filter { itemsBefore[it] !== itemsAfter[it] }.toIntArray())
    }

    fun refreshOpVars() {
        var eatFlags = 0
        for (i in 0 until 28) {
            val item = player.inventory.getItem(i) ?: continue
            if (Foods.Food.forId(item.id) != null || Potion.forId(item.id) != null) eatFlags = eatFlags or (1 shl i)
        }
        player.packets.sendVarc(Varc.varc_95, eatFlags)
        player.packets.sendVarc(Varc.varc_96, eatFlags.inv())
    }

    fun generateContainer(): Array<Item?> = (bankTabs.drop(1).flatMap { it.asList() } + bankTabs[0]).toTypedArray<Item?>()

    fun unlockButtons() {
        player.packets.setIFEvents(IFEvents(Component.interface_762.component_762_95, 0, MAX_BANK_SIZE).enableRightClickOptions(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).setDepth(2).enableDrag())
        player.packets.setIFEvents(IFEvents(Component.interface_763.component_763_0, 0, 27).enableUseOptions(UseFlag.ICOMPONENT).enableRightClickOptions(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).setDepth(1).enableDrag())
    }

    fun switchWithdrawNotes() {
        withdrawNotes = !withdrawNotes
    }

    fun switchInsertItems() {
        insertItems = !insertItems
        player.vars.setVar(Varp.varp_305, if (insertItems) 1 else 0)
    }

    fun enteredPIN(): Boolean {
        if (bankPin.toInt() == 0 || sessionPin) return true
        val entered = enteredPin ?: return false
        entered.values.removeIf { System.currentTimeMillis() - it > PIN_VALIDITY_TIME }
        if (!entered.containsKey(player.session.ip)) return false
        sessionPin = true
        return true
    }

    fun setEnteredPIN() {
        (enteredPin ?: ConcurrentHashMap<String, Long>().also { enteredPin = it })[player.session.ip] = System.currentTimeMillis()
        sessionPin = true
    }

    fun checkPin(): Boolean {
        if (bankPin.toInt() == 0 || enteredPIN()) return true
        openPin()
        return false
    }

    internal fun handleItemOp(slot: Int, op: Int) {
        when (op) {
            1 -> withdrawItem(slot, 1)
            2 -> withdrawItem(slot, 5)
            3 -> withdrawItem(slot, 10)
            4 -> withdrawLastAmount(slot)
            5 -> player.sendInputInteger("How many would you like to withdraw?") { amount ->
                if (amount < 0) return@sendInputInteger
                lastX = amount
                refreshLastX()
                withdrawItem(slot, amount)
            }
            6 -> withdrawItem(slot, Int.MAX_VALUE)
            7 -> withdrawItemButOne(slot)
            9 -> releasePlaceholder(slot)
            10 -> sendExamine(slot)
        }
    }

    internal fun handleInvOp(slot: Int, op: Int, banking: Boolean) {
        when (op) {
            1 -> depositItem(slot, 1, banking)
            2 -> depositItem(slot, 5, banking)
            3 -> depositItem(slot, 10, banking)
            4 -> if (banking) depositLastAmount(slot) else depositItem(slot, Int.MAX_VALUE, false)
            5 -> player.sendInputInteger("How many would you like to deposit?") { amount ->
                if (amount < 0) return@sendInputInteger
                lastX = amount
                refreshLastX()
                depositItem(slot, amount, !player.interfaceManager.topOpen(Interface.interface_11))
            }
            6 -> if (banking) depositItem(slot, Int.MAX_VALUE, true) else player.inventory.sendExamine(slot)
            8 -> {
                val item = player.inventory.getItem(slot) ?: return
                val potion = Potion.forId(item.id)
                if (Foods.Food.forId(item.id) != null) Foods.eat(player, item, slot, null)
                else if (potion != null) potion.drink(player, item.id, slot)
                else PluginManager.handle(ItemClickEvent(player, item, slot, item.definitions.getInventoryOption(1)))
            }
            10 -> player.inventory.sendExamine(slot)
        }
    }

    companion object {
        const val MAX_BANK_SIZE = 2000
        private const val PIN_VALIDITY_TIME = 6 * 60 * 60 * 1000L

        @JvmStatic
        fun isPlaceholder(item: Item?) = item != null && item.amount == 0
    }
}

@ServerStartupEvent
fun mapBankPlugins() {
    onItemAddedToBank(*BLOCKED_BANK_ITEMS.toTypedArray()) { e ->
        e.player.sendMessage("A magical force prevents you from banking this item.")
        e.cancel()
    }

    onButtonClick(Interface.interface_11) { e ->
        when (e.component) {
            Component.interface_11.component_11_17 -> e.player.bank.handleInvOp(e.slotId, e.opNum, false)
            Component.interface_11.component_11_18 -> e.player.bank.depositAllInventory(false)
            Component.interface_11.component_11_22 -> e.player.bank.depositAllEquipment(false)
            Component.interface_11.component_11_24 -> e.player.bank.depositAllBob(false)
        }
    }

    onButtonClick(Interface.interface_762) { e ->
        val bank = e.player.bank
        if (bank.viewingOtherBank) return@onButtonClick
        when (e.component) {
            Component.interface_762.component_762_15 -> bank.switchInsertItems()
            Component.interface_762.component_762_19 -> bank.switchWithdrawNotes()
            Component.interface_762.placeholder_toggle_button -> bank.togglePlaceholders()
            Component.interface_762.placeholder_release_button -> bank.promptReleaseAllPlaceholders()
            Component.interface_762.component_762_33 -> bank.depositAllInventory(true)
            Component.interface_762.component_762_35 -> bank.depositPouchCoins()
            Component.interface_762.component_762_37 -> bank.depositAllEquipment(true)
            Component.interface_762.component_762_39 -> bank.depositAllBob(true)
            Component.interface_762.component_762_46 -> {
                e.player.closeInterfaces()
                e.player.interfaceManager.sendInterface(Interface.interface_767)
                e.player.setCloseInterfacesEvent {
                    e.player.abortDialogue()
                    e.player.packets.sendRunScript(Cs2.clientscript_objdialog_close)
                }
            }
            in Component.interface_762.component_762_47..Component.interface_762.component_762_64 -> {
                val tabId = 9 - (e.component - Component.interface_762.component_762_46) / 2
                if (e.opNum == 1) bank.currentTab = tabId
                else if (e.opNum == 2) bank.collapse(tabId)
            }
            Component.interface_762.component_762_95 -> bank.handleItemOp(e.slotId, e.opNum)
            Component.interface_762.component_762_119 -> Equipment.openEquipmentBonuses(e.player, true)
        }
    }

    onButtonClick(Interface.interface_763) { e ->
        if (e.player.bank.viewingOtherBank || e.component != Component.interface_763.component_763_0) return@onButtonClick
        e.player.bank.handleInvOp(e.slotId, e.opNum, true)
    }

    onButtonClick(Interface.interface_13, Interface.interface_14, Interface.interface_759) { e ->
        when (e.interfaceId) {
            Interface.interface_14 -> e.player.bank.handlePinSettings(e.component)
            Interface.interface_759 -> e.player.bank.enterPinDigit(e.componentId)
        }
    }
}
