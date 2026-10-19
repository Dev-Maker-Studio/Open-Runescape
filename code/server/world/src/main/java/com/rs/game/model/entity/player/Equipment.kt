package com.rs.game.model.entity.player

import com.rs.game.content.Effect
import com.rs.game.content.ItemConstants
import com.rs.game.content.combat.special_attacks.handleClick
import com.rs.game.content.interfacehandlers.ItemsKeptOnDeath
import com.rs.game.content.skills.firemaking.getBonfireBoostMultiplier
import com.rs.game.content.transportation.ItemTeleports
import com.rs.game.model.entity.player.managers.PriceChecker
import com.rs.game.model.item.ItemsContainer
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.ButtonClickEvent
import com.rs.plugin.events.IFOnIFEvent
import com.rs.plugin.events.ItemClickEvent
import com.rs.plugin.events.ItemEquipEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onInterfaceOnInterface
import com.rs.utils.ItemConfig
import com.rs.utils.copyItem
import com.rs.utils.equipSlot
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Bas
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Inv
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.model.Bonus
import org.darkan.core.model.IFEvents
import org.darkan.core.model.Item
import org.darkan.core.model.UseFlag

class Equipment {
    private val items = ItemsContainer<Item>(SIZE, false)

    @Transient
    private lateinit var player: Player

    @Transient
    var equipmentHpIncrease = 0

    @Transient
    var equipmentWeight = 0.0
        private set

    fun setPlayer(player: Player) {
        this.player = player
    }

    fun replace(slot: Int, newItem: Item?) {
        items[slot]?.let { PluginManager.handle(ItemEquipEvent(player, it, false)) }
        newItem?.let { PluginManager.handle(ItemEquipEvent(player, it, true)) }
        items[slot] = newItem
        refresh()
    }

    fun init() {
        for (slot in 0 until items.size) {
            val item = items[slot] ?: continue
            if (ItemConstants.canWear(item, player)) continue
            if (player.inventory.hasFreeSlots()) {
                player.inventory.addItem(item)
                player.sendMessage("You no longer meet the requirements for ${item.definitions.name} and it has been unequipped.")
            } else {
                player.bank.addItem(item, false)
                player.sendMessage("You no longer meet the requirements for ${item.definitions.name} and it has been sent to your bank.")
            }
            items[slot] = null
        }
        items.initSlots()
        player.packets.sendItems(Inv.worn, items)
        items.clearDirty()
        refreshStats(true)
    }

    fun refresh(vararg slots: Int) {
        items.markDirty(*slots)
        player.combatDefinitions.checkAttackStyle()
        refreshStats(false)
    }

    fun refreshAll() {
        items.markAllDirty()
        refreshStats(true)
    }

    fun processRefresh() {
        if (player.packets.sendDirtyItems(Inv.worn, items)) player.combatDefinitions.refreshBonuses()
    }

    private fun refreshStats(init: Boolean) {
        player.combatDefinitions.refreshBonuses()
        refreshConfigs(init)
    }

    fun reset() {
        for (item in itemsCopy) if (item != null) PluginManager.handle(ItemEquipEvent(player, item, false))
        items.reset()
        init()
    }

    fun getItem(slot: Int): Item? = items[slot]

    fun getItemById(id: Int): Item? = items.array().firstOrNull { it != null && it.id == id }

    fun sendExamine(slotId: Int) {
        val item = items[slotId] ?: return
        player.sendMessage(ItemConfig.get(item.id).getExamine(item))
        if (item.getMetaData("combatCharges") != null)
            player.sendMessage("<col=FF0000>It looks like it will last another " + Utils.ticksToTime(item.getMetaDataI("combatCharges").toDouble()))
        else if (item.getMetaData("brawlerCharges") != null)
            player.sendMessage("These gloves have " + item.getMetaDataI("brawlerCharges") + " charges left.")
    }

    fun containsOneItem(vararg itemIds: Int): Boolean = itemIds.any { items.containsOne(Item(it, 1)) }

    fun hasItemInSlot(vararg slots: Int): Boolean = slots.any { getId(it) != -1 }

