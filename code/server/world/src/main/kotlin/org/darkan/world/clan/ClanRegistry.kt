package org.darkan.world.clan

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.darkan.core.Logger.logWarn
import org.darkan.core.clan.ClanAction
import org.darkan.core.clan.ClanChannelState
import org.darkan.core.clan.ClanDecision
import org.darkan.core.clan.ClanPackets
import org.darkan.core.clan.ClanRules
import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.model.Clan
import org.darkan.core.model.ClanMemberData
import org.darkan.core.net.prot.ServerProt
import org.darkan.core.net.prot.packet.ClanSettingsFull
import org.darkan.core.net.prot.packet.VarclanDisable
import org.darkan.core.net.prot.packet.VarclanEnable
import org.darkan.core.social.gateway.ClanChannelUpdate
import org.darkan.core.social.gateway.ClanChatMessage
import org.darkan.core.social.gateway.ClanCommand
import org.darkan.core.social.gateway.ClanCommandResult
import org.darkan.core.social.gateway.ClanDeleted
import org.darkan.core.social.gateway.ClanFetch
import org.darkan.core.social.gateway.ClanMembership
import org.darkan.core.social.gateway.ClanQuickChatMessage
import org.darkan.core.social.gateway.ClanSnapshot
import org.darkan.core.social.gateway.SocialGatewayWireMessage
import org.darkan.world.server.WorldServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * The world's read-only replica of the clan system (`docs/clans.md`).
 *
 * The lobby owns every clan document and every channel; this holds the copies the world needs to
 * draw interfaces and to pre-check permissions, and it is the only way out: a button becomes a
 * typed [ClanAction] on a [ClanCommand], never a forwarded client packet. Nothing here mutates a
 * clan - a change reaches the world as the [ClanSnapshot] the lobby broadcasts afterwards, and the
 * copies are replaced wholesale.
 *
 * A clan the world has never seen is asked for with [ClanFetch] ([fetch]); the copies are dropped
 * whenever the gateway socket changes state ([clear]) so a reconnect starts clean and the lobby's
 * connect-time resync refills them.
 *
 * Threading: gateway events arrive on the social client's coroutine, so everything that touches a
 * player is handed to the world thread with [WorldTasks] (the `WorldServer.onWorldThread`
 * pattern). Outgoing messages go the other way, off the world thread, through [transport].
 */
object ClanRegistry {

    /**
     * How long a [ClanCommand] may go unanswered before the player is told, in ticks (~10s). The
     * lobby answers every command with a [ClanCommandResult], so this only fires when the gateway
     * or the lobby is in trouble - it exists so a lost answer cannot leave a dialogue lock set
     * until the player relogs, the way the legacy `ccCreateLock` could.
     */
    const val REQUEST_TIMEOUT_TICKS = 17

    private const val TIMEOUT_MESSAGE = "The clan service did not respond. Please try again."
    private const val UNAVAILABLE_MESSAGE = "The clan service is currently unavailable. Please try again later."

    /** The clan settings interface, which needs script 4295 re-run when its clan changes. */
    private const val CLAN_SETTINGS_INTERFACE = 1096

    private val clans = ConcurrentHashMap<String, Clan>()
    private val channels = ConcurrentHashMap<String, ClanChannelState>()
    private val fetching = ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<Clan?>>>()
    private val pending = ConcurrentHashMap<Int, Request>()
    private val nextRequestId = AtomicInteger(1)

    private class Request(
        val username: String,
        val action: ClanAction,
        val callback: Consumer<ClanCommandResult>?,
    )

    /**
     * Where outgoing gateway messages go. The default is the world's social client; a test swaps
     * in a recorder, which is the only way to assert that a button produced the right action and
     * that a denied pre-check produced nothing at all.
     */
    var transport: suspend (SocialGatewayWireMessage) -> Boolean = { WorldServer.sendToGateway(it) }

    /**
     * The scope outgoing sends run on - never the world thread, because [transport] writes to a
     * socket. A test swaps in an immediate dispatcher so a recorded send is visible by the time
     * the button handler returns.
     */
    var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ─── The replica ─────────────────────────────────────────────────

