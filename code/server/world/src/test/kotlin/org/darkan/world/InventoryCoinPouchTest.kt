package org.darkan.world

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import org.darkan.world.harness.WorldHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryCoinPouchTest {

    private fun <T> withPlayer(name: String, block: (Player) -> T): T {
        val harnessPlayer = WorldHarness.login(name)
        try {
            return block(harnessPlayer.player)
        } finally {
            WorldHarness.logout(harnessPlayer)
        }
    }

    @Test
    fun `a payout beyond the int range is held whole in the pouch`() = withPlayer("rich") { player ->
        val payout = Int.MAX_VALUE.toLong() * 4L
        player.inventory.addCoins(payout)

        assertEquals(payout, player.inventory.getCoins())
        assertTrue(player.inventory.hasCoins(payout))
        assertFalse(player.inventory.hasCoins(payout + 1L))
    }

    @Test
    fun `every positive payout reaches the player once the pouch is past the int range`() = withPlayer("alcher") { player ->
        player.inventory.addCoins(Int.MAX_VALUE.toLong() * 2L)
        val before = player.inventory.getCoins()

        player.inventory.addCoins(30000)

        assertEquals(before + 30000L, player.inventory.getCoins())
    }

    @Test
    fun `a cost beyond the int range is taken from the pouch`() = withPlayer("spender") { player ->
        val cost = Int.MAX_VALUE.toLong() * 3L
        player.inventory.addCoins(cost + 500L)

        player.inventory.removeCoins(cost)

        assertEquals(500L, player.inventory.getCoins())
    }

    @Test
    fun `a pouch at the long ceiling stops growing instead of wrapping negative`() = withPlayer("capped") { player ->
        player.inventory.addCoins(Long.MAX_VALUE)
        player.inventory.addCoins(Long.MAX_VALUE)

        assertEquals(Long.MAX_VALUE, player.inventory.getCoins())
    }

    @Test
    fun `an inventory stack counts toward the total without wrapping a full pouch`() = withPlayer("mixed") { player ->
        player.inventory.addCoins(Long.MAX_VALUE)
        player.inventory.addItem(Item(Obj.coins, 1000))

        assertEquals(Long.MAX_VALUE, player.inventory.getCoins())
    }
}
