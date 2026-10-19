package com.rs.game.content.minigames.creations

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.model.Item

private const val KILN_CLASS_KEY = "sc_kiln_class"
private const val KILN_PROTECTION_KEY = "sc_kiln_protected_until"
private val KILN_PROTECTION_TICKS = Ticks.fromSeconds(10)
private const val RUNES_PER_CLASS = 15
private const val ARROWS_PER_CLAY = 15

private class KilnClass(val clay: Int, val clayVarc: Int, val button: Int, val highlight: Int)

private val KILN_CLASSES = listOf(
    KilnClass(Obj.sc_clay_1, Varc.varc_583, Component.interface_813.component_813_99, Component.interface_813.component_813_98),
    KilnClass(Obj.sc_clay_2, Varc.varc_584, Component.interface_813.component_813_101, Component.interface_813.component_813_100),
    KilnClass(Obj.sc_clay_3, Varc.varc_585, Component.interface_813.component_813_103, Component.interface_813.component_813_102),
    KilnClass(Obj.sc_clay_4, Varc.varc_586, Component.interface_813.component_813_105, Component.interface_813.component_813_104),
    KilnClass(Obj.sc_clay_5, Varc.varc_587, Component.interface_813.component_813_107, Component.interface_813.component_813_106),
)

enum class KilnProduct(val component: Int, val skill: Int, private val products: IntArray, private val amountPerClay: (Int) -> Int = { 1 }) {
    HATCHET(Component.interface_813.component_813_37, Constants.SMITHING, intArrayOf(Obj.sc_hatchet_1, Obj.sc_hatchet_2, Obj.sc_hatchet_3, Obj.sc_hatchet_4, Obj.sc_hatchet_5)),
    PICKAXE(Component.interface_813.component_813_38, Constants.SMITHING, intArrayOf(Obj.sc_pickaxe_1, Obj.sc_pickaxe_2, Obj.sc_pickaxe_3, Obj.sc_pickaxe_4, Obj.sc_pickaxe_5)),
    HARPOON(Component.interface_813.component_813_39, Constants.CRAFTING, intArrayOf(Obj.sc_harpoon_1, Obj.sc_harpoon_2, Obj.sc_harpoon_3, Obj.sc_harpoon_4, Obj.sc_harpoon_5)),
    BUTTERFLY_NET(Component.interface_813.component_813_40, Constants.CRAFTING, intArrayOf(Obj.sc_butterflynet_1, Obj.sc_butterflynet_2, Obj.sc_butterflynet_3, Obj.sc_butterflynet_4, Obj.sc_butterflynet_5)),
    BARRIER(Component.interface_813.component_813_41, Constants.CONSTRUCTION, intArrayOf(Obj.sc_plank_1, Obj.sc_plank_2, Obj.sc_plank_3, Obj.sc_plank_4, Obj.sc_plank_5)),
    FOOD(Component.interface_813.component_813_42, Constants.COOKING, intArrayOf(Obj.sc_food_1, Obj.sc_food_2, Obj.sc_food_3, Obj.sc_food_4, Obj.sc_food_5)),
    HELM(Component.interface_813.component_813_43, Constants.SMITHING, intArrayOf(Obj.sc_helmet_1, Obj.sc_helmet_2, Obj.sc_helmet_3, Obj.sc_helmet_4, Obj.sc_helmet_5)),
    PLATELEGS(Component.interface_813.component_813_44, Constants.SMITHING, intArrayOf(Obj.sc_platelegs_1, Obj.sc_platelegs_2, Obj.sc_platelegs_3, Obj.sc_platelegs_4, Obj.sc_platelegs_5)),
    PLATEBODY(Component.interface_813.component_813_45, Constants.SMITHING, intArrayOf(Obj.sc_platebody_1, Obj.sc_platebody_2, Obj.sc_platebody_3, Obj.sc_platebody_4, Obj.sc_platebody_5)),
    COIF(Component.interface_813.component_813_46, Constants.CRAFTING, intArrayOf(Obj.sc_ranger_head_1, Obj.sc_ranger_head_2, Obj.sc_ranger_head_3, Obj.sc_ranger_head_4, Obj.sc_ranger_head_5)),
    CHAPS(Component.interface_813.component_813_47, Constants.CRAFTING, intArrayOf(Obj.sc_ranger_legs_1, Obj.sc_ranger_legs_2, Obj.sc_ranger_legs_3, Obj.sc_ranger_legs_4, Obj.sc_ranger_legs_5)),
    LEATHER_BODY(Component.interface_813.component_813_48, Constants.CRAFTING, intArrayOf(Obj.sc_ranger_body_1, Obj.sc_ranger_body_2, Obj.sc_ranger_body_3, Obj.sc_ranger_body_4, Obj.sc_ranger_body_5)),
    HAT(Component.interface_813.component_813_49, Constants.CRAFTING, intArrayOf(Obj.sc_hat_1, Obj.sc_hat_2, Obj.sc_hat_3, Obj.sc_hat_4, Obj.sc_hat_5)),
    ROBE_BOTTOM(Component.interface_813.component_813_50, Constants.CRAFTING, intArrayOf(Obj.sc_robe_bottom_1, Obj.sc_robe_bottom_2, Obj.sc_robe_bottom_3, Obj.sc_robe_bottom_4, Obj.sc_robe_bottom_5)),
    ROBE_TOP(Component.interface_813.component_813_51, Constants.CRAFTING, intArrayOf(Obj.sc_robe_top_1, Obj.sc_robe_top_2, Obj.sc_robe_top_3, Obj.sc_robe_top_4, Obj.sc_robe_top_5)),
    DAGGER(Component.interface_813.component_813_52, Constants.SMITHING, intArrayOf(Obj.sc_dagger_1, Obj.sc_dagger_2, Obj.sc_dagger_3, Obj.sc_dagger_4, Obj.sc_dagger_5)),
    SCIMITAR(Component.interface_813.component_813_53, Constants.SMITHING, intArrayOf(Obj.sc_scimitar_1, Obj.sc_scimitar_2, Obj.sc_scimitar_3, Obj.sc_scimitar_4, Obj.sc_scimitar_5)),
    WARHAMMER(Component.interface_813.component_813_54, Constants.SMITHING, intArrayOf(Obj.sc_warhammer_1, Obj.sc_warhammer_2, Obj.sc_warhammer_3, Obj.sc_warhammer_4, Obj.sc_warhammer_5)),
    BOW(Component.interface_813.component_813_55, Constants.FLETCHING, intArrayOf(Obj.sc_bow_1, Obj.sc_bow_2, Obj.sc_bow_3, Obj.sc_bow_4, Obj.sc_bow_5)),
    ARROWS(Component.interface_813.component_813_56, Constants.FLETCHING, intArrayOf(Obj.sc_arrow_1, Obj.sc_arrow_2, Obj.sc_arrow_3, Obj.sc_arrow_4, Obj.sc_arrow_5), { ARROWS_PER_CLAY }),
    ELEMENTAL_RUNES(Component.interface_813.component_813_57, Constants.RUNECRAFTING, IntArray(5) { Obj.hvh_elemental_rune }, { RUNES_PER_CLASS * it }),
    CATALYTIC_RUNES(Component.interface_813.component_813_58, Constants.RUNECRAFTING, IntArray(5) { Obj.hvh_catalytic_rune }, { RUNES_PER_CLASS * it }),
    POUCH(Component.interface_813.component_813_59, Constants.SUMMONING, intArrayOf(Obj.sc_lore_beast, Obj.sc_lore_beast_2, Obj.sc_lore_beast_3, Obj.sc_lore_beast_4, Obj.sc_lore_beast_5)),
    STAFF(Component.interface_813.component_813_60, Constants.CRAFTING, intArrayOf(Obj.sc_staff_1, Obj.sc_staff_2, Obj.sc_staff_3, Obj.sc_staff_4, Obj.sc_staff_5)),
    SCROLL(Component.interface_813.component_813_61, Constants.SUMMONING, IntArray(5) { Obj.sc_lore_clay_deposit }, { it }),
    PRAYER_POTION(Component.interface_813.component_813_64, Constants.HERBLORE, ScPotion.PRAYER.doses),
    ENERGY_POTION(Component.interface_813.component_813_65, Constants.HERBLORE, ScPotion.ENERGY.doses),
    ATTACK_POTION(Component.interface_813.component_813_66, Constants.HERBLORE, ScPotion.ATTACK.doses),
    STRENGTH_POTION(Component.interface_813.component_813_67, Constants.HERBLORE, ScPotion.STRENGTH.doses),
    RANGING_POTION(Component.interface_813.component_813_68, Constants.HERBLORE, ScPotion.RANGING.doses),
    DEFENCE_POTION(Component.interface_813.component_813_69, Constants.HERBLORE, ScPotion.DEFENCE.doses),
    MAGIC_POTION(Component.interface_813.component_813_70, Constants.HERBLORE, ScPotion.MAGIC.doses),
    SUMMONING_POTION(Component.interface_813.component_813_71, Constants.HERBLORE, ScPotion.SUMMONING.doses);