    /** The world's copy of [name], or null if it has never been fetched. */
    @JvmStatic
    fun clan(name: String?): Clan? = name?.let { clans[it] }

    /**
     * The world's copy of [name], asking the lobby for it when there is none. Returns null on
     * that first call - the legacy `ClansManager.getClan` contract - so callers that need the
     * clan there and then use [fetch] instead.
     */
    @JvmStatic
    fun clanOrFetch(name: String?): Clan? {
        if (name.isNullOrEmpty()) return null
        val known = clans[name]
        if (known != null) return known
        fetch(name, null)
        return null
    }

    /**
     * Hands [callback] the world's copy of [name], fetching it from the lobby first if needed. A
     * clan the lobby says does not exist ([ClanDeleted]) hands back null. The callback runs on the
     * world thread.
     */
    @JvmStatic
    fun fetch(name: String?, callback: Consumer<Clan?>?) {
        if (name.isNullOrEmpty()) {
            callback?.accept(null)
            return
        }
        val known = clans[name]
        if (known != null) {
            callback?.accept(known)
            return
        }
        var first = false
        fetching.compute(name) { _, existing ->
            val waiters = existing ?: CopyOnWriteArrayList<Consumer<Clan?>>().also { first = true }
            if (callback != null) waiters.add(callback)
            waiters
        }
        if (first) dispatch(ClanFetch(name))
    }

    /** The chatter list of [clanName]'s channel as the lobby last broadcast it. */
    @JvmStatic
    fun channel(clanName: String?): ClanChannelState? = clanName?.let { channels[it] }

    /**
     * The `ClanSettingsFull` this world builds for [clan]; display names come from the local
     * players, falling back to the formatted username for anybody not in this world.
     */
    @JvmStatic
    fun settingsFull(clan: Clan, main: Boolean): ClanSettingsFull =
        ClanPackets.settingsFull(clan, main, 0, ::displayNameOf)

    /**
     * The clan's members in the order [settingsFull] lists them, which is the order the rows of
     * interface 1096 are in - a slot id from that interface indexes this, not `Clan.members`'
     * iteration order.
     */
    @JvmStatic
    fun orderedMembers(clan: Clan): List<String> = clan.members.entries
        .sortedWith(compareBy<Map.Entry<String, ClanMemberData>> { it.value.joinDate }.thenBy { it.key })
        .map { it.key }

    /** The banned users in the order [settingsFull] lists them, which the ban list indexes. */
    @JvmStatic
    fun orderedBans(clan: Clan): List<String> = clan.bannedUsers.sorted()

    /**
     * Forgets every copy and every in-flight request. Called on each gateway connection change:
     * a world that lost the socket must not keep drawing interfaces from state the lobby may have
     * changed while it was away, and the lobby's connect-time resync refills the replica.
     */
    fun clear() {
        clans.clear()
        channels.clear()
        pending.clear()
        val orphaned = fetching.keys.toList().mapNotNull { fetching.remove(it) }.flatten()
        if (orphaned.isEmpty()) return
        // A waiter that never hears back is a dialogue that never closes, so they are answered
        // with a null - on the world thread, because that is where they touch players.
        WorldTasks.schedule(0) { orphaned.forEach { runCatching { it.accept(null) } } }
    }

    // ─── Outgoing ────────────────────────────────────────────────────

    /** [send] without a callback. */
    @JvmStatic
    fun send(player: Player, action: ClanAction): Boolean = send(player, action, null)