    fun fullGuthansEquipped(): Boolean {
        val ids = intArrayOf(hatId, chestId, legsId, weaponId)
        return ids.none { it == -1 } && ids.all { ItemDefinitions.getDefs(it).name.contains("Guthan's") }
    }

    fun refreshConfigs(init: Boolean) {
        var hpIncrease = items.array().sumOf { item -> item?.let { HP_BOOSTS[it.id] } ?: 0 }.toDouble()
        val maxHp = player.skills.getLevel(Constants.HITPOINTS) * 10
        if (player.hasEffect(Effect.BONFIRE))
            hpIncrease += (maxHp + hpIncrease.toInt()) * getBonfireBoostMultiplier(player)
        if (player.hasEffect(Effect.OOG_THERMAL_POOL))
            hpIncrease += (maxHp + hpIncrease.toInt()) * 0.03
        if (player.hpBoostMultiplier != 0.0)
            hpIncrease += maxHp * player.hpBoostMultiplier
        if (hpIncrease != equipmentHpIncrease.toDouble()) {
            equipmentHpIncrease = hpIncrease.toInt()
            if (!init) player.refreshHitPoints()
        }
        equipmentWeight = items.array().sumOf { if (it == null) 0.0 else ItemConfig.get(it.id).getWeight(true) }
        player.packets.refreshWeight(player.inventory.inventoryWeight + equipmentWeight)
    }

    val weaponBAS: Int
        get() = items[WEAPON]?.definitions?.getRenderAnimId() ?: Bas.default_human

    fun hasShield(): Boolean = items[SHIELD] != null

    val weaponName: String
        get() = items[WEAPON]?.definitions?.name ?: ""

    val weaponId: Int get() = getId(WEAPON)

    val chestId: Int get() = getId(CHEST)

    val hatId: Int get() = getId(HEAD)

    val shieldId: Int get() = getId(SHIELD)

    val legsId: Int get() = getId(LEGS)

    val auraId: Int get() = getId(AURA)

    val capeId: Int get() = getId(CAPE)

    val neckId: Int get() = getId(NECK)

    val ringId: Int get() = getId(RING)

    val ammoId: Int get() = getId(AMMO)

    val bootsId: Int get() = getId(FEET)

    val glovesId: Int get() = getId(HANDS)

    val amuletId: Int get() = getId(NECK)

    fun removeAmmo(slot: Int, amount: Int) {
        val ammo = items[slot] ?: return
        items.remove(slot, Item(ammo.id, amount))
        refresh(slot)
        if (items[slot] == null) {
            player.sendMessage("That was your last one!")
            player.appearance.generateAppearanceData()
        }
    }

    fun deleteItem(itemId: Int, amount: Int) {
        val itemsBefore = itemsCopy
        if (items.contains(Item(itemId, amount)))
            PluginManager.handle(ItemEquipEvent(player, Item(itemId, amount), false))
        items.remove(Item(itemId, amount))
        refreshItems(itemsBefore)
    }

    fun refreshItems(itemsBefore: Array<Item?>) {
        val current = items.array()
        refresh(*itemsBefore.indices.filter { itemsBefore[it] !== current[it] }.toIntArray())
    }

    fun wearingGloves(): Boolean = items[HANDS] != null

    fun getIdInSlot(slot: Int): Int = getId(slot)

    fun wearingSlot(slot: Int, vararg items: Int): Boolean = getIdInSlot(slot) in items

    operator fun get(slot: Int): Item? = items[slot]

    fun setSlot(slot: Int, item: Item?): Item? {
        val prev = items[slot]
        if (prev != null) {
            if (!fireEquipEvent(player, prev, false)) return Item(-1, -1)
            if (item == null) {
                items[slot] = null
                refresh(slot)
                return prev
            }
        }
        item ?: return null
        if (!fireEquipEvent(player, item, true)) return Item(-1, -1)
        items[slot] = item
        refresh(slot)
        return prev
    }

    fun deleteSlot(slot: Int): Boolean {
        val prev = setSlot(slot, null)
        if (prev != null && prev.id == -1) return false
        refresh(slot)
        return true
    }

    fun setNoPluginTrigger(slot: Int, item: Item?) {
        items[slot] = item
    }

    fun isEmpty(): Boolean = items.isEmpty

    val itemsCopy: Array<Item?> get() = items.itemsCopy

