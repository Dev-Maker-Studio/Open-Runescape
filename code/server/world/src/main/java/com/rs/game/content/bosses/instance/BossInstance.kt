package com.rs.game.content.bosses.instance

import com.rs.game.World
import com.rs.game.map.instance.Instance
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.Logger
import world.gregs.voidps.type.Tile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BossInstance(val def: BossInstanceDef, val ownerUsername: String, var settings: InstanceSettings) {
    val id: UUID = UUID.randomUUID()
    val instance: Instance = Instance.of(def.returnTo, def.widthRegions * 8, def.heightRegions * 8, true)
    private var paidUntil: Long = System.currentTimeMillis() + LIFETIME_MILLIS
    private val members = ConcurrentHashMap.newKeySet<String>()
    private var built = false
    private var exhausted = false
    private var destroyed = false

    val memberCount: Int get() = members.size
    val millisLeft: Long get() = paidUntil - System.currentTimeMillis()
    val expired: Boolean get() = millisLeft <= 0
    val minutesLeft: Int get() = ((millisLeft + 59999L) / 60000L).toInt().coerceAtLeast(0)

    fun build(onReady: () -> Unit) {
        if (built) {
            onReady()
            return
        }
        built = true
        val startedAt = System.currentTimeMillis()
        instance.copyMap(0, 0, def.planes.toIntArray(), def.originChunkX, def.originChunkY, def.planes.toIntArray(), def.widthRegions * 8, def.heightRegions * 8)
            .thenAccept {
                Logger.info(BossInstance::class.java, "build", "${def.key} instance built in ${System.currentTimeMillis() - startedAt}ms (${def.widthRegions * 8 * def.heightRegions * 8 * def.planes.size} chunks)")
                applySpawnSpeed()
                stageBoss()
                spawnExit()
                scheduleExpiry()
                onReady()
            }
    }

    fun localTile(worldTile: Tile): Tile {
        val offset = def.localOffset(worldTile)
        return instance.getLocalTile(offset[0], offset[1], offset[2])
    }

    fun refusalFor(joiner: Player, enteredPin: Int): String? {
        val owner = World.getPlayerByUsername(ownerUsername)
        return when {
            expired -> "That instance has expired."
            memberCount >= settings.maxPlayers -> "That instance is full."
            joiner.skills.combatLevel < settings.minCombat -> "You need a combat level of at least ${settings.minCombat} to join that instance."
            owner != null && !settings.protection.admits(owner, joiner, settings.pin, enteredPin) -> "You are not allowed to join that instance."
            else -> null
        }
    }

    /** A fresh payment buys more time on top of whatever is left, and wakes the arena back up. */
    @JvmOverloads
    fun renew(hours: Int = 1) {
        paidUntil = maxOf(System.currentTimeMillis(), paidUntil) + hours * LIFETIME_MILLIS
        exhausted = false
        applySpawnSpeed()
    }

    fun add(player: Player) = members.add(player.username)

    fun remove(player: Player) {
        members.remove(player.username)
    }

    fun npcsInside(): List<NPC> = World.NPCs.filterNotNull().filter { !it.hasFinished() && it.tile.getRegionId() in instanceRegions }

    private val instanceRegions: Set<Int>
        get() = (0 until def.widthRegions).flatMap { rx ->
            (0 until def.heightRegions).map { ry -> instance.tileBase.transform(rx * 64, ry * 64, 0).getRegionId() }
        }.toSet()

    fun applySpawnSpeed() {
        npcsInside().forEach { it.setRespawnDelayOverride(settings.spawnSpeed.ticks) }
    }

    /** The boss is not waiting when the door opens: it arrives on the timer the owner paid for. */
    private fun stageBoss() {
        val copied = npcsInside().filter { it.id in def.bossNpcs }
        copied.forEach { it.setRespawnTask(settings.spawnSpeed.ticks) }
        def.bossNpcs.filterNot { id -> copied.any { it.id == id } }.forEach(::spawnBoss)
    }

    /** A boss its arena summons rather than spawns, like Nex, has nothing to copy: give it one. */
    private fun spawnBoss(npcId: Int) {
        World.spawnNPC(npcId, localTile(def.home), false, true).apply {
            setRespawnDelayOverride(settings.spawnSpeed.ticks)
            setRespawnTask(settings.spawnSpeed.ticks)
        }
    }

    private fun spawnExit() {
        val tile = def.exitTile ?: return
        if (def.instanceExitLoc == -1) return
        val original = World.getObjectWithId(tile, def.exitLoc) ?: return
        World.spawnObject(GameObject(def.instanceExitLoc, original.type, original.rotation, localTile(tile)))
    }

    private fun stopRespawns() {
        npcsInside().forEach { it.setRespawnDelayOverride(Int.MAX_VALUE) }
        broadcast("Your instance has expired. Nothing further will respawn until you pay to renew it.")
    }

    private fun scheduleExpiry() {
        WorldTasks.scheduleTimer(Ticks.fromMinutes(1), Ticks.fromMinutes(1)) { _ ->
            if (destroyed) return@scheduleTimer false
            val minutesLeft = (millisLeft / 60000L).toInt()
            if (minutesLeft in WARN_MINUTES) broadcast("Your boss instance expires in $minutesLeft minutes.")
            if (expired && !exhausted) {
                exhausted = true
                stopRespawns()
            }
            true
        }
    }

    private fun broadcast(message: String) = onlineMembers().forEach { it.sendMessage(message) }

    private fun onlineMembers(): List<Player> = members.mapNotNull { World.getPlayerByUsername(it) }

    fun destroy() {
        destroyed = true
        npcsInside().forEach { it.permanentlyDelete() }
        onlineMembers().forEach { it.controllerManager.forceStop() }
        members.clear()
        instance.destroy()
        BossInstances.forget(this)
    }

    companion object {
        const val LIFETIME_MILLIS: Long = 60L * 60L * 1000L
        private val WARN_MINUTES = setOf(30, 15, 5)
    }
}
