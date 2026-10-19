package org.darkan.lobby.server.packet

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.ResumeClanForumQFCDialog
import org.darkan.lobby.social.LobbyClanScreen
import org.darkan.lobby.web.model.LobbyPlayer

class ResumeClanForumQfcDialogHandler : PacketHandler<LobbyPlayer, ResumeClanForumQFCDialog> {
    override suspend fun handle(player: LobbyPlayer, packet: ResumeClanForumQFCDialog) {
        LobbyClanScreen.forumQfc(player, packet.forumQfc)
    }
}
