package org.darkan.lobby.name

import kotlinx.coroutines.runBlocking
import org.darkan.core.model.Account
import org.darkan.core.mongo.collections.Accounts
import org.darkan.lobby.harness.LobbyHarness
import org.darkan.lobby.harness.RecordingSession
import org.darkan.lobby.web.model.LobbyPlayer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LobbyNameScreenTest {

    @BeforeTest
    fun reset() = LobbyHarness.reset()

    @Test
    fun `an account holding a name of its own is never sent to the name screen`() {
        assertFalse(LobbyNameScreen.holds(LobbyHarness.account("named")))
        assertFalse(LobbyNameScreen.holds(Account("x", "x@test.invalid", "hash")))
    }

    @Test
    fun `a sign-up still under its email is held until it takes a name`() {
        assertTrue(LobbyNameScreen.holds(Account("signup@test.invalid", "signup@test.invalid", "hash")))
    }

    @Test
    fun `naming a sign-up takes the username it typed`() = runBlocking<Unit> {
        val signup = Account("signup@test.invalid", "signup@test.invalid", "hash")
        LobbyHarness.accounts.put(signup)

        name(signup, "Signup")

        assertEquals("signup", Accounts.find("signup@test.invalid")?.username)
        assertEquals("Signup", Accounts.find("signup")?.displayName)
    }

    @Test
    fun `renaming a named account leaves the username it logs in with alone`() = runBlocking<Unit> {
        val account = Account("x", "x@test.invalid", "hash")
        LobbyHarness.accounts.put(account)

        name(account, "Xx")

        assertEquals("x", Accounts.find("x")?.username)
        assertEquals("Xx", Accounts.find("x")?.displayName)
        assertNull(Accounts.find("xx"))
    }

    private suspend fun name(account: Account, input: String) {
        val player = LobbyPlayer(RecordingSession(LobbyHarness.codec), account)
        LobbyNameScreen.submit(player, input)
        LobbyNameScreen.submit(player, input)
    }
}
