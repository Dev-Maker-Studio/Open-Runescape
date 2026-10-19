package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.ButtonClickEvent
import org.darkan.core.net.prot.packet.IfContinue
import org.darkan.core.net.prot.handler.PacketHandler

class IfContinueHandler : PacketHandler<Player, IfContinue> {
    override suspend fun handle(player: Player, packet: IfContinue) {
        if (!player.hasStarted() || !player.isRunning)
            return
        if (!player.interfaceManager.topOpen(packet.interfaceId))
            return
        if (player.conversation != null) {
            player.conversation.process(packet.interfaceId, packet.componentId)
            return
        }
        PluginManager.handle(ButtonClickEvent(player, packet.interfaceId, packet.componentId, -1, -1, -1))
    }
}