    fun wearingArmour(): Boolean = ARMOUR_SLOTS.any { items[it] != null }

    fun getId(slot: Int): Int = items[slot]?.id ?: -1

    fun hasTwoHandedWeapon(): Boolean = items[WEAPON]?.let { isTwoHandedWeapon(it) } ?: false

    fun wearingBlackMask(): Boolean = hatId in BLACK_MASKS

    fun wearingHexcrest(): Boolean = hatId == Obj.slayer_mage_mask

    fun wearingFocusSight(): Boolean = hatId == Obj.slayer_range_mask

    fun wearingSlayerHelmet(): Boolean = hatId in SLAYER_HELMETS

    val witchDoctorBoost: Float
        get() {
            var boost = 1.0f
            if (hatId == Obj.eco_shaman_mask) boost += 0.01f
            if (legsId == Obj.eco_shaman_legs) boost += 0.02f
            if (chestId == Obj.eco_shaman_body) boost += 0.02f
            return boost
        }

    fun isWearingSlayerHelmet(): Boolean = items[HEAD]?.definitions?.name?.lowercase()?.contains("slayer helmet") ?: false

    fun wearingFullCeremonial(): Boolean = CEREMONIAL.all { (slot, id) -> getId(slot) == id }

    fun hasFireCape(): Boolean = capeId in FIRE_CAPES

    val salveAmulet: Int
        get() = when (amuletId) {
            Obj.crystalshard_necklace -> 0
            Obj.lotr_crystalshard_necklace_upgrade -> 1
            else -> -1
        }

    fun wearingRingOfWealth(): Boolean = ringId != -1 && ItemDefinitions.getDefs(ringId).name.lowercase().contains("ring of wealth")

    companion object {
        const val HEAD = 0
        const val CAPE = 1
        const val NECK = 2
        const val WEAPON = 3
        const val CHEST = 4
        const val SHIELD = 5
        const val LEGS = 7
        const val HANDS = 9
        const val FEET = 10
        const val RING = 12
        const val AMMO = 13
        const val AURA = 14
        const val SIZE = 15

        @JvmField
        val DISABLED_SLOTS = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0)

        @JvmStatic
        fun hideArms(item: Item?): Boolean = item != null && item.definitions.isEquipType(6)

        @JvmStatic
        fun hideHair(item: Item): Boolean = item.definitions.isEquipType(8)

        @JvmStatic
        fun hideBeard(item: Item): Boolean = item.definitions.isEquipType(11)

        @JvmStatic
        fun getItemSlot(itemId: Int): Int = ItemDefinitions.getDefs(itemId).equipSlot

        @JvmStatic
        fun isTwoHandedWeapon(item: Item): Boolean = item.definitions.isEquipType(5)

        @JvmStatic
        @JvmOverloads
        fun remove(player: Player, slotId: Int, stopAll: Boolean = true) {
            if (slotId >= SIZE) return
            player.stopAll(false, false)
            val item = player.equipment.getItem(slotId)
            if (item == null || !player.inventory.hasRoomFor(item)) return
            if (!player.equipment.deleteSlot(slotId)) return
            player.inventory.addItem(item)
            player.equipment.refresh(slotId)
            player.appearance.generateAppearanceData()
            if (slotId == WEAPON) player.combatDefinitions.drainSpec(0)
        }

