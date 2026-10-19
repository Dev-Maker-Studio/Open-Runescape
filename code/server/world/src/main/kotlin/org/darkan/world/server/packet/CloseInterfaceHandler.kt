package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.packet.CloseInterface
import org.darkan.core.net.prot.handler.PacketHandler

class CloseInterfaceHandler : PacketHandler<Player, CloseInterface> {
    override suspend fun handle(player: Player, packet: CloseInterface) {
        if (player.hasStarted() && !player.isRunning) {
            player.run()
            return
        }
        if (player.interfaceManager.containsScreenInter() || !player.interfaceManager.isDefaultTopOpen())
            player.stopAll()
    }
}
