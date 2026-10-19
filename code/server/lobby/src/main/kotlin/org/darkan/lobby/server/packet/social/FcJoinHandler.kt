package org.darkan.lobby.server.packet.social

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.FcJoin
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class FcJoinHandler : PacketHandler<LobbyPlayer, FcJoin> {
    override suspend fun handle(player: LobbyPlayer, packet: FcJoin) {
        SocialGateway.act(player.account.username, packet.fcName?.let { SocialAction.JoinFriendsChat(it) } ?: SocialAction.LeaveFriendsChat)
    }
}
