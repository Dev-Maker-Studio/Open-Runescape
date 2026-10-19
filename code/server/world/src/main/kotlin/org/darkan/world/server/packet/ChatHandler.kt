package org.darkan.world.server.packet

import com.rs.engine.command.Commands
import com.rs.game.content.skills.dungeoneering.DungeonController
import com.rs.game.model.entity.player.Player
import org.darkan.core.clan.ClanAction
import org.darkan.core.net.prot.packet.Chat
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.world.clan.ClanRegistry
import org.darkan.world.server.WorldServer

class ChatHandler : PacketHandler<Player, Chat> {
    override suspend fun handle(player: Player, packet: Chat) {
        if (!player.hasStarted()) return

        val message = packet.message
        if (message.startsWith("::") || message.startsWith(";;")) {
            Commands.processCommand(player, message.substring(2), false, false)
            return
        }

        // Which channel the message belongs to is server-side state, set by ChatTypeHandler. The
        // client never puts it in the Chat packet (`Chat.type` decodes as -1 always), so reading it
        // off the packet sent every friends-chat and clan-chat line out as public chat instead.
        val chatType = player.chatType
        // 2 is the player's own clan channel, 3 the guest channel they are listening to. Clan chat
        // is not forwarded as a client packet: it goes to the lobby as a typed action, which also
        // means the talk-rank and mute rules are checked here first (docs/clans.md).
        if (chatType == 2 || chatType == 3) {
            if (message.isBlank()) return
            val effects = (packet.color shl 8) or (packet.effect and 0xff)
            ClanRegistry.send(
                player,
                ClanAction.Chat(guest = chatType == 3, message = PublicChatMessage(message, effects)),
            )
            return
        }
        if (message.isBlank()) return
        if (chatType == 1) {
            val effects = (packet.color shl 8) or (packet.effect and 0xff)
            WorldServer.act(player.username, SocialAction.SayInFriendsChat(PublicChatMessage(message, effects)))
            return
        }

        if (player.account.checkMuted()) {
            player.sendMessage("You are muted. The mute will be lifted at ${player.account.getUnmuteDate()}")
            return
        }

        val effects = (packet.color shl 8) or (packet.effect and 0xff)
        val chatMessage = PublicChatMessage(message, effects)
        val controller = player.controllerManager.controller
        if (controller is DungeonController) {
            for (party in player.dungManager.party.team)
                party.packets.sendPublicMessage(player, chatMessage)
            return
        }
        player.sendPublicChatMessage(chatMessage)
    }
}
