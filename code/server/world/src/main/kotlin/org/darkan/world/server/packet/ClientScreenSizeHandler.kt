package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.InterfaceManager.ScreenMode
import org.darkan.core.net.prot.packet.ClientScreenSize
import org.darkan.core.net.prot.handler.PacketHandler

class ClientScreenSizeHandler : PacketHandler<Player, ClientScreenSize> {
    override suspend fun handle(player: Player, packet: ClientScreenSize) {
        player.screenWidth = packet.width
        player.screenHeight = packet.height
        if (!player.hasStarted() || player.hasFinished() || !player.interfaceManager.topOpen(742))
            return
        val mode = ScreenMode.forId(packet.displayMode) ?: run {
            player.sendMessage("Invalid screen mode: ${packet.displayMode}")
            return
        }
        player.interfaceManager.switchDisplayModes(mode)
    }
}
