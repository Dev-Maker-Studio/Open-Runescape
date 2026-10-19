package org.darkan.lobby

import kotlinx.coroutines.runBlocking
import org.darkan.core.clan.ClanAction
import org.darkan.core.clan.ClanChatter
import org.darkan.core.model.Account
import org.darkan.core.model.Clan
import org.darkan.core.model.ClanMemberData
import org.darkan.core.model.ClanRank
import org.darkan.core.model.ClanSetting
import org.darkan.core.net.prot.packet.ClanChannelFull
import org.darkan.core.net.prot.packet.ClanSettingsFull
import org.darkan.core.net.prot.packet.MessageClanChannel
import org.darkan.core.net.prot.packet.MessageQuickChatClanChannel
import org.darkan.core.net.prot.packet.VarclanDisable
import org.darkan.core.net.prot.packet.VarclanEnable
import org.darkan.core.net.prot.packet.VarclanSetByte
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.gateway.ClanChannelUpdate
import org.darkan.core.social.gateway.ClanChatMessage
import org.darkan.core.social.gateway.ClanCommandResult
import org.darkan.core.social.gateway.ClanDeleted
import org.darkan.core.social.gateway.ClanFetch
import org.darkan.core.social.gateway.ClanMembership
import org.darkan.core.social.gateway.ClanQuickChatMessage
import org.darkan.core.social.gateway.ClanSnapshot
import org.darkan.core.social.gateway.PresenceSyncReason
import org.darkan.lobby.harness.LobbyHarness
import org.darkan.lobby.harness.LobbyHarness.FakeWorld
import org.darkan.lobby.social.ClanService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The lobby's clan service, driven through [SocialGateway] with a fake world and the in-memory
 * stores (see [LobbyHarness]): every action's allow and deny path, owner succession, bans, chat
 * routing to worlds and lobby-screen recipients, temp bans, and the reconnect resync.
 */
class ClanServiceTest {

    private val clanName = "Darkan"

    @BeforeTest
    fun clearGateway() = LobbyHarness.reset()

    /** The last result this world was sent for [account]. */
    private fun FakeWorld.lastResult(account: Account): ClanCommandResult =
        assertNotNull(messagesOf<ClanCommandResult>().lastOrNull { it.username == account.username }, "no result for ${account.username}")

    private fun FakeWorld.lastSnapshot(): Clan = assertNotNull(messagesOf<ClanSnapshot>().lastOrNull()).clan

    private fun FakeWorld.lastChannel(): ClanChannelUpdate = assertNotNull(messagesOf<ClanChannelUpdate>().lastOrNull())

    private fun FakeWorld.lastMembership(account: Account): ClanMembership =
        assertNotNull(messagesOf<ClanMembership>().lastOrNull { it.username == account.username }, "no membership for ${account.username}")

    private suspend fun FakeWorld.ok(account: Account, action: ClanAction): ClanCommandResult {
        command(account, action)
        val result = lastResult(account)
        assertTrue(result.ok, "expected ${action.serialName} to be allowed, got: ${result.message}")
        return result
    }

    private suspend fun FakeWorld.denied(account: Account, action: ClanAction, message: String? = null): ClanCommandResult {
        command(account, action)
        val result = lastResult(account)
        assertFalse(result.ok, "expected ${action.serialName} to be denied")
        if (message != null) assertEquals(message, result.message)
        return result
    }

    /** An owner online on [world] with a freshly created clan. */
    private suspend fun FakeWorld.founded(owner: Account): Clan {
        playerOnline(owner)
        ok(owner, ClanAction.Create(clanName))
        return assertNotNull(LobbyHarness.clans.find(clanName))
    }

    /** [member] joins [owner]'s clan from [world] on the owner's invitation. */
    private suspend fun FakeWorld.joined(owner: Account, member: Account, rank: ClanRank = ClanRank.RECRUIT) {
        playerOnline(member)
        ok(member, ClanAction.AcceptInvite(clanName, owner.username))
        if (rank != ClanRank.RECRUIT) ok(owner, ClanAction.UpdateMember(member.username, rank = rank))
    }

    // ─── Create and names ────────────────────────────────────────────

    @Test
    fun `creating a clan makes the actor its owner and tells every world`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val world = LobbyHarness.world(1).connect()
        val other = LobbyHarness.world(2).connect()