        @JvmStatic
        @JvmOverloads
        fun sendWear(player: Player, slotId: Int, itemId: Int, overrideWear: Boolean = false): Boolean {
            val targetSlot = getItemSlot(itemId)
            if (player.hasFinished() || player.isDead) return false
            player.stopAll(false, false)
            val item = player.inventory.getItem(slotId)
            if (item == null || item.id != itemId) return false
            val defs = item.definitions
            if (!overrideWear) {
                if (!defs.containsOption("Wear") && !defs.containsOption("Wield") && !defs.containsOption("Equip")) return false
                if (defs.isNoted() || !defs.isWearItem(player.appearance.isMale)) {
                    player.sendMessage("You can't wear that.")
                    return true
                }
            }
            if (targetSlot !in 0 until SIZE) {
                player.sendMessage("You can't wear that.")
                return true
            }
            if (!ItemConstants.canWear(item, player)) return true
            val equipment = player.equipment
            val isTwoHandedWeapon = targetSlot == WEAPON && isTwoHandedWeapon(item)
            if (isTwoHandedWeapon && !player.inventory.hasFreeSlots() && equipment.hasShield()) {
                player.sendMessage("Not enough free space in your inventory.")
                return true
            }
            if (!player.controllerManager.canEquip(targetSlot, itemId)) return false
            player.stopAll(false, false)

            if (!fireEquipEvent(player, item, true)) return false
            equipment.get(targetSlot)?.let { if (!fireEquipEvent(player, it, false)) return false }
            val displacedSlot = when {
                isTwoHandedWeapon && equipment.getItem(SHIELD) != null -> SHIELD
                targetSlot == SHIELD && equipment.getItem(WEAPON)?.let { isTwoHandedWeapon(it) } == true -> WEAPON
                else -> -1
            }
            val displaced = equipment.getItem(displacedSlot)
            if (displaced != null && !fireEquipEvent(player, displaced, false)) return false

            player.inventory.deleteItem(slotId, item)

            if (displaced != null) {
                if (!player.inventory.addItem(displaced)) {
                    player.inventory.items[slotId] = item
                    player.inventory.refresh(slotId)
                    player.packets.sendVarc(Varc.player_kit_torso_client, player.appearance.renderEmote)
                    return true
                }
                equipment.setNoPluginTrigger(displacedSlot, null)
            }

            val worn = equipment.getItem(targetSlot)
            if (worn != null && (itemId != worn.id || !defs.isStackable())) {
                if (player.inventory.items[slotId] == null && !defs.isStackable()) {
                    player.inventory.items[slotId] = worn.copyItem()
                    player.inventory.refresh(slotId)
                } else
                    player.inventory.addItem(worn.copyItem())
                equipment.setNoPluginTrigger(targetSlot, null)
            }
            val oldAmount = equipment.getItem(targetSlot)?.amount ?: 0
            equipment.setNoPluginTrigger(targetSlot, Item(itemId, oldAmount + item.amount, item.getMetaData()))
            equipment.refresh(targetSlot, if (targetSlot == WEAPON) SHIELD else WEAPON)

            player.appearance.generateAppearanceData()
            player.packets.sendVarc(Varc.player_kit_torso_client, player.appearance.renderEmote)
            player.soundEffect(ItemConfig.get(item.id).equipSound, false)
            if (targetSlot == WEAPON) player.combatDefinitions.drainSpec(0)
            return true
        }

        @JvmStatic
        fun openEquipmentBonuses(player: Player, banking: Boolean) {
            player.stopAll()
            player.vars.setVarBit(Varbit.varbit_4894, if (banking) 1 else 0)
            player.vars.setVarBit(Varbit.bank_show_equipscreen, 1)
            player.vars.syncVarsToClient()
            player.packets.sendItems(Inv.inv, player.inventory.items)
            player.packets.setIFEvents(
                IFEvents(Component.interface_667.component_667_9, 0, 20).enableUseOption(UseFlag.ICOMPONENT)
                    .enableUseTargetability()
                    .enableRightClickOptions(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            )
            player.packets.setIFEvents(
                IFEvents(Component.inventory_wear2.layer_1, 0, 27).enableUseOption(UseFlag.ICOMPONENT)
                    .enableUseTargetability()
                    .enableRightClickOptions(0, 1, 2, 3)
            )
            refreshEquipBonuses(player)
            player.interfaceManager.sendInventoryInterface(Interface.inventory_wear2)
            player.interfaceManager.sendInterface(Interface.interface_667)
            WorldTasks.delay(0) {
                player.packets.sendVarc(Varc.player_kit_torso_client, player.appearance.renderEmote)
                player.packets.sendRunScript(Cs2.cs2_2319)
            }
            if (banking) {
                player.tempAttribs.setB("Banking", true)
                player.setCloseInterfacesEvent { player.tempAttribs.removeB("Banking") }
            }
        }

        @JvmStatic
        fun refreshEquipBonuses(player: Player) {
            for ((component, label, bonus) in BONUS_TEXTS)
                player.packets.setIFText(component, label + player.combatDefinitions.getBonus(bonus) + percentSuffix(bonus))
        }

        @JvmStatic
        fun getBonus(player: Player, item: Item, bonus: Bonus): Int {
            val value = getBonus(item, bonus)
            return when (item.id) {
                in GOD_ARROWS -> if (bonus == Bonus.RANGE_STR) value + Utils.clampI((player.skills.getLevelForXp(Constants.RANGE) * 0.7).toInt(), 0, 49) else value
                in DOMINION_GLOVES -> when {
                    player.equipment.weaponId != -1 -> value
                    bonus == Bonus.MELEE_STR -> 96
                    bonus == Bonus.CRUSH_ATT -> 95
                    else -> value
                }
                else -> value
            }
        }

        @JvmStatic
        fun getBonus(item: Item, bonus: Bonus): Int {
            val value = item.definitions.getBonuses()[bonus.ordinal]
            if (item.id in DRAGONFIRE_SHIELDS && bonus in DRAGONFIRE_SHIELD_BONUSES) return value + item.getMetaDataI("dfsCharges", 0)
            return value
        }
    }
}

