package com.rs.game.content.interfacehandlers

import com.rs.game.content.BuffBar
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.IFEvents

enum class GameSetting(val varbit: Int, val values: Int, val isToggle: Boolean = false) {
    INTERFACE_SKIN(Varbit.option_gameframe_skin, 2),
    BUFF_BAR(Varbit.option_buff_bar, 2, isToggle = true) {
        override fun changed(player: Player) = BuffBar.refresh(player)
    };

    fun select(player: Player, value: Int) {
        if (value !in 0 until values || player.vars.getVarBit(varbit) == value) return
        player.vars.saveVarBit(varbit, value)
        changed(player)
    }

    fun toggle(player: Player) {
        if (isToggle) select(player, if (player.vars.getVarBit(varbit) == 0) 1 else 0)
    }

    open fun changed(player: Player) {}
}

object GameSettings {
    private const val MAX_VALUES = 16
    private const val MAX_ROW_COMPONENTS = 64

    fun open(player: Player) {
        if (player.interfaceManager.containsScreenInter()) {
            player.sendMessage("Please close the interface you have open before changing your game settings.")
            return
        }
        player.stopAll()
        player.interfaceManager.sendInterface(Interface.game_settings)
        player.packets.setIFEvents(IFEvents(Component.game_settings.dropdown, 0, MAX_VALUES - 1).enableRightClickOptions(*rows { !it.isToggle }))
        player.packets.setIFEvents(IFEvents(Component.game_settings.rows, 0, MAX_ROW_COMPONENTS - 1).enableRightClickOptions(*rows { it.isToggle }))
    }

    fun select(player: Player, row: Int, value: Int) {
        GameSetting.entries.getOrNull(row)?.takeUnless { it.isToggle }?.select(player, value)
    }

    fun toggle(player: Player, row: Int) {
        GameSetting.entries.getOrNull(row)?.toggle(player)
    }

    private fun rows(filter: (GameSetting) -> Boolean): IntArray =
        GameSetting.entries.filter(filter).map { it.ordinal }.toIntArray()
}

@ServerStartupEvent
fun mapGameSettings() {
    onButtonClick(Interface.game_settings) { e ->
        when (e.component) {
            Component.game_settings.close_button -> e.player.interfaceManager.removeCentralInterface()
            Component.game_settings.dropdown -> GameSettings.select(e.player, e.opNum - 1, e.slotId)
            Component.game_settings.rows -> GameSettings.toggle(e.player, e.opNum - 1)
        }
    }
}
