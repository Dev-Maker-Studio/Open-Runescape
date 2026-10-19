package com.rs.game.content

import com.rs.game.content.minigames.creations.substituteSacredClayTool
import com.rs.game.content.skills.mining.Pickaxe
import com.rs.game.content.skills.woodcutting.Hatchet
import com.rs.game.model.entity.player.Player
import com.rs.net.decoders.handlers.InventoryOptionsHandler
import com.rs.plugin.annotations.PluginEventHandler
import com.rs.plugin.handlers.ButtonClickHandler
import com.rs.plugin.handlers.ItemClickHandler
import org.darkan.core.Constants
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.Gameval
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item

@PluginEventHandler
class Toolbelt {
    enum class Tools(
        val ownedVarbit: Int,
        private val baseItems: IntArray = intArrayOf(),
        val tierVarbit: Int? = null,
        private val tierEnum: Int? = null,
        val skill: Int? = null,
        val withdrawable: Boolean = false,
    ) {
        PICKAXE(
            Varbit.toolbelt_pickaxe,
            tierVarbit = Gameval.requireId(Gameval.VARBIT, "toolbelt_pickaxe_tier"),
            tierEnum = Gameval.requireId(Gameval.ENUM, "toolbelt_pickaxe_objects"),
            skill = Constants.MINING,
            withdrawable = true,
        ),
        HAMMER(Varbit.toolbelt_hammer, intArrayOf(Obj.hammer)),
        CHISEL(Varbit.toolbelt_chisel, intArrayOf(Obj.chisel)),
        HATCHET(
            Varbit.toolbelt_hatchet,
            tierVarbit = Gameval.requireId(Gameval.VARBIT, "toolbelt_hatchet_tier"),
            tierEnum = Gameval.requireId(Gameval.ENUM, "toolbelt_hatchet_objects"),
            skill = Constants.WOODCUTTING,
            withdrawable = true,
        ),
        KNIFE(Varbit.toolbelt_knife, intArrayOf(Obj.knife)),
        TINDERBOX(Varbit.toolbelt_tinderbox, intArrayOf(Obj.tinderbox)),
        SAW(Varbit.toolbelt_saw, intArrayOf(Obj.poh_saw)),
        PESTLE_AND_MORTAR(Varbit.toolbelt_pestle_mortar, intArrayOf(Obj.pestle_and_mortar)),
        MACHETE(
            Varbit.toolbelt_machete,
            tierVarbit = Gameval.requireId(Gameval.VARBIT, "toolbelt_machete_tier"),
            tierEnum = Gameval.requireId(Gameval.ENUM, "toolbelt_machete_objects"),
        ),
        WATCH(Varbit.toolbelt_watch, intArrayOf(Obj.trail_watch)),
        CHART(Varbit.toolbelt_chart, intArrayOf(Obj.trail_chart)),
        SHEARS(Varbit.toolbelt_shears, intArrayOf(Obj.shears)),
        NOOSE_WAND(Varbit.toolbelt_noose_wand, intArrayOf(Obj.noose_wand)),

        CRAYFISH_CAGE(Varbit.toolbelt_crayfish_cage, intArrayOf(Obj.crayfish_cage)),
        FISHING_ROD(Varbit.toolbelt_fishing_rod, intArrayOf(Obj.fishing_rod)),
        SMALL_FISHING_NET(Varbit.toolbelt_small_net, intArrayOf(Obj.net)),
        BIG_FISHING_NET(Varbit.toolbelt_big_net, intArrayOf(Obj.big_net)),
        FLY_FISHING_ROD(Varbit.toolbelt_fly_fishing_rod, intArrayOf(Obj.fly_fishing_rod)),
        HARPOON(Varbit.toolbelt_harpoon, intArrayOf(Obj.harpoon)),
        LOBSTER_POT(Varbit.toolbelt_lobster_pot, intArrayOf(Obj.lobster_pot)),
        BARBARIAN_ROD(Varbit.toolbelt_barbarian_rod, intArrayOf(Obj.brut_fishing_rod)),

        NEEDLE(Varbit.toolbelt_needle, intArrayOf(Obj.needle)),
        GLASSBLOWING_PIPE(Varbit.toolbelt_glassblowing_pipe, intArrayOf(Obj.glassblowingpipe)),
        AMULET_MOULD(Varbit.toolbelt_amulet_mould, intArrayOf(Obj.amulet_mould)),
        BRACELET_MOULD(Varbit.toolbelt_bracelet_mould, intArrayOf(Obj.jewl_bracelet_mould)),
        NECKLACE_MOULD(Varbit.toolbelt_necklace_mould, intArrayOf(Obj.necklace_mould)),
        RING_MOULD(Varbit.toolbelt_ring_mould, intArrayOf(Obj.ring_mould)),
        TIARA_MOULD(Varbit.toolbelt_tiara_mould, intArrayOf(Obj.tiara_mould)),
        AMMO_MOULD(Varbit.toolbelt_ammo_mould, intArrayOf(Obj.ammo_mould)),
        BOLT_MOULD(Varbit.toolbelt_bolt_mould, intArrayOf(Obj.xbows_silver_bolt_mould)),
        HOLY_MOULD(Varbit.toolbelt_holy_mould, intArrayOf(Obj.holy_symbol_mould)),
        UNHOLY_MOULD(Varbit.toolbelt_unholy_mould, intArrayOf(Obj.unholy_symbol_mould)),
        SICKLE_MOULD(Varbit.toolbelt_sickle_mould, intArrayOf(Obj.sickle_mould)),
        CHAIN_LINK_MOULD(Varbit.toolbelt_chain_mould, intArrayOf(Obj.myq4_chain_mould_inv)),

        RAKE(Varbit.toolbelt_rake, intArrayOf(Obj.rake)),
        SEED_DIBBER(Varbit.toolbelt_dibber, intArrayOf(Obj.dibber)),
        SPADE(Varbit.toolbelt_spade, intArrayOf(Obj.spade)),
        GARDENING_TROWEL(Varbit.toolbelt_gardening_trowel, intArrayOf(Obj.gardening_trowel)),
        SECATEURS(
            Varbit.toolbelt_secateurs,
            tierVarbit = Gameval.requireId(Gameval.VARBIT, "toolbelt_secateurs_tier"),
            tierEnum = Gameval.requireId(Gameval.ENUM, "toolbelt_secateurs_objects"),
        ),

        DUNG_PICKAXE(
            Varbit.toolbelt_rand_pickaxe,
            intArrayOf(
                Obj.rand_pickaxe_1,
                Obj.rand_pickaxe_2,
                Obj.rand_pickaxe_3,
                Obj.rand_pickaxe_4,
                Obj.rand_pickaxe_5,
                Obj.rand_pickaxe_6,
                Obj.rand_pickaxe_7,
                Obj.rand_pickaxe_8,
                Obj.rand_pickaxe_9,
                Obj.rand_pickaxe_10,
                Obj.rand_pickaxe_11,
            ),
        ),
        DUNG_HATCHET(
            Varbit.toolbelt_rand_hatchet,
            intArrayOf(
                Obj.rand_hatchet_1,
                Obj.rand_hatchet_2,
                Obj.rand_hatchet_3,
                Obj.rand_hatchet_4,
                Obj.rand_hatchet_5,
                Obj.rand_hatchet_6,
                Obj.rand_hatchet_7,
                Obj.rand_hatchet_8,
                Obj.rand_hatchet_9,
                Obj.rand_hatchet_10,
                Obj.rand_hatchet_11,
            ),
        ),
        DUNG_KNIFE(Varbit.toolbelt_rand_knife, intArrayOf(Obj.rand_knife)),
        DUNG_HAMMER(Varbit.toolbelt_rand_hammer, intArrayOf(Obj.rand_hammer)),
        DUNG_CHISEL(Varbit.toolbelt_rand_chisel, intArrayOf(Obj.rand_chisel)),
        DUNG_FLY_FISHING_ROD(Varbit.toolbelt_rand_fishing_rod, intArrayOf(Obj.rand_fishing_rod)),
        DUNG_NEEDLE(Varbit.toolbelt_rand_needle, intArrayOf(Obj.rand_needle)),
        DUNG_TINDERBOX(Varbit.toolbelt_rand_tinderbox, intArrayOf(Obj.rand_tinderbox));

        val itemIds: IntArray by lazy {
            tierEnum?.let { enumId ->
                val definition = EnumDefinitions.getEnum(enumId)
                (1..definition.length).map { definition.getIntValue(it) }.toIntArray()
            } ?: baseItems
        }

        val baseItemId: Int
            get() = itemIds.first()

        fun getValue(itemId: Int) = itemIds.indexOf(itemId).let { if (it < 0) 0 else it + 1 }

        fun itemAt(value: Int) = itemIds.getOrNull(value - 1)

        fun contains(value: Int, itemId: Int): Boolean {
            val itemValue = getValue(itemId)
            return itemValue > 0 && itemValue <= value.coerceAtMost(itemIds.size)
        }

        fun requiredLevel(itemId: Int): Int? = when (this) {
            PICKAXE -> Pickaxe.values().firstOrNull { it.itemId == itemId }?.level
            HATCHET -> Hatchet.values().firstOrNull { it.itemId == itemId }?.useLevel
            else -> null
        }

        companion object {
            private val byItemId by lazy {
                entries.flatMap { tool -> tool.itemIds.map { it to tool } }.toMap()
            }

            @JvmStatic
            fun forId(itemId: Int) = byItemId[itemId]
        }
    }

