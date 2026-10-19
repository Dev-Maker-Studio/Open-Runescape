package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.packet.Ping
import org.darkan.core.net.prot.handler.PacketHandler

class PingHandler : PacketHandler<Player, Ping> {
    override suspend fun handle(player: Player, packet: Ping) {
        // Keep-alive, no action needed
    }
}
