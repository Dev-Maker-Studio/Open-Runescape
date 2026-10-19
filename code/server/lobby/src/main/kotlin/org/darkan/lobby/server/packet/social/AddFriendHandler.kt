package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.AddFriend
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class AddFriendHandler : PacketHandler<LobbyPlayer, AddFriend> {
    override suspend fun handle(player: LobbyPlayer, packet: AddFriend) {
        SocialGateway.act(player.account.username, SocialAction.AddFriend(packet.name))
    }
}
