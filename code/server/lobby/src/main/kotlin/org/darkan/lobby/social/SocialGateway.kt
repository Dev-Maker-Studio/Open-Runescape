package org.darkan.lobby.social

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.darkan.core.Logger
import org.darkan.core.Logger.logInfo
import org.darkan.core.Logger.logTrace
import org.darkan.core.Logger.logWarn
import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.model.Account
import org.darkan.core.model.ChatMessageType
import org.darkan.core.model.FriendsChatRank
import org.darkan.core.model.Rights
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.Session
import org.darkan.core.net.prot.ServerProt
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.social.ChatMessage
import org.darkan.core.social.FriendEntry
import org.darkan.core.social.FriendsChatMember
import org.darkan.core.social.FriendsChatState
import org.darkan.core.social.IgnoredPlayer
import org.darkan.core.social.NoticeKind
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.SocialAction
import org.darkan.core.social.SocialEvent
import org.darkan.core.social.SocialPackets
import org.darkan.core.social.Speaker
import org.darkan.core.social.gateway.AccountAction
import org.darkan.core.social.gateway.AccountCommand
import org.darkan.core.social.gateway.AccountUpdate
import org.darkan.core.social.gateway.AnnouncementBroadcast
import org.darkan.core.social.gateway.AnnouncementTarget
import org.darkan.core.social.gateway.ClanCommand
import org.darkan.core.social.gateway.ClanFetch
import org.darkan.core.social.gateway.DisplayNameLookup
import org.darkan.core.social.gateway.DisplayNameResult
import org.darkan.core.social.gateway.GatewayPlayerPresence
import org.darkan.core.social.gateway.GatewayWorldInfo
import org.darkan.core.social.gateway.PlayerOffline
import org.darkan.core.social.gateway.PlayerOnline
import org.darkan.core.social.gateway.PresenceSyncReason
import org.darkan.core.social.gateway.SocialCommand
import org.darkan.core.social.gateway.SocialGatewayWireJson
import org.darkan.core.social.gateway.SocialGatewayWireMessage
import org.darkan.core.social.gateway.VerifyLoginToken
import org.darkan.core.social.gateway.WorldHello
import org.darkan.core.social.gateway.WorldHelloAck
import org.darkan.core.social.gateway.WorldPresenceSnapshot
import org.darkan.core.worldlist.World
import org.darkan.lobby.Lobby
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** Somewhere a player's social events are delivered: a world, a lobby-screen session, a website socket. */
interface SocialSink {
    suspend fun deliver(usernames: List<String>, event: SocialEvent)
}

/**
 * The lobby's social service. A player has any number of attachments - one per world they are
 * on, per lobby-screen session, per website socket - and every attachment gets the same
 * [SocialEvent]s and may send [SocialAction]s. Presence is derived from the attachments that
 * carry a world: a player is online while any of those exists, and reads as being on the newest
 * game world, else the lobby screen. Per-player work is serialised so a late goodbye from one
 * connection can never undo a fresh hello from another.
 */
object SocialGateway {
    const val LOBBY_WORLD_ID = 1115
    private const val LISTENER_WORLD_NAME = "Web"
    private const val FRIENDS_CHAT_CAPACITY = 100
    private const val KICK_BAN_MS = 3_600_000L

    private class Attachment(
        val username: String,
        val displayName: String,
        val worldId: Int?,
        val sink: SocialSink,
        val order: Long,
    ) {
        var bot = false
    }

    private class LobbySink(val session: Session, private val username: String) : SocialSink {
        override suspend fun deliver(usernames: List<String>, event: SocialEvent) {
            for (packet in SocialPackets.render(username, event)) session.send(packet)
        }
    }

    private data class Location(val worldId: Int, val worldName: String)

    private class FcRoom(
        val ownerUsername: String,
        val members: MutableSet<String> = ConcurrentHashMap.newKeySet(),
        val tempBans: ConcurrentHashMap<String, Long> = ConcurrentHashMap(),
    )

    private val worldsById = ConcurrentHashMap<Int, WorldSession>()
    private val worldInfoById = ConcurrentHashMap<Int, GatewayWorldInfo>()
    private val attachmentsByUsername = ConcurrentHashMap<String, CopyOnWriteArrayList<Attachment>>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val attachmentOrder = AtomicLong()
    private val friendWatchers = ConcurrentHashMap<String, MutableSet<String>>()
    private val fcRoomsByOwner = ConcurrentHashMap<String, FcRoom>()
    private val fcOwnerByMember = ConcurrentHashMap<String, String>()

    internal var communityWorlds = CommunityWorlds()

    // ─── Attachments ─────────────────────────────────────────────────

    suspend fun registerLobbyPlayerSession(account: Account, session: Session) =
        attach(Attachment(account.username, account.displayName, LOBBY_WORLD_ID, LobbySink(session, account.username), attachmentOrder.incrementAndGet()))