    companion object {
        private val withdrawComponent = Gameval.requireComponentId("interface_1178:tiered_tool_model")

        private val toolbeltOptions = arrayOf(
            "Add-to-toolbelt",
            "Carve",
            "Light",
            "Grind",
            "Powder",
            "Squeeze",
            "Craft",
            "Fletch",
            "Gut",
        )

        @JvmField
        val handleToolbeltOps = ItemClickHandler(toolbeltOptions) { e ->
            when (e.option) {
                "Add-to-toolbelt" -> {
                    if (e.player.addToolbelt(e.item.id)) {
                        e.player.inventory.deleteItem(e.item.id, 1)
                        refreshToolbelt(e.player)
                    }
                }

                "Light" -> InventoryOptionsHandler.handleItemOnItem(
                    e.player,
                    Item(Tools.TINDERBOX.baseItemId, 1),
                    e.item,
                    -1,
                    e.slotId,
                )

                "Grind", "Powder", "Squeeze" -> InventoryOptionsHandler.handleItemOnItem(
                    e.player,
                    Item(Tools.PESTLE_AND_MORTAR.baseItemId, 1),
                    e.item,
                    -1,
                    e.slotId,
                )

                "Craft", "Carve" -> handleCraft(e.player, e.item, e.slotId)
                "Fletch" -> handleFletch(e.player, e.item, e.slotId)
                "Gut" -> InventoryOptionsHandler.handleItemOnItem(
                    e.player,
                    Item(Tools.KNIFE.baseItemId, 1),
                    e.item,
                    -1,
                    e.slotId,
                )
            }
        }

        @JvmField
        val handleWithdraw = ButtonClickHandler(Interface.interface_1178) { e ->
            if (e.component == withdrawComponent && e.opNum == 1)
                e.player.withdrawToolbeltItem(e.slotId2)
        }

        @JvmStatic
        fun refreshToolbelt(player: Player) = player.refreshToolbelt()

        @JvmStatic
        fun addToolbelt(player: Player, itemId: Int) = player.addToolbelt(itemId)

        @JvmStatic
        fun containsTool(player: Player, itemId: Int) = player.containsTool(itemId)

        @JvmStatic
        fun getToolValue(player: Player, tool: Tools) = player.getToolValue(tool)

        private fun handleCraft(player: Player, item: Item, slotId: Int) {
            if (useTool(player, Tools.KNIFE, item, slotId) || useTool(player, Tools.CHISEL, item, slotId))
                return
            if (useTool(player, Tools.NEEDLE, item, slotId) || useTool(player, Tools.GLASSBLOWING_PIPE, item, slotId))
                return
            if (useTool(player, Tools.DUNG_NEEDLE, item, slotId) || useTool(player, Tools.DUNG_KNIFE, item, slotId))
                return
            useTool(player, Tools.DUNG_CHISEL, item, slotId)
        }

        private fun handleFletch(player: Player, item: Item, slotId: Int) {
            if (useTool(player, Tools.DUNG_KNIFE, item, slotId) || useTool(player, Tools.DUNG_CHISEL, item, slotId))
                return
            if (useTool(player, Tools.KNIFE, item, slotId))
                return
            useTool(player, Tools.CHISEL, item, slotId)
        }

        private fun useTool(player: Player, tool: Tools, item: Item, slotId: Int) =
            InventoryOptionsHandler.handleItemOnItem(player, Item(tool.baseItemId, 1), item, -1, slotId)
    }
}

