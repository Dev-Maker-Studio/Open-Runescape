package com.rs.game.content.world.areas.runecrafting_guild

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Enum
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp

private const val MAX_AMOUNT = 16383
private const val BUY_ONE = 1

private enum class GuildReward(val button: Int, val obj: Int, val delivered: Int = obj, val single: Boolean = false) {
    AIR_TALISMAN(Component.interface_779.component_779_6, Obj.air_talisman),
    BODY_TALISMAN(Component.interface_779.component_779_7, Obj.body_talisman),
    CHAOS_TALISMAN(Component.interface_779.component_779_8, Obj.chaos_talisman),
    COSMIC_TALISMAN(Component.interface_779.component_779_9, Obj.cosmic_talisman),
    EARTH_TALISMAN(Component.interface_779.component_779_10, Obj.earth_talisman),
    FIRE_TALISMAN(Component.interface_779.component_779_11, Obj.fire_talisman),
    LAW_TALISMAN(Component.interface_779.component_779_12, Obj.law_talisman),
    MIND_TALISMAN(Component.interface_779.component_779_13, Obj.mind_talisman),
    NATURE_TALISMAN(Component.interface_779.component_779_14, Obj.nature_talisman),
    WATER_TALISMAN(Component.interface_779.component_779_15, Obj.water_talisman),
    BLUE_HAT(Component.interface_779.component_779_36, Obj.rcguild_hat_blue),
    YELLOW_HAT(Component.interface_779.component_779_37, Obj.rcguild_hat_yellow),
    GREEN_HAT(Component.interface_779.component_779_38, Obj.rcguild_hat_green),
    BLUE_ROBE(Component.interface_779.component_779_39, Obj.rcguild_robetop_blue),
    YELLOW_ROBE(Component.interface_779.component_779_40, Obj.rcguild_robetop_yellow),
    GREEN_ROBE(Component.interface_779.component_779_41, Obj.rcguild_robetop_green),
    BLUE_BOTTOMS(Component.interface_779.component_779_42, Obj.rcguild_robebottom_blue),
    YELLOW_BOTTOMS(Component.interface_779.component_779_43, Obj.rcguild_robebottom_yellow),
    GREEN_BOTTOMS(Component.interface_779.component_779_44, Obj.rcguild_robebottom_green),
    BLUE_GLOVES(Component.interface_779.component_779_45, Obj.rcguild_gloves_blue),
    YELLOW_GLOVES(Component.interface_779.component_779_46, Obj.rcguild_gloves_yellow),
    GREEN_GLOVES(Component.interface_779.component_779_47, Obj.rcguild_gloves_green),
    MASTER_HAT(Component.interface_779.component_779_48, Obj.obj_21517, single = true),
    MASTER_ROBE(Component.interface_779.component_779_49, Obj.obj_21516, single = true),
    MASTER_SKIRT(Component.interface_779.component_779_50, Obj.obj_21518, single = true),
    MASTER_BOOTS(Component.interface_779.component_779_51, Obj.obj_21519, single = true),
    AIR_TABLET(Component.interface_779.component_779_76, Obj.rcguild_tablet_air),
    BODY_TABLET(Component.interface_779.component_779_77, Obj.rcguild_tablet_body),
    CHAOS_TABLET(Component.interface_779.component_779_78, Obj.rcguild_tablet_chaos),
    COSMIC_TABLET(Component.interface_779.component_779_79, Obj.rcguild_tablet_cosmic),
    DEATH_TABLET(Component.interface_779.component_779_80, Obj.rcguild_tablet_death),
    EARTH_TABLET(Component.interface_779.component_779_81, Obj.rcguild_tablet_earth),
    FIRE_TABLET(Component.interface_779.component_779_82, Obj.rcguild_tablet_fire),
    LAW_TABLET(Component.interface_779.component_779_83, Obj.rcguild_tablet_law),
    MIND_TABLET(Component.interface_779.component_779_84, Obj.rcguild_tablet_mind),
    ASTRAL_TABLET(Component.interface_779.component_779_85, Obj.rcguild_tablet_astral),
    NATURE_TABLET(Component.interface_779.component_779_86, Obj.rcguild_tablet_nature),
    WATER_TABLET(Component.interface_779.component_779_87, Obj.rcguild_tablet_water),
    BLOOD_TABLET(Component.interface_779.component_779_88, Obj.rcguild_tablet_blood),
    GUILD_TABLET(Component.interface_779.component_779_89, Obj.rcguild_tablet_guild),
    RUNECRAFTING_STAFF(Component.interface_779.component_779_118, Obj.rcguild_staff_blank),
    ESSENCE(Component.interface_779.component_779_119, Obj.cert_blankrune, Obj.cert_blankrune_high);