        val clan = world.founded(owner)

        assertEquals(owner.username, clan.leaderUsername)
        assertEquals(ClanRank.OWNER, clan.members[owner.username]?.rank)
        assertEquals(clanName, owner.social.clanName)
        assertTrue(owner.social.connectedToClan)
        assertEquals(clanName, world.lastSnapshot().name)
        assertEquals(clanName, other.lastSnapshot().name, "the snapshot is broadcast to every world")
        val membership = world.lastMembership(owner)
        assertEquals(clanName, membership.clanName)
        assertTrue(membership.inChannel)
        val chatter = assertNotNull(world.lastChannel().state.chatter(owner.username), "the owner was not put in the channel")
        assertEquals(ClanRank.OWNER, chatter.rank)
        assertEquals(1, chatter.worldId)
    }

    @Test
    fun `names are validated, unique, and refused to somebody already in a clan`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val other = LobbyHarness.account("other")
        val world = LobbyHarness.world(1).connect()
        world.playerOnline(other)
        world.founded(owner)

        world.denied(other, ClanAction.CheckName(clanName), "The name $clanName is already taken. Please choose another.")
        world.denied(other, ClanAction.Create(clanName))
        world.denied(other, ClanAction.CheckName("far too long a name"), "Clan names must be 12 or fewer characters and be alphanumeric.")
        world.denied(other, ClanAction.Create("bad-name"))
        world.ok(other, ClanAction.CheckName("Second"))
        world.denied(owner, ClanAction.CheckName("Second"), "You are already in the clan $clanName. You must leave before joining a new one.")
        assertNull(LobbyHarness.clans.find("Second"), "a check must not create anything")
    }

    // ─── Invitations ─────────────────────────────────────────────────

    @Test
    fun `an invitation from a member with INVITE is accepted`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val member = LobbyHarness.account("member")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)

        world.joined(owner, member)

        val clan = world.lastSnapshot()
        assertEquals(ClanRank.RECRUIT, clan.members[member.username]?.rank)
        assertEquals(clanName, member.social.clanName)
        assertEquals(clanName, world.lastMembership(member).clanName)
        assertNotNull(world.lastChannel().state.chatter(member.username), "a new member joins the channel")
    }

    @Test
    fun `an invitation is refused without INVITE, when banned, when full, or when already in a clan`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val recruit = LobbyHarness.account("recruit")
        val banned = LobbyHarness.account("banned")
        val hopeful = LobbyHarness.account("hopeful")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, recruit)
        world.playerOnline(banned)
        world.playerOnline(hopeful)

        // A recruit holds no INVITE permission by default.
        world.denied(hopeful, ClanAction.AcceptInvite(clanName, recruit.username), "That player does not have permission to invite you to the clan.")

        world.ok(owner, ClanAction.Ban(banned.username))
        world.denied(banned, ClanAction.AcceptInvite(clanName, owner.username), "You have been banned from that clan.")

        world.denied(recruit, ClanAction.AcceptInvite(clanName, owner.username), "You are already in the clan $clanName. You must leave before joining a new one.")

        val clan = assertNotNull(LobbyHarness.clans.find(clanName))
        var filler = 0
        while (clan.members.size < Clan.MAX_MEMBERS) clan.members["filler${filler++}"] = ClanMemberData(ClanRank.RECRUIT)
        world.denied(hopeful, ClanAction.AcceptInvite(clanName, owner.username), "That clan is full.")

        world.denied(hopeful, ClanAction.AcceptInvite("Nowhere", owner.username), "That clan no longer exists.")
    }

    // ─── Members ─────────────────────────────────────────────────────

    @Test
    fun `only an admin above the target can remove a member`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val admin = LobbyHarness.account("admin")
        val recruit = LobbyHarness.account("recruit")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, admin, ClanRank.ADMIN)
        world.joined(owner, recruit)

        world.denied(recruit, ClanAction.RemoveMember(admin.username), "Only clan admins can kick clan members.")
        world.denied(admin, ClanAction.RemoveMember(owner.username), "The clan owner cannot be kicked from the clan.")
        world.denied(admin, ClanAction.RemoveMember(admin.username), "You can only kick players with lesser rank than you.")

        world.ok(admin, ClanAction.RemoveMember(recruit.username))

        assertFalse(world.lastSnapshot().members.containsKey(recruit.username))
        assertNull(recruit.social.clanName)
        assertFalse(recruit.social.connectedToClan)
        val membership = world.lastMembership(recruit)
        assertNull(membership.clanName)
        assertFalse(membership.inChannel)
        assertNull(world.lastChannel().state.chatter(recruit.username), "a removed member leaves the channel")
    }

    @Test
    fun `rank changes stay below the actor and the owner needs a deputy to step down`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val admin = LobbyHarness.account("admin")
        val recruit = LobbyHarness.account("recruit")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, admin, ClanRank.ADMIN)
        world.joined(owner, recruit)

        world.denied(recruit, ClanAction.UpdateMember(admin.username, rank = ClanRank.CORPORAL), "Only clan admins can edit ranks and jobs.")
        world.denied(admin, ClanAction.UpdateMember(recruit.username, rank = ClanRank.ADMIN), "You cannot promote someone to your own rank or higher.")
        world.denied(admin, ClanAction.UpdateMember(recruit.username, rank = ClanRank.OWNER), "That rank cannot be assigned.")
        world.denied(
            owner,
            ClanAction.UpdateMember(owner.username, rank = ClanRank.ADMIN),
            "You are currently the clan owner, you must at least have one deputy owner in the clan to demote yourself.",
        )

        world.ok(admin, ClanAction.UpdateMember(recruit.username, rank = ClanRank.CAPTAIN, job = 3, banFromKeep = true))
        val promoted = assertNotNull(world.lastSnapshot().members[recruit.username])
        assertEquals(ClanRank.CAPTAIN, promoted.rank)
        assertEquals(3, promoted.job)
        assertEquals(true, promoted.banFromKeep)
        assertEquals(ClanRank.CAPTAIN, world.lastChannel().state.chatter(recruit.username)?.rank, "the channel shows the new rank")

        world.ok(owner, ClanAction.UpdateMember(admin.username, rank = ClanRank.DEPUTY_OWNER))
        world.ok(owner, ClanAction.UpdateMember(owner.username, rank = ClanRank.ADMIN))
        val handedOver = world.lastSnapshot()
        assertEquals(admin.username, handedOver.leaderUsername)
        assertEquals(ClanRank.OWNER, handedOver.members[admin.username]?.rank)
        assertEquals(ClanRank.ADMIN, handedOver.members[owner.username]?.rank)
    }

    // ─── Leaving ─────────────────────────────────────────────────────

    @Test
    fun `an owner leaving hands the clan to the highest rank`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val general = LobbyHarness.account("general")
        val recruit = LobbyHarness.account("recruit")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, recruit)
        world.joined(owner, general, ClanRank.GENERAL)

        world.ok(owner, ClanAction.Leave)

        val clan = world.lastSnapshot()
        assertEquals(general.username, clan.leaderUsername)
        assertEquals(ClanRank.OWNER, clan.members[general.username]?.rank)
        assertFalse(clan.members.containsKey(owner.username))
        assertNull(owner.social.clanName)
        assertEquals(ClanRank.OWNER, world.lastChannel().state.chatter(general.username)?.rank)
        assertNull(world.lastChannel().state.chatter(owner.username))
        world.denied(owner, ClanAction.Leave, "You aren't in a clan.")
    }

    @Test
    fun `the last member leaving deletes the clan`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val world = LobbyHarness.world(1).connect()
        val other = LobbyHarness.world(2).connect()
        world.founded(owner)

        world.ok(owner, ClanAction.Leave)

        assertNull(LobbyHarness.clans.find(clanName))
        assertEquals(clanName, assertNotNull(other.messagesOf<ClanDeleted>().lastOrNull()).clanName)
        assertNull(owner.social.clanName)
        assertFalse(owner.social.connectedToClan)
        assertNull(world.lastMembership(owner).clanName)
        assertTrue(world.lastChannel().state.chatters.isEmpty(), "the channel is published empty before the clan goes")
        other.deliver(ClanFetch(clanName))
        assertEquals(ClanDeleted(clanName), other.messagesOf<ClanDeleted>().last(), "a fetch of a deleted clan answers deleted")
    }

    // ─── Bans ────────────────────────────────────────────────────────

    @Test
    fun `a ban removes the member and keeps them out as a guest`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val recruit = LobbyHarness.account("recruit")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, recruit)

        world.denied(recruit, ClanAction.Ban(owner.username), "You do not have permission to ban users.")
        world.ok(owner, ClanAction.Ban(recruit.username))

        val clan = world.lastSnapshot()
        assertTrue(recruit.username in clan.bannedUsers)
        assertFalse(clan.members.containsKey(recruit.username))
        assertNull(recruit.social.clanName)
        assertNull(world.lastChannel().state.chatter(recruit.username))
        world.denied(recruit, ClanAction.JoinGuestChannel(clanName), "You have been banned from that clan.")

        world.ok(owner, ClanAction.Unban(recruit.username))
        assertFalse(recruit.username in world.lastSnapshot().bannedUsers)
        world.ok(recruit, ClanAction.JoinGuestChannel(clanName))
        assertEquals(ClanRank.NONE, world.lastChannel().state.chatter(recruit.username)?.rank)
    }

    // ─── Settings ────────────────────────────────────────────────────

    @Test
    fun `settings need an admin and a wholly valid map`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val recruit = LobbyHarness.account("recruit")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, recruit)
        val snapshotsBefore = world.messagesOf<ClanSnapshot>().size

        world.denied(recruit, ClanAction.UpdateSettings(mapOf(ClanSetting.MOTTO.name to "no")), "Only clan admins can edit the clan settings.")
        world.denied(owner, ClanAction.UpdateSettings(mapOf(ClanSetting.MOTTO.name to "yes", "NOT_A_SETTING" to 1)), "Unknown clan setting: NOT_A_SETTING.")
        world.denied(owner, ClanAction.UpdateSettings(mapOf(ClanSetting.MOTTO.name to 5)), "Clan setting MOTTO expects a String value.")
        assertEquals(snapshotsBefore, world.messagesOf<ClanSnapshot>().size, "a refused update must change nothing")
        assertEquals("", assertNotNull(LobbyHarness.clans.find(clanName)).getSetting(ClanSetting.MOTTO))

        world.ok(owner, ClanAction.UpdateSettings(mapOf(ClanSetting.MOTTO.name to "Hello", ClanSetting.FORUM_QFC.name to 42)))
        assertEquals("Hello", world.lastSnapshot().getSetting(ClanSetting.MOTTO))
        // The store keeps the setting's own type: the Int on the wire was widened to the Long setting.
        assertEquals(42L, assertNotNull(LobbyHarness.clans.find(clanName)).getSetting(ClanSetting.FORUM_QFC))

        world.ok(owner, ClanAction.SetChannelRanks(talkRank = ClanRank.CORPORAL, kickRank = ClanRank.GENERAL))
        assertEquals(ClanRank.CORPORAL, world.lastChannel().state.talkRank)
        assertEquals(ClanRank.GENERAL, world.lastChannel().state.kickRank)
        world.denied(recruit, ClanAction.SetVar(3, 1), "You do not have permission to change clan settings.")
        world.ok(owner, ClanAction.SetVar(3, 1))
        assertEquals(1, world.lastSnapshot().getVar(3))
    }

    // ─── Channels ────────────────────────────────────────────────────

    @Test
    fun `leaving and joining the channel moves the chatter list and the membership flag`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)

        world.ok(owner, ClanAction.LeaveChannel)
        assertTrue(world.lastChannel().state.chatters.isEmpty())
        assertFalse(owner.social.connectedToClan)
        assertFalse(world.lastMembership(owner).inChannel)
        world.denied(owner, ClanAction.LeaveChannel, "You are not currently connected to a clan chat.")

        world.ok(owner, ClanAction.JoinChannel)
        assertNotNull(world.lastChannel().state.chatter(owner.username))
        assertTrue(world.lastMembership(owner).inChannel)
    }

    @Test
    fun `a lobby-screen member gets settings, channel and clan vars as client packets`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val member = LobbyHarness.account("member")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, member)
        world.ok(owner, ClanAction.SetVar(3, 1))
        world.playerOffline(member)

        val session = LobbyHarness.lobbyLogin(member)

        val settings = assertNotNull(session.sent.filterIsInstance<ClanSettingsFull>().lastOrNull(), "no settings for a lobby member")
        assertTrue(settings.main)
        assertEquals(clanName, settings.clanName)
        assertEquals(2, settings.members?.size)
        val channel = assertNotNull(session.sent.filterIsInstance<ClanChannelFull>().lastOrNull(), "no channel for a lobby member")
        assertTrue(channel.main)
        assertEquals(ClanChatter.LOBBY_WORLD_ID, channel.chatters?.firstOrNull { it.displayName == member.displayName }?.worldId)
        assertTrue(session.sent.any { it is VarclanEnable })
        assertEquals(VarclanSetByte(3, 1), session.sent.filterIsInstance<VarclanSetByte>().lastOrNull())
        assertEquals(ClanChatter.LOBBY_WORLD_ID, world.lastChannel().state.chatter(member.username)?.worldId, "the worlds see the lobby chatter")

        session.clearSent()
        val result = ClanService.handle(member.username, ClanAction.LeaveChannel)
        assertTrue(result.ok)
        assertEquals(ClanChannelFull(main = true), session.sent.filterIsInstance<ClanChannelFull>().lastOrNull())
        assertTrue(session.sent.any { it is VarclanDisable })
        assertNull(world.lastChannel().state.chatter(member.username))

        session.clearSent()
        assertTrue(ClanService.handle(member.username, ClanAction.Leave).ok)
        assertEquals(ClanSettingsFull(main = true), session.sent.filterIsInstance<ClanSettingsFull>().lastOrNull(), "leaving the clan sends the one-byte form")
    }

    @Test
    fun `a lobby logout leaves the channel but keeps the flags for the next login`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.playerOffline(owner)
        LobbyHarness.lobbyLogin(owner)
        assertEquals(ClanChatter.LOBBY_WORLD_ID, world.lastChannel().state.chatter(owner.username)?.worldId)

        LobbyHarness.lobbyLogout(owner)

        assertNull(world.lastChannel().state.chatter(owner.username))
        assertTrue(owner.social.connectedToClan, "a logout must not forget the channel")
        val session = LobbyHarness.lobbyLogin(owner)
        assertTrue(session.sent.filterIsInstance<ClanChannelFull>().any { it.clanName == clanName }, "the next login did not rejoin")
    }

    // ─── Chat ────────────────────────────────────────────────────────

    @Test
    fun `chat reaches each world once and each lobby chatter with its own guest flag`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val member = LobbyHarness.account("member")
        val lobbyMember = LobbyHarness.account("lobbymember")
        val guest = LobbyHarness.account("guest")
        val worldA = LobbyHarness.world(1).connect()
        val worldB = LobbyHarness.world(2).connect()
        val idle = LobbyHarness.world(3).connect()
        worldA.founded(owner)
        worldA.joined(owner, member)
        worldB.joined(owner, lobbyMember)
        worldB.playerOffline(lobbyMember)
        val memberSession = LobbyHarness.lobbyLogin(lobbyMember)
        val guestSession = LobbyHarness.lobbyLogin(guest)
        assertTrue(ClanService.handle(guest.username, ClanAction.JoinGuestChannel(clanName)).ok)
        worldB.playerOnline(member)
        memberSession.clearSent()
        guestSession.clearSent()

        worldA.ok(owner, ClanAction.Chat(guest = false, message = PublicChatMessage("hello clan", 7)))

        val toA = worldA.messagesOf<ClanChatMessage>()
        assertEquals(1, toA.size, "the owner's world gets the line once")
        assertEquals(PublicChatMessage("hello clan", 7), toA.single().message)
        assertEquals(owner.displayName, toA.single().sender)
        assertEquals(clanName, toA.single().clanName)
        assertEquals(1, worldB.messagesOf<ClanChatMessage>().size, "the member's world gets the line once")
        assertTrue(idle.messagesOf<ClanChatMessage>().isEmpty(), "a world with no chatter hears nothing")

        val toMember = assertNotNull(memberSession.sent.filterIsInstance<MessageClanChannel>().lastOrNull())
        assertFalse(toMember.guest, "a member hears their own clan as affined")
        assertEquals("hello clan", toMember.message)
        val toGuest = assertNotNull(guestSession.sent.filterIsInstance<MessageClanChannel>().lastOrNull())
        assertTrue(toGuest.guest, "a guest hears the clan as listened")

        // The guest speaks in the guest channel; guests may talk by default.
        assertTrue(ClanService.handle(guest.username, ClanAction.QuickChat(guest = true, message = QuickChatMessage("", 5))).ok)
        assertEquals(1, worldA.messagesOf<ClanQuickChatMessage>().size)
        assertTrue(memberSession.sent.filterIsInstance<MessageQuickChatClanChannel>().any { !it.guest })
        assertTrue(guestSession.sent.filterIsInstance<MessageQuickChatClanChannel>().any { it.guest })
    }

    @Test
    fun `chat is refused while muted, outside the channel, and below the talk rank`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val recruit = LobbyHarness.account("recruit")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, recruit)

        recruit.muteDays(1)
        world.denied(recruit, ClanAction.Chat(false, PublicChatMessage("hi")), "You are muted.")
        recruit.unmute()

        world.ok(owner, ClanAction.SetChannelRanks(talkRank = ClanRank.CAPTAIN))
        world.denied(recruit, ClanAction.Chat(false, PublicChatMessage("hi")), "You are not allowed to speak in that clan chat.")

        world.ok(recruit, ClanAction.LeaveChannel)
        world.denied(recruit, ClanAction.Chat(false, PublicChatMessage("hi")), "You are not currently connected to a clan chat.")
        world.denied(recruit, ClanAction.Chat(true, PublicChatMessage("hi")), "You are not currently connected to a clan chat.")
        assertTrue(world.messagesOf<ClanChatMessage>().isEmpty())
    }

    // ─── Kicks and guests ────────────────────────────────────────────

    @Test
    fun `a kicked guest is out for an hour and a kicked member just leaves the channel`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val recruit = LobbyHarness.account("recruit")
        val guest = LobbyHarness.account("guest")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, recruit)
        world.playerOnline(guest)
        world.ok(guest, ClanAction.JoinGuestChannel(clanName))
        assertEquals(clanName, guest.social.guestedClanChat)
        assertEquals(clanName, world.lastMembership(guest).guestChannel)

        world.denied(recruit, ClanAction.KickFromChannel(guest.username, guest = true), "You do not have permission to kick from this clan chat.")
        world.ok(owner, ClanAction.KickFromChannel(guest.username, guest = true))

        assertNull(world.lastChannel().state.chatter(guest.username))
        assertNull(guest.social.guestedClanChat)
        assertNull(world.lastMembership(guest).guestChannel)
        world.denied(guest, ClanAction.JoinGuestChannel(clanName), "You are temporarily banned from this clan channel.")

        world.ok(owner, ClanAction.KickFromChannel(recruit.username, guest = false))
        assertNull(world.lastChannel().state.chatter(recruit.username))
        assertEquals(clanName, recruit.social.clanName, "a channel kick is not a clan kick")
        assertFalse(recruit.social.connectedToClan)
        assertFalse(world.lastMembership(recruit).inChannel)
    }

    @Test
    fun `guests need the clan to allow them and are not members`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val guest = LobbyHarness.account("guest")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.playerOnline(guest)

        world.denied(guest, ClanAction.JoinGuestChannel("Nowhere"), "The clan chat you tried to join does not exist.")
        world.denied(owner, ClanAction.JoinGuestChannel(clanName), "You are already a member of that clan.")
        world.ok(owner, ClanAction.UpdateSettings(mapOf(ClanSetting.GUESTS_CAN_ENTER_CC.name to 0)))
        world.denied(guest, ClanAction.JoinGuestChannel(clanName), "This clan doesn't allow guests.")
        world.ok(owner, ClanAction.UpdateSettings(mapOf(ClanSetting.GUESTS_CAN_ENTER_CC.name to 1)))

        world.ok(guest, ClanAction.JoinGuestChannel(clanName))
        val chatter = assertNotNull(world.lastChannel().state.chatter(guest.username))
        assertTrue(chatter.guest)
        world.ok(guest, ClanAction.LeaveGuestChannel)
        assertNull(world.lastChannel().state.chatter(guest.username))
        assertNull(guest.social.guestedClanChat)
        world.denied(guest, ClanAction.LeaveGuestChannel, "You are not currently connected to a clan chat.")
    }

    // ─── Worlds coming and going ─────────────────────────────────────

    @Test
    fun `a world socket dropping takes its chatters out of every channel`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val member = LobbyHarness.account("member")
        val worldA = LobbyHarness.world(1).connect()
        val worldB = LobbyHarness.world(2).connect()
        worldA.founded(owner)
        worldB.joined(owner, member)
        assertEquals(2, worldA.lastChannel().state.chatters.size)

        worldB.disconnect()

        val state = worldA.lastChannel().state
        assertNull(state.chatter(member.username), "the dropped world's chatter stayed in the channel")
        assertNotNull(state.chatter(owner.username))
        assertTrue(member.social.connectedToClan, "a socket drop is not the player's choice")
    }

    @Test
    fun `a reconnecting world gets membership, snapshot and channel for every listed player`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val member = LobbyHarness.account("member")
        val guest = LobbyHarness.account("guest")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        world.joined(owner, member)
        world.playerOnline(guest)
        world.ok(guest, ClanAction.JoinGuestChannel(clanName))

        // The world restarts without the lobby seeing the socket close.
        val reconnected = LobbyHarness.world(1).connect()
        reconnected.presenceSnapshot(PresenceSyncReason.CONNECT, owner, member, guest)

        for (account in listOf(owner, member)) {
            val membership = reconnected.lastMembership(account)
            assertEquals(clanName, membership.clanName)
            assertTrue(membership.inChannel)
        }
        assertEquals(clanName, reconnected.lastMembership(guest).guestChannel)
        assertTrue(reconnected.messagesOf<ClanSnapshot>().any { it.clan.name == clanName }, "the snapshot was not re-sent")
        val state = reconnected.lastChannel().state
        assertNotNull(state.chatter(owner.username))
        assertNotNull(state.chatter(member.username))
        assertTrue(assertNotNull(state.chatter(guest.username)).guest)
    }

    @Test
    fun `a fetch answers with the clan or with deleted`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val world = LobbyHarness.world(1).connect()
        world.founded(owner)
        val other = LobbyHarness.world(2).connect()
        other.clear()

        other.deliver(ClanFetch(clanName))
        assertEquals(clanName, other.lastSnapshot().name)

        other.deliver(ClanFetch("Nowhere"))
        assertEquals("Nowhere", assertNotNull(other.messagesOf<ClanDeleted>().lastOrNull()).clanName)
    }

    @Test
    fun `a clan already in the store is served without ever having been created here`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val member = LobbyHarness.account("member")
        LobbyHarness.seedClan(
            Clan(
                name = clanName,
                leaderUsername = owner.username,
                members = mutableMapOf(owner.username to ClanMemberData(ClanRank.OWNER), member.username to ClanMemberData(ClanRank.RECRUIT)),
            ),
        )
        member.social.connectedToClan = true
        val world = LobbyHarness.world(1).connect()

        world.playerOnline(member)

        assertEquals(clanName, world.lastMembership(member).clanName)
        assertEquals(clanName, world.lastSnapshot().name)
        assertNotNull(world.lastChannel().state.chatter(member.username))
    }

    @Test
    fun `every command gets exactly one result carrying its request id`() = runBlocking<Unit> {
        val owner = LobbyHarness.account("owner")
        val world = LobbyHarness.world(1).connect()
        world.playerOnline(owner)

        world.command(owner, ClanAction.Create(clanName), requestId = 11)
        world.command(owner, ClanAction.Create(clanName), requestId = 12)
        world.command(owner, ClanAction.JoinChannel, requestId = 13)
        world.command(owner, ClanAction.Leave, requestId = 14)
        world.command(owner, ClanAction.Leave, requestId = 15)

        val results = world.messagesOf<ClanCommandResult>()
        assertEquals(listOf(11, 12, 13, 14, 15), results.map { it.requestId })
        assertEquals(listOf(true, false, true, true, false), results.map { it.ok })
        assertEquals(listOf("Create", "Create", "JoinChannel", "Leave", "Leave"), results.map { it.action })
        assertTrue(results.all { it.username == owner.username })
    }
}