    suspend fun unregisterLobbyPlayerSession(username: String, session: Session) {
        LobbyClanScreen.forget(username)
        detach(username) { (it.sink as? LobbySink)?.session === session }
    }

    suspend fun attachListener(account: Account, sink: SocialSink) =
        attach(Attachment(account.username, account.displayName, null, sink, attachmentOrder.incrementAndGet()))

    suspend fun detachListener(username: String, sink: SocialSink) = detach(username) { it.sink === sink }

    private suspend fun attachWorldPlayer(session: WorldSession, player: GatewayPlayerPresence) =
        attach(Attachment(player.username.formatPlayerNameForProtocol(), player.displayName, session.worldId, session, attachmentOrder.incrementAndGet()))

    private suspend fun <T> locked(username: String, block: suspend () -> T): T =
        locks.computeIfAbsent(username) { Mutex() }.withLock { block() }

    private suspend fun attach(attachment: Attachment) = locked(attachment.username) {
        val username = attachment.username
        val account = Accounts.find(username) ?: return@locked
        val list = attachmentsByUsername.computeIfAbsent(username) { CopyOnWriteArrayList() }
        val before = locationOf(username)
        val first = list.isEmpty()
        attachment.bot = account.bot
        val replaced = list.removeIf { it.sink === attachment.sink }
        list += attachment
        if (!replaced) countOnline(attachment, +1)
        if (first) registerFriendWatcher(username, account.social.friends)
        attachment.sink.deliver(listOf(username), snapshotOf(account))
        if (attachment.worldId != null) ClanService.onLogin(account)
        if (first) rejoinFriendsChat(account)
        if (locationOf(username) != before) locationChanged(username, account)
    }

    private suspend fun detach(username: String, matches: (Attachment) -> Boolean) = locked(username) {
        val list = attachmentsByUsername[username] ?: return@locked
        val before = locationOf(username)
        val removed = list.filter(matches)
        if (removed.isEmpty()) return@locked
        list.removeAll(removed)
        removed.forEach { countOnline(it, -1) }
        val account = Accounts.find(username)
        if (list.isNotEmpty()) {
            if (locationOf(username) != before) locationChanged(username, account)
            return@locked
        }
        attachmentsByUsername.remove(username)
        notifyWatchers(username, account)
        if (account != null) deregisterFriendWatcher(username, account.social.friends)
        leaveFriendsChat(username, forget = false)
        ClanService.onLogout(username)
    }

    private suspend fun locationChanged(username: String, account: Account?) {
        notifyWatchers(username, account)
        refreshRoomOf(username)
    }

    private fun countOnline(attachment: Attachment, delta: Int) {
        if (attachment.sink !is WorldSession || attachment.bot) return
        val world = Lobby.worldList.get(attachment.sink.worldId) ?: return
        world.playersOnline = maxOf(0, world.playersOnline + delta)
    }

    private fun attachmentsOf(username: String): List<Attachment> = attachmentsByUsername[username].orEmpty()

    private fun isOnline(username: String): Boolean = attachmentsOf(username).isNotEmpty()

    private fun worldTextFor(worldId: Int): String =
        if (worldId > 1100) "Lobby ${worldId - 1100}" else "World $worldId"

    private fun locationOf(username: String): Location? {
        val located = attachmentsOf(username).filter { it.worldId != null }
        val best = located.filter { it.worldId != LOBBY_WORLD_ID }.maxByOrNull { it.order }
            ?: located.maxByOrNull { it.order }
            ?: return null
        val worldId = best.worldId ?: return null
        return Location(worldId, worldTextFor(worldId))
    }

    private fun onLobby(username: String): Boolean = attachmentsOf(username).any { it.sink is LobbySink }

    private fun isOnWorld(username: String, session: WorldSession): Boolean =
        attachmentsOf(username.formatPlayerNameForProtocol()).any { it.sink === session }

    private fun worldsOf(username: String): List<WorldSession> =
        attachmentsOf(username).mapNotNull { it.sink as? WorldSession }.distinct()

    private suspend fun emit(usernames: Collection<String>, event: SocialEvent) {
        val bySink = LinkedHashMap<SocialSink, LinkedHashSet<String>>()
        for (username in usernames) {
            for (attachment in attachmentsOf(username)) bySink.getOrPut(attachment.sink) { LinkedHashSet() }.add(username)
        }
        for ((sink, names) in bySink) {
            try {
                sink.deliver(names.toList(), event)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logWarn("SocialGateway: failed delivering ${event::class.simpleName} to $names", e)
            }
        }
    }

    private suspend fun emit(username: String, event: SocialEvent) = emit(listOf(username), event)

    private suspend fun notice(username: String, kind: NoticeKind, text: String) = emit(username, SocialEvent.Notice(kind, text))

    // ─── Friends ─────────────────────────────────────────────────────

