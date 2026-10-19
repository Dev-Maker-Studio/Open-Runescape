package com.rs.game.content.interfacehandlers

import com.rs.game.content.skills.dungeoneering.DungeonController
import com.rs.game.content.world.Rest
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.InterfaceManager
import com.rs.game.model.entity.player.managers.InterfaceManager.Sub
import com.rs.game.model.entity.player.managers.PriceChecker
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.ButtonClickEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.utils.ReportsManager
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Animation
import org.darkan.core.model.IFEvents
import org.darkan.world.server.WorldServer
import java.text.NumberFormat
import java.util.Locale
import org.darkan.core.social.gateway.AccountAction

private fun component(hash: Int) = Utils.componentIdFromHash(hash)

private fun toggleVarBit(player: Player, varbit: Int) {
    player.vars.saveVarBit(varbit, 1 - player.vars.getVarBit(varbit))
}

private fun busy(player: Player, message: String): Boolean {
    if (player.interfaceManager.containsScreenInter() || player.inCombat(10000) || player.hasBeenHit(10000)) {
        player.sendMessage(message)
        return true
    }
    return false
}

@ServerStartupEvent
fun mapGameFrameButtons() {
    onButtonClick(Interface.interface_749) { e ->
        if (e.component != Component.interface_749.component_749_4) return@onButtonClick
        when (e.opNum) {
            1 -> e.player.prayer.switchQuickPrayers()
            2 -> e.player.prayer.switchSettingQuickPrayer()
        }
    }

    onButtonClick(Interface.interface_750) { e ->
        if (e.component != Component.interface_750.component_750_4) return@onButtonClick
        when (e.opNum) {
            1 -> {
                e.player.toggleRun(!e.player.isResting)
                if (e.player.isResting) e.player.stopAll()
            }
            2 -> rest(e.player)
        }
    }

    onButtonClick(Interface.interface_429) { e ->
        if (e.component == Component.interface_429.component_429_18) e.player.interfaceManager.sendSubDefault(Sub.TAB_SETTINGS)
    }

    onButtonClick(Interface.interface_982) { e ->
        val vars = e.player.vars
        when (e.componentId) {
            component(Component.interface_982.component_982_5) -> e.player.interfaceManager.sendSubDefault(Sub.TAB_SETTINGS)
            component(Component.interface_982.component_982_41) -> vars.saveVar(287, if (vars.getVar(287) == 0) 1 else 0)
            in component(Component.interface_982.component_982_17)..component(Component.interface_982.component_982_36) ->
                vars.saveVarBit(Varbit.option_clanchatcolour, e.componentId - component(Component.interface_982.component_982_17))
            in component(Component.interface_982.component_982_97)..component(Component.interface_982.component_982_116) ->
                vars.saveVarBit(Varbit.option_guestchatcolour, e.componentId - component(Component.interface_982.component_982_97))
            in component(Component.interface_982.component_982_49)..component(Component.interface_982.component_982_66) ->
                vars.saveVar(287, e.componentId - component(Component.interface_982.component_982_48))
            in component(Component.interface_982.component_982_72)..component(Component.interface_982.component_982_91) ->
                vars.saveVarBit(Varbit.option_friendchatcolour, e.componentId - component(Component.interface_982.component_982_72))
        }
    }

    onButtonClick(Interface.interface_261) { e ->
        val player = e.player
        if (player.interfaceManager.containsInventoryInter()) return@onButtonClick
        when (e.component) {
            Component.interface_261.component_261_22 -> {
                if (player.interfaceManager.containsScreenInter()) {
                    player.sendMessage("Please close the interface you have open before setting your graphic options.")
                    return@onButtonClick
                }
                player.stopAll()
                player.interfaceManager.sendInterface(Interface.interface_742)
            }
            Component.interface_261.component_261_6 -> toggleVarBit(player, Varbit.snapshot_right_click_enabled)
            Component.interface_261.component_261_11 -> toggleVarBit(player, Varbit.option_chat_filter_off)
            Component.interface_261.component_261_12 -> player.switchAllowChatEffects()
            Component.interface_261.component_261_13 -> player.interfaceManager.sendSub(Sub.TAB_SETTINGS, Interface.interface_982)
            Component.interface_261.component_261_14 -> player.switchMouseButtons()
            Component.interface_261.component_261_15 -> player.vars.saveVar(Varp.varp_427, 1 - player.vars.getVar(Varp.varp_427))
            Component.interface_261.component_261_26 -> AdventurersLog.openSettings(player)
            Component.interface_261.component_261_24 -> player.interfaceManager.sendSub(Sub.TAB_SETTINGS, Interface.interface_429)
            Component.interface_261.component_261_16 -> player.interfaceManager.sendSub(Sub.TAB_SETTINGS, Interface.interface_398)
            Component.interface_261.game_settings_button_text -> GameSettings.open(player)
        }
    }

    onButtonClick(Interface.interface_751) { e ->
        val player = e.player
        when (e.component) {
            Component.interface_751.component_751_14 -> ReportsManager.report(player)
            Component.interface_751.component_751_23 -> when (e.opNum) {
                2 -> player.setClanStatus(0)
                3 -> player.setClanStatus(1)
                4 -> player.setClanStatus(2)
            }
            Component.interface_751.component_751_32 -> when (e.opNum) {
                2 -> player.setFilterGame(false)
                4 -> player.setFilterGame(true)
            }
            Component.interface_751.component_751_0 -> {
                val status = when (e.opNum) {
                    2 -> 0
                    3 -> 1
                    4 -> 2
                    else -> return@onButtonClick
                }
                player.social.fcStatus = status.toByte()
                WorldServer.accountAction(player, AccountAction.SetFcStatus(status))
            }
            Component.interface_751.component_751_17 -> when (e.opNum) {
                2 -> player.setAssistStatus(0)
                3 -> player.setAssistStatus(1)
                4 -> player.setAssistStatus(2)
            }
        }
    }

    onButtonClick(Interface.interface_755) { e ->
        when (e.component) {
            Component.interface_755.component_755_44 -> {
                e.player.interfaceManager.setWindowsPane(e.player.interfaceManager.defaultTopInterface())
                e.player.setNextAnimation(Animation(-1))
            }
            Component.interface_755.component_755_42 -> closeWorldMapHint(e.player)
            Component.interface_755.component_755_49 -> worldMapKeyOption(e)
            Component.interface_755.component_755_54 -> {
                val vars = e.player.vars
                vars.saveVarBit(Varbit.worldmap_keysort, (vars.getVarBit(Varbit.worldmap_keysort) + 1) % 3)
            }
        }
    }

    onButtonClick(InterfaceManager.FIXED_TOP, InterfaceManager.RESIZEABLE_TOP) { e ->
        when (e.component) {
            Component.interface_548.component_548_167, Component.interface_746.component_746_208 -> moneyPouch(e)
            Component.interface_548.component_548_157, Component.interface_746.component_746_200 -> worldMap(e)
            Component.interface_548.component_548_35, Component.interface_746.component_746_55 -> xpCounter(e)
            Component.interface_746.component_746_207, Component.interface_548.component_548_159 -> if (e.opNum == 4) priceChecker(e.player)
        }
    }

    onButtonClick(Interface.interface_743) { e ->
        if (e.component == Component.interface_743.component_743_20) e.player.stopAll()
    }
}

