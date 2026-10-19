package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.QuickChatPrivate
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.world.server.WorldServer

class QCPrivateHandler : PacketHandler<Player, QuickChatPrivate> {
    override suspend fun handle(player: Player, packet: QuickChatPrivate) {
        if (!player.hasStarted()) return
        val message = QuickChatMessage(message = "", fileId = packet.qcId, data = packet.messageData)
        WorldServer.act(player.username, SocialAction.SendPrivateMessage(packet.toUsername, message))
    }
}
