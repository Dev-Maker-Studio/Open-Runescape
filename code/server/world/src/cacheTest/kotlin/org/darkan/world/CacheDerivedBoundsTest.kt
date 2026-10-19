package org.darkan.world

import com.rs.engine.command.Commands
import org.darkan.core.Utils
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.cache.Cache
import world.gregs.voidps.cache.Config
import world.gregs.voidps.cache.Index
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the packed cache actually holds: table sizes, sparse archives and the id space they imply. */
class CacheDerivedBoundsTest {

    /**
     * The varp array was sized by the archive's *file count*, but config archive 16 is sparse, so
     * the top varps (2697-2714) sat past the end of the array and `setVar` returned silently.
     */
    @Test
    fun varpsPastTheArchiveFileCountAreSettable() {
        WorldHarness.bootWithCacheDefinitions()
        val cache = Cache.get()
        val lastId = cache.lastFileId(Index.CONFIGS, Config.VARP)
        val count = cache.fileCount(Index.CONFIGS, Config.VARP)
        assertTrue(
            lastId + 1 > count,
            "config archive ${Config.VARP} is expected to be sparse (lastFileId=$lastId, fileCount=$count)"
        )

        val harness = WorldHarness.login("varpsize")
        harness.player.vars.setVar(lastId, 7)
        assertEquals(7, harness.player.vars.getVar(lastId), "varp $lastId is past the end of the varp array")
        harness.player.vars.setVar(count, 3)
        assertEquals(3, harness.player.vars.getVar(count), "varp $count (== fileCount) was unreachable")
    }

    /** These returned a hard-coded 0, which made every `i >= size` loop guard break immediately. */
    @Test
    fun definitionSizeHelpersAreCacheDerived() {
        WorldHarness.bootWithCacheDefinitions()
        assertTrue(Utils.getVarbitDefinitionsSize() > 2000, "varbit table size is ${Utils.getVarbitDefinitionsSize()}")
        assertTrue(Utils.getBASAnimDefSize() > 0, "BAS table size is ${Utils.getBASAnimDefSize()}")
        assertTrue(Utils.getConfigDefinitionsSize() > 0, "config size is ${Utils.getConfigDefinitionsSize()}")
        assertTrue(Utils.getAnimationDefinitionsSize() > 0, "animation table size is 0")
        assertTrue(Utils.getNPCDefinitionsSize() > 0, "npc table size is 0")
    }

    /** `::varloop` had no bound at all, `::varbitloop`'s bound was 0 so it set nothing. */
    @Test
    fun varLoopAndVarbitLoopReachTheTopOfTheirIdSpace() {
        WorldHarness.bootWithCacheDefinitions()
        val harness = WorldHarness.login("varloops")
        val lastVarp = Cache.get().lastFileId(Index.CONFIGS, Config.VARP)

        assertTrue(
            Commands.processCommand(harness.player, "varloop ${lastVarp - 3} ${lastVarp + 1} 9", false, false),
            "::varloop was not dispatched"
        )
        assertEquals(9, harness.player.vars.getVar(lastVarp), "::varloop stopped before the last varp")

        assertTrue(
            Commands.processCommand(harness.player, "varbitloop 0 4 1", false, false),
            "::varbitloop was not dispatched"
        )
    }

    /** `::companim` printed `renderEmote=N` instead of the compatible animation list. */
    @Test
    fun companimPrintsTheCompatibleAnimationList() {
        WorldHarness.bootWithCacheDefinitions()
        val harness = WorldHarness.login("companim")
        harness.clearSent()

        val npcIds = listOf(1, 2, 3, 4, 5, 6)
        npcIds.forEach { id ->
            assertTrue(
                Commands.processCommand(harness.player, "companim $id", false, false),
                "::companim was not dispatched for npc $id"
            )
        }

        val lines = harness.sent.filterIsInstance<GameMessage>().map { it.message }
            .filter { line -> npcIds.any { line.startsWith("$it: ") } }
        assertTrue(lines.isNotEmpty(), "::companim printed nothing; sent=${harness.sent}")
        assertTrue(
            lines.none { it.contains("renderEmote") },
            "::companim still prints the raw render emote: $lines"
        )
        assertTrue(lines.all { it.contains("[") }, "::companim did not print an animation list: $lines")
        assertTrue(
            lines.any { !it.endsWith(": []") },
            "::companim found no compatible animations for any of $npcIds: $lines"
        )
    }
}
