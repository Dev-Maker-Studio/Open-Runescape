package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.QuickChatPrivate
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class QuickChatPrivateHandler : PacketHandler<LobbyPlayer, QuickChatPrivate> {
    override suspend fun handle(player: LobbyPlayer, packet: QuickChatPrivate) {
        val message = QuickChatMessage(message = "", fileId = packet.qcId, data = packet.messageData)
        SocialGateway.act(player.account.username, SocialAction.SendPrivateMessage(packet.toUsername, message))
    }
}
