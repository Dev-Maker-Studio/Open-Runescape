package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.ChatSetFilter
import org.darkan.core.social.SocialAction
import org.darkan.world.server.WorldServer

class ChatSetFilterHandler : PacketHandler<Player, ChatSetFilter> {
    override suspend fun handle(player: Player, packet: ChatSetFilter) {
        player.publicStatus = packet.publicFilter
        player.tradeStatus = packet.tradeFilter
        WorldServer.act(player.username, SocialAction.SetPrivateStatus(packet.privateFilter))
    }
}
