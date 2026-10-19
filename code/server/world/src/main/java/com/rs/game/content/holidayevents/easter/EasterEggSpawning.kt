package com.rs.game.content.holidayevents.easter

import com.rs.game.World
import com.rs.game.map.Chunk
import com.rs.game.map.ChunkManager
import com.rs.game.tasks.WorldTasks
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import it.unimi.dsi.fastutil.ints.IntSet

const val ENABLED = false

private var eggsPerChunk = 2
private var regionsToSpawn = IntSet.of(12850, 11828, 12084, 12853, 12597, 12342, 10806, 10547, 13105)

@ServerStartupEvent(ServerStartupEvent.Priority.POST_PROCESS)
fun startTasks() {
    if (!ENABLED) return
    ChunkManager.permanentlyPreloadRegions(regionsToSpawn)
    WorldTasks.scheduleHalfHourly {
        spawnEggs()
        World.sendWorldMessage("<col=FF0000><shad=000000>Easter Eggs have spawned in various cities around the world!", false)
    }
}

private fun countEggs(chunkId: Int): Int {
    var eggsCount = 0
    val itemSpawns = ChunkManager.getChunk(chunkId).allGroundItems
    if (itemSpawns != null && itemSpawns.size > 0) itemSpawns.forEach { spawn ->
        if (spawn.id == 1961) eggsCount += 1
    }
    return eggsCount
}

private fun spawnEggs() {
    World.mapRegionIdsToChunks(regionsToSpawn, 0).forEach { chunkId ->
        val chunk = ChunkManager.getChunk(chunkId)
        val eggsNeeded = eggsPerChunk - countEggs(chunkId)

        if (eggsNeeded > 0) {
            repeat(eggsNeeded) {
                World.getRandomWalkableTileInChunk(chunk)?.let {
                    World.addGroundItemNoExpire(Item(1961), it)
                }
            }
        }
    }
}