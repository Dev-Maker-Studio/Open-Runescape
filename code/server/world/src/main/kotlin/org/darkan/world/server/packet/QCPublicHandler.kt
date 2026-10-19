package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.clan.ClanAction
import org.darkan.core.net.prot.packet.QuickChatPublic
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.world.clan.ClanRegistry
import org.darkan.world.server.WorldServer

class QCPublicHandler : PacketHandler<Player, QuickChatPublic> {
    override suspend fun handle(player: Player, packet: QuickChatPublic) {
        if (!player.hasStarted()) return
        // 2 is the player's own clan channel, 3 the guest channel: a typed clan action rather than
        // a forwarded client packet (docs/clans.md). The text is the client's, so only the file id
        // and its data travel.
        if (packet.chatType == 2 || packet.chatType == 3) {
            ClanRegistry.send(
                player,
                ClanAction.QuickChat(
                    guest = packet.chatType == 3,
                    message = QuickChatMessage(message = "", fileId = packet.qcId, data = packet.messageData),
                ),
            )
            return
        }
        if (packet.chatType == 1) {
            WorldServer.act(
                player.username,
                SocialAction.SayInFriendsChat(QuickChatMessage(message = "", fileId = packet.qcId, data = packet.messageData)),
            )
            return
        }
        // TODO: Quick chat public message handling
    }
}
