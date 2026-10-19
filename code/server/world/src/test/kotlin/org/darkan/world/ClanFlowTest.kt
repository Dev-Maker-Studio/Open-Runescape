package org.darkan.world

import com.rs.game.content.clans.ClansManager
import com.rs.plugin.PluginManager
import com.rs.plugin.events.ButtonClickEvent
import com.rs.plugin.events.InputHSLEvent
import com.rs.plugin.events.InputStringEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.darkan.core.clan.ClanAction
import org.darkan.core.clan.ClanChannelState
import org.darkan.core.clan.ClanChatter
import org.darkan.core.model.Clan
import org.darkan.core.model.ClanMemberData
import org.darkan.core.model.ClanRank
import org.darkan.core.model.ClanSetting
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.packet.Chat
import org.darkan.core.net.prot.packet.ClanChannelFull
import org.darkan.core.net.prot.packet.ClanSettingsFull
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.net.prot.packet.MessageClanChannel
import org.darkan.core.net.prot.packet.MessageQuickChatClanChannel
import org.darkan.core.net.prot.packet.QuickChatPublic
import org.darkan.core.net.prot.packet.VarclanDisable
import org.darkan.core.net.prot.packet.VarclanEnable
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.social.gateway.ClanChannelUpdate
import org.darkan.core.social.gateway.ClanChatMessage
import org.darkan.core.social.gateway.ClanCommand
import org.darkan.core.social.gateway.ClanCommandResult
import org.darkan.core.social.gateway.ClanDeleted
import org.darkan.core.social.gateway.ClanFetch
import org.darkan.core.social.gateway.ClanMembership
import org.darkan.core.social.gateway.ClanQuickChatMessage
import org.darkan.core.social.gateway.ClanSnapshot
import org.darkan.core.social.gateway.SocialCommand
import org.darkan.core.social.gateway.SocialGatewayWireMessage
import org.darkan.world.clan.ClanRegistry
import org.darkan.world.harness.HarnessPlayer
import org.darkan.world.harness.WorldHarness
import org.darkan.world.server.WorldServer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The clan system as a world player drives it (`docs/clans.md`).
 *
 * The invariant under test is that the world **never** decides anything about a clan: a button
 * becomes exactly one typed [ClanAction] on the gateway, a denial the world can already see
 * produces a message and no traffic at all, and every clan packet the player sees comes from a
 * lobby event rather than a local mutation. Nothing clan-shaped may leave as a
 * [SocialCommand] - the social path is not the clan path, and a regression to it would quietly
 * give the two servers two different rule sets.
 *
 * The seam is [ClanRegistry.transport]: a recorder in place of the social client, driven on an
 * immediate dispatcher so a send is visible by the time the button handler returns.
 */
class ClanFlowTest {

    private companion object {
        const val CLAN = "Testers"
        const val OTHER_CLAN = "Others"

        /** The interfaces the flows live on. */
        const val CLAN_CHAT = 1110
        const val SETTINGS = 1096
        const val INVITE = 1095
        const val MOTIF = 1105
        const val KEYWORDS = 1097
        const val FLAG = 1089
    }

    private val wire = CopyOnWriteArrayList<SocialGatewayWireMessage>()
    private val loggedIn = mutableListOf<HarnessPlayer>()

    @BeforeTest
    fun installRecorder() {
        WorldHarness.boot()
        ClanRegistry.clear()
        wire.clear()
        ClanRegistry.scope = CoroutineScope(Dispatchers.Unconfined)
        ClanRegistry.transport = { msg ->
            wire.add(msg)
            true
        }
    }

    @AfterTest
    fun restore() {
        loggedIn.forEach { WorldHarness.logout(it) }
        loggedIn.clear()
        ClanRegistry.clear()
        ClanRegistry.transport = { WorldServer.sendToGateway(it) }
    }

    // ─── Buttons → actions ───────────────────────────────────────────