private val HP_BOOSTS: Map<Int, Int> = listOf(
    66 to intArrayOf(
        Obj.godwars2_helm_melee_100, Obj.godwars2_helm_melee_50, Obj.godwars2_helm_range_100,
        Obj.godwars2_helm_range_50, Obj.godwars2_helm_magic_100, Obj.godwars2_helm_magic_50,
    ),
    200 to intArrayOf(
        Obj.godwars2_body_melee_100, Obj.godwars2_body_melee_50, Obj.godwars2_body_range_100,
        Obj.godwars2_body_range_50, Obj.godwars2_body_magic_100, Obj.godwars2_body_magic_50,
    ),
    134 to intArrayOf(
        Obj.godwars2_legs_melee_100, Obj.godwars2_legs_melee_50, Obj.godwars2_legs_range_100,
        Obj.godwars2_legs_range_50, Obj.godwars2_legs_magic_100, Obj.godwars2_legs_magic_50,
    ),
    25 to intArrayOf(
        Obj.godwars2_gloves_range_100, Obj.godwars2_gloves_range_50, Obj.godwars2_gloves_melee_100,
        Obj.godwars2_gloves_melee_50, Obj.godwars2_gloves_magic_100, Obj.godwars2_gloves_magic_50,
        Obj.godwars2_boots_melee_100, Obj.godwars2_boots_melee_50, Obj.godwars2_boots_magic_100,
        Obj.godwars2_boots_magic_50, Obj.godwars2_boots_range_100, Obj.godwars2_boots_range_50,
        Obj.godwars2_gloves_range_100_new, Obj.godwars2_gloves_melee_100_new, Obj.godwars2_gloves_magic_100_new,
        Obj.godwars2_boots_melee_100_new, Obj.godwars2_boots_magic_100_new, Obj.godwars2_boots_range_100_new,
    ),
).flatMap { (hp, ids) -> ids.map { it to hp } }.toMap()

private val ARMOUR_SLOTS = intArrayOf(
    Equipment.HEAD, Equipment.CAPE, Equipment.NECK, Equipment.WEAPON, Equipment.CHEST,
    Equipment.SHIELD, Equipment.LEGS, Equipment.HANDS, Equipment.FEET,
)

private val BLACK_MASKS = intArrayOf(
    Obj.harmless_black_mask, Obj.harmless_black_mask_1, Obj.harmless_black_mask_2, Obj.harmless_black_mask_3,
    Obj.harmless_black_mask_4, Obj.harmless_black_mask_5, Obj.harmless_black_mask_6, Obj.harmless_black_mask_7,
    Obj.harmless_black_mask_8, Obj.harmless_black_mask_9, Obj.harmless_black_mask_10,
)

