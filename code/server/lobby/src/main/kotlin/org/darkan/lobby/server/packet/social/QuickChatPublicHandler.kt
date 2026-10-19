package org.darkan.lobby.server.packet.social

import org.darkan.core.clan.ClanAction
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.QuickChatPublic
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.lobby.social.ClanService
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class QuickChatPublicHandler : PacketHandler<LobbyPlayer, QuickChatPublic> {
    override suspend fun handle(player: LobbyPlayer, packet: QuickChatPublic) {
        val username = player.account.username
        val type = if (packet.chatType != 0) packet.chatType else player.chatType
        val message = QuickChatMessage(message = "", fileId = packet.qcId, data = packet.messageData)
        when (type) {
            ChatHandler.FRIENDS_CHAT -> SocialGateway.act(username, SocialAction.SayInFriendsChat(message))
            ChatHandler.CLAN_CHAT, ChatHandler.GUEST_CLAN_CHAT -> {
                val action = ClanAction.QuickChat(guest = type == ChatHandler.GUEST_CLAN_CHAT, message = message)
                val result = ClanService.handle(username, action)
                result.message?.let { player.message(it) }
            }
        }
    }
}
