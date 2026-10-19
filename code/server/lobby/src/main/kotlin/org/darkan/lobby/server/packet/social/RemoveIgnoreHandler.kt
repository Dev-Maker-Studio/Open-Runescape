package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.RemoveIgnore
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class RemoveIgnoreHandler : PacketHandler<LobbyPlayer, RemoveIgnore> {
    override suspend fun handle(player: LobbyPlayer, packet: RemoveIgnore) {
        SocialGateway.act(player.account.username, SocialAction.RemoveIgnore(packet.name))
    }
}