    private fun registerFriendWatcher(username: String, friends: Collection<String>) {
        for (friend in friends) {
            friendWatchers.computeIfAbsent(friend) { ConcurrentHashMap.newKeySet() }.add(username)
        }
    }

    private fun deregisterFriendWatcher(username: String, friends: Collection<String>) {
        for (friend in friends) {
            val watchers = friendWatchers[friend] ?: continue
            watchers.remove(username)
            if (watchers.isEmpty()) friendWatchers.remove(friend, watchers)
        }
    }

    private fun friendEntryFor(viewer: Account, friend: Account): FriendEntry {
        val location = if (viewer.onlineTo(friend)) locationOf(friend.username) else null
        return FriendEntry(
            username = friend.username,
            displayName = friend.displayName,
            previousDisplayName = friend.prevDisplayName,
            worldId = location?.worldId ?: 0,
            worldName = location?.worldName ?: "",
            rank = viewer.social.friendsChat.getRank(friend.username),
        )
    }

    private suspend fun notifyWatchers(username: String, subject: Account?) {
        val watchers = friendWatchers[username]?.toSet().orEmpty()
        if (watchers.isEmpty()) return
        val account = subject ?: Accounts.find(username) ?: return
        for (watcher in Accounts.findByUsernames(watchers)) {
            if (!watcher.social.friends.contains(username)) continue
            emit(watcher.username, SocialEvent.FriendUpdated(friendEntryFor(watcher, account)))
        }
    }

    private fun ignoresOf(account: Account, related: Map<String, Account>): List<IgnoredPlayer> =
        account.social.ignores.map { IgnoredPlayer(it, related[it]?.displayName ?: it.formatPlayerNameForDisplay()) }

    private suspend fun snapshotOf(account: Account): SocialEvent.Snapshot {
        val related = Accounts.findByUsernames(account.social.friends + account.social.ignores).associateBy { it.username }
        val friends = account.social.friends.mapNotNull { related[it] }.map { friendEntryFor(account, it) }
        return SocialEvent.Snapshot(
            friends = friends,
            ignores = ignoresOf(account, related),
            privateStatus = account.social.status.toInt(),
            friendsChat = roomStateOf(account.username),
        )
    }

    private suspend fun addFriend(username: String, name: String) {
        val from = Accounts.find(username) ?: return
        val target = Accounts.findByDisplayName(name) ?: return notice(username, NoticeKind.FRIEND, "Player not found.")
        if (from.social.friends.add(target.username)) Accounts.save(from)
        registerFriendWatcher(username, listOf(target.username))
        notifyWatchers(username, from)
        emit(username, SocialEvent.FriendUpdated(friendEntryFor(from, target)))
    }

    private suspend fun removeFriend(username: String, name: String) {
        val from = Accounts.find(username) ?: return
        val target = Accounts.findByDisplayName(name) ?: return
        if (from.social.friends.remove(target.username)) Accounts.save(from)
        deregisterFriendWatcher(username, listOf(target.username))
        notifyWatchers(username, from)
        emit(username, SocialEvent.FriendRemoved(target.username))
    }

    private suspend fun addIgnore(username: String, name: String, untilLogout: Boolean) {
        val from = Accounts.find(username) ?: return
        val target = Accounts.findByDisplayName(name) ?: Accounts.find(name)
            ?: return notice(username, NoticeKind.IGNORE, "Player not found.")
        from.social.ignores.add(target.username)
        if (untilLogout) {
            val temporary = from.social.tillLogoutIgnores ?: mutableSetOf<String>().also { from.social.tillLogoutIgnores = it }
            temporary.add(target.username)
        }
        Accounts.save(from)
        emitIgnores(from)
    }

    private suspend fun removeIgnore(username: String, name: String) {
        val from = Accounts.find(username) ?: return
        val target = Accounts.findByDisplayName(name) ?: Accounts.find(name) ?: return
        from.social.ignores.remove(target.username)
        from.social.tillLogoutIgnores?.remove(target.username)
        Accounts.save(from)
        emitIgnores(from)
    }

    private suspend fun emitIgnores(account: Account) {
        val related = Accounts.findByUsernames(account.social.ignores).associateBy { it.username }
        emit(account.username, SocialEvent.IgnoresChanged(ignoresOf(account, related)))
    }

    private suspend fun setPrivateStatus(username: String, status: Int) {
        val account = Accounts.find(username) ?: return
        account.social.status = status.toByte()
        Accounts.save(account)
        emit(username, SocialEvent.PrivateStatusChanged(status))
        notifyWatchers(username, account)
    }

    // ─── Friends chat ────────────────────────────────────────────────

    private fun fcRankOf(owner: Account, member: Account): FriendsChatRank = when {
        member.hasRights(Rights.ADMIN) -> FriendsChatRank.JMOD
        member.username == owner.username -> FriendsChatRank.OWNER
        else -> owner.social.friendsChat.getRank(member.username)
    }