    fun product(clayClass: Int) = Item(products[clayClass - 1], amountPerClay(clayClass))

    companion object {
        private val byComponent = entries.associateBy { Utils.componentIdFromHash(it.component) }

        fun forComponent(componentId: Int) = byComponent[componentId]
    }
}

object Kiln {
    private val classButtons = KILN_CLASSES.withIndex().associate { (index, kilnClass) -> Utils.componentIdFromHash(kilnClass.button) to index }

    fun isProtected(player: Player) = player.tempAttribs.getL(KILN_PROTECTION_KEY) >= World.getServerTicks()

    fun open(player: Player) {
        val best = KILN_CLASSES.indices.lastOrNull { player.inventory.containsOneItem(KILN_CLASSES[it].clay) }
        if (best == null) {
            player.sendMessage("You try using the processing point, but quickly realize that you have no sacred clay with you.")
            return
        }
        player.interfaceManager.sendInterface(Interface.interface_813)
        player.packets.sendRunScriptReverse(Cs2.cs2_1910)
        player.tempAttribs.setI(KILN_CLASS_KEY, best)
        player.tempAttribs.setL(KILN_PROTECTION_KEY, World.getServerTicks() + KILN_PROTECTION_TICKS)
        player.setCloseInterfacesEvent { player.tempAttribs.removeL(KILN_PROTECTION_KEY) }
        refresh(player)
    }

