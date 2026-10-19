package org.darkan.lobby.server.packet.social

import org.darkan.core.model.FriendsChatRank
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.FcSetRank
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class FcSetRankHandler : PacketHandler<LobbyPlayer, FcSetRank> {
    override suspend fun handle(player: LobbyPlayer, packet: FcSetRank) {
        SocialGateway.act(player.account.username, SocialAction.SetFriendsChatRank(packet.username, FriendsChatRank.fromId(packet.rank)))
    }
}