    private fun rankAllowed(rank: FriendsChatRank, required: FriendsChatRank): Boolean =
        required.id <= FriendsChatRank.UNRANKED.id || rank.id >= required.id

    private suspend fun stateOf(owner: Account, room: FcRoom): FriendsChatState {
        val members = Accounts.findByUsernames(room.members).map { member ->
            val location = locationOf(member.username)
            FriendsChatMember(
                username = member.username,
                displayName = member.displayName,
                worldId = location?.worldId ?: 0,
                worldName = location?.worldName ?: LISTENER_WORLD_NAME,
                rank = fcRankOf(owner, member),
            )
        }
        return FriendsChatState(
            ownerUsername = owner.username,
            ownerDisplayName = owner.displayName,
            chatName = owner.social.friendsChat.name ?: owner.username,
            kickRank = owner.social.friendsChat.rankToKick,
            members = members,
        )
    }

    private suspend fun roomStateOf(username: String): FriendsChatState? {
        val ownerUsername = fcOwnerByMember[username] ?: return null
        val room = fcRoomsByOwner[ownerUsername] ?: return null
        val owner = Accounts.find(ownerUsername) ?: return null
        return stateOf(owner, room)
    }

    private suspend fun broadcastRoom(owner: Account, room: FcRoom) =
        emit(room.members.toList(), SocialEvent.FriendsChatChanged(stateOf(owner, room)))

    private suspend fun refreshRoomOf(username: String) {
        val ownerUsername = fcOwnerByMember[username] ?: return
        val room = fcRoomsByOwner[ownerUsername] ?: return
        val owner = Accounts.find(ownerUsername) ?: return
        broadcastRoom(owner, room)
    }

    private suspend fun rejoinFriendsChat(account: Account) {
        val saved = account.social.currentFriendsChat?.takeIf { it.isNotBlank() } ?: return
        joinFriendsChat(account.username, saved)
    }

    private suspend fun forgetCurrentFriendsChat(username: String) {
        val account = Accounts.find(username) ?: return
        if (account.social.currentFriendsChat == null) return
        account.social.currentFriendsChat = null
        Accounts.save(account)
    }

    private suspend fun refuseJoin(username: String, text: String) {
        notice(username, NoticeKind.FRIENDS_CHAT, text)
        emit(username, SocialEvent.FriendsChatChanged(null))
    }

    private suspend fun joinFriendsChat(username: String, ownerName: String) {
        val from = Accounts.find(username) ?: return
        if (from.bot) {
            forgetCurrentFriendsChat(username)
            emit(username, SocialEvent.FriendsChatChanged(null))
            return
        }
        val owner = Accounts.findByDisplayName(ownerName) ?: Accounts.find(ownerName)
            ?: return refuseJoin(username, "Unable to find that friends chat.")
        if (owner.social.friendsChat.name == null)
            return refuseJoin(username, "The friends chat you are trying to join is not set up yet.")
        if (owner.social.ignores.contains(username))
            return refuseJoin(username, "You are not allowed to join this user's friends chat channel.")
        if (!rankAllowed(fcRankOf(owner, from), owner.social.friendsChat.rankToEnter))
            return refuseJoin(username, "You are not allowed to join that friends chat.")
        val existing = fcRoomsByOwner[owner.username]
        if (existing != null && username !in existing.members && existing.members.size >= FRIENDS_CHAT_CAPACITY)
            return refuseJoin(username, "The channel you tried to join is currently full.")
        val banExpiry = existing?.tempBans?.get(username)
        if (banExpiry != null) {
            if (System.currentTimeMillis() < banExpiry)
                return refuseJoin(username, "You have been temporarily banned from this friends chat.")
            existing.tempBans.remove(username)
        }
        if (from.social.currentFriendsChat != owner.displayName) {
            from.social.currentFriendsChat = owner.displayName
            Accounts.save(from)
        }
        val previousOwner = fcOwnerByMember[username]
        if (previousOwner != null && previousOwner != owner.username) leaveRoom(username, previousOwner)
        val room = fcRoomsByOwner.computeIfAbsent(owner.username) { FcRoom(it) }
        room.members.add(username)
        fcOwnerByMember[username] = owner.username
        broadcastRoom(owner, room)
    }

    private suspend fun leaveFriendsChat(username: String, forget: Boolean) {
        if (forget) forgetCurrentFriendsChat(username)
        val ownerUsername = fcOwnerByMember.remove(username)
        if (ownerUsername != null) leaveRoom(username, ownerUsername)
        if (forget) emit(username, SocialEvent.FriendsChatChanged(null))
    }

    private suspend fun leaveRoom(username: String, ownerUsername: String) {
        val room = fcRoomsByOwner[ownerUsername] ?: return
        room.members.remove(username)
        if (room.members.isEmpty()) {
            fcRoomsByOwner.remove(ownerUsername, room)
            return
        }
        val owner = Accounts.find(ownerUsername) ?: return
        broadcastRoom(owner, room)
    }