    /**
     * Asks the lobby for [action] on [player]'s behalf.
     *
     * [ClanRules] is checked here first so a player gets the denial straight away and nothing
     * hits the wire; the lobby checks the same rules again against its own state and is the one
     * that counts. [callback] runs on the world thread when the [ClanCommandResult] lands - the
     * result's `message`, if any, has already been shown to the player.
     *
     * @return false if the local pre-check refused, in which case nothing was sent.
     */
    @JvmStatic
    fun send(player: Player, action: ClanAction, callback: Consumer<ClanCommandResult>?): Boolean {
        val decision = precheck(player, action)
        if (decision is ClanDecision.Denied) {
            player.sendMessage(decision.message)
            return false
        }
        val username = player.username
        val requestId = nextRequestId.getAndIncrement()
        pending[requestId] = Request(username, action, callback)
        WorldTasks.schedule(REQUEST_TIMEOUT_TICKS) { fail(requestId, TIMEOUT_MESSAGE) }
        dispatch(ClanCommand(username = username, requestId = requestId, action = action)) {
            fail(requestId, UNAVAILABLE_MESSAGE)
        }
        return true
    }

    /**
     * Answers a command the lobby never will - the gateway refused the write, or the timeout ran
     * out - with a failed result, so a caller holding a dialogue lock always gets its callback and
     * cannot leave the player locked out until they relog.
     */
    private fun fail(requestId: Int, message: String) {
        val request = pending.remove(requestId) ?: return
        deliver(
            ClanCommandResult(
                username = request.username,
                requestId = requestId,
                action = request.action.serialName,
                ok = false,
                message = message,
            ),
            request,
        )
    }

    /**
     * The local verdict on [action] for [player]. Rules that only need the player's own social
     * fields (a clan name, whether they are already in a clan) are decided here; rules that need
     * a clan the replica has not got yet are left to the lobby rather than guessed at, so a cold
     * replica never blocks a legitimate click.
     */
    fun precheck(player: Player, action: ClanAction): ClanDecision {
        val actor = player.username
        val social = player.social
        val own = clan(social.clanName)
        return when (action) {
            is ClanAction.CheckName -> ClanRules.checkName(social.clanName, action.name)
            is ClanAction.Create -> ClanRules.create(social.clanName, action.name)
            is ClanAction.Leave -> own?.let { ClanRules.leave(it, actor) }
                ?: if (social.clanName == null) ClanDecision.Denied("You aren't in a clan.") else ClanDecision.Allowed
            is ClanAction.AcceptInvite -> clan(action.clanName)
                ?.let { ClanRules.acceptInvite(it, actor, social.clanName, action.inviter) }
                ?: ClanDecision.Allowed
            is ClanAction.RemoveMember -> own?.let { ClanRules.removeMember(it, actor, action.target) }
                ?: ClanDecision.Allowed
            is ClanAction.UpdateMember -> own?.let { ClanRules.updateMember(it, actor, action) }
                ?: ClanDecision.Allowed
            is ClanAction.Ban -> own?.let { ClanRules.ban(it, actor, action.target) } ?: ClanDecision.Allowed
            is ClanAction.Unban -> own?.let { ClanRules.unban(it, actor, action.target) } ?: ClanDecision.Allowed
            is ClanAction.UpdateSettings -> own?.let { ClanRules.updateSettings(it, actor, action.settings) }
                ?: ClanDecision.Allowed
            is ClanAction.SetChannelRanks -> own?.let {
                ClanRules.setChannelRanks(it, actor, action.talkRank, action.kickRank)
            } ?: ClanDecision.Allowed
            is ClanAction.SetVar -> own?.let { ClanRules.setVar(it, actor) } ?: ClanDecision.Allowed
            // A clan name with no copy yet is a cold replica, not "you are not in a clan".
            is ClanAction.JoinChannel ->
                if (own == null && social.clanName != null) ClanDecision.Allowed
                else ClanRules.joinChannel(own, actor)
            is ClanAction.LeaveChannel ->
                if (own == null && social.clanName != null) ClanDecision.Allowed
                else ClanRules.leaveChannel(own, channel(social.clanName), actor)
            is ClanAction.JoinGuestChannel -> clan(action.clanName)
                ?.let { ClanRules.joinGuestChannel(it, actor, tempBanned = false) }
                ?: ClanDecision.Allowed
            is ClanAction.LeaveGuestChannel ->
                if (social.guestedClanChat == null) ClanRules.leaveGuestChannel(null, actor)
                else ClanDecision.Allowed
            is ClanAction.KickFromChannel -> own
                ?.let { ClanRules.kickFromChannel(it, channel(it.name), actor, action.target) }
                ?: ClanDecision.Allowed
            is ClanAction.Chat -> chatDecision(player, action.guest)
            is ClanAction.QuickChat -> chatDecision(player, action.guest)
        }
    }

