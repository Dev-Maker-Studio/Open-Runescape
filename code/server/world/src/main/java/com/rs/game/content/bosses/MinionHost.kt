package com.rs.game.content.bosses

import com.rs.game.World
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import world.gregs.voidps.type.Tile

data class MinionSpawn(val npcId: Int, val offsetX: Int, val offsetY: Int, val offsetZ: Int = 0) {
    fun tileFrom(host: Tile): Tile = host.transform(offsetX, offsetY, offsetZ)
}

/**
 * What happens to the minions when the boss dies. Godwars bodyguards outlive their general and are
 * killed and respawned on their own; Nex's four take their power back with her.
 */
enum class MinionPolicy { PERSIST, BOUND }

/** A minion that needs to talk back to the boss that placed it. */
interface HostedMinion {
    var host: MinionHost?
}

/**
 * A boss that brings its own minions rather than relying on world spawns: they are placed relative
 * to wherever the boss itself spawned, so the same boss arrives complete in its real arena, in an
 * instance copy of it, or dropped anywhere in the open world.
 */
open class MinionHost(
    id: Int,
    tile: Tile,
    spawned: Boolean,
    private val minionSpawns: List<MinionSpawn>,
    private val policy: MinionPolicy = MinionPolicy.PERSIST,
) : NPC(id, tile, spawned) {
    private val minions = ArrayList<NPC>(minionSpawns.size)

    init {
        spawnMinions()
    }

    val liveMinions: List<NPC> get() = minions.filter { !it.hasFinished() && !it.isDead }

    fun minion(npcId: Int): NPC? = minions.firstOrNull { it.id == npcId }

    override fun onRespawn() {
        reviveMinions()
    }

    override fun finish() {
        super.finish()
        if (policy != MinionPolicy.BOUND) return
        minions.forEach { it.permanentlyDelete() }
        minions.clear()
    }

    fun reviveMinions(delay: Int = 2) {
        schedule {
            if (minions.isEmpty()) spawnMinions()
            else minions.forEach { if (it.hasFinished() || it.isDead) it.setRespawnTask(delay) }
        }
    }

    /**
     * A bound minion is marked as spawned so that dying never schedules a respawn of its own: the
     * host is the only thing that may bring it back, and one that respawned itself outlived the
     * roster it was in and haunted the arena.
     */
    private fun spawnMinions() {
        val ownLifecycle = policy == MinionPolicy.BOUND
        minionSpawns.mapTo(minions) { spawn ->
            World.spawnNPC(spawn.npcId, spawn.tileFrom(respawnTile), ownLifecycle, true).also {
                (it as? HostedMinion)?.host = this
            }
        }
    }
}