    private suspend fun kickFromFriendsChat(username: String, name: String) {
        val target = Accounts.findByDisplayName(name) ?: Accounts.find(name)
            ?: return notice(username, NoticeKind.FRIENDS_CHAT, "Target player does not exist.")
        if (target.username == username) return notice(username, NoticeKind.FRIENDS_CHAT, "You can't kick yourself!")
        val ownerUsername = fcOwnerByMember[username]
            ?: return notice(username, NoticeKind.FRIENDS_CHAT, "You are not currently connected to a friend's chat.")
        val room = fcRoomsByOwner[ownerUsername] ?: return
        if (target.username !in room.members) return notice(username, NoticeKind.FRIENDS_CHAT, "Target player does not exist.")
        val owner = Accounts.find(ownerUsername) ?: return
        val kicker = Accounts.find(username) ?: return
        val kickerRank = fcRankOf(owner, kicker)
        val targetRank = fcRankOf(owner, target)
        if (targetRank.id >= FriendsChatRank.OWNER.id)
            return notice(username, NoticeKind.FRIENDS_CHAT, "You cannot kick the owner of a channel.")
        if (!rankAllowed(kickerRank, owner.social.friendsChat.rankToKick))
            return notice(username, NoticeKind.FRIENDS_CHAT, "You do not have permission to kick from this friends chat.")
        if (username != owner.username && targetRank.id >= kickerRank.id)
            return notice(username, NoticeKind.FRIENDS_CHAT, "You cannot kick that user.")
        room.tempBans[target.username] = System.currentTimeMillis() + KICK_BAN_MS
        fcOwnerByMember.remove(target.username, ownerUsername)
        forgetCurrentFriendsChat(target.username)
        emit(target.username, SocialEvent.FriendsChatChanged(null))
        leaveRoom(target.username, ownerUsername)
    }

    private suspend fun setFriendsChatRank(username: String, name: String, rank: FriendsChatRank) {
        val owner = Accounts.find(username) ?: return
        val target = Accounts.findByDisplayName(name) ?: Accounts.find(name) ?: return
        owner.social.friendsChat.setRank(target.username, rank.takeIf { it != FriendsChatRank.UNRANKED })
        Accounts.save(owner)
        fcRoomsByOwner[username]?.let { broadcastRoom(owner, it) }
        if (target.username in owner.social.friends) emit(username, SocialEvent.FriendUpdated(friendEntryFor(owner, target)))
    }

    private suspend fun updateFriendsChatSettings(username: String, action: SocialAction.UpdateFriendsChatSettings) {
        val account = Accounts.find(username) ?: return
        with(account.social.friendsChat) {
            name = action.name
            rankToEnter = action.rankToEnter
            rankToSpeak = action.rankToSpeak
            rankToKick = action.rankToKick
            rankToLS = action.rankToLootShare
        }
        Accounts.save(account)
        val room = fcRoomsByOwner[username] ?: return
        if (action.name == null) dissolveRoom(room) else broadcastRoom(account, room)
    }

    private suspend fun dissolveRoom(room: FcRoom) {
        fcRoomsByOwner.remove(room.ownerUsername, room)
        for (member in room.members.toList()) {
            fcOwnerByMember.remove(member, room.ownerUsername)
            forgetCurrentFriendsChat(member)
            emit(member, SocialEvent.FriendsChatChanged(null))
            notice(member, NoticeKind.FRIENDS_CHAT, "The channel has been disabled.")
        }
        room.members.clear()
    }

    private suspend fun sayInOwnFriendsChat(username: String, message: ChatMessage) {
        val ownerUsername = fcOwnerByMember[username]
            ?: return notice(username, NoticeKind.FRIENDS_CHAT, "You are not currently connected to a friend's chat.")
        speakInFriendsChat(username, ownerUsername, message, requireMembership = true)
    }

    /** The website's way of saying a line into [ownerUsername]'s channel without being a member of it. */
    suspend fun sayInFriendsChat(fromUsername: String, ownerUsername: String, message: String) =
        speakInFriendsChat(
            fromUsername.formatPlayerNameForProtocol(),
            ownerUsername.formatPlayerNameForProtocol(),
            PublicChatMessage(message),
            requireMembership = false,
        )

    private suspend fun speakInFriendsChat(fromUsername: String, ownerUsername: String, message: ChatMessage, requireMembership: Boolean) {
        val room = fcRoomsByOwner[ownerUsername] ?: return
        if (requireMembership && fromUsername !in room.members) return
        val owner = Accounts.find(ownerUsername) ?: return
        val from = Accounts.find(fromUsername) ?: return
        if (from.checkMuted()) return notice(fromUsername, NoticeKind.FRIENDS_CHAT, "You are muted.")
        if (!rankAllowed(fcRankOf(owner, from), owner.social.friendsChat.rankToSpeak))
            return notice(fromUsername, NoticeKind.FRIENDS_CHAT, "You are not allowed to speak in that friends chat.")
        val chatName = owner.social.friendsChat.name ?: owner.username
        emit(room.members.toList(), SocialEvent.FriendsChatLine(chatName, speakerOf(from), message))
        if (message !is QuickChatMessage)
            SocialSubscribers.publish(FriendsChatMessageEvent(owner.displayName, from.displayName, from.rights, message.message))
    }

