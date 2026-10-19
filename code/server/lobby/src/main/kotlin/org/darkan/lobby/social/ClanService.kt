package org.darkan.lobby.social

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.darkan.core.Logger.logWarn
import org.darkan.core.clan.ClanAction
import org.darkan.core.clan.ClanChannelState
import org.darkan.core.clan.ClanChatter
import org.darkan.core.clan.ClanDecision
import org.darkan.core.clan.ClanPackets
import org.darkan.core.clan.ClanRules
import org.darkan.core.clan.ClanVarKind
import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.model.Account
import org.darkan.core.model.ChatMessageType
import org.darkan.core.model.Clan
import org.darkan.core.model.ClanMemberData
import org.darkan.core.model.ClanRank
import org.darkan.core.model.ClanSetting
import org.darkan.core.model.NO_CLIENT_ID
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.mongo.collections.Clans
import org.darkan.core.net.prot.ServerProt
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.net.prot.packet.VarclanDisable
import org.darkan.core.net.prot.packet.VarclanEnable
import org.darkan.core.social.ChatMessage
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.gateway.ClanChannelUpdate
import org.darkan.core.social.gateway.ClanChatMessage
import org.darkan.core.social.gateway.ClanCommandResult
import org.darkan.core.social.gateway.ClanDeleted
import org.darkan.core.social.gateway.ClanMembership
import org.darkan.core.social.gateway.ClanQuickChatMessage
import org.darkan.core.social.gateway.ClanSnapshot
import org.darkan.core.social.gateway.SocialGatewayWireMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * The lobby's half of `docs/clans.md`: the one writer of the `clans` collection and the owner of
 * every clan channel.
 *
 * Every intent - a `ClanCommand` from a world or a button on the lobby screen - comes through
 * [handle] as a [ClanAction], is judged by [ClanRules] (the caller's own check is never trusted)
 * and, if allowed, applied under one lock. Every mutation ends the same way: the clan is written
 * back, its [ClanSnapshot] goes to every connected world, [ClanMembership] goes to the world of
 * each player whose `Social` clan fields changed (and those fields are saved here, so the world's
 * own account saves can never clobber them), the channel's [ClanChannelUpdate] goes out if the
 * chatter list moved, and the players sitting on the lobby screen get their client packets from
 * [ClanPackets] exactly as a world builds them from the same state.
 *
 * State: the clan cache (a write-through copy of the store, filled lazily per clan), the
 * channels (chatters and an hourly temp ban per kicked guest), and nothing about sessions - the
 * lobby-screen sessions and the world sockets stay in [SocialGateway], which this object sends
 * through.
 */
object ClanService {

    /** How long a guest kicked from a channel stays out of it. */
    const val TEMP_BAN_MS = 60L * 60L * 1000L

    private const val NOT_IN_CHANNEL = "You are not currently connected to a clan chat."
    private const val NOT_IN_CLAN = "You must be in a clan to do that."

    private val mutex = Mutex()

    private val clansByName = ConcurrentHashMap<String, Clan>()
    private val settingsUpdates = ConcurrentHashMap<String, Int>()
    private val channelsByClan = ConcurrentHashMap<String, Channel>()
    private val tempBansByClan = ConcurrentHashMap<String, MutableMap<String, Long>>()

    /**
     * The stack type of a clan var, which picks its `VarclanSet*` packet. The cache decides when
     * it knows the var; a var the cache does not define is sent by the type of its stored value.
     * A test with no cache loaded installs a value-only resolver here.
     */
    internal var varKind: (id: Int, value: Any) -> ClanVarKind = { id, value ->
        ClanPackets.cacheVarKind(id) ?: kindOf(value)
    }

    /** One clan channel: its chatters in join order and what the worlds were last told. */
    private class Channel(val clanName: String) {
        var updateNum = 0L
        val chatters = LinkedHashMap<String, ClanChatter>()
        var published: ClanChannelState? = null

        fun state(clan: Clan): ClanChannelState = ClanChannelState(
            clanName = clanName,
            updateNum = updateNum,
            talkRank = clan.ccChatRank,
            kickRank = clan.ccKickRank,
            chatters = chatters.values.toList(),
        )

        /** Re-reads each chatter's rank from [clan]; a member promoted or a guest who joined shows it. */
        fun refreshRanks(clan: Clan) {
            for ((username, chatter) in chatters) {
                val rank = ClanRules.rankOf(clan, username)
                if (rank != chatter.rank) chatters[username] = chatter.copy(rank = rank)
            }
        }
    }

    // ─── Public API ──────────────────────────────────────────────────

    /**
     * Judges and applies [action] for [username]. Never throws: a failure is a denied result so
     * every command yields exactly one [ClanCommandResult], echoing [requestId].
     */
    suspend fun handle(username: String, action: ClanAction, requestId: Int = 0): ClanCommandResult {
        val actor = username.formatPlayerNameForProtocol()
        val decision = try {
            mutex.withLock { apply(actor, action) }
        } catch (e: Exception) {
            logWarn("ClanService: ${action.serialName} by $actor failed", e)
            ClanDecision.Denied("Unable to complete that clan action right now.")
        }
        return ClanCommandResult(
            username = actor,
            requestId = requestId,
            action = action.serialName,
            ok = decision.allowed,
            message = (decision as? ClanDecision.Denied)?.message,
        )
    }

    /** The answer to a `ClanFetch`: the clan as a [ClanSnapshot], or [ClanDeleted]. */
    suspend fun fetch(clanName: String): SocialGatewayWireMessage = mutex.withLock {
        clan(clanName)?.let { ClanSnapshot(it) } ?: ClanDeleted(clanName)
    }

    /** The clan [username] is a member of, or null. */
    suspend fun clanOf(username: String): Clan? = mutex.withLock {
        val account = Accounts.find(username.formatPlayerNameForProtocol()) ?: return@withLock null
        memberClan(account)
    }

    /** The current chatter list of [clanName]'s channel, or null when nobody is in it. */
    suspend fun channelState(clanName: String): ClanChannelState? = mutex.withLock {
        val clan = clan(clanName) ?: return@withLock null
        channelsByClan[clanName]?.state(clan)
    }

    // ─── Presence hooks (called by SocialGateway) ────────────────────

    /**
     * A player appeared, on a world or on the lobby screen: their world gets [ClanMembership]
     * and the snapshots of the clans they are in, and they re-join the channel they were in
     * (`connectedToClan`) and the guest channel they were listening to (`guestedClanChat`). A
     * world reconnect runs this for every listed player, so the channel state is re-sent to that
     * world even when the chatter list did not move.
     */
    internal suspend fun onLogin(account: Account) = mutex.withLock {
        val username = account.username
        val worldId = SocialGateway.worldIdOf(username) ?: return@withLock
        val onLobby = worldId == ClanChatter.LOBBY_WORLD_ID
        val clan = memberClan(account)

        if (!onLobby) {
            SocialGateway.sendToWorld(worldId, membershipOf(account))
            if (clan != null) SocialGateway.sendToWorld(worldId, ClanSnapshot(clan))
        } else if (clan != null) {
            SocialGateway.sendToLobbyPlayer(username, memberSettings(clan))
        }

        if (clan != null && account.social.connectedToClan) {
            joinMemberChannel(clan, account, worldId)
            resyncChannel(clan, worldId, username)
        }

        val guestName = account.social.guestedClanChat ?: return@withLock
        val guestClan = clan(guestName)
        val decision = ClanRules.joinGuestChannel(guestClan, username, isTempBanned(guestName, username))
        if (!decision.allowed || guestClan == null) {
            account.social.guestedClanChat = null
            saveMembership(account)
            return@withLock
        }
        if (!onLobby) SocialGateway.sendToWorld(worldId, ClanSnapshot(guestClan))
        joinGuestChannel(guestClan, account, worldId)
        resyncChannel(guestClan, worldId, username)
    }

    /** A player is gone from everywhere: out of the channels, flags kept so the next login rejoins. */
    internal suspend fun onLogout(username: String) = mutex.withLock {
        for (channel in channelsByClan.values.toList()) {
            if (channel.chatters.remove(username) == null) continue
            val clan = clan(channel.clanName) ?: continue
            publishChannelIfChanged(clan)
        }
    }

    /** A world socket dropped: every chatter it held leaves every channel. */
    internal suspend fun onWorldDetached(worldId: Int) = mutex.withLock {
        for (channel in channelsByClan.values.toList()) {
            if (!channel.chatters.values.removeAll { it.worldId == worldId }) continue
            val clan = clan(channel.clanName) ?: continue
            publishChannelIfChanged(clan)
        }
    }

    // ─── Dispatch ────────────────────────────────────────────────────

    private suspend fun apply(actor: String, action: ClanAction): ClanDecision {
        val account = Accounts.find(actor) ?: return ClanDecision.Denied("Unknown player.")
        return when (action) {
            is ClanAction.CheckName -> checkName(account, action.name)
            is ClanAction.Create -> create(account, action.name)
            is ClanAction.Leave -> leave(account)
            is ClanAction.AcceptInvite -> acceptInvite(account, action.clanName, action.inviter.formatPlayerNameForProtocol())
            is ClanAction.RemoveMember -> removeMember(account, action.target.formatPlayerNameForProtocol())
            is ClanAction.UpdateMember -> updateMember(account, action.copy(target = action.target.formatPlayerNameForProtocol()))
            is ClanAction.Ban -> ban(account, action.target.formatPlayerNameForProtocol())
            is ClanAction.Unban -> unban(account, action.target.formatPlayerNameForProtocol())
            is ClanAction.UpdateSettings -> updateSettings(account, action.settings)
            is ClanAction.SetChannelRanks -> setChannelRanks(account, action.talkRank, action.kickRank)
            is ClanAction.SetVar -> setVar(account, action.id, action.value)
            is ClanAction.JoinChannel -> joinChannel(account)
            is ClanAction.LeaveChannel -> leaveChannel(account)
            is ClanAction.JoinGuestChannel -> joinGuestChannel(account, action.clanName)
            is ClanAction.LeaveGuestChannel -> leaveGuestChannel(account)
            is ClanAction.KickFromChannel -> kickFromChannel(account, action.target.formatPlayerNameForProtocol())
            is ClanAction.Chat -> chat(account, action.guest, action.message)
            is ClanAction.QuickChat -> quickChat(account, action.guest, action.message)
        }
    }

    // ─── Names and creation ──────────────────────────────────────────

    private suspend fun checkName(account: Account, name: String): ClanDecision {
        val decision = ClanRules.checkName(account.social.clanName, name)
        if (!decision.allowed) return decision
        if (clan(name) != null) return ClanDecision.Denied("The name $name is already taken. Please choose another.")
        return ClanDecision.Allowed
    }

    private suspend fun create(account: Account, name: String): ClanDecision {
        val decision = checkName(account, name)
        if (!decision.allowed) return decision
        val clan = Clan(name = canonicalName(name), leaderUsername = account.username)
        // A fresh clan starts from the client's defaults (motif, recruiting, home world, guest CC)
        // rather than an empty settings block the interface would render as all zeroes.
        for (setting in ClanSetting.entries) {
            if (setting.id == NO_CLIENT_ID) continue
            val default = setting.defaultValue
            if (default is String && default.isEmpty()) continue
            clan.setSetting(setting, default)
        }
        clan.members[account.username] = ClanMemberData(rank = ClanRank.OWNER)
        account.social.clanName = clan.name
        account.social.connectedToClan = true
        SocialGateway.worldIdOf(account.username)?.let { joinMemberChannel(clan, account, it) }
        commit(clan)
        saveMembership(account)
        return ClanDecision.Allowed
    }

    // ─── Membership ──────────────────────────────────────────────────

    private suspend fun leave(account: Account): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied("You aren't in a clan.")
        val decision = ClanRules.leave(clan, account.username)
        if (!decision.allowed) return decision
        if (account.username == clan.leaderUsername) {
            val successor = ClanRules.successor(clan)
            if (successor == null) {
                deleteClan(clan, account)
                return ClanDecision.Allowed
            }
            clan.leaderUsername = successor
            clan.members[successor]?.rank = ClanRank.OWNER
        }
        dropMember(clan, account)
        commit(clan)
        return ClanDecision.Allowed
    }

    private suspend fun acceptInvite(account: Account, clanName: String, inviter: String): ClanDecision {
        val clan = clan(clanName) ?: return ClanDecision.Denied("That clan no longer exists.")
        val decision = ClanRules.acceptInvite(clan, account.username, account.social.clanName, inviter)
        if (!decision.allowed) return decision
        val username = account.username
        if (account.social.guestedClanChat == clan.name) {
            // A guest who joins stops being a guest; the chatter is re-added as a member below.
            channelsByClan[clan.name]?.chatters?.remove(username)
            account.social.guestedClanChat = null
        }
        clan.members[username] = ClanMemberData(rank = ClanRank.RECRUIT)
        account.social.clanName = clan.name
        account.social.connectedToClan = true
        SocialGateway.worldIdOf(username)?.let { joinMemberChannel(clan, account, it) }
        commit(clan)
        saveMembership(account)
        return ClanDecision.Allowed
    }

    private suspend fun removeMember(account: Account, target: String): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.removeMember(clan, account.username, target)
        if (!decision.allowed) return decision
        val targetAccount = Accounts.find(target)
        if (targetAccount != null) dropMember(clan, targetAccount) else clan.members.remove(target)
        lobbyMessage(target, "You have been kicked from the clan ${clan.name}.")
        commit(clan)
        return ClanDecision.Allowed
    }

    private suspend fun updateMember(account: Account, action: ClanAction.UpdateMember): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.updateMember(clan, account.username, action)
        if (!decision.allowed) return decision
        val data = clan.members[action.target] ?: return ClanDecision.Denied("That player is not a clan member.")
        val rank = action.rank
        if (rank != null) {
            if (action.target == account.username && account.username == clan.leaderUsername) {
                // The owner stepping down: the deputy owner the rule required takes over.
                val successor = ClanRules.successor(clan) ?: return ClanDecision.Denied("There is nobody to hand the clan to.")
                clan.leaderUsername = successor
                clan.members[successor]?.rank = ClanRank.OWNER
            }
            data.rank = rank
        }
        action.job?.let { data.job = it }
        action.banFromKeep?.let { data.banFromKeep = it }
        action.banFromCitadel?.let { data.banFromCitadel = it }
        action.banFromIsland?.let { data.banFromIsland = it }
        commit(clan)
        return ClanDecision.Allowed
    }

    private suspend fun ban(account: Account, target: String): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.ban(clan, account.username, target)
        if (!decision.allowed) return decision
        clan.bannedUsers.add(target)
        val targetAccount = Accounts.find(target)
        if (ClanRules.isMember(clan, target)) {
            if (targetAccount != null) dropMember(clan, targetAccount) else clan.members.remove(target)
            lobbyMessage(target, "You have been banned from the clan ${clan.name}.")
        } else if (targetAccount != null) {
            ejectGuest(clan, targetAccount)
        }
        commit(clan)
        return ClanDecision.Allowed
    }

    private suspend fun unban(account: Account, target: String): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.unban(clan, account.username, target)
        if (!decision.allowed) return decision
        clan.bannedUsers.remove(target)
        commit(clan)
        return ClanDecision.Allowed
    }

    // ─── Settings ────────────────────────────────────────────────────

    private suspend fun updateSettings(account: Account, settings: Map<String, Any>): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.updateSettings(clan, account.username, settings)
        if (!decision.allowed) return decision
        val delta = ClanRules.settingsDelta(clan, settings).getOrThrow()
        if (delta.isEmpty()) return ClanDecision.Allowed
        for ((setting, value) in delta) clan.setSetting(setting, value)
        commit(clan)
        return ClanDecision.Allowed
    }

    private suspend fun setChannelRanks(account: Account, talkRank: ClanRank?, kickRank: ClanRank?): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.setChannelRanks(clan, account.username, talkRank, kickRank)
        if (!decision.allowed) return decision
        talkRank?.let { clan.ccChatRank = it }
        kickRank?.let { clan.ccKickRank = it }
        commit(clan)
        return ClanDecision.Allowed
    }

    private suspend fun setVar(account: Account, id: Int, value: Any): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val decision = ClanRules.setVar(clan, account.username)
        if (!decision.allowed) return decision
        clan.setVar(id, value)
        commit(clan)
        val packet = ClanPackets.clanVarPacket(id, value, varKind(id, value)) ?: return ClanDecision.Allowed
        val channel = channelsByClan[clan.name] ?: return ClanDecision.Allowed
        for (chatter in channel.chatters.values) {
            if (chatter.guest || chatter.worldId != ClanChatter.LOBBY_WORLD_ID) continue
            SocialGateway.sendToLobbyPlayer(chatter.username, packet)
        }
        return ClanDecision.Allowed
    }

    // ─── Channels ────────────────────────────────────────────────────

    private suspend fun joinChannel(account: Account): ClanDecision {
        val clan = memberClan(account)
        val decision = ClanRules.joinChannel(clan, account.username)
        if (!decision.allowed || clan == null) return decision
        if (!account.social.connectedToClan) {
            account.social.connectedToClan = true
            saveMembership(account)
        }
        val worldId = SocialGateway.worldIdOf(account.username) ?: return ClanDecision.Allowed
        joinMemberChannel(clan, account, worldId)
        resyncChannel(clan, worldId, account.username)
        return ClanDecision.Allowed
    }

    private suspend fun leaveChannel(account: Account): ClanDecision {
        val clan = memberClan(account)
        val decision = ClanRules.leaveChannel(clan, clan?.let { channelsByClan[it.name]?.state(it) }, account.username)
        if (!decision.allowed || clan == null) return decision
        account.social.connectedToClan = false
        saveMembership(account)
        leaveMemberChannel(clan, account.username)
        publishChannelIfChanged(clan)
        return ClanDecision.Allowed
    }

    private suspend fun joinGuestChannel(account: Account, clanName: String): ClanDecision {
        val clan = clan(clanName)
        val decision = ClanRules.joinGuestChannel(clan, account.username, isTempBanned(clanName, account.username))
        if (!decision.allowed || clan == null) return decision
        val previous = account.social.guestedClanChat
        if (previous != null && previous != clan.name) {
            leaveGuestChannel(previous, account.username)
            clan(previous)?.let { publishChannelIfChanged(it) }
        }
        if (account.social.guestedClanChat != clan.name) {
            account.social.guestedClanChat = clan.name
            saveMembership(account)
        }
        val worldId = SocialGateway.worldIdOf(account.username) ?: return ClanDecision.Allowed
        joinGuestChannel(clan, account, worldId)
        resyncChannel(clan, worldId, account.username)
        return ClanDecision.Allowed
    }

    private suspend fun leaveGuestChannel(account: Account): ClanDecision {
        val clanName = account.social.guestedClanChat
        val clan = clanName?.let { clan(it) }
        val decision = ClanRules.leaveGuestChannel(clan?.let { channelsByClan[it.name]?.state(it) }, account.username)
        if (!decision.allowed || clan == null) return decision
        account.social.guestedClanChat = null
        saveMembership(account)
        leaveGuestChannel(clan.name, account.username)
        publishChannelIfChanged(clan)
        return ClanDecision.Allowed
    }

    private suspend fun kickFromChannel(account: Account, target: String): ClanDecision {
        val clan = memberClan(account) ?: return ClanDecision.Denied(NOT_IN_CLAN)
        val channel = channelsByClan[clan.name]
        val decision = ClanRules.kickFromChannel(clan, channel?.state(clan), account.username, target)
        if (!decision.allowed || channel == null) return decision
        val targetAccount = Accounts.find(target)
        if (ClanRules.isMember(clan, target)) {
            leaveMemberChannel(clan, target)
            if (targetAccount != null && targetAccount.social.connectedToClan) {
                targetAccount.social.connectedToClan = false
                saveMembership(targetAccount)
            }
            lobbyMessage(target, "You have been kicked from the clan chat.")
        } else {
            tempBansByClan.getOrPut(clan.name) { ConcurrentHashMap() }[target] = System.currentTimeMillis() + TEMP_BAN_MS
            leaveGuestChannel(clan.name, target)
            if (targetAccount != null && targetAccount.social.guestedClanChat == clan.name) {
                targetAccount.social.guestedClanChat = null
                saveMembership(targetAccount)
            }
            lobbyMessage(target, "You have been kicked from the clan chat and are temporarily banned from it.")
        }
        publishChannelIfChanged(clan)
        return ClanDecision.Allowed
    }

    // ─── Chat ────────────────────────────────────────────────────────

    private suspend fun chat(account: Account, guest: Boolean, message: ChatMessage): ClanDecision {
        val clan = speakingClan(account, guest) ?: return ClanDecision.Denied(NOT_IN_CHANNEL)
        val state = channelsByClan[clan.name]?.state(clan)
        val decision = ClanRules.chat(clan, state, account.username, account.checkMuted())
        if (!decision.allowed || state == null) return decision
        val crown = account.rights.crown
        route(state, ClanChatMessage(clan.name, account.displayName, crown, message)) { chatter ->
            ClanPackets.chatMessage(chatter.guest, crown, account.displayName, message.message)
        }
        return ClanDecision.Allowed
    }

    private suspend fun quickChat(account: Account, guest: Boolean, message: QuickChatMessage): ClanDecision {
        val clan = speakingClan(account, guest) ?: return ClanDecision.Denied(NOT_IN_CHANNEL)
        val state = channelsByClan[clan.name]?.state(clan)
        val decision = ClanRules.chat(clan, state, account.username, account.checkMuted())
        if (!decision.allowed || state == null) return decision
        val crown = account.rights.crown
        route(state, ClanQuickChatMessage(clan.name, account.displayName, crown, message)) { chatter ->
            ClanPackets.quickChatMessage(chatter.guest, crown, account.displayName, message)
        }
        return ClanDecision.Allowed
    }

    /** The clan whose channel the actor speaks in: their own, or the one they listen to as a guest. */
    private suspend fun speakingClan(account: Account, guest: Boolean): Clan? =
        if (guest) account.social.guestedClanChat?.let { clan(it) } else memberClan(account)

    /**
     * One line to everyone in [state]: the wire message once per world holding a chatter, and
     * the client packet straight to each lobby-screen chatter, `guest` as that recipient sees it.
     */
    private suspend fun route(state: ClanChannelState, wire: SocialGatewayWireMessage, lobbyPacket: (ClanChatter) -> ServerProt) {
        val worlds = LinkedHashSet<Int>()
        for (chatter in state.chatters) {
            if (chatter.worldId == ClanChatter.LOBBY_WORLD_ID) {
                SocialGateway.sendToLobbyPlayer(chatter.username, lobbyPacket(chatter))
            } else {
                worlds += chatter.worldId
            }
        }
        for (worldId in worlds) SocialGateway.sendToWorld(worldId, wire)
    }

    // ─── Clan cache and persistence ──────────────────────────────────

    /** The cached clan, read from the store on first use. */
    /**
     * Clan names are stored in display form ("Darkan Devs"); whatever a player typed - a guest
     * channel name, a name check - is resolved through the same canonical form.
     */
    private fun canonicalName(name: String): String =
        name.trim().split(' ').filter { it.isNotEmpty() }.joinToString(" ").formatPlayerNameForDisplay()

    private suspend fun clan(name: String): Clan? {
        val key = canonicalName(name)
        return clansByName[key] ?: Clans.find(key)?.also { clansByName[it.name] = it }
    }

    /**
     * The clan [account] is a member of. An account naming a clan that no longer lists it (a
     * deleted clan, a kick that never reached the account) is healed on the spot.
     */
    private suspend fun memberClan(account: Account): Clan? {
        val name = account.social.clanName ?: return null
        val clan = clan(name)
        if (clan != null && ClanRules.isMember(clan, account.username)) return clan
        account.social.clanName = null
        account.social.connectedToClan = false
        saveMembership(account)
        return null
    }

    /**
     * The end of every clan mutation: write the clan back, tell every world, refresh the
     * lobby-screen members' settings, and publish the channel if a rank or list changed with it.
     */
    private suspend fun commit(clan: Clan) {
        clansByName[clan.name] = clan
        Clans.save(clan)
        settingsUpdates.merge(clan.name, 1, Int::plus)
        SocialGateway.broadcastToWorlds(ClanSnapshot(clan))
        refreshLobbyScreen(clan)
        publishChannelIfChanged(clan)
    }

    /** The last member left: everyone out of the channel, the document gone, every world told. */
    private suspend fun deleteClan(clan: Clan, last: Account) {
        val channel = channelsByClan[clan.name]
        if (channel != null) {
            for (chatter in channel.chatters.values.toList()) {
                if (chatter.guest) {
                    val guest = Accounts.find(chatter.username) ?: continue
                    ejectGuest(clan, guest)
                } else {
                    leaveMemberChannel(clan, chatter.username)
                }
            }
            publishChannelIfChanged(clan)
        }
        clan.members.remove(last.username)
        last.social.clanName = null
        last.social.connectedToClan = false
        SocialGateway.sendToLobbyPlayer(last.username, ClanPackets.settingsLeft(main = true))
        saveMembership(last)
        clansByName.remove(clan.name)
        settingsUpdates.remove(clan.name)
        tempBansByClan.remove(clan.name)
        Clans.delete(clan.name)
        SocialGateway.broadcastToWorlds(ClanDeleted(clan.name))
    }

    /** Takes [account] out of [clan]'s member list, its channel and their own clan fields. */
    private suspend fun dropMember(clan: Clan, account: Account) {
        clan.members.remove(account.username)
        leaveMemberChannel(clan, account.username)
        if (account.social.clanName == clan.name) {
            account.social.clanName = null
            account.social.connectedToClan = false
        }
        SocialGateway.sendToLobbyPlayer(account.username, ClanPackets.settingsLeft(main = true))
        saveMembership(account)
    }

    /** Throws a guest out of [clan]'s channel and forgets it as their guest channel. */
    private suspend fun ejectGuest(clan: Clan, account: Account) {
        leaveGuestChannel(clan.name, account.username)
        if (account.social.guestedClanChat == clan.name) {
            account.social.guestedClanChat = null
            saveMembership(account)
        }
    }

    // ─── Membership pushes ───────────────────────────────────────────

    private fun membershipOf(account: Account) = ClanMembership(
        username = account.username,
        clanName = account.social.clanName,
        inChannel = account.social.connectedToClan,
        guestChannel = account.social.guestedClanChat,
    )

    /** Saves [account]'s clan fields and pushes them to the world the player is on, if any. */
    private suspend fun saveMembership(account: Account) {
        Accounts.save(account)
        val worldId = SocialGateway.worldIdOf(account.username) ?: return
        if (worldId == ClanChatter.LOBBY_WORLD_ID) return
        SocialGateway.sendToWorld(worldId, membershipOf(account))
    }

    // ─── Channel mechanics ───────────────────────────────────────────

    private fun isTempBanned(clanName: String, username: String): Boolean {
        val bans = tempBansByClan[clanName] ?: return false
        val until = bans[username] ?: return false
        if (System.currentTimeMillis() < until) return true
        bans.remove(username)
        return false
    }

    /** Puts [account] in [clan]'s channel as a member; a lobby-screen member also gets the clan vars. */
    private suspend fun joinMemberChannel(clan: Clan, account: Account, worldId: Int) {
        val username = account.username
        val channel = channelsByClan.getOrPut(clan.name) { Channel(clan.name) }
        channel.chatters[username] = ClanChatter(
            username = username,
            displayName = SocialGateway.displayNameOf(username) ?: account.displayName,
            rank = ClanRules.rankOf(clan, username),
            worldId = worldId,
        )
        if (worldId == ClanChatter.LOBBY_WORLD_ID) {
            for (packet in varPackets(clan)) SocialGateway.sendToLobbyPlayer(username, packet)
        }
    }

    private suspend fun leaveMemberChannel(clan: Clan, username: String) {
        val channel = channelsByClan[clan.name] ?: return
        val chatter = channel.chatters.remove(username) ?: return
        if (chatter.worldId == ClanChatter.LOBBY_WORLD_ID) {
            SocialGateway.sendToLobbyPlayer(username, ClanPackets.channelLeft(main = true))
            SocialGateway.sendToLobbyPlayer(username, VarclanDisable())
        }
    }

    private suspend fun joinGuestChannel(clan: Clan, account: Account, worldId: Int) {
        val username = account.username
        val channel = channelsByClan.getOrPut(clan.name) { Channel(clan.name) }
        channel.chatters[username] = ClanChatter(
            username = username,
            displayName = SocialGateway.displayNameOf(username) ?: account.displayName,
            rank = ClanRank.NONE,
            worldId = worldId,
        )
        if (worldId == ClanChatter.LOBBY_WORLD_ID) {
            SocialGateway.sendToLobbyPlayer(username, guestSettings(clan))
        }
    }

    private suspend fun leaveGuestChannel(clanName: String, username: String) {
        val channel = channelsByClan[clanName] ?: return
        val chatter = channel.chatters.remove(username) ?: return
        if (chatter.worldId == ClanChatter.LOBBY_WORLD_ID) {
            SocialGateway.sendToLobbyPlayer(username, ClanPackets.settingsLeft(main = false))
            SocialGateway.sendToLobbyPlayer(username, ClanPackets.channelLeft(main = false))
        }
    }

    /**
     * Broadcasts [clan]'s channel if its chatters or ranks differ from what was last published,
     * and hands every lobby-screen chatter their `ClanChannelFull`. Answers whether it did. An
     * emptied channel is published once more, empty, and then dropped.
     */
    private suspend fun publishChannelIfChanged(clan: Clan): Boolean {
        val channel = channelsByClan[clan.name] ?: return false
        channel.refreshRanks(clan)
        val previous = channel.published
        val current = channel.state(clan)
        val unchanged = previous != null &&
            previous.chatters == current.chatters &&
            previous.talkRank == current.talkRank &&
            previous.kickRank == current.kickRank
        if (unchanged) return false
        channel.updateNum++
        val state = channel.state(clan)
        channel.published = state
        SocialGateway.broadcastToWorlds(ClanChannelUpdate(state))
        for (chatter in state.chatters) {
            if (chatter.worldId != ClanChatter.LOBBY_WORLD_ID) continue
            SocialGateway.sendToLobbyPlayer(chatter.username, ClanPackets.channelFull(state, main = !chatter.guest))
        }
        if (channel.chatters.isEmpty()) channelsByClan.remove(clan.name, channel)
        return true
    }

    /**
     * After a join: publish the channel if it moved, else re-send it to where the joiner is - a
     * reconnected world or a fresh lobby session has no copy yet even when the list is the same.
     */
    private suspend fun resyncChannel(clan: Clan, worldId: Int, username: String) {
        if (publishChannelIfChanged(clan)) return
        val channel = channelsByClan[clan.name] ?: return
        val state = channel.state(clan)
        if (worldId == ClanChatter.LOBBY_WORLD_ID) {
            val chatter = channel.chatters[username] ?: return
            SocialGateway.sendToLobbyPlayer(username, ClanPackets.channelFull(state, main = !chatter.guest))
        } else {
            SocialGateway.sendToWorld(worldId, ClanChannelUpdate(state))
        }
    }

    // ─── Lobby-screen packets ────────────────────────────────────────

    /** Every lobby-screen member gets the clan's settings; every lobby-screen guest the guest form. */
    private suspend fun refreshLobbyScreen(clan: Clan) {
        val members = ClanPackets.settingsFull(clan, main = true, updateCount = updateCount(clan), displayName = displayNames(clan))
        for (username in clan.members.keys) SocialGateway.sendToLobbyPlayer(username, members)
        val channel = channelsByClan[clan.name] ?: return
        val guests = channel.chatters.values.filter { it.guest && it.worldId == ClanChatter.LOBBY_WORLD_ID }
        if (guests.isEmpty()) return
        val guestPacket = ClanPackets.settingsFull(clan, main = false, updateCount = updateCount(clan), displayName = displayNames(clan))
        for (guest in guests) SocialGateway.sendToLobbyPlayer(guest.username, guestPacket)
    }

    private suspend fun memberSettings(clan: Clan) =
        ClanPackets.settingsFull(clan, main = true, updateCount = updateCount(clan), displayName = displayNames(clan))

    private suspend fun guestSettings(clan: Clan) =
        ClanPackets.settingsFull(clan, main = false, updateCount = updateCount(clan), displayName = displayNames(clan))

    private fun updateCount(clan: Clan): Int = settingsUpdates[clan.name] ?: 0

    /** `VarclanEnable` and one `VarclanSet*` per var, the kind resolved by [varKind]. */
    private fun varPackets(clan: Clan): List<ServerProt> {
        val packets = ArrayList<ServerProt>(clan.vars.size + 1)
        packets += VarclanEnable()
        for ((key, value) in clan.vars) {
            val id = key.toIntOrNull() ?: continue
            packets += ClanPackets.clanVarPacket(id, value, varKind(id, value)) ?: continue
        }
        return packets
    }

    /** Display names for everyone the settings packet lists: online players first, then the store. */
    private suspend fun displayNames(clan: Clan): (String) -> String {
        val usernames = clan.members.keys + clan.bannedUsers
        val names = HashMap<String, String>()
        for (username in usernames) SocialGateway.displayNameOf(username)?.let { names[username] = it }
        val missing = usernames.filter { it !in names }
        if (missing.isNotEmpty()) {
            for (account in Accounts.findByUsernames(missing)) names[account.username] = account.displayName
        }
        return { username -> names[username] ?: username.formatPlayerNameForDisplay() }
    }

    private suspend fun lobbyMessage(username: String, text: String) {
        SocialGateway.sendToLobbyPlayer(username, GameMessage(ChatMessageType.FC_NOTIFICATION, text))
    }

    private fun kindOf(value: Any): ClanVarKind = when (value) {
        is String -> ClanVarKind.STRING
        is Long -> ClanVarKind.LONG
        else -> ClanVarKind.INT
    }
}
