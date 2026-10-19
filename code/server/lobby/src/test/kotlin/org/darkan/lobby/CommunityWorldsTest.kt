package org.darkan.lobby

import kotlinx.coroutines.runBlocking
import org.darkan.core.model.Account
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.login.LoginToken
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.social.gateway.AnnouncementBroadcast
import org.darkan.core.social.gateway.AnnouncementChannel
import org.darkan.core.social.SocialAction
import org.darkan.core.social.gateway.SocialCommand
import org.darkan.core.social.gateway.LoginRefusal
import org.darkan.core.social.gateway.LoginTokenVerdict
import org.darkan.core.social.gateway.PlayerOnline
import org.darkan.core.social.gateway.VerifyLoginToken
import org.darkan.core.worldlist.Country
import org.darkan.core.worldlist.WorldMetadata
import org.darkan.lobby.harness.LobbyHarness
import org.darkan.lobby.harness.LobbyHarness.FakeWorld
import org.darkan.lobby.social.CommunityWorlds
import org.darkan.lobby.social.CommunityWorlds.Tier
import org.darkan.lobby.social.SocialGateway
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommunityWorldsTest {

    private val policy = CommunityWorlds(
        enabled = true,
        communityToken = COMMUNITY_TOKEN,
        officialToken = OFFICIAL_TOKEN,
        officialWorldMax = 99,
        localWorldId = 300,
        loginTokenSecret = SECRET,
    )

    @BeforeTest
    fun resetGateway() {
        LobbyHarness.reset()
        SocialGateway.communityWorlds = policy
    }

    private fun tokenFor(account: Account, secret: String = SECRET) =
        LoginToken.issue(account.username, System.currentTimeMillis(), 60_000L, secret)

    private suspend fun FakeWorld.verify(typedName: String, token: String): LoginTokenVerdict {
        deliver(VerifyLoginToken(requestId = 7, username = typedName, token = token))
        return messagesOf<LoginTokenVerdict>().last()
    }

    private fun numbered(number: Int) =
        WorldMetadata(number, "203.0.113.7", 43595, "", Country.USA, false, true, true, false, false)

    @Test
    fun `only the private token is official and the public one needs debug`() {
        assertEquals(Tier.OFFICIAL, policy.tierFor(OFFICIAL_TOKEN))
        assertEquals(Tier.COMMUNITY, policy.tierFor(COMMUNITY_TOKEN))
        assertNull(policy.tierFor("guess"))
        assertNull(CommunityWorlds(enabled = false, communityToken = COMMUNITY_TOKEN, officialToken = OFFICIAL_TOKEN).tierFor(COMMUNITY_TOKEN))
        assertEquals(Tier.COMMUNITY, CommunityWorlds(enabled = true, communityToken = COMMUNITY_TOKEN, officialToken = COMMUNITY_TOKEN).tierFor(COMMUNITY_TOKEN))
    }

    @Test
    fun `community worlds cannot take an official, local or lobby number`() {
        assertNotNull(policy.refusalFor(numbered(1)))
        assertNotNull(policy.refusalFor(numbered(99)))
        assertNotNull(policy.refusalFor(numbered(300)))
        assertNotNull(policy.refusalFor(numbered(1100)))
        assertNull(policy.refusalFor(numbered(100)))
    }

    @Test
    fun `a community world cannot displace a world that is online`() = runBlocking<Unit> {
        LobbyHarness.world(150, community = true).connect()
        assertFalse(LobbyHarness.world(150, community = true).tryConnect())
        assertTrue(LobbyHarness.world(151, community = true).tryConnect())
    }

    @Test
    fun `a login token admits its own account on every world it is presented to`() = runBlocking<Unit> {
        val player = LobbyHarness.account("player")
        val world = LobbyHarness.world(150, community = true).connect()
        val token = tokenFor(player)

        val verdict = world.verify(player.username, token)
        assertEquals(player.username, verdict.username)
        assertEquals(player.displayName, verdict.displayName)
        assertEquals(player.username, verdict.account?.username, "the world gets a credential-free view of the account")
        assertEquals(player.username, world.verify(player.username, token).username, "the client re-presents its one token on every world")
    }

    @Test
    fun `a token admits the account it was issued for whatever name was typed, and a forged one nobody at all`() = runBlocking<Unit> {
        val player = LobbyHarness.account("player")
        val other = LobbyHarness.account("other")
        val world = LobbyHarness.world(150, community = true).connect()

        assertEquals(player.username, world.verify(other.username, tokenFor(player)).username)
        assertNull(world.verify(player.username, tokenFor(player, secret = "not-the-lobby-secret")).username)
    }

    @Test
    fun `a token issued before the name screen still admits the account it renamed`() = runBlocking<Unit> {
        val signup = Account("signup@test.invalid", "signup@test.invalid", "hash")
        LobbyHarness.accounts.put(signup)
        val world = LobbyHarness.world(150, community = true).connect()
        val token = tokenFor(signup)

        @Suppress("DEPRECATION")
        signup.initializeUsername("signup")
        signup.nameChangeRequired = false
        LobbyHarness.accounts.rename("signup@test.invalid", signup)

        assertEquals("signup", world.verify("signup", token).username)
    }

    @Test
    fun `every refusal names itself so the world can tell the player what is actually wrong`() = runBlocking<Unit> {
        val world = LobbyHarness.world(150, community = true).connect()
        val banned = LobbyHarness.account("banned").also { it.banPerm() }
        val unnamed = Account("unnamed@test.invalid", "unnamed@test.invalid", "hash")
        LobbyHarness.accounts.put(unnamed)
        val ghost = LoginToken.issue("ghost", System.currentTimeMillis(), 60_000L, SECRET)

        assertEquals(LoginRefusal.BANNED, world.verify(banned.username, tokenFor(banned)).refusal)
        assertEquals(LoginRefusal.NAME_REQUIRED, world.verify(unnamed.username, tokenFor(unnamed)).refusal)
        assertEquals(LoginRefusal.NO_ACCOUNT, world.verify("ghost", ghost).refusal)
        assertEquals(LoginRefusal.TOKEN, world.verify(banned.username, tokenFor(banned, secret = "not-the-lobby-secret")).refusal)
    }

    @Test
    fun `a community world brings only its admitted players online, under their own names`() = runBlocking<Unit> {
        val admitted = LobbyHarness.account("admitted")
        val stranger = LobbyHarness.account("stranger")
        val world = LobbyHarness.world(150, community = true).connect()
        world.verify(admitted.username, tokenFor(admitted))

        world.deliver(PlayerOnline(admitted.username, displayName = "Trent", rightsCrown = 2, privateStatus = 0))
        world.playerOnline(stranger)

        assertEquals(150, SocialGateway.worldIdOf(admitted.username))
        assertEquals(admitted.displayName, SocialGateway.displayNameOf(admitted.username))
        assertNull(SocialGateway.worldIdOf(stranger.username))
    }

    @Test
    fun `a community world can neither act for players it does not have nor announce`() = runBlocking<Unit> {
        val victim = LobbyHarness.account("victim")
        val target = LobbyHarness.account("target")
        val world = LobbyHarness.world(150, community = true).connect()
        val lobby = LobbyHarness.lobbyLogin(target)
        lobby.clearSent()

        world.deliver(SocialCommand(victim.username, SocialAction.AddFriend(target.displayName)))
        world.deliver(AnnouncementBroadcast(AnnouncementChannel.GAME, "Free bonds on world 150"))

        assertTrue(assertNotNull(Accounts.find(victim.username)).social.friends.isEmpty())
        assertTrue(lobby.sent.none { it is GameMessage })
    }

    private companion object {
        const val SECRET = "community-worlds-test-secret"
        const val COMMUNITY_TOKEN = "community-token"
        const val OFFICIAL_TOKEN = "official-token"
    }
}