    private fun speakerOf(account: Account) = Speaker(account.username, account.displayName, account.rights.crown)

    // ─── Private messages ────────────────────────────────────────────

    private suspend fun sendPrivateMessage(username: String, to: String, message: ChatMessage) {
        val from = Accounts.find(username) ?: return
        if (from.checkMuted()) return notice(username, NoticeKind.FRIEND, "You are muted.")
        val target = Accounts.findByDisplayName(to) ?: return notice(username, NoticeKind.FRIEND, "Unable to find $to.")
        if (!isOnline(target.username) || !target.onlineTo(from))
            return notice(username, NoticeKind.FRIEND, "${target.displayName} is offline.")
        emit(username, SocialEvent.PrivateMessageSent(target.displayName, message))
        if (target.social.ignores.contains(from.username)) return
        emit(target.username, SocialEvent.PrivateMessageReceived(speakerOf(from), message))
    }

    // ─── Actions ─────────────────────────────────────────────────────

    suspend fun act(username: String, action: SocialAction) = locked(username) { apply(username, action) }

    private suspend fun apply(username: String, action: SocialAction) {
        when (action) {
            is SocialAction.AddFriend -> addFriend(username, action.name)
            is SocialAction.RemoveFriend -> removeFriend(username, action.name)
            is SocialAction.AddIgnore -> addIgnore(username, action.name, action.untilLogout)
            is SocialAction.RemoveIgnore -> removeIgnore(username, action.name)
            is SocialAction.SetPrivateStatus -> setPrivateStatus(username, action.status)
            is SocialAction.JoinFriendsChat -> joinFriendsChat(username, action.owner)
            is SocialAction.LeaveFriendsChat -> leaveFriendsChat(username, forget = true)
            is SocialAction.KickFromFriendsChat -> kickFromFriendsChat(username, action.name)
            is SocialAction.SetFriendsChatRank -> setFriendsChatRank(username, action.name, action.rank)
            is SocialAction.UpdateFriendsChatSettings -> updateFriendsChatSettings(username, action)
            is SocialAction.SayInFriendsChat -> sayInOwnFriendsChat(username, action.message)
            is SocialAction.SendPrivateMessage -> sendPrivateMessage(username, action.to, action.message)
        }
    }

    // ─── Worlds ──────────────────────────────────────────────────────

    suspend fun handleWorldSocket(socket: DefaultWebSocketServerSession) = coroutineScope {
        var worldSession: WorldSession? = null
        var inbox: Job? = null

        try {
            for (frame in socket.incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                val msg = try {
                    SocialGatewayWireJson.json.decodeFromString(SocialGatewayWireMessage.serializer(), text)
                } catch (e: Exception) {
                    logWarn("SocialGateway: invalid message JSON", e)
                    continue
                }

                if (msg is WorldHello) {
                    val tier = communityWorlds.tierFor(msg.token)
                        ?: return@coroutineScope refuseWorld(socket, msg.world, "Unauthorized")
                    val metadata = msg.world.toWorldMetadata()
                    if (!metadata.isValid)
                        return@coroutineScope refuseWorld(socket, msg.world, "Invalid world registration")
                    val community = tier == CommunityWorlds.Tier.COMMUNITY
                    if (community)
                        communityWorlds.refusalFor(metadata)?.let { return@coroutineScope refuseWorld(socket, msg.world, it) }

                    val worldId = msg.world.worldId
                    val session = WorldSession(worldId, msg.world.worldName, socket, community)
                    if (!attachWorld(session, msg.world))
                        return@coroutineScope refuseWorld(socket, msg.world, "World $worldId is already online")
                    worldSession = session
                    inbox = launch { session.processInbox() }

                    logInfo("SocialGateway: ${tier.name.lowercase()} world connected worldId=${worldId} name=${msg.world.worldName} host=${msg.world.publicHost}:${msg.world.port}")
                    session.send(WorldHelloAck(ok = true, message = null))
                    continue
                }

                val world = worldSession ?: continue
                world.post { handleWorldMessage(world, msg) }
            }
        } catch (_: CancellationException) {
            // ignore
        } catch (e: Exception) {
            Logger.logError("SocialGateway socket error", e)
        } finally {
            worldSession?.let { world ->
                world.closeInbox()
                withContext(NonCancellable) {
                    inbox?.join()
                    detachWorld(world)
                }
            }
        }
    }

