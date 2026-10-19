package org.darkan.lobby.server.packet

import org.darkan.core.gameval.gen.Interface
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.IfButton
import org.darkan.lobby.name.LobbyNameScreen
import org.darkan.lobby.social.LobbyClanScreen
import org.darkan.lobby.web.model.LobbyPlayer

class IfButtonHandler : PacketHandler<LobbyPlayer, IfButton> {
    override suspend fun handle(player: LobbyPlayer, packet: IfButton) {
        if (packet.interfaceId == Interface.interface_890) {
            if (packet.componentId == LobbyNameScreen.CONTINUE_BUTTON) LobbyNameScreen.confirm(player)
            return
        }
        if (LobbyNameScreen.holds(player.account)) return
        LobbyClanScreen.button(player, packet)
    }
}
