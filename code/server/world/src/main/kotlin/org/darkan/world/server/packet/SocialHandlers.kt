package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.clan.ClanAction
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.model.FriendsChatRank
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.AddFriend
import org.darkan.core.net.prot.packet.AddIgnoreReq
import org.darkan.core.net.prot.packet.ClanChannelKickUser
import org.darkan.core.net.prot.packet.FcJoin
import org.darkan.core.net.prot.packet.FcKick
import org.darkan.core.net.prot.packet.FcSetRank
import org.darkan.core.net.prot.packet.RemoveFriend
import org.darkan.core.net.prot.packet.RemoveIgnore
import org.darkan.core.social.SocialAction
import org.darkan.world.clan.ClanRegistry
import org.darkan.world.server.WorldServer

class AddFriendHandler : PacketHandler<Player, AddFriend> {
    override suspend fun handle(player: Player, packet: AddFriend) {
        WorldServer.act(player.username, SocialAction.AddFriend(packet.name))
    }
}

class RemoveFriendHandler : PacketHandler<Player, RemoveFriend> {
    override suspend fun handle(player: Player, packet: RemoveFriend) {
        WorldServer.act(player.username, SocialAction.RemoveFriend(packet.name))
    }
}

class AddIgnoreHandler : PacketHandler<Player, AddIgnoreReq> {
    override suspend fun handle(player: Player, packet: AddIgnoreReq) {
        WorldServer.act(player.username, SocialAction.AddIgnore(packet.username, packet.temporary))
    }
}

class RemoveIgnoreHandler : PacketHandler<Player, RemoveIgnore> {
    override suspend fun handle(player: Player, packet: RemoveIgnore) {
        WorldServer.act(player.username, SocialAction.RemoveIgnore(packet.name))
    }
}

class FcJoinHandler : PacketHandler<Player, FcJoin> {
    override suspend fun handle(player: Player, packet: FcJoin) {
        WorldServer.act(player.username, packet.fcName?.let { SocialAction.JoinFriendsChat(it) } ?: SocialAction.LeaveFriendsChat)
    }
}

class FcKickHandler : PacketHandler<Player, FcKick> {
    override suspend fun handle(player: Player, packet: FcKick) {
        WorldServer.act(player.username, SocialAction.KickFromFriendsChat(packet.username))
    }
}

class FcSetRankHandler : PacketHandler<Player, FcSetRank> {
    override suspend fun handle(player: Player, packet: FcSetRank) {
        WorldServer.act(player.username, SocialAction.SetFriendsChatRank(packet.username, FriendsChatRank.fromId(packet.rank)))
    }
}

/**
 * Kicking somebody out of a clan channel. `packet.guest` is which of the two lists the player
 * clicked on; the lobby decides from its own membership whether the target really is a guest.
 */
class ClanChannelKickHandler : PacketHandler<Player, ClanChannelKickUser> {
    override suspend fun handle(player: Player, packet: ClanChannelKickUser) {
        if (!player.hasStarted()) return
        ClanRegistry.send(player, ClanAction.KickFromChannel(packet.username.formatPlayerNameForProtocol(), packet.guest))
    }
}