private val SLAYER_HELMETS = intArrayOf(
    Obj.smki_inv_full_slayer_helmet, Obj.obj_14636, Obj.obj_14637, Obj.slayer_helmet_allcombats,
    Obj.obj_15496, Obj.obj_15497,
    Obj.slayer_helmet_allcombats_red, Obj.bought_slayer_helmet_allcombats_red,
    Obj.obj_22530, Obj.obj_22531, Obj.obj_22532, Obj.obj_22533,
    Obj.slayer_helmet_allcombats_blue, Obj.bought_slayer_helmet_allcombats_blue,
    Obj.obj_22536, Obj.obj_22537, Obj.obj_22538, Obj.obj_22539,
    Obj.slayer_helmet_allcombats_green, Obj.bought_slayer_helmet_allcombats_green,
    Obj.obj_22542, Obj.obj_22543, Obj.obj_22544, Obj.obj_22545,
    Obj.slayer_helmet_allcombats_yellow, Obj.bought_slayer_helmet_allcombats_yellow,
    Obj.obj_22548, Obj.obj_22549, Obj.obj_22550, Obj.obj_22551,
)

private val CEREMONIAL = listOf(
    Equipment.HEAD to Obj.godwars2_zaros_helm,
    Equipment.CHEST to Obj.godwars2_zaros_torso,
    Equipment.LEGS to Obj.godwars2_zaros_legs,
    Equipment.HANDS to Obj.godwars2_zaros_gloves,
    Equipment.FEET to Obj.godwars2_zaros_boots,
)

private val FIRE_CAPES = intArrayOf(Obj.tzhaar_cape_fire, Obj.skillcape_completionist, Obj.obj_20771, Obj.obj_23659)

private val GOD_ARROWS = intArrayOf(Obj.trail_saradomin_arrow, Obj.trail_guthix_arrow, Obj.trail_zamorak_arrow)

private val DOMINION_GLOVES = intArrayOf(Obj.dom_gloves_melee_col1, Obj.dom_gloves_melee_col2, Obj.dom_gloves_melee_col3, Obj.dom_gloves_melee_col4)

private val DRAGONFIRE_SHIELDS = intArrayOf(Obj.dragonfire_shield, Obj.dragonfire_shield_uncharged)

private val DRAGONFIRE_SHIELD_BONUSES = setOf(Bonus.STAB_DEF, Bonus.SLASH_DEF, Bonus.CRUSH_DEF, Bonus.RANGE_DEF)

private val PERCENT_BONUSES = setOf(Bonus.ABSORB_MELEE, Bonus.ABSORB_MAGIC, Bonus.ABSORB_RANGE, Bonus.MAGIC_STR)

private val BONUS_TEXTS = listOf(
    Triple(Component.interface_667.component_667_28, "Stab: ", Bonus.STAB_ATT),
    Triple(Component.interface_667.component_667_29, "Slash: ", Bonus.SLASH_ATT),
    Triple(Component.interface_667.component_667_30, "Crush: ", Bonus.CRUSH_ATT),
    Triple(Component.interface_667.component_667_31, "Magic: ", Bonus.MAGIC_ATT),
    Triple(Component.interface_667.component_667_32, "Range: ", Bonus.RANGE_ATT),
    Triple(Component.interface_667.component_667_33, "Stab: ", Bonus.STAB_DEF),
    Triple(Component.interface_667.component_667_34, "Slash: ", Bonus.SLASH_DEF),
    Triple(Component.interface_667.component_667_35, "Crush: ", Bonus.CRUSH_DEF),
    Triple(Component.interface_667.component_667_36, "Magic: ", Bonus.MAGIC_DEF),
    Triple(Component.interface_667.component_667_37, "Range: ", Bonus.RANGE_DEF),
    Triple(Component.interface_667.component_667_38, "Summoning: ", Bonus.SUMM_DEF),
    Triple(Component.interface_667.component_667_39, "Absorb Melee: ", Bonus.ABSORB_MELEE),
    Triple(Component.interface_667.component_667_40, "Absorb Magic: ", Bonus.ABSORB_MAGIC),
    Triple(Component.interface_667.component_667_41, "Absorb Ranged: ", Bonus.ABSORB_RANGE),
    Triple(Component.interface_667.component_667_42, "Strength: ", Bonus.MELEE_STR),
    Triple(Component.interface_667.component_667_43, "Ranged Str: ", Bonus.RANGE_STR),
    Triple(Component.interface_667.component_667_44, "Prayer: ", Bonus.PRAYER),
    Triple(Component.interface_667.component_667_45, "Magic Damage: ", Bonus.MAGIC_STR),
)

