package org.darkan.lobby.server.packet

import org.darkan.core.net.prot.packet.Ping
import org.darkan.core.net.prot.packet.Pong
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.lobby.web.model.LobbyPlayer

class PingHandler : PacketHandler<LobbyPlayer, Ping> {
    override suspend fun handle(player: LobbyPlayer, packet: Ping) {
        player.session.send(Pong())
    }
}