fun Player.addToolbelt(itemId: Int): Boolean {
    val tool = Toolbelt.Tools.forId(itemId) ?: return false
    val value = tool.getValue(itemId)
    val currentValue = getToolValue(tool)
    if (currentValue == value) {
        sendMessage("You already have this tool on your belt.")
        return false
    }
    if (currentValue > value) {
        sendMessage("You already have a better tool on your belt.")
        return false
    }
    val requiredLevel = tool.requiredLevel(itemId)
    val skill = tool.skill
    if (requiredLevel != null && skill != null && skills.getLevel(skill) < requiredLevel) {
        sendMessage("You need a ${Constants.SKILL_NAME[skill]} level of $requiredLevel to add this tool to your belt.")
        return false
    }
    if (!inventory.hasFreeSlots()) {
        sendMessage("You need a free inventory slot to add this tool to your belt.")
        return false
    }
    if (tool.tierVarbit != null && currentValue >= 2) {
        val currentItemId = tool.itemAt(currentValue) ?: return false
        if (!inventory.addItem(currentItemId, 1))
            return false
    }
    toolbelt[tool] = value
    sendMessage("You add the ${ItemDefinitions.getDefs(itemId).name} to your toolbelt.")
    return true
}

fun Player.containsTool(itemId: Int): Boolean {
    val tool = Toolbelt.Tools.forId(itemId)
    return substituteSacredClayTool(itemId) || (tool != null && tool.contains(getToolValue(tool), itemId))
}

fun Player.getToolValue(tool: Toolbelt.Tools) = toolbelt[tool] ?: 0

fun Player.refreshToolbelt() {
    for (tool in Toolbelt.Tools.entries) {
        val value = getToolValue(tool)
        vars.setVarBit(tool.ownedVarbit, if (tool.tierVarbit == null) value else if (value > 0) 1 else 0)
        tool.tierVarbit?.let { vars.setVarBit(it, value) }
    }
}

fun Player.withdrawToolbeltItem(itemId: Int): Boolean {
    val tool = Toolbelt.Tools.forId(itemId) ?: return false
    if (!tool.withdrawable)
        return false
    val value = getToolValue(tool)
    if (value < 2)
        return false
    if (tool.itemAt(value) != itemId)
        return false
    if (!inventory.hasFreeSlots()) {
        sendMessage("You need a free inventory slot to withdraw this tool.")
        return false
    }
    if (!inventory.addItem(itemId, 1))
        return false
    toolbelt[tool] = 1
    refreshToolbelt()
    return true
}
