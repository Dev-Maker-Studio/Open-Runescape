package org.darkan.lobby.command

import org.darkan.core.model.Rights
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LobbyCommandsTest {

    @Test
    fun `the password commands are registered at the right tiers`() {
        val player = LobbyCommands.available(Rights.PLAYER, "someone").map { it.name }
        val owner = LobbyCommands.available(Rights.OWNER, "trent").map { it.name }
        assertTrue("changepass" in player)
        assertTrue(player.none { it.startsWith("changepassother") })
        assertTrue(owner.any { it.startsWith("changepassother") })
    }

    @Test
    fun `the owner tier is locked to the configured owner name`() {
        assertTrue(LobbyCommands.allowed(Rights.OWNER, Rights.OWNER, "trent", debug = false, owner = "trent"))
        assertTrue(LobbyCommands.allowed(Rights.OWNER, Rights.OWNER, "Trent", debug = false, owner = "trent"))
        assertFalse(LobbyCommands.allowed(Rights.OWNER, Rights.OWNER, "impostor", debug = false, owner = "trent"))
        assertFalse(LobbyCommands.allowed(Rights.OWNER, Rights.ADMIN, "trent", debug = false, owner = "trent"))
    }

    @Test
    fun `debug mode locks every administrative tier to the configured owner`() {
        assertTrue(LobbyCommands.allowed(Rights.ADMIN, Rights.ADMIN, "someone", debug = false, owner = "trent"))
        assertFalse(LobbyCommands.allowed(Rights.ADMIN, Rights.ADMIN, "someone", debug = true, owner = "trent"))
        assertFalse(LobbyCommands.allowed(Rights.MOD, Rights.OWNER, "someone", debug = true, owner = "trent"))
        assertTrue(LobbyCommands.allowed(Rights.ADMIN, Rights.OWNER, "trent", debug = true, owner = "trent"))
        assertTrue(LobbyCommands.allowed(Rights.PLAYER, Rights.PLAYER, "someone", debug = true, owner = "trent"))
    }
}
