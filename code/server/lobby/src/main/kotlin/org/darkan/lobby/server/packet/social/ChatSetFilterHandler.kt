package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.ChatSetFilter
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class ChatSetFilterHandler : PacketHandler<LobbyPlayer, ChatSetFilter> {
    override suspend fun handle(player: LobbyPlayer, packet: ChatSetFilter) {
        SocialGateway.act(player.account.username, SocialAction.SetPrivateStatus(packet.privateFilter))
    }
}
