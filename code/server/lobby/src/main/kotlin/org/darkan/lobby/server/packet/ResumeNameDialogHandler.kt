package org.darkan.lobby.server.packet

import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.ResumeNameDialog
import org.darkan.lobby.name.LobbyNameScreen
import org.darkan.lobby.social.LobbyClanScreen
import org.darkan.lobby.web.model.LobbyPlayer

class ResumeNameDialogHandler : PacketHandler<LobbyPlayer, ResumeNameDialog> {
    override suspend fun handle(player: LobbyPlayer, packet: ResumeNameDialog) {
        if (LobbyNameScreen.holds(player.account)) LobbyNameScreen.submit(player, packet.name)
        else LobbyClanScreen.nameInput(player, packet.name)
    }
}