private fun rest(player: Player) {
    if (player.isResting) {
        player.stopAll()
        return
    }
    if (player.emotesManager.isAnimating) {
        player.sendMessage("You can't rest while performing an emote.")
        return
    }
    if (player.isLocked) {
        player.sendMessage("You can't rest while performing an action.")
        return
    }
    player.stopAll()
    player.actionManager.setAction(Rest())
}

private fun closeWorldMapHint(player: Player) {
    player.hintIconsManager.removeAll()
    player.vars.setVar(Varp.varp_1159, -1)
}

private fun worldMapKeyOption(e: ButtonClickEvent) {
    if (e.opNum != 1) return
    val vars = e.player.vars
    val varbit = when (e.slotId) {
        0, 1 -> Varbit.worldmap_yah_hidden
        3, 4 -> Varbit.worldmap_samemaplinks_hidden
        11, 12 -> Varbit.worldmap_textlabels_hidden
        14, 16 -> Varbit.worldmap_tooltips_hidden
        18, 19 -> {
            closeWorldMapHint(e.player)
            return
        }
        else -> return
    }
    vars.saveVarBit(varbit, 1 - vars.getVarBit(varbit))
}

private fun moneyPouch(e: ButtonClickEvent) {
    val player = e.player
    if (player.controllerManager.isIn(DungeonController::class.java)) return
    when (e.opNum) {
        1 -> player.packets.sendRunScript(5557, 1)
        2 -> {
            if (busy(player, "Please finish what you're doing before withdrawing coins.")) return
            player.sendInputInteger("How much would you like to withdraw?") { num -> player.inventory.coinPouchToInventory(num) }
        }
        3 -> player.sendMessage("Your pouch contains " + NumberFormat.getNumberInstance(Locale.US).format(player.inventory.getCoins()) + " coins.")
        4 -> {
            if (busy(player, "Please finish what you're doing before opening the price checker.")) return
            priceChecker(player)
        }
        else -> player.sendMessage("Unknown coin pouch option.")
    }
}

private fun worldMap(e: ButtonClickEvent) {
    val player = e.player
    if (e.opNum == 2) {
        closeWorldMapHint(player)
        return
    }
    if (player.interfaceManager.containsScreenInter() || player.interfaceManager.containsInventoryInter() || player.inCombat(10000)) {
        player.sendMessage("Please finish what you're doing before opening the world map.")
        return
    }
    player.interfaceManager.setTopInterface(Interface.interface_755, false)
    player.packets.setIFEvents(IFEvents(Component.interface_755.component_755_49, 0, 19).enableRightClickOption(0))
    player.setNextAnimation(Animation(840))
    val posHash = player.x shl 14 or player.y
    player.packets.sendVarc(622, posHash)
    player.packets.sendVarc(674, posHash)
}

private fun xpCounter(e: ButtonClickEvent) {
    val player = e.player
    when (e.opNum) {
        1 -> player.skills.switchXPDisplay()
        2 -> player.skills.switchXPPopup()
        3 -> {
            if (player.interfaceManager.containsScreenInter() || player.interfaceManager.containsInventoryInter() || player.inCombat(10000)) {
                player.sendMessage("Please finish what you're doing before opening the XP counter customizer.")
                return
            }
            player.skills.setupXPCounter()
        }
    }
}

private fun priceChecker(player: Player) {
    if (player.interfaceManager.containsScreenInter()) {
        player.sendMessage("Please finish what you're doing before opening the price checker.")
        return
    }
    player.stopAll()
    PriceChecker.openPriceCheck(player)
}
