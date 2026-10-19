package org.darkan.world

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.map.instance.Instance
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstancedChunkTest {

    @Test
    fun rotatedCopiedLocIsRestoredAsItsBaseLocAfterRemoval() {
        WorldHarness.bootWithCacheDefinitions()
        val template = Tile.of(2656, 2585, 0)
        val instance = Instance.of(template, 1, 1)
        val copy = instance.copyChunk(0, 0, 0, template.getChunkX(), template.getChunkY(), 0, 1, true)
        WorldHarness.tickUntil { copy.isDone }
        val chunk = ChunkManager.getChunk(instance.getChunkId(0, 0, 0))
        val copied = chunk.baseObjects.first { it.id == Loc.pest_wall_gate_fixed }

        World.removeObject(copied)
        World.spawnObject(GameObject(copied.id, copied.type, copied.rotation, copied.tile))

        assertTrue(chunk.spawnedObjects.none { it.tile.matches(copied.tile) })
        assertEquals(copied, World.getObjectWithId(copied.tile, copied.id))
    }
}
