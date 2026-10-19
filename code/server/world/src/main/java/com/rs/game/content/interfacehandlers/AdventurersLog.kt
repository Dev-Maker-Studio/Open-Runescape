package com.rs.game.content.interfacehandlers

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface

object AdventurersLog {
    fun openSettings(player: Player) {
        if (player.interfaceManager.containsScreenInter()) {
            player.sendMessage("Please close the interface you have open before changing your Adventurer's Log settings.")
            return
        }
        player.stopAll()
        player.interfaceManager.sendInterface(Interface.interface_623)
    }
}

@ServerStartupEvent
fun mapAdventurersLogSettings() {
    onButtonClick(Interface.interface_623) { e ->
        when (e.component) {
            Component.interface_623.component_623_62, Component.interface_623.component_623_74 ->
                e.player.interfaceManager.removeCentralInterface()
        }
    }
}