    /** 1110:82 with a clan but no channel joins the player's own channel. */
    @Test
    fun clanChatButtonJoinsOwnChannel() {
        val owner = member(CLAN, ClanRank.OWNER)
        owner.player.social.connectedToClan = false

        click(owner, CLAN_CHAT, 82)

        assertEquals(ClanAction.JoinChannel, action())
    }

    /** ...and with the channel already joined, the same button leaves it. */
    @Test
    fun clanChatButtonLeavesOwnChannel() {
        val owner = member(CLAN, ClanRank.OWNER)
        owner.player.social.connectedToClan = true
        joinChannel(CLAN, owner.player.username, ClanRank.OWNER)

        click(owner, CLAN_CHAT, 82)

        assertEquals(ClanAction.LeaveChannel, action())
    }

    /** 1110:91 asks for a clan name and listens in on it as a guest. */
    @Test
    fun guestChannelButtonAsksForANameThenJoins() {
        val player = login("guest")

        click(player, CLAN_CHAT, 91)
        assertTrue(wire.isEmpty(), "the name prompt sent something before the player answered it")
        answerName(player, OTHER_CLAN)

        assertEquals(ClanAction.JoinGuestChannel(OTHER_CLAN), action())
    }

    /** ...and leaves the guest channel when there is one. */
    @Test
    fun guestChannelButtonLeaves() {
        val player = login("guestleave")
        player.player.social.guestedClanChat = OTHER_CLAN

        click(player, CLAN_CHAT, 91)

        assertEquals(ClanAction.LeaveGuestChannel, action())
    }