@ServerStartupEvent
fun mapEquipment() {
    onButtonClick(Interface.interface_884) { e ->
        when (e.component) {
            Component.interface_884.component_884_4 -> handleClick(e.player)
            in Component.interface_884.component_884_7..Component.interface_884.component_884_10 ->
                e.player.combatDefinitions.setAttackStyle(e.component - Component.interface_884.component_884_7)
            Component.interface_884.component_884_11 -> e.player.combatDefinitions.switchAutoRetaliate()
        }
    }

    onInterfaceOnInterface(true, intArrayOf(Interface.inventory_wear2), null, intArrayOf(Interface.interface_667), null, ::compareDraggedItems)
    onInterfaceOnInterface(false, intArrayOf(Interface.inventory_wear2), null, intArrayOf(Interface.inventory_wear2), null, ::compareDraggedItems)

    onButtonClick(Interface.interface_667, Interface.inventory_wear2) { e ->
        val player = e.player
        when (e.component) {
            Component.interface_667.component_667_9 -> {
                if (e.slotId >= Equipment.SIZE) return@onButtonClick
                val item = player.equipment.getItem(e.slotId) ?: return@onButtonClick
                when (e.opNum) {
                    10 -> {
                        player.sendMessage(ItemConfig.get(item.id).getExamine(item))
                        if (item.getMetaData("combatCharges") != null)
                            player.sendMessage("<col=FF0000>It looks like it will last another " + Utils.ticksToTime(item.getMetaDataI("combatCharges").toDouble()))
                    }
                    1 -> {
                        Equipment.remove(player, e.slotId)
                        Equipment.refreshEquipBonuses(player)
                    }
                }
            }
            Component.interface_667.component_667_46 -> if (player.tempAttribs.removeB("Banking")) player.bank.open()
            Component.inventory_wear2.layer_1 -> {
                if (e.slotId >= player.inventory.itemsContainerSize) return@onButtonClick
                val item = player.inventory.getItem(e.slotId) ?: return@onButtonClick
                when (e.opNum) {
                    1 -> if (!player.isEquipDisabled && Equipment.sendWear(player, e.slotId, item.id)) Equipment.refreshEquipBonuses(player)
                    4 -> player.inventory.sendExamine(e.slotId)
                }
            }
        }
    }

    onButtonClick(Interface.interface_387) { e ->
        val player = e.player
        if (player.interfaceManager.containsInventoryInter()) return@onButtonClick
        when (e.component) {
            Component.interface_387.component_387_40 -> {
                if (player.interfaceManager.containsScreenInter()) {
                    player.sendMessage("Please finish what you're doing before opening the price checker.")
                    return@onButtonClick
                }
                player.stopAll()
                PriceChecker.openPriceCheck(player)
            }
            Component.interface_387.component_387_38 -> repeat(2) { Equipment.openEquipmentBonuses(player, false) }
            Component.interface_387.component_387_41 -> {
                player.stopAll()
                ItemsKeptOnDeath.openItemsKeptOnDeath(player)
            }
            Component.interface_387.component_387_42 -> player.interfaceManager.sendInterface(Interface.interface_1178)
            Component.interface_387.component_387_43 -> PriceChecker.openPriceCheck(player)
            else -> clickWornItem(e)
        }
    }
}

private fun clickWornItem(e: ButtonClickEvent) {
    val player = e.player
    val slot = Equipment.getItemSlot(e.slotId2)
    val item = player.equipment.getItem(slot) ?: return
    if (PluginManager.handle(ItemClickEvent(player, item, slot, item.definitions.getEquipmentOption(optionForPacket(e.opNum)), true))) return
    if (e.opNum == 10) {
        player.equipment.sendExamine(slot)
        return
    }
    if (e.opNum == 1 && PluginManager.handle(ItemClickEvent(player, item, e.slotId, "Remove", true))) return
    when (e.component) {
        Component.interface_387.component_387_12, Component.interface_387.component_387_33 -> wornItemTeleport(player, item, e.opNum)
        Component.interface_387.component_387_15 ->
            if (e.opNum == 2 && player.equipment.weaponId == Obj.machinima_camera) player.interfaceManager.gazeOrbOfOculus()
    }
}

