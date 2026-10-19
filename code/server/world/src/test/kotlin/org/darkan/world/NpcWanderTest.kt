package org.darkan.world

import com.rs.game.model.entity.npc.parseWanderRanges
import com.rs.game.model.entity.npc.wanderDestination
import org.darkan.core.gameval.Gameval
import org.darkan.core.gameval.GamevalCatalog
import world.gregs.voidps.type.Tile
import java.io.File
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpcWanderTest {

    private val tempDirs = mutableListOf<File>()

    @AfterTest
    fun deleteSyntheticCatalogs() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
    }

    private fun synthetic(): GamevalCatalog {
        val dir = Files.createTempDirectory("npcwander-test").toFile()
        tempDirs += dir
        File(dir, "npc.json").writeText("""{"revision":727,"type":"npc","entries":{"41":"chicken","42":"npc_42","100":"goblin"}}""")
        return GamevalCatalog(dir, retryMillis = 0L)
    }

    @Test
    fun `ranges key by gameval name or by id and skip unknown names`() {
        val ranges = parseWanderRanges("""{"chicken":5,"goblin":0,"42":15,"no_such_npc":3}""", synthetic())
        assertEquals(mapOf(41 to 5, 100 to 0, 42 to 15), ranges)
    }

    @Test
    fun `a wander destination stays within the range of the respawn tile`() {
        val respawn = Tile(3200, 3200, 0)
        val range = 3
        var reachedEdge = false
        repeat(2000) {
            val destination = wanderDestination(respawn, range)
            val distance = max(abs(destination.x - respawn.x), abs(destination.y - respawn.y))
            assertTrue(distance <= range)
            assertEquals(respawn.level, destination.level)
            if (distance == range) reachedEdge = true
        }
        assertTrue(reachedEdge)
    }

    @Test
    fun `every entry in the wander data resolves to an npc`() {
        if (!Gameval.has(Gameval.NPC)) return
        val file = File("data/npcs/wander.json")
        if (!file.isFile) return
        val keys = Regex("\"([^\"]+)\"\\s*:").findAll(file.readText()).count()
        assertEquals(keys, parseWanderRanges(file.readText(), Gameval).size)
    }
}