    private fun chatDecision(player: Player, guest: Boolean): ClanDecision {
        val social = player.social
        val name = if (guest) social.guestedClanChat else social.clanName
        val clan = clan(name) ?: return ClanDecision.Allowed
        return ClanRules.chat(clan, channel(clan.name), player.username, player.account.checkMuted())
    }

    private fun dispatch(msg: SocialGatewayWireMessage, onDropped: () -> Unit = {}) {
        scope.launch {
            val delivered = try {
                transport(msg)
            } catch (e: Exception) {
                logWarn("ClanRegistry failed to send ${msg::class.simpleName}", e)
                false
            }
            if (!delivered) onDropped()
        }
    }

    // ─── Incoming ────────────────────────────────────────────────────

    /**
     * Consumes one gateway message if it is a clan event.
     *
     * @return true when the message was a clan event and has been handled, so `WorldServer` can
     *         fall through to its own dispatch for everything else.
     */
    fun handle(msg: SocialGatewayWireMessage): Boolean {
        when (msg) {
            is ClanSnapshot -> onSnapshot(msg.clan)
            is ClanDeleted -> onDeleted(msg.clanName)
            is ClanChannelUpdate -> onChannelUpdate(msg.state)
            is ClanMembership -> onMembership(msg)
            is ClanChatMessage -> onChannelLine(msg.clanName) { guest ->
                ClanPackets.chatMessage(guest, msg.rights, msg.sender, msg.message.message)
            }
            is ClanQuickChatMessage -> onChannelLine(msg.clanName) { guest ->
                ClanPackets.quickChatMessage(guest, msg.rights, msg.sender, msg.message)
            }
            is ClanCommandResult -> onResult(msg)
            else -> return false
        }
        return true
    }

    /**
     * The lobby has changed [clan]. The copy is replaced wholesale and every local player who can
     * see it gets a fresh `ClanSettingsFull`: members its own (`main = true`), guests of the
     * channel the read-only form.
     *
     * Members also get the clan's vars re-sent and their appearance regenerated, because the motif
     * colours ride on the appearance block, and a member sitting in the settings interface gets
     * script 4295 re-run to redraw it - all three as `syncClanFromLobby` did.
     */
    private fun onSnapshot(clan: Clan) {
        val name = clan.name
        if (name.isEmpty()) {
            logWarn("ClanRegistry received a ClanSnapshot with no clan name")
            return
        }
        clans[name] = clan
        val waiters = fetching.remove(name)
        WorldTasks.schedule(0) {
            waiters?.forEach { runCatching { it.accept(clan) } }
            val members = ClanPackets.settingsFull(clan, main = true, displayName = ::displayNameOf)
            val guestView = ClanPackets.settingsFull(clan, main = false, displayName = ::displayNameOf)
            // syncClanFromLobby re-sent the values only; VarclanEnable belongs to a channel join.
            val vars = ClanPackets.clanVarPackets(clan).filter { it !is VarclanEnable }
            for (username in clan.members.keys + clan.leaderUsername) {
                val player = localPlayer(username) ?: continue
                player.session.queuePacket(members)
                for (varPacket in vars) player.session.queuePacket(varPacket)
                player.appearance.generateAppearanceData()
                if (player.interfaceManager.topOpen(CLAN_SETTINGS_INTERFACE))
                    WorldTasks.delay(0) { player.packets.sendRunScript(4295) }
            }
            for (player in localPlayers()) {
                if (player.social.guestedClanChat != name) continue
                player.session.queuePacket(guestView)
            }
        }
    }

    /** The clan is gone; drop the copies and hand any waiting [fetch] a null. */
    private fun onDeleted(clanName: String) {
        clans.remove(clanName)
        channels.remove(clanName)
        val waiters = fetching.remove(clanName) ?: return
        WorldTasks.schedule(0) { waiters.forEach { runCatching { it.accept(null) } } }
    }