    private suspend fun refuseWorld(socket: WebSocketSession, world: GatewayWorldInfo, reason: String) {
        logWarn("SocialGateway: refused world ${world.worldId} (${world.worldName}) at ${world.publicHost}:${world.port}: $reason")
        val resp = WorldHelloAck(ok = false, message = reason)
        socket.send(Frame.Text(SocialGatewayWireJson.json.encodeToString(SocialGatewayWireMessage.serializer(), resp)))
        socket.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, reason))
    }

    internal suspend fun attachWorld(session: WorldSession, info: GatewayWorldInfo): Boolean {
        if (session.community) {
            if (worldsById.putIfAbsent(session.worldId, session) != null) return false
        } else {
            worldsById.put(session.worldId, session)?.let { old ->
                old.markClosed()
                dropAttachmentsOf(old)
            }
        }
        worldInfoById[session.worldId] = info
        val world = World(info.toWorldMetadata())
        world.customCache = servesDifferentCache(info)
        Lobby.worldList.put(world)
        return true
    }

    private fun servesDifferentCache(info: GatewayWorldInfo): Boolean {
        val lobby = Lobby.cacheCrcs
        if (info.cacheCrcs.isEmpty() || lobby.isEmpty()) return false
        val differing = (0 until maxOf(lobby.size, info.cacheCrcs.size))
            .filter { lobby.getOrNull(it) != info.cacheCrcs.getOrNull(it) }
        if (differing.isNotEmpty())
            logInfo("SocialGateway: world ${info.worldId} serves its own cache (indices ${differing.joinToString()} differ)")
        return differing.isNotEmpty()
    }

    internal suspend fun detachWorld(session: WorldSession) {
        session.markClosed()
        if (worldsById.remove(session.worldId, session)) {
            Lobby.worldList.markOffline(session.worldId)
            worldInfoById.remove(session.worldId)
        }
        dropAttachmentsOf(session)
        ClanService.onWorldDetached(session.worldId)
        logInfo("SocialGateway: world disconnected worldId=${session.worldId}")
    }

    private suspend fun dropAttachmentsOf(session: WorldSession) {
        val usernames = attachmentsByUsername.filterValues { list -> list.any { it.sink === session } }.keys.toList()
        for (username in usernames) detach(username) { it.sink === session }
    }

    internal suspend fun handleWorldMessage(session: WorldSession, msg: SocialGatewayWireMessage) {
        if (session.community && !communityMayDeliver(session, msg)) {
            logTrace("SocialGateway: community world ${session.worldId} may not send ${msg::class.simpleName}")
            return
        }
        when (msg) {
            is PlayerOnline -> {
                val claimed = GatewayPlayerPresence(msg.username, msg.displayName, msg.rightsCrown, msg.privateStatus)
                val player = if (session.community) communityWorlds.admitted(session.worldId, claimed) ?: return else claimed
                attachWorldPlayer(session, player)
            }

            is PlayerOffline -> {
                val username = msg.username.formatPlayerNameForProtocol()
                communityWorlds.forget(username, session.worldId)
                detach(username) { it.sink === session }
            }

            is WorldPresenceSnapshot -> {
                val snapshot = if (!session.community) msg
                    else msg.copy(players = msg.players.mapNotNull { communityWorlds.admitted(session.worldId, it) })
                reconcileWorldPresence(session, snapshot)
            }

            is VerifyLoginToken -> sendToWorld(session.worldId, communityWorlds.verify(session.worldId, msg))

            is SocialCommand -> act(msg.username.formatPlayerNameForProtocol(), msg.action)

            is AnnouncementBroadcast -> broadcastAnnouncement(msg)

            is AccountCommand -> handleAccountCommand(session, msg)

            is DisplayNameLookup -> sendToWorld(session.worldId, DisplayNameResult(msg.requestId, Accounts.find(msg.username)?.displayName))

            is ClanCommand -> sendToWorld(session.worldId, ClanService.handle(msg.username, msg.action, msg.requestId))

            is ClanFetch -> sendToWorld(session.worldId, ClanService.fetch(msg.clanName))

            else -> logTrace("SocialGateway ignoring message type=${msg::class.simpleName}")
        }
    }

    /**
     * The world is the authority for who is on it: anybody it lists is attached through it,
     * anybody it does not list is detached. On [PresenceSyncReason.CONNECT] every listed player is
     * re-sent their whole state, because a world that outlived a lobby restart is full of players
     * whose client still shows an empty friend list.
     */
    private suspend fun reconcileWorldPresence(session: WorldSession, msg: WorldPresenceSnapshot) {
        val listed = LinkedHashMap<String, GatewayPlayerPresence>()
        for (player in msg.players) listed[player.username.formatPlayerNameForProtocol()] = player
        val present = attachmentsByUsername.filterValues { list -> list.any { it.sink === session } }.keys.toSet()

        val stale = present.filter { it !in listed }
        for (username in stale) detach(username) { it.sink === session }

        val full = msg.reason == PresenceSyncReason.CONNECT
        for ((username, player) in listed) {
            if (username in present && !full) continue
            attachWorldPlayer(session, player)
        }

        logInfo("SocialGateway: reconciled world=${session.worldId} reason=${msg.reason} online=${listed.size} dropped=${stale.size}")
    }

    private fun communityMayDeliver(session: WorldSession, msg: SocialGatewayWireMessage): Boolean = when (msg) {
        is SocialCommand -> isOnWorld(msg.username, session)
        is ClanCommand -> isOnWorld(msg.username, session)
        is AccountCommand -> false
        is AnnouncementBroadcast -> false
        else -> true
    }

    internal suspend fun sendToWorld(worldId: Int, msg: SocialGatewayWireMessage) {
        val session = worldsById[worldId] ?: return
        try {
            session.send(msg)
        } catch (e: Exception) {
            logWarn("Failed sending message to world=$worldId type=${msg::class.simpleName}", e)
        }
    }

    internal suspend fun broadcastToWorlds(msg: SocialGatewayWireMessage) {
        val text = SocialGatewayWireJson.json.encodeToString(SocialGatewayWireMessage.serializer(), msg)
        for (session in worldsById.values) {
            try {
                session.sendText(text)
            } catch (e: Exception) {
                logWarn("Failed sending message to world=${session.worldId} type=${msg::class.simpleName}", e)
            }
        }
    }

    // ─── Seams for ClanService ───────────────────────────────────────

    internal fun worldIdOf(username: String): Int? = locationOf(username)?.worldId

    internal fun displayNameOf(username: String): String? = attachmentsOf(username).maxByOrNull { it.order }?.displayName

    internal suspend fun sendToLobbyPlayer(username: String, packet: ServerProt): Boolean {
        val sessions = attachmentsOf(username).mapNotNull { (it.sink as? LobbySink)?.session }
        for (session in sessions) {
            try {
                session.send(packet)
            } catch (e: Exception) {
                logWarn("Failed sending packet to lobby user=$username type=${packet::class.simpleName}", e)
            }
        }
        return sessions.isNotEmpty()
    }

    // ─── Accounts ────────────────────────────────────────────────────

    private suspend fun handleAccountCommand(session: WorldSession, msg: AccountCommand) {
        val actorName = msg.username.formatPlayerNameForProtocol()
        if (!isOnWorld(actorName, session)) return
        val actor = Accounts.find(actorName) ?: return
        val target = when (val action = msg.action) {
            is AccountAction.SetFcStatus -> actor.apply { social.fcStatus = action.fcStatus.toByte() }
            is AccountAction.SetRights -> moderated(actor, action.target, Rights.ADMIN)?.takeIf { actor.hasRights(action.rights) }?.apply { rights = action.rights }
            is AccountAction.Ban -> moderated(actor, action.target, Rights.MOD)?.apply { banned = action.until }
            is AccountAction.Mute -> moderated(actor, action.target, Rights.MOD)?.apply { muted = action.until }
        }
        if (target == null) {
            logWarn("SocialGateway: refused ${msg.action::class.simpleName} by $actorName from world ${session.worldId}")
            return
        }
        Accounts.save(target)
        val update = AccountUpdate(target.view())
        for (world in worldsOf(target.username)) sendToWorld(world.worldId, update)
    }

    private suspend fun moderated(actor: Account, targetName: String, required: Rights): Account? {
        if (!actor.hasRights(required)) return null
        val target = Accounts.find(targetName.formatPlayerNameForProtocol()) ?: return null
        return target.takeIf { it.username == actor.username || !it.hasRights(actor.rights) || actor.hasRights(Rights.OWNER) }
    }

    // ─── Announcements ───────────────────────────────────────────────

    private suspend fun deliverAnnouncementToLobby(msg: AnnouncementBroadcast) {
        val recipients: Collection<String> = when (val target = msg.target) {
            is AnnouncementTarget.AllWorlds -> lobbyUsernames()
            is AnnouncementTarget.World -> if (target.worldId == LOBBY_WORLD_ID) lobbyUsernames() else emptyList()
            is AnnouncementTarget.User -> listOf(target.username.formatPlayerNameForProtocol()).filter { onLobby(it) }
            is AnnouncementTarget.FriendsChat ->
                fcRoomsByOwner[target.ownerUsername.formatPlayerNameForProtocol()]?.members?.filter { onLobby(it) }.orEmpty()
        }
        for (username in recipients) sendToLobbyPlayer(username, GameMessage(ChatMessageType.FC_NOTIFICATION, msg.message))
    }

    private fun lobbyUsernames(): List<String> =
        attachmentsByUsername.filterValues { list -> list.any { it.sink is LobbySink } }.keys.toList()

    suspend fun broadcastAnnouncement(msg: AnnouncementBroadcast) {
        deliverAnnouncementToLobby(msg)
        broadcastToWorlds(msg)
    }
}