    /** The ban prompt sends the typed name protocol-formatted, which is how the lobby keys members. */
    @Test
    fun banPromptSendsBan() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, CLAN_CHAT, 95)
        answerName(owner, "Some Guy")

        assertEquals(ClanAction.Ban("some_guy"), action())
    }

    /**
     * The ban list rows are in the order `ClanSettingsFull` sent them - sorted - not the order the
     * document happens to store them in, so a slot id has to be read against that same order or
     * the wrong player is unbanned.
     */
    @Test
    fun unbanBySlotFollowsTheOrderTheSettingsPacketSent() {
        val owner = login("banner")
        owner.player.social.clanName = CLAN
        val clan = clanOf(CLAN, "banner", ClanRank.OWNER)
        clan.bannedUsers.add("zed")
        clan.bannedUsers.add("amy")
        snapshot(clan)
        wire.clear()

        click(owner, CLAN_CHAT, 11, slot = 0)

        assertEquals(ClanAction.Unban("amy"), action())
    }

    /** Leaving the clan takes both confirmations, and neither one alone sends anything. */
    @Test
    fun leavingTheClanNeedsBothConfirmations() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, CLAN_CHAT, 109)
        val conversation = assertNotNull(owner.player.conversation, "leaving the clan asked for no confirmation")
        assertTrue(wire.isEmpty(), "the leave confirmation sent before it was answered")

        conversation.process(0)
        assertTrue(wire.isEmpty(), "one confirmation was enough to leave the clan")

        conversation.process(0)

        assertEquals(ClanAction.Leave, action())
    }

    /** A setting toggle sends only the entry that changed, not the whole settings map. */
    @Test
    fun settingToggleSendsOnlyTheChangedEntry() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, SETTINGS, 94)

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(mapOf(ClanSetting.GUESTS_CAN_ENTER_CC.name to 0), settings.settings)
    }

    /**
     * A rule the world can already see is applied before anything is sent: the player is told why,
     * in the wording both servers share, and the lobby is not troubled at all.
     */
    @Test
    fun aDeniedPrecheckMessagesThePlayerAndSendsNothing() {
        val recruit = member(CLAN, ClanRank.RECRUIT)

        click(recruit, SETTINGS, 94)

        assertTrue(wire.isEmpty(), "a denied action still went out: ${wire.map { it::class.simpleName }}")
        assertTrue(
            messages(recruit).any { it.contains("Only clan admins can edit the clan settings") },
            "the player was not told why: ${messages(recruit)}",
        )
    }

    /** The signpost slider is a clan var, not a setting. */
    @Test
    fun signpostSliderSendsSetVar() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, SETTINGS, 222, slot = 3)

        assertEquals(ClanAction.SetVar(2811, 3), action())
    }

    /** The talk-rank button carries only the talk rank; the kick rank is left alone, and vice versa. */
    @Test
    fun channelRankButtonsSendOneRankEach() {
        val owner = member(CLAN, ClanRank.OWNER)
        owner.player.tempAttribs.setO<Any>("permissionRankEditing", ClanRank.CAPTAIN)

        click(owner, SETTINGS, 558)
        assertEquals(ClanAction.SetChannelRanks(talkRank = ClanRank.CAPTAIN, kickRank = null), action())

        wire.clear()
        click(owner, SETTINGS, 570)
        assertEquals(ClanAction.SetChannelRanks(talkRank = null, kickRank = ClanRank.CAPTAIN), action())
    }

    /** The clanmate panel's save button is one UpdateMember for whatever was picked. */
    @Test
    fun clanmateSaveSendsUpdateMember() {
        val owner = member(CLAN, ClanRank.OWNER, "recruit" to ClanRank.RECRUIT)
        owner.player.tempAttribs.setO<Any>("editClanMate", "recruit")
        owner.player.tempAttribs.setI("editClanMateRank", ClanRank.CAPTAIN.iconId)

        click(owner, SETTINGS, 318)

        assertEquals(ClanAction.UpdateMember("recruit", rank = ClanRank.CAPTAIN), action())
    }

    /** ...and the kick button is a RemoveMember, once confirmed. */
    @Test
    fun clanmateKickSendsRemoveMember() {
        val owner = member(CLAN, ClanRank.OWNER, "recruit" to ClanRank.RECRUIT)
        owner.player.tempAttribs.setO<Any>("editClanMate", "recruit")

        click(owner, SETTINGS, 309)
        assertTrue(wire.isEmpty(), "the kick confirmation sent before it was answered")
        assertNotNull(owner.player.conversation, "the kick asked for no confirmation").process(0)

        assertEquals(ClanAction.RemoveMember("recruit"), action())
    }

    /**
     * Inviting is two halves: the target is shown the inviter's clan read-only (`main = false`,
     * built from the replica - the legacy path read a cached update block that was never filled in,
     * so this interface never opened at all), and accepting sends the invite action.
     */
    @Test
    fun acceptingAnInviteSendsAcceptInvite() {
        val inviter = member(CLAN, ClanRank.OWNER, name = "inviter")
        val target = login("invitee")
        target.clearSent()

        ClansManager.openClanInvite(target.player, inviter.player, inviter.player.clan)

        val settings = target.sent.filterIsInstance<ClanSettingsFull>().singleOrNull()
        assertNotNull(settings, "the invite interface was opened with no clan settings block")
        assertFalse(settings.main, "an invite showed the clan as the player's own")
        assertEquals(CLAN, settings.clanName)

        click(target, INVITE, 33)

        assertEquals(ClanAction.AcceptInvite(CLAN, "inviter"), action())
    }

    /** The motif editor holds its changes locally and saves them in one go. */
    @Test
    fun motifSaveSendsWhatChanged() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, MOTIF, 66, slot = 5) // the top icon, stored one-based
        assertTrue(wire.isEmpty(), "picking a motif icon wrote to the clan before the save")
        click(owner, MOTIF, 120)

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(mapOf(ClanSetting.MOTIF_TOP_ICON.name to 6), settings.settings)
    }

    /** ...and a save with nothing picked sends nothing. */
    @Test
    fun motifSaveWithNoChangeSendsNothing() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, MOTIF, 120)

        assertTrue(wire.isEmpty(), "an unchanged motif was still saved")
    }

    /** The HSL picker leaves a pending motif colour, not a write. */
    @Test
    fun motifColourPickerSavesWithTheRest() {
        val owner = member(CLAN, ClanRank.OWNER)

        ClansManager.openSetMotifColor(owner.player, 0)
        assertNotNull(
            owner.player.tempAttribs.removeO<InputHSLEvent>("pluginHSL"),
            "the colour picker asked for no input",
        ).run(1234)
        assertTrue(wire.isEmpty(), "a motif colour was written to the clan before the save")

        click(owner, MOTIF, 120)

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(mapOf(ClanSetting.MOTIF_TOP_COLOR.name to 1234), settings.settings)
    }

    /** The national flag picker saves the same way. */
    @Test
    fun flagSaveSendsTheNationalFlag() {
        val owner = member(CLAN, ClanRank.OWNER)

        click(owner, FLAG, 30, slot = 7)
        assertTrue(wire.isEmpty(), "picking a flag wrote to the clan before the save")
        click(owner, FLAG, 26)

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(mapOf(ClanSetting.NATIONAL_FLAG.name to 7), settings.settings)
    }

    /** The keyword editor saves all ten pairs it built up in the player's own varbits. */
    @Test
    fun keywordSaveSendsEveryKeywordSetting() {
        val owner = member(CLAN, ClanRank.OWNER)
        owner.player.vars.setVarBit(9155, 3)
        owner.player.vars.setVarBit(9156, 2)

        click(owner, KEYWORDS, 42)

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(20, settings.settings.size, "not every keyword slot was saved")
        assertEquals(3, settings.settings[ClanSetting.KEYWORD0_CATEGORY.name])
        assertEquals(2, settings.settings[ClanSetting.KEYWORD0_INDEX.name])
    }

    /** The forum thread code is a base-36 long setting, and an unparsable one never leaves. */
    @Test
    fun forumQfcSendsUpdateSettings() {
        val owner = member(CLAN, ClanRank.OWNER)

        ClansManager.openForumThreadInterface(owner.player)
        assertNotNull(
            owner.player.tempAttribs.removeO<InputStringEvent>("pluginQFCD"),
            "the forum thread interface asked for no input",
        ).run("15,16,17,18")
        assertTrue(wire.isEmpty(), "an unparsable forum code was still sent")

        ClansManager.openForumThreadInterface(owner.player)
        assertNotNull(owner.player.tempAttribs.removeO<InputStringEvent>("pluginQFCD")).run("zz1")

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(mapOf(ClanSetting.FORUM_QFC.name to "zz1".toLong(36)), settings.settings)
    }

    /** The motto is a string setting. */
    @Test
    fun mottoSendsUpdateSettings() {
        val owner = member(CLAN, ClanRank.OWNER)

        ClansManager.openClanMottoInterface(owner.player)
        assertNotNull(
            owner.player.tempAttribs.removeO<InputStringEvent>("pluginEnterLongText"),
            "the motto interface asked for no input",
        ).run("For the clan")

        val settings = assertIs<ClanAction.UpdateSettings>(action())
        assertEquals(mapOf(ClanSetting.MOTTO.name to "For the clan"), settings.settings)
    }

    /** Creating a clan is a name check first, then the create the player confirmed. */
    @Test
    fun creatingAClanChecksTheNameFirst() {
        val player = login("founder")

        ClansManager.promptName(player.player)
        answerName(player, "New Clan")

        val check = wire.filterIsInstance<ClanCommand>().single()
        assertEquals(ClanAction.CheckName("New Clan"), check.action)

        wire.clear()
        ClanRegistry.handle(ClanCommandResult("founder", check.requestId, "CheckName", ok = true))
        WorldHarness.tick()
        assertNotNull(player.player.conversation, "an approved name asked for no confirmation").process(0)

        assertEquals(ClanAction.Create("New Clan"), action())
    }

    /** A name the world can already see is invalid never reaches the lobby. */
    @Test
    fun anInvalidClanNameIsRefusedLocally() {
        val player = login("founder2")
        player.clearSent()

        ClansManager.promptName(player.player)
        answerName(player, "a name far too long to be a clan")

        assertTrue(wire.isEmpty(), "an invalid clan name still went out")
        assertTrue(
            messages(player).any { it.contains("12 or fewer characters") },
            "the player was not told why: ${messages(player)}",
        )
    }

    // ─── Chat ────────────────────────────────────────────────────────

    /**
     * Clan chat is a typed action, not a forwarded `Chat` packet. The control is
     * [friendsChatIsASocialAction]: chat type 1 goes out as a social action and, with no gateway
     * attached in the harness, says so - clan chat must not produce that.
     */
    @Test
    fun clanChatIsNotASocialAction() {
        val owner = member(CLAN, ClanRank.OWNER)
        joinChannel(CLAN, owner.player.username, ClanRank.OWNER)
        owner.clearSent()
        owner.player.chatType = 2

        owner.push(Chat(type = -1, color = 0, effect = 0, message = "hello clan"))
        WorldHarness.tick()
        WorldHarness.tick()

        assertEquals(ClanAction.Chat(guest = false, message = PublicChatMessage("hello clan", 0)), action())
        assertFalse(
            wire.any { it is SocialCommand },
            "clan chat left as a social action: ${wire.map { it::class.simpleName }}",
        )
        assertFalse(
            messages(owner).any { it.contains(WorldServer.FRIEND_SERVER_UNAVAILABLE) },
            "clan chat took the social action path",
        )
    }

    /** The control: friends chat does still forward, so the assertion above can fail. */
    @Test
    fun friendsChatIsASocialAction() {
        val player = login("fcline")
        player.clearSent()
        player.player.chatType = 1

        player.push(Chat(type = -1, color = 0, effect = 0, message = "hello friends"))
        WorldHarness.tick()
        WorldHarness.tick()

        assertTrue(
            messages(player).any { it.contains(WorldServer.FRIEND_SERVER_UNAVAILABLE) },
            "friends chat no longer goes out as a social action: ${messages(player)}",
        )
    }

    /** Guest chat is the same action with the guest flag set. */
    @Test
    fun guestChatIsSentAsAGuestAction() {
        snapshot(clanOf(CLAN, "owner", ClanRank.OWNER))
        val guest = login("listener")
        guest.player.social.guestedClanChat = CLAN
        joinChannel(CLAN, "listener", ClanRank.NONE)
        guest.player.chatType = 3
        wire.clear()

        guest.push(Chat(type = -1, color = 0, effect = 0, message = "hi from outside"))
        WorldHarness.tick()

        assertTrue(assertIs<ClanAction.Chat>(action()).guest, "a guest line was sent as a member's")
    }

    /** Quick chat takes the same route, carrying the file id rather than any text. */
    @Test
    fun quickClanChatIsSentAsAnAction() {
        val owner = member(CLAN, ClanRank.OWNER)
        joinChannel(CLAN, owner.player.username, ClanRank.OWNER)
        wire.clear()

        owner.push(QuickChatPublic(chatType = 2, qcId = 42, messageData = null))
        WorldHarness.tick()

        val quickChat = assertIs<ClanAction.QuickChat>(action())
        assertEquals(42, quickChat.message.fileId)
        assertFalse(quickChat.guest)
    }

    // ─── Lobby events → client packets ───────────────────────────────

    /**
     * A snapshot is a full resend: members get their own clan, guests of the channel the read-only
     * view. The client is never sent a delta.
     */
    @Test
    fun aSnapshotPushesSettingsToMembersAndGuests() {
        val owner = login("snapowner")
        owner.player.social.clanName = CLAN
        val guest = login("snapguest")
        guest.player.social.guestedClanChat = CLAN
        owner.clearSent()
        guest.clearSent()

        snapshot(clanOf(CLAN, "snapowner", ClanRank.OWNER))

        val toMember = owner.sent.filterIsInstance<ClanSettingsFull>().singleOrNull()
        assertNotNull(toMember, "a member got no settings block on a snapshot")
        assertTrue(toMember.main, "a member got the guest view of their own clan")

        val toGuest = guest.sent.filterIsInstance<ClanSettingsFull>().singleOrNull()
        assertNotNull(toGuest, "a guest of the channel got no settings block on a snapshot")
        assertFalse(toGuest.main, "a guest got the clan as their own")
    }

    /** Joining a channel is the channel plus the clan's vars; leaving is the one-byte form. */
    @Test
    fun channelUpdatesJoinAndLeaveThePlayer() {
        val owner = member(CLAN, ClanRank.OWNER, name = "chanowner")
        owner.clearSent()

        joinChannel(CLAN, "chanowner", ClanRank.OWNER)

        val joined = owner.sent.filterIsInstance<ClanChannelFull>().singleOrNull()
        assertNotNull(joined, "joining the channel sent no channel block")
        assertTrue(joined.main, "a member joined their own channel as a guest")
        assertEquals(1, joined.chatters?.size)
        assertTrue(owner.sent.any { it is VarclanEnable }, "the clan vars were not enabled on join")

        owner.clearSent()
        ClanRegistry.handle(ClanChannelUpdate(channelState(CLAN, emptyList())))
        WorldHarness.tick()

        val left = owner.sent.filterIsInstance<ClanChannelFull>().singleOrNull()
        assertNotNull(left, "leaving the channel sent no channel block")
        assertNull(left.clanName, "leaving the channel sent a whole channel instead of the leave form")
        assertTrue(owner.sent.any { it is VarclanDisable }, "the clan vars were not disabled on leave")
    }

    /** A player still in a changed channel gets the whole thing again. */
    @Test
    fun aChangedChannelIsResentWhole() {
        val owner = member(CLAN, ClanRank.OWNER, name = "resend")
        joinChannel(CLAN, "resend", ClanRank.OWNER)
        owner.clearSent()

        joinChannel(
            CLAN,
            listOf(
                ClanChatter("resend", "Resend", ClanRank.OWNER, 1),
                ClanChatter("newcomer", "Newcomer", ClanRank.RECRUIT, 1),
            ),
        )

        val resent = owner.sent.filterIsInstance<ClanChannelFull>().singleOrNull()
        assertNotNull(resent, "a changed channel was not resent")
        assertEquals(2, resent.chatters?.size)
    }

    /** The lobby's word on membership is written into the account, not merely displayed. */
    @Test
    fun aMembershipPushWritesTheSocialFields() {
        val player = login("joiner")

        ClanRegistry.handle(ClanMembership("joiner", clanName = CLAN, inChannel = true, guestChannel = OTHER_CLAN))
        WorldHarness.tick()

        assertEquals(CLAN, player.player.social.clanName)
        assertTrue(player.player.social.connectedToClan)
        assertEquals(OTHER_CLAN, player.player.social.guestedClanChat)
    }

    /** ...and being told of no clan is what sends the client the leave form. */
    @Test
    fun aMembershipPushWithNoClanSendsTheLeaveForm() {
        val player = login("leaver")
        player.player.social.clanName = CLAN
        player.clearSent()

        ClanRegistry.handle(ClanMembership("leaver", clanName = null, inChannel = false, guestChannel = null))
        WorldHarness.tick()

        val settings = player.sent.filterIsInstance<ClanSettingsFull>().singleOrNull()
        assertNotNull(settings, "leaving the clan told the client nothing")
        assertNull(settings.clanName, "leaving the clan sent a whole settings block")
        assertTrue(settings.main)
        assertNull(player.player.social.clanName)
    }

    /**
     * One line in a channel reaches every local chatter, marked per recipient: affined for the
     * clan's own members, listened for guests. Getting this backwards paints guest chat in the
     * member colour and the other way round.
     */
    @Test
    fun aChannelLineIsMarkedPerRecipient() {
        val owner = login("linemember")
        owner.player.social.clanName = CLAN
        val guest = login("lineguest")
        guest.player.social.guestedClanChat = CLAN
        snapshot(clanOf(CLAN, "linemember", ClanRank.OWNER))
        joinChannel(
            CLAN,
            listOf(
                ClanChatter("linemember", "Linemember", ClanRank.OWNER, 1),
                ClanChatter("lineguest", "Lineguest", ClanRank.NONE, 1),
            ),
        )
        owner.clearSent()
        guest.clearSent()

        ClanRegistry.handle(ClanChatMessage(CLAN, sender = "Linemember", rights = 2, message = PublicChatMessage("hi", 0)))
        WorldHarness.tick()

        val toMember = owner.sent.filterIsInstance<MessageClanChannel>().singleOrNull()
        assertNotNull(toMember, "a member got no clan line")
        assertFalse(toMember.guest, "a member's own clan line was marked as a guest's")
        assertEquals("hi", toMember.message)
        assertEquals(2, toMember.crown)

        val toGuest = guest.sent.filterIsInstance<MessageClanChannel>().singleOrNull()
        assertNotNull(toGuest, "a guest got no clan line")
        assertTrue(toGuest.guest, "a guest's view of a clan line was marked as a member's")
    }

    /** Quick chat lines are routed the same way. */
    @Test
    fun aQuickChatChannelLineReachesTheChatters() {
        val owner = login("qcmember")
        owner.player.social.clanName = CLAN
        snapshot(clanOf(CLAN, "qcmember", ClanRank.OWNER))
        joinChannel(CLAN, "qcmember", ClanRank.OWNER)
        owner.clearSent()

        ClanRegistry.handle(
            ClanQuickChatMessage(CLAN, sender = "Qcmember", rights = 0, message = QuickChatMessage(message = "", fileId = 7)),
        )
        WorldHarness.tick()

        val line = owner.sent.filterIsInstance<MessageQuickChatClanChannel>().singleOrNull()
        assertNotNull(line, "a quick chat clan line was not delivered")
        assertEquals(7, line.message.fileId)
    }

    /** A result carrying a message shows it verbatim. */
    @Test
    fun aCommandResultMessageIsShownToThePlayer() {
        val player = login("resulted")
        player.clearSent()

        ClanRegistry.handle(
            ClanCommandResult("resulted", requestId = 1, action = "Create", ok = false, message = "That clan name is taken."),
        )
        WorldHarness.tick()

        assertTrue(
            messages(player).any { it == "That clan name is taken." },
            "the lobby's message was not shown: ${messages(player)}",
        )
    }

    // ─── The replica itself ──────────────────────────────────────────

    /** A clan the world has never seen is asked for once, however many callers want it. */
    @Test
    fun fetchAsksOnceAndAnswersEverybodyOnTheSnapshot() {
        val answers = CopyOnWriteArrayList<Clan?>()

        ClanRegistry.fetch(CLAN, Consumer { answers.add(it) })
        ClanRegistry.fetch(CLAN, Consumer { answers.add(it) })

        assertEquals(listOf(ClanFetch(CLAN)), wire.toList(), "the clan was not asked for exactly once")
        assertTrue(answers.isEmpty(), "a fetch answered before the snapshot arrived")

        snapshot(clanOf(CLAN, "owner", ClanRank.OWNER))

        assertEquals(2, answers.size, "not every waiting fetch was answered")
        assertEquals(CLAN, answers.first()?.name)
    }

    /** A clan the lobby says is gone answers its waiters with null rather than leaving them hanging. */
    @Test
    fun aDeletedClanAnswersWaitingFetchesWithNull() {
        val answers = CopyOnWriteArrayList<Clan?>()
        ClanRegistry.fetch(CLAN, Consumer { answers.add(it) })

        ClanRegistry.handle(ClanDeleted(CLAN))
        WorldHarness.tick()

        assertEquals(1, answers.size)
        assertNull(answers.first())
        assertNull(ClanRegistry.clan(CLAN))
    }

    /**
     * The replica is only valid while the socket that filled it is up, so a reconnect starts from
     * nothing and the lobby's connect-time resync refills it. `WorldServer.start` wires this to the
     * social client's connection changes.
     */
    @Test
    fun aReconnectDropsTheReplica() {
        snapshot(clanOf(CLAN, "owner", ClanRank.OWNER))
        joinChannel(CLAN, "owner", ClanRank.OWNER)
        assertNotNull(ClanRegistry.clan(CLAN))
        assertNotNull(ClanRegistry.channel(CLAN))
        val answers = CopyOnWriteArrayList<Clan?>()
        ClanRegistry.fetch(OTHER_CLAN, Consumer { answers.add(it) })

        ClanRegistry.clear()
        WorldHarness.tick()

        assertNull(ClanRegistry.clan(CLAN), "a reconnect kept a stale clan")
        assertNull(ClanRegistry.channel(CLAN), "a reconnect kept a stale channel")
        assertEquals(1, answers.size, "a fetch outstanding across a reconnect was never answered")
        assertNull(answers.first())
    }

    // ─── Harness plumbing ────────────────────────────────────────────

    private fun login(username: String): HarnessPlayer =
        WorldHarness.login(username = username, rights = Rights.PLAYER).also { loggedIn.add(it) }

    /** A logged-in player who is [rank] of [clanName], with the clan already in the replica. */
    private fun member(
        clanName: String,
        rank: ClanRank,
        vararg others: Pair<String, ClanRank>,
        name: String = "owner",
    ): HarnessPlayer {
        val player = login(name)
        player.player.social.clanName = clanName
        snapshot(clanOf(clanName, if (rank == ClanRank.OWNER) name else "theowner", rank, name, *others))
        wire.clear()
        player.clearSent()
        return player
    }

    private fun clanOf(
        name: String,
        leader: String,
        actorRank: ClanRank,
        actor: String = leader,
        vararg others: Pair<String, ClanRank>,
    ): Clan {
        val clan = Clan(name = name, leaderUsername = leader)
        clan.members[leader] = ClanMemberData(rank = ClanRank.OWNER, joinDate = 1)
        clan.members[actor] = ClanMemberData(rank = actorRank, joinDate = 2)
        others.forEachIndexed { index, (username, rank) ->
            clan.members[username] = ClanMemberData(rank = rank, joinDate = (index + 3).toLong())
        }
        return clan
    }

    private fun snapshot(clan: Clan) {
        ClanRegistry.handle(ClanSnapshot(clan))
        WorldHarness.tick()
    }

    private fun channelState(clanName: String, chatters: List<ClanChatter>) = ClanChannelState(
        clanName = clanName,
        updateNum = 1,
        talkRank = ClanRank.NONE,
        kickRank = ClanRank.ADMIN,
        chatters = chatters,
    )

    private fun joinChannel(clanName: String, username: String, rank: ClanRank) =
        joinChannel(clanName, listOf(ClanChatter(username, username, rank, 1)))

    private fun joinChannel(clanName: String, chatters: List<ClanChatter>) {
        ClanRegistry.handle(ClanChannelUpdate(channelState(clanName, chatters)))
        WorldHarness.tick()
    }

    private fun click(player: HarnessPlayer, interfaceId: Int, component: Int, slot: Int = -1, op: Int = -1) {
        PluginManager.handle(ButtonClickEvent(player.player, interfaceId, component, slot, -1, op))
    }

    private fun answerName(player: HarnessPlayer, name: String) {
        assertNotNull(
            player.player.tempAttribs.removeO<InputStringEvent>("pluginEnterName"),
            "no name prompt was open",
        ).run(name)
    }

    /** The single [ClanAction] the recorder saw; fails if the world sent anything else. */
    private fun action(): ClanAction {
        val commands = wire.filterIsInstance<ClanCommand>()
        assertEquals(1, commands.size, "expected exactly one clan command, got ${wire.map { it::class.simpleName }}")
        return commands.single().action
    }

    private fun messages(player: HarnessPlayer): List<String> =
        player.sent.filterIsInstance<GameMessage>().map { it.message }
}
