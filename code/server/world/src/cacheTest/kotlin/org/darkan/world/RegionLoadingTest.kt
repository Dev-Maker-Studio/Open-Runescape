package org.darkan.world

import com.rs.game.map.ChunkManager
import org.darkan.core.map.WorldCollision
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionLoadingTest {

    @Test
    fun regionCollisionStaysFullyBlockedUntilItsMapIsPublished() {
        WorldHarness.bootWithCacheDefinitions()
        val unvisited = Tile.of(3552, 4512, 0)

        ChunkManager.getChunk(unvisited.getChunkId())

        assertEquals(-1, WorldCollision.getFlags(unvisited))
        WorldHarness.tickUntil { WorldCollision.getFlags(unvisited) != -1 }
    }
}
