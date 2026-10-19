package org.darkan.lobby.harness

import org.darkan.core.clan.ClanAction
import org.darkan.core.clan.ClanVarKind
import org.darkan.core.clan.InMemoryClanStore
import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.model.Account
import org.darkan.core.model.Clan
import org.darkan.core.model.Rights
import org.darkan.core.mongo.collections.AccountStore
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.mongo.collections.Clans
import org.darkan.core.net.prot.Codec
import org.darkan.core.social.SocialAction
import org.darkan.core.social.SocialEvent
import org.darkan.core.social.gateway.ClanCommand
import org.darkan.core.social.gateway.GatewayPlayerPresence
import org.darkan.core.social.gateway.GatewayWorldInfo
import org.darkan.core.social.gateway.PlayerOffline
import org.darkan.core.social.gateway.PlayerOnline
import org.darkan.core.social.gateway.PresenceSyncReason
import org.darkan.core.social.gateway.SocialCommand
import org.darkan.core.social.gateway.SocialGatewayWireJson
import org.darkan.core.social.gateway.SocialNotification
import org.darkan.core.social.gateway.SocialGatewayWireMessage
import org.darkan.core.social.gateway.WorldPresenceSnapshot
import org.darkan.lobby.social.ClanService
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.social.SocialSink
import org.darkan.lobby.social.WorldSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Drives [SocialGateway] with no Mongo, no socket and no client, so the lobby's social behaviour
 * can be asserted packet by packet.
 *
 * Three seams make that possible and nothing else about the gateway is changed. [InMemoryAccounts]
 * is installed as [Accounts.store], so account reads and writes go to a map; an
 * [InMemoryClanStore] is installed as [Clans.store] the same way; and [FakeWorld] builds a
 * [WorldSession] over a lambda instead of a WebSocket and feeds it to the gateway's
 * `handleWorldMessage`, which is the same entry point the real socket loop uses.
 *
 * The gateway is a process-wide `object`, so a test that inherited the previous test's presence,
 * channels and watcher index would pass or fail depending on the order JUnit happened to pick.
 * [reset] empties every map it holds, which is what makes each test independent; call it from a
 * `@BeforeTest`. Accounts are also created with a fresh username each time, so nothing survives
 * even by name.
 */
object LobbyHarness {

    /** The gateway never encodes anything in a test, so an empty codec is all a session needs. */
    val codec: Codec = Codec()

    private val ids = AtomicInteger()

    val accounts = InMemoryAccounts()

    private val lobbySessions = ConcurrentHashMap<String, RecordingSession>()

    /** The clan store behind [Clans]; a fresh one per [reset] so no clan outlives its test. */
    var clans = InMemoryClanStore()
        private set

    init {
        Accounts.store = accounts
        Clans.store = clans
        // No cache is loaded under test, so clan vars are typed by their stored value.
        ClanService.varKind = { _, value ->
            when (value) {
                is String -> ClanVarKind.STRING
                is Long -> ClanVarKind.LONG
                else -> ClanVarKind.INT
            }
        }
    }

    /**
     * Empties the gateway and the account store, so a test starts from a lobby with nobody on it.
     *
     * The gateway keeps its state in private maps and has no reset hook - adding one would be
     * test-only API in production code - so they are cleared reflectively. Anything map-shaped is
     * fair game: every one of them is presence, membership or index state that a fresh lobby has
     * empty.
     */
    fun reset() {
        clearMaps(SocialGateway)
        clearMaps(ClanService)
        accounts.clear()
        lobbySessions.clear()
        clans = InMemoryClanStore()
        Clans.store = clans
    }

    private fun clearMaps(owner: Any) {
        for (field in owner.javaClass.declaredFields) {
            field.isAccessible = true
            val value = try {
                field.get(owner)
            } catch (_: Throwable) {
                continue
            }
            if (value is MutableMap<*, *>) value.clear()
        }
    }

    /**
     * A fresh account, saved to the in-memory store. The name is unique per call so parallel or
     * repeated tests cannot collide in the gateway's shared maps.
     */
    fun account(
        name: String,
        rights: Rights = Rights.PLAYER,
        privateStatus: Int = 0,
        friends: Set<String> = emptySet(),
        currentFriendsChat: String? = null,
    ): Account {
        val username = "${name}${ids.incrementAndGet()}".formatPlayerNameForProtocol()
        val account = Account(username, "$username@test.invalid", "hash")
        account.rights = rights
        account.social.status = privateStatus.toByte()
        account.social.friends = friends.toMutableSet()
        account.social.currentFriendsChat = currentFriendsChat
        accounts.put(account)
        return account
    }

    /** Puts [clan] in the store as if it had always been there; the service reads it on first use. */
    suspend fun seedClan(clan: Clan) {
        clans.save(clan)
        for (username in clan.members.keys) {
            val account = accounts.find(username) ?: continue
            account.social.clanName = clan.name
            accounts.put(account)
        }
    }

    /** Makes [owner] the owner of a set-up friends chat called [chatName]. */
    fun openFriendsChat(owner: Account, chatName: String = owner.displayName) {
        owner.social.friendsChat.name = chatName
        accounts.put(owner)
    }

    /** Adds [friend] to [account]'s friends list (one-directional, as the client's list is). */
    fun befriend(account: Account, friend: Account) {
        account.social.friends.add(friend.username)
        accounts.put(account)
    }

    /**
     * Logs [account] into the lobby the way `LobbyPlayer.login` does, and returns the recorder its
     * packets land in.
     */
    suspend fun lobbyLogin(account: Account): RecordingSession {
        val session = RecordingSession(codec)
        lobbySessions[account.username] = session
        SocialGateway.registerLobbyPlayerSession(account, session)
        return session
    }

