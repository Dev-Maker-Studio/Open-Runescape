package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.PrivateMessage
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class PrivateMessageHandler : PacketHandler<LobbyPlayer, PrivateMessage> {
    override suspend fun handle(player: LobbyPlayer, packet: PrivateMessage) {
        SocialGateway.act(player.account.username, SocialAction.SendPrivateMessage(packet.toDisplayName, PublicChatMessage(packet.message)))
    }
}