    /**
     * The chatter list of one channel changed. The client is only ever sent whole channels, so
     * the diff is per player and only decides which of the three things they get: a join is the
     * channel plus `VarclanEnable` and the clan's vars, a leave is the one-byte form plus
     * `VarclanDisable`, and staying in is a full resend.
     */
    private fun onChannelUpdate(state: ClanChannelState) {
        val previous = channels.put(state.clanName, state)
        val affected = LinkedHashSet<String>()
        previous?.chatters?.forEach { affected.add(it.username) }
        state.chatters.forEach { affected.add(it.username) }
        val clan = clanOrFetch(state.clanName)
        WorldTasks.schedule(0) {
            for (username in affected) {
                val player = localPlayer(username) ?: continue
                val was = previous != null && username in previous
                val now = username in state
                val main = player.social.clanName == state.clanName
                when {
                    !was && now -> {
                        player.session.queuePacket(ClanPackets.channelFull(state, main))
                        if (clan != null) {
                            for (varPacket in ClanPackets.clanVarPackets(clan)) player.session.queuePacket(varPacket)
                        } else {
                            player.session.queuePacket(VarclanEnable())
                        }
                    }
                    was && !now -> {
                        player.session.queuePacket(ClanPackets.channelLeft(main))
                        player.session.queuePacket(VarclanDisable())
                    }
                    now -> player.session.queuePacket(ClanPackets.channelFull(state, main))
                }
            }
        }
    }

    /**
     * The lobby's word on one player's clan fields. They are written straight into the account's
     * [org.darkan.core.model.Social] so the world's own account saves cannot clobber them; being
     * told of no clan is also what sends the one-byte `ClanSettingsFull` that tells the client it
     * left, and drops the motif from the appearance.
     */
    private fun onMembership(msg: ClanMembership) {
        if (msg.clanName != null) clanOrFetch(msg.clanName)
        WorldServer.onWorldThread(msg.username) { player ->
            val social = player.social
            val left = social.clanName != null && msg.clanName == null
            social.clanName = msg.clanName
            social.connectedToClan = msg.inChannel
            social.guestedClanChat = msg.guestChannel
            if (msg.clanName == null) {
                player.session.queuePacket(ClanPackets.settingsLeft(main = true))
                if (left) player.appearance.generateAppearanceData()
            }
        }
    }

    /**
     * One line said in [clanName]'s channel, delivered to every chatter this world holds.
     * [line] is asked for the recipient's own view: affined for a member of that clan, listened
     * for a guest.
     */
    private fun onChannelLine(clanName: String, line: (guest: Boolean) -> ServerProt) {
        val state = channels[clanName] ?: return
        WorldTasks.schedule(0) {
            for (chatter in state.chatters) {
                val player = localPlayer(chatter.username) ?: continue
                player.session.queuePacket(line(player.social.clanName != clanName))
            }
        }
    }

    /** The outcome of one command: show the lobby's message, then run whatever asked for it. */
    private fun onResult(result: ClanCommandResult) = deliver(result, pending.remove(result.requestId))

    private fun deliver(result: ClanCommandResult, request: Request?) {
        WorldTasks.schedule(0) {
            val player = localPlayer(result.username)
            val message = result.message
            if (player != null && !message.isNullOrBlank()) player.sendMessage(message)
            request?.callback?.let { runCatching { it.accept(result) } }
        }
    }

    // ─── Players ─────────────────────────────────────────────────────

    private fun displayNameOf(username: String): String =
        World.getPlayerByUsername(username)?.displayName ?: username.formatPlayerNameForDisplay()

    private fun localPlayer(username: String): Player? {
        val player = World.getPlayerByUsername(username) ?: return null
        if (!player.hasStarted() || player.hasFinished()) return null
        return player
    }

    private fun localPlayers(): List<Player> = World.players.filterNotNull()
        .filter { it.hasStarted() && !it.hasFinished() }
}