    suspend fun lobbyLogout(account: Account) {
        val session = lobbySessions.remove(account.username) ?: return
        SocialGateway.unregisterLobbyPlayerSession(account.username, session)
    }

    /** [account] asks for [action], from whichever of their connections. */
    suspend fun act(account: Account, action: SocialAction) {
        SocialGateway.act(account.username, action)
    }

    /** A website-style listener on [account]: no presence, just the events. */
    suspend fun listen(account: Account): RecordingSink {
        val sink = RecordingSink()
        SocialGateway.attachListener(account, sink)
        return sink
    }

    suspend fun stopListening(account: Account, sink: RecordingSink) {
        SocialGateway.detachListener(account.username, sink)
    }

    class RecordingSink : SocialSink {
        private val received = mutableListOf<SocialEvent>()

        override suspend fun deliver(usernames: List<String>, event: SocialEvent) {
            synchronized(received) { received += event }
        }

        val events: List<SocialEvent> get() = synchronized(received) { received.toList() }

        fun clear() = synchronized(received) { received.clear() }
    }

    fun world(worldId: Int, worldName: String = "Test $worldId", community: Boolean = false): FakeWorld =
        FakeWorld(worldId, worldName, community)

    /**
     * A connected world with a recording transport. Messages are handed to the gateway through the
     * same `handleWorldMessage` the real socket loop calls, wrapped in a `coroutineScope` so the
     * branches that launch background work have finished by the time the call returns.
     */
    class FakeWorld(val worldId: Int, val worldName: String, community: Boolean = false) {
        private val received = mutableListOf<SocialGatewayWireMessage>()

        val session = WorldSession(worldId, worldName, community) { text ->
            synchronized(received) {
                received += SocialGatewayWireJson.json.decodeFromString(SocialGatewayWireMessage.serializer(), text)
            }
        }

        val info = GatewayWorldInfo(
            worldId = worldId,
            worldName = worldName,
            host = "127.0.0.1",
            publicHost = "127.0.0.1",
            port = 43595 + worldId,
        )

        /** Completes the handshake, i.e. what the gateway does once a [WorldHello] checks out. */
        suspend fun connect(): FakeWorld {
            SocialGateway.attachWorld(session, info)
            return this
        }

        suspend fun tryConnect(): Boolean = SocialGateway.attachWorld(session, info)

        /** Drops the socket, the way the gateway's `finally` block does. */
        suspend fun disconnect() {
            SocialGateway.detachWorld(session)
        }

        suspend fun deliver(msg: SocialGatewayWireMessage) = SocialGateway.handleWorldMessage(session, msg)

        /** [account] asks for [action] from this world, the way `WorldServer.act` does. */
        suspend fun act(account: Account, action: SocialAction) = deliver(SocialCommand(account.username, action))

        suspend fun playerOnline(account: Account) = deliver(
            PlayerOnline(
                username = account.username,
                displayName = account.displayName,
                rightsCrown = account.rights.crown,
                privateStatus = account.social.status.toInt(),
            )
        )

        suspend fun playerOffline(account: Account) = deliver(PlayerOffline(account.username))

        /** [account] asks for [action] from this world, the way `ClanRegistry.send` does. */
        suspend fun command(account: Account, action: ClanAction, requestId: Int = 1) =
            deliver(ClanCommand(username = account.username, requestId = requestId, action = action))

        /** Every message of type [T] the lobby has pushed at this world, oldest first. */
        inline fun <reified T : SocialGatewayWireMessage> messagesOf(): List<T> = sent().filterIsInstance<T>()

        /** Announces exactly [players] as this world's population. */
        suspend fun presenceSnapshot(reason: PresenceSyncReason, vararg players: Account) = deliver(
            WorldPresenceSnapshot(
                players = players.map {
                    GatewayPlayerPresence(it.username, it.displayName, it.rights.crown, it.social.status.toInt())
                },
                reason = reason,
            )
        )

        /** Everything the lobby has pushed at this world, oldest first. */
        fun sent(): List<SocialGatewayWireMessage> = synchronized(received) { received.toList() }

        /** The [SocialEvent]s the lobby addressed to [account] through this world. */
        fun eventsFor(account: Account): List<SocialEvent> = sent()
            .filterIsInstance<SocialNotification>()
            .filter { account.username in it.usernames }
            .map { it.event }

        fun clear() = synchronized(received) { received.clear() }
    }
}

/** An [AccountStore] over a map, so the gateway's account reads and writes need no database. */
class InMemoryAccounts : AccountStore {
    private val byUsername = ConcurrentHashMap<String, Account>()

    fun put(account: Account) {
        byUsername[account.username] = account
    }

    fun clear() = byUsername.clear()

    override suspend fun find(usernameOrEmail: String): Account? =
        byUsername[usernameOrEmail.formatPlayerNameForProtocol()]
            ?: byUsername.values.firstOrNull { it.email == usernameOrEmail }

    override suspend fun findByDisplayName(displayName: String): Account? {
        val byName = displayName.formatPlayerNameForProtocol()
        val byDisplay = byName.formatPlayerNameForDisplay()
        return byUsername[byName]
            ?: byUsername.values.firstOrNull { it.displayName == displayName || it.displayName == byDisplay }
    }

    override suspend fun findByUsernames(usernames: Collection<String>): List<Account> =
        usernames.mapNotNull { byUsername[it.formatPlayerNameForProtocol()] }

    override suspend fun save(account: Account) {
        byUsername[account.username] = account
    }

    override suspend fun rename(previousUsername: String, account: Account) {
        byUsername.remove(previousUsername)
        byUsername[account.username] = account
    }

    override suspend fun findAll(): List<Account> = byUsername.values.toList()
}
