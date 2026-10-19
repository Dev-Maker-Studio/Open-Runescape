package org.darkan.lobby.server.packet

import org.darkan.core.Logger.logDebug
import org.darkan.core.net.prot.packet.LobbyHyperlink
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.lobby.web.model.LobbyPlayer

/**
 * The lobby's account links (account_settings.ws?mod=email / mod=recoveries / mod=messages,
 * userdetails.ws for membership). The legacy lobby never opened a browser for these either -
 * it left the OpenURL write commented out - so this only records the request until the web
 * pages exist.
 */
class LobbyHyperlinkHandler : PacketHandler<LobbyPlayer, LobbyHyperlink> {
    override suspend fun handle(player: LobbyPlayer, packet: LobbyHyperlink) {
        logDebug("Lobby hyperlink from ${player.account.username}: service=${packet.service} page=${packet.page} flags=${packet.flags}")
    }
}