    fun handleButton(player: Player, componentId: Int, opNum: Int, onMade: (clayClass: Int, clayUsed: Int) -> Unit) {
        classButtons[componentId]?.let { select(player, it) }
        val product = KilnProduct.forComponent(componentId) ?: return
        val index = selected(player)
        when (opNum) {
            1 -> make(player, product, 1, onMade)
            2 -> make(player, product, 5, onMade)
            3 -> player.sendInputInteger("Enter amount:") { make(player, product, it, onMade) }
            4 -> make(player, product, player.inventory.getAmountOf(KILN_CLASSES[index].clay), onMade)
        }
    }

    private fun selected(player: Player) = player.tempAttribs.getI(KILN_CLASS_KEY, 0).coerceIn(KILN_CLASSES.indices)

    private fun select(player: Player, index: Int) {
        if (!player.inventory.containsItem(KILN_CLASSES[index].clay, 1)) return
        player.tempAttribs.setI(KILN_CLASS_KEY, index)
        refresh(player)
    }

    private fun make(player: Player, product: KilnProduct, requested: Int, onMade: (Int, Int) -> Unit) {
        val index = selected(player)
        val clayClass = index + 1
        val level = StealingCreation.levelFor(clayClass)
        if (player.skills.getLevel(product.skill) < level) {
            player.sendMessage("You need a ${Constants.SKILL_NAME[product.skill]} level of $level to work that quality of clay.")
            return
        }
        val clay = KILN_CLASSES[index].clay
        val amount = minOf(requested, player.inventory.getAmountOf(clay))
        if (amount <= 0) {
            player.sendMessage("You have no clay to process.")
            return
        }
        val made = product.product(clayClass)
        player.inventory.deleteItem(clay, amount)
        player.inventory.addItem(made.id, made.amount * amount, true)
        onMade(clayClass, amount)
        refresh(player)
    }

    private fun refresh(player: Player) {
        var index = selected(player)
        if (player.inventory.getAmountOf(KILN_CLASSES[index].clay) == 0)
            KILN_CLASSES.indices.lastOrNull { player.inventory.getAmountOf(KILN_CLASSES[it].clay) > 0 }?.let {
                index = it
                player.tempAttribs.setI(KILN_CLASS_KEY, it)
            }
        KILN_CLASSES.forEachIndexed { i, kilnClass ->
            player.packets.sendVarc(kilnClass.clayVarc, player.inventory.getAmountOf(kilnClass.clay))
            player.packets.setIFHidden(Interface.interface_813, Utils.componentIdFromHash(kilnClass.highlight), index != i)
        }
    }
}
