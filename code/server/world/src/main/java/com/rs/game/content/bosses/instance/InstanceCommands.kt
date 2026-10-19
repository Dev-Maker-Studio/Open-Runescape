package com.rs.game.content.bosses.instance

import com.rs.engine.command.Commands
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.model.Rights

@ServerStartupEvent
fun mapInstanceCommands() {
    Commands.add(Rights.PLAYER, "extendinstance", "Pays to add another hour to the instance you own.") { player, _ ->
        BossInstances.extend(player)?.let { player.sendMessage(it) }
    }
}
