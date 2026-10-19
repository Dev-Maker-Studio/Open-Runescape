package org.darkan.lobby.server.packet.social

import org.darkan.core.clan.ClanAction
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.ClanChannelKickUser
import org.darkan.lobby.social.ClanService
import org.darkan.lobby.web.model.LobbyPlayer

class CcKickHandler : PacketHandler<LobbyPlayer, ClanChannelKickUser> {
    override suspend fun handle(player: LobbyPlayer, packet: ClanChannelKickUser) {
        val action = ClanAction.KickFromChannel(packet.username.formatPlayerNameForProtocol(), packet.guest)
        val result = ClanService.handle(player.account.username, action)
        result.message?.let { player.message(it) }
    }
}
