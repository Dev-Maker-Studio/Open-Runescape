package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.AddIgnoreReq
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class AddIgnoreReqHandler : PacketHandler<LobbyPlayer, AddIgnoreReq> {
    override suspend fun handle(player: LobbyPlayer, packet: AddIgnoreReq) {
        SocialGateway.act(player.account.username, SocialAction.AddIgnore(packet.username, packet.temporary))
    }
}
