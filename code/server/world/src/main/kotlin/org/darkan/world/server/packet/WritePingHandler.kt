package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.packet.WritePing
import org.darkan.core.net.prot.handler.PacketHandler

class WritePingHandler : PacketHandler<Player, WritePing> {
    override suspend fun handle(player: Player, packet: WritePing) {
        player.nsv.setI("ping", packet.ping)
    }
}
