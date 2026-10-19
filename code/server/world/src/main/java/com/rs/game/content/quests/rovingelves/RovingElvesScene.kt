package com.rs.game.content.quests.rovingelves

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

private val ROVING_CLEARINGS = listOf(Tile.of(2291, 3147, 0), Tile.of(2208, 3156, 0))
private val ROVING_CHARACTERS = mutableMapOf<Int, RovingElf>()
private var clearing = 0
private var rovingPlaced = false
private val ILFEEN_TILE = Tile.of(2268, 3221, 0)

@ServerStartupEvent
fun mapRovingElvesScene() {
    instantiateNpc(Npc.npc_1679, Npc.roving_bowyer) { id, tile ->
        ROVING_CHARACTERS[id]?.takeUnless { it.hasFinished() } ?: RovingElf(id, tile).apply { ROVING_CHARACTERS[id] = this }
    }
    (ROVING_CLEARINGS + ILFEEN_TILE).forEach { ChunkManager.getChunk(it.getChunkId()) }
    WorldTasks.scheduleTimer(10, 500) {
        val centre = ROVING_CLEARINGS[clearing]
        val locations = mapOf(Npc.npc_1679 to centre.transform(-1, 0, 0), Npc.roving_bowyer to centre)
            .mapValues { (_, tile) -> rovingWalkable(tile) ?: return@scheduleTimer true }
        val relocate = {
            locations.forEach { (id, destination) ->
                val npc = ROVING_CHARACTERS[id]?.takeUnless { it.hasFinished() }
                    ?: World.spawnNPC(id, destination) as RovingElf
                ROVING_CHARACTERS[id] = npc
                npc.moveToCamp(destination)
            }
            rovingPlaced = true
            clearing = 1 - clearing
        }
        if (rovingPlaced && ROVING_CHARACTERS.values.count { !it.hasFinished() } == 2) {
            if (!rovingPlayersNearby()) {
                ROVING_CHARACTERS[Npc.roving_bowyer]?.forceTalk("It is time we left.")
                ROVING_CHARACTERS[Npc.npc_1679]?.forceTalk("Yes, let us move on.")
                WorldTasks.schedule(3) { if (!rovingPlayersNearby()) relocate() }
            }
        } else relocate()
        if (World.getNPCsInChunkRange(ILFEEN_TILE.getChunkId(), 1).none { it.id == Npc.roving_update_female_woodelf }) {
            rovingWalkable(ILFEEN_TILE)?.let { World.spawnNPC(Npc.roving_update_female_woodelf, it).setRandomWalk(false) }
        }
        true
    }
}

private fun rovingWalkable(tile: Tile): Tile? =
    (listOf(tile) + (-2..2).flatMap { x -> (-2..2).map { y -> tile.transform(x, y, 0) } })
        .firstOrNull { World.floorAndWallsFree(it, 1) }

private fun rovingPlayersNearby() = ROVING_CHARACTERS.values.any { npc ->
    !npc.hasFinished() && World.getPlayersInChunkRange(npc.chunkId, 2).any {
        !it.hasFinished() && it.withinDistance(npc.tile, 14)
    }
}