    val price get() = EnumDefinitions.getEnum(Enum.rcguild_reward_shop).getIntValue(obj)

    companion object {
        private val byButton = entries.associateBy { it.button }
        private val byObj = entries.associateBy { it.obj }

        fun forButton(component: Int) = byButton[component]
        fun forObj(obj: Int) = byObj[obj]
    }
}

@ServerStartupEvent
fun mapGuildRewards() {
    onButtonClick(Interface.interface_779) { e ->
        val player = e.player
        when (e.component) {
            Component.interface_779.component_779_168 -> purchase(player)
            Component.interface_779.component_779_131 -> player.closeInterfaces()
            else -> {
                val reward = GuildReward.forButton(e.component) ?: return@onButtonClick
                if (e.opNum == BUY_ONE || reward.single) select(player, reward, 1)
                else player.sendInputInteger("How many would you like to buy?") { select(player, reward, it) }
            }
        }
    }
}

fun openGuildRewards(player: Player) {
    player.interfaceManager.sendInterface(Interface.interface_779)
    clearSelection(player)
    player.setCloseInterfacesEvent { clearSelection(player) }
}

private fun clearSelection(player: Player) {
    player.vars.setVar(Varp.varp_1278, -1)
    player.vars.setVarBit(Varbit.rcguild_selected_amount, 0)
    player.packets.setIFText(Component.interface_779.component_779_140, "")
    player.packets.setIFText(Component.interface_779.component_779_141, "")
}

private fun select(player: Player, reward: GuildReward, requested: Int) {
    if (requested <= 0) return
    val amount = requested.coerceAtMost(MAX_AMOUNT)
    player.vars.setVar(Varp.varp_1278, reward.obj)
    player.vars.setVarBit(Varbit.rcguild_selected_amount, amount)
    val name = ItemDefinitions.getDefs(reward.delivered).let { if (it.isNoted()) ItemDefinitions.getDefs(it.noteId).name else it.name }
    player.packets.setIFText(Component.interface_779.component_779_140, if (amount == 1) name else "$name x ${Utils.formatNumber(amount)}")
    player.packets.setIFText(Component.interface_779.component_779_141, "${Utils.formatNumber(reward.price * amount)} tokens")
}

private fun purchase(player: Player) {
    val reward = GuildReward.forObj(player.vars.getVar(Varp.varp_1278))
    val amount = player.vars.getVarBit(Varbit.rcguild_selected_amount)
    if (reward == null || amount <= 0) {
        player.sendMessage("Choose a reward first.")
        return
    }
    val cost = reward.price * amount
    val tokens = player.inventory.getAmountOf(Obj.rcguild_reward_token)
    val defs = ItemDefinitions.getDefs(reward.delivered)
    val delivered = if (amount > 1 && !defs.isStackable() && defs.noteId != -1) defs.noteId else reward.delivered
    val slotsNeeded = if (ItemDefinitions.getDefs(delivered).isStackable()) (if (player.inventory.containsItem(delivered)) 0 else 1) else amount
    when {
        tokens < cost -> player.sendMessage("You need ${Utils.formatNumber(cost)} tokens for that, but you only have ${Utils.formatNumber(tokens)}.")
        player.inventory.freeSlots < slotsNeeded -> player.sendMessage("You don't have enough inventory space for that.")
        else -> {
            player.inventory.deleteItem(Obj.rcguild_reward_token, cost)
            player.inventory.addItem(delivered, amount)
            player.sendMessage("You spend ${Utils.formatNumber(cost)} tokens.")
            clearSelection(player)
        }
    }
}
