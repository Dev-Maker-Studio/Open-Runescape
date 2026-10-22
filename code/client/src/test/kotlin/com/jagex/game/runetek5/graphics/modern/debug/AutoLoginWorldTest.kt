package com.jagex.game.runetek5.graphics.modern.debug

import com.jagex.game.runetek5.network.world.WorldDescriptor
import org.junit.Assert.*
import org.junit.Test

class AutoLoginWorldTest {
    private fun world(id: Int, players: Int = 0) = WorldDescriptor().apply {
        worldNumber = id
        playerCount = players
    }

    @Test
    fun selectsAnAdvertisedAvailableWorldWithoutUsingTheCacheEndpoint() {
        val selected = AutoLogin.advertisedWorld(listOf(world(50), world(12), world(1, -1)), null)
        assertEquals(12, selected!!.worldNumber)
    }

    @Test
    fun explicitTargetIsNeverReplacedByAnotherWorld() {
        assertEquals(50, AutoLogin.advertisedWorld(listOf(world(12), world(50)), 50)!!.worldNumber)
        assertNull(AutoLogin.advertisedWorld(listOf(world(12)), 50))
        assertNull(AutoLogin.advertisedWorld(listOf(world(50, -1)), 50))
    }

    @Test
    fun emptyListWaitsInsteadOfGuessingAnEndpoint() {
        assertNull(AutoLogin.advertisedWorld(emptyList(), null))
    }
}