private fun compareDraggedItems(e: IFOnIFEvent) {
    val player = e.player
    val item1 = draggedItem(player, e.fromInterfaceId, e.fromSlotId)
    val item2 = draggedItem(player, e.toInterfaceId, e.toSlotId)
    if (item1 == null || item2 == null || item1 === item2) return
    compareItems(player, item1, item2)
}

private fun draggedItem(player: Player, interfaceId: Int, slot: Int): Item? =
    if (interfaceId == Interface.inventory_wear2) player.inventory.getItem(slot) else player.equipment.get(slot)

private fun compareItems(player: Player, item1: Item, item2: Item) {
    if (item1.definitions.equipSlot != item2.definitions.equipSlot) {
        player.sendMessage("These two items cannot be compared because they are not the same type.")
        return
    }
    player.packets.sendVarcString(Varcstr.varcstr_321, "Comparison")
    val categories = StringBuilder()
    val subCategories = StringBuilder()
    val item1Desc = StringBuilder(item1.definitions.name)
    val item2Desc = StringBuilder(item2.definitions.name)
    for (bonus in Bonus.entries) {
        val bonus1 = Equipment.getBonus(player, item1, bonus)
        val bonus2 = Equipment.getBonus(player, item2, bonus)
        if (bonus1 == 0 && bonus2 == 0) continue
        if (!categories.contains("Attack Bonuses") && bonus.ordinal in Bonus.STAB_ATT.ordinal..Bonus.RANGE_ATT.ordinal) {
            categories.append("<br>Attack Bonuses<br>")
            subCategories.append("<br><br>")
            item1Desc.append("<br>")
            item2Desc.append("<br>")
        }
        if (!categories.contains("Defense Bonuses") && bonus.ordinal in Bonus.STAB_DEF.ordinal..Bonus.ABSORB_RANGE.ordinal) {
            categories.append("<br>Defense Bonuses<br>")
            subCategories.append("<br><br>")
            item1Desc.append("<br><br>")
            item2Desc.append("<br><br>")
        }
        if (!categories.contains("Other") && bonus.ordinal in Bonus.MELEE_STR.ordinal..Bonus.MAGIC_STR.ordinal) {
            categories.append("<br>Other<br>")
            subCategories.append("<br><br>")
            item1Desc.append("<br><br>")
            item2Desc.append("<br><br>")
        }
        categories.append("<br>")
        subCategories.append(Utils.formatPlayerNameForDisplay(bonus.name.replace("_ATT", "").replace("_DEF", ""))).append(":<br>")
        item1Desc.append("<br>").append(wrapIfSuperior("$bonus1${percentSuffix(bonus)}", bonus1, bonus2))
        item2Desc.append("<br>").append(wrapIfSuperior("$bonus2${percentSuffix(bonus)}", bonus2, bonus1))
    }
    for (builder in arrayOf(categories, subCategories, item1Desc, item2Desc)) builder.append("<br> ")
    player.packets.sendVarcString(Varcstr.varcstr_322, categories.toString())
    player.packets.sendVarcString(Varcstr.varcstr_323, subCategories.toString())
    player.packets.sendVarcString(Varcstr.varcstr_324, item1Desc.toString())
    player.packets.sendVarcString(Varcstr.varcstr_325, item2Desc.toString())
}

private fun percentSuffix(bonus: Bonus): String = if (bonus in PERCENT_BONUSES) "%" else ""

private fun wrapIfSuperior(text: String, bonus1: Int, bonus2: Int): String = if (bonus1 > bonus2) "<col=FFFFFF>$text</col>" else text

private fun wornItemTeleport(player: Player, item: Item, opNum: Int) {
    val option = optionForPacket(opNum)
    if (option !in 0..3) return
    if (item.definitions.getEquipmentOption(option) == "Rub")
        ItemTeleports.wornTransportationDialogue(player, item)
    else
        ItemTeleports.sendTeleport(player, item, option, true)
}

private fun optionForPacket(opNum: Int): Int = if (opNum in 2..10) opNum - 2 else -1

private fun fireEquipEvent(player: Player, item: Item, equip: Boolean): Boolean {
    val event = ItemEquipEvent(player, item, equip)
    PluginManager.handle(event)
    return !event.isCancelled
}
