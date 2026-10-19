package org.darkan.world

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.map.ClipFlag
import org.darkan.core.map.WorldCollision
import org.darkan.core.model.ObjectShape
import org.darkan.world.harness.TestDefinitions
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunkManagerCollisionTest {

    @Test
    fun `a loc crossing into a region published after it keeps its collision there`() {
        WorldHarness.boot()
        val base = Tile.of(126, 80, 0)
        val acrossEdge = Tile.of(128, 80, 0)
        val pastLoc = Tile.of(129, 80, 0)

        ChunkManager.getChunk(base.getChunkId())
        WorldHarness.tickUntil { WorldCollision.getFlags(base) != -1 }
        World.spawnObject(GameObject(TestDefinitions.WIDE_LOC, ObjectShape.SCENERY_INTERACT, 0, base))
        ChunkManager.getChunk(acrossEdge.getChunkId())
        WorldHarness.tickUntil { WorldCollision.getFlags(acrossEdge) != -1 }

        assertTrue(ClipFlag.flagged(WorldCollision.getFlags(acrossEdge), ClipFlag.BW_FULL))
        assertFalse(ClipFlag.flagged(WorldCollision.getFlags(pastLoc), ClipFlag.BW_FULL))
    }
}
