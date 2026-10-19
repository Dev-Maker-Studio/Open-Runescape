package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.ChatType
import org.darkan.lobby.web.model.LobbyPlayer

class ChatTypeHandler : PacketHandler<LobbyPlayer, ChatType> {
    override suspend fun handle(player: LobbyPlayer, packet: ChatType) {
        player.chatType = packet.type
    }
}
