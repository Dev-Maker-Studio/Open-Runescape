package org.darkan.world

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.utils.blocks
import com.rs.utils.clipType
import org.darkan.core.cache.ObjectDefinitions
import org.darkan.core.gameval.gen.Loc
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineOfSightTest {

    @Test
    fun locThatClearsProjectileBlockingStillBlocksWalking() {
        WorldHarness.bootWithCacheDefinitions()
        val bars = ObjectDefinitions.getDefs(Loc.fai_varrock_bank_bars_door)
        assertEquals(2, bars.clipType)
        assertFalse(bars.blocks())
    }

    @Test
    fun projectilesPassThroughBarsThatBlockWalking() {
        WorldHarness.bootWithCacheDefinitions()
        val outside = Tile.of(3192, 9825, 0)
        val inside = Tile.of(3192, 9822, 0)
        ChunkManager.getChunk(outside.getChunkId())
        ChunkManager.getChunk(inside.getChunkId())
        WorldHarness.tickUntil { World.getObjectWithId(outside, Loc.fai_varrock_bank_bars_door_mirror) != null }
        assertNotNull(World.getObjectWithId(outside, Loc.fai_varrock_bank_bars_door_mirror))
        assertTrue(World.hasLineOfSight(outside, 1, inside, 1))
    }
}
