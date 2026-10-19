package org.darkan.world

import com.rs.game.model.entity.player.Player
import org.darkan.core.JsonFileManager
import org.darkan.world.harness.WorldHarness
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerNumericStateTest {

    private fun <T> withPlayer(name: String, block: (Player) -> T): T {
        val harnessPlayer = WorldHarness.login(name)
        try {
            return block(harnessPlayer.player)
        } finally {
            WorldHarness.logout(harnessPlayer)
        }
    }

    @Test
    fun `a counter keeps counting past the int range instead of wrapping negative`() = withPlayer("counters") { player ->
        player.incrementCount("power", Long.MAX_VALUE / 2)
        player.incrementCount("power", Long.MAX_VALUE / 2)
        player.incrementCount("power", Long.MAX_VALUE / 2)

        assertEquals(Long.MAX_VALUE, player.getCounterValueL("power"))
        assertEquals(Int.MAX_VALUE, player.getCounterValue("power"))
    }

    @Test
    fun `summing counters past the int range saturates rather than wrapping`() = withPlayer("summed") { player ->
        player.incrementCount("codex_a", Long.MAX_VALUE)
        player.incrementCount("codex_b", Long.MAX_VALUE)

        assertEquals(Long.MAX_VALUE, player.getCounterValueContainingL("codex_"))
    }

    @Test
    fun `a counter above the int range survives a save and load`() = withPlayer("saved") { player ->
        val value = Int.MAX_VALUE.toLong() * 100L
        player.incrementCount("alched", value)
        player.set("AlchemyCoins", value)
        player.sendNPCKill("kalphite queen")

        val loaded = JsonFileManager.fromJSONString(JsonFileManager.toJson(player), Player::class.java)

        assertEquals(value, loaded.getCounterValueL("alched"))
        assertEquals(value, loaded.getL("AlchemyCoins", 0L))
        assertEquals(1L, loaded.getNumberKilledL("kalphite queen"))
    }

    @Test
    fun `an attribute reads back whether it was stored as an int, a long or a json double`() = withPlayer("attribs") { player ->
        player.set("asInt", 7)
        player.set("asLong", 8L)
        player.set("asDouble", 9.0)

        assertEquals(7L, player.getL("asInt", 0L))
        assertEquals(8L, player.getL("asLong", 0L))
        assertEquals(9L, player.getL("asDouble", 0L))
        assertEquals(7, player.getI("asInt", 0))
        assertEquals(8, player.getI("asLong", 0))
        assertEquals(9, player.getI("asDouble", 0))
    }

    @Test
    fun `an attribute larger than the int range clamps instead of wrapping when read as an int`() = withPlayer("clamped") { player ->
        player.set("huge", Long.MAX_VALUE)
        player.incrementL("huge", 1L)

        assertEquals(Long.MAX_VALUE, player.getL("huge", 0L))
        assertEquals(Int.MAX_VALUE, player.getI("huge", 0))
    }

    @Test
    fun `a missing or non numeric attribute falls back to the default instead of throwing`() = withPlayer("absent") { player ->
        player.set("text", "codex")

        assertEquals(-5L, player.getL("missing", -5L))
        assertEquals(-5, player.getI("text", -5))
    }
}
