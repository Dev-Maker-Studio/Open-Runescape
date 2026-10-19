package org.darkan.lobby.server.packet.social

import org.darkan.core.clan.ClanAction
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.Chat
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.lobby.command.LobbyCommands
import org.darkan.lobby.social.ClanService
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.model.LobbyPlayer

class ChatHandler : PacketHandler<LobbyPlayer, Chat> {
    override suspend fun handle(player: LobbyPlayer, packet: Chat) {
        val message = packet.message
        if (message.startsWith("::") || message.startsWith(";;")) {
            if (!LobbyCommands.process(player, message.substring(2)))
                player.message("Unknown command: $message")
            return
        }
        if (message.isBlank()) return
        val username = player.account.username
        val type = if (packet.type != -1) packet.type else player.chatType
        val effects = (packet.color shl 8) or (packet.effect and 0xff)
        val line = PublicChatMessage(message, effects)
        when (type) {
            FRIENDS_CHAT -> SocialGateway.act(username, SocialAction.SayInFriendsChat(line))
            CLAN_CHAT, GUEST_CLAN_CHAT -> {
                val result = ClanService.handle(username, ClanAction.Chat(guest = type == GUEST_CLAN_CHAT, message = line))
                result.message?.let { player.message(it) }
            }
        }
    }

    companion object {
        const val FRIENDS_CHAT = 1
        const val CLAN_CHAT = 2
        const val GUEST_CLAN_CHAT = 3
    }
}
