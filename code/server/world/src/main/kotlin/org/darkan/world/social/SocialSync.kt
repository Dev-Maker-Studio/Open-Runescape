package org.darkan.world.social

import com.rs.game.model.entity.player.Player
import org.darkan.core.social.SocialEvent

/** Keeps the world's copy of a player's social settings in step with what the lobby says they are. */
object SocialSync {
    fun apply(player: Player, event: SocialEvent) {
        val social = player.account.social
        when (event) {
            is SocialEvent.Snapshot -> {
                social.friends = event.friends.mapTo(HashSet()) { it.username }
                social.ignores = event.ignores.mapTo(HashSet()) { it.username }
                social.status = event.privateStatus.toByte()
                social.currentFriendsChat = event.friendsChat?.ownerDisplayName
            }
            is SocialEvent.FriendUpdated -> social.friends.add(event.friend.username)
            is SocialEvent.FriendRemoved -> social.friends.remove(event.username)
            is SocialEvent.IgnoresChanged -> social.ignores = event.ignores.mapTo(HashSet()) { it.username }
            is SocialEvent.PrivateStatusChanged -> social.status = event.status.toByte()
            is SocialEvent.FriendsChatChanged -> social.currentFriendsChat = event.channel?.ownerDisplayName
            is SocialEvent.FriendsChatLine,
            is SocialEvent.PrivateMessageReceived,
            is SocialEvent.PrivateMessageSent,
            is SocialEvent.Notice -> Unit
        }
    }
}
