package org.darkan.world

import com.rs.game.content.death.GraveRecord
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import org.darkan.core.JsonFileManager
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerDeathTest {

    private class OwnDeathController : Controller() {
        var deaths = 0
        override fun start() {}
        override fun sendDeath(): Boolean {
            deaths++
            return false
        }
    }

    private fun <T> withDeadPlayer(name: String, block: (Player, OwnDeathController) -> T): T {
        val harnessPlayer = WorldHarness.login(name)
        try {
            val controller = OwnDeathController()
            harnessPlayer.player.controllerManager.startController(controller)
            harnessPlayer.player.hitpoints = 0
            harnessPlayer.player.sendDeath(null)
            return block(harnessPlayer.player, controller)
        } finally {
            WorldHarness.logout(harnessPlayer)
        }
    }

    @Test
    fun `a controller handling its own death leaves the player locked until it restores their hitpoints`() = withDeadPlayer("ctrldeath") { player, _ ->
        assertTrue(player.isDying)
        assertTrue(player.isLocked)

        player.reset()
        WorldHarness.tick()

        assertFalse(player.isDying)
        assertFalse(player.isLocked)
    }

    @Test
    fun `healing cannot revive a dying player`() = withDeadPlayer("deadheal") { player, _ ->
        player.heal(player.maxHitpoints)
        WorldHarness.tick(20)

        assertTrue(player.isDead)
        assertTrue(player.isDying)
    }

    @Test
    fun `a second death cannot start while one is in progress`() = withDeadPlayer("doubledeath") { player, controller ->
        player.sendDeath(null)
        player.sendDeath(null)

        assertEquals(1, controller.deaths)
    }

    @Test
    fun `a grave record survives a save and load`() {
        val harnessPlayer = WorldHarness.login("graverecord")
        try {
            val player = harnessPlayer.player
            player.graveRecord = GraveRecord(Tile.of(3222, 3219, 0), 0, 123, true, listOf(Item(Obj.abyssal_whip, 1), Item(Obj.coins, 5000)))

            val loaded = JsonFileManager.fromJSONString(JsonFileManager.toJson(player), Player::class.java).graveRecord

            assertEquals(Tile.of(3222, 3219, 0).id, loaded.tile.id)
            assertEquals(123, loaded.ticks)
            assertTrue(loaded.blessed)
            assertEquals(listOf(Obj.abyssal_whip to 1, Obj.coins to 5000), loaded.items.map { it.id to it.amount })
        } finally {
            WorldHarness.logout(harnessPlayer)
        }
    }
}
