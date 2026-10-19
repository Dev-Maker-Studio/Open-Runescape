package com.rs.game.content.minigames.creations

import com.rs.game.World
import com.rs.game.map.instance.Instance
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile
import java.util.concurrent.CompletableFuture
import kotlin.math.hypot

enum class PlotType(val templateChunkX: Int, val templateChunkY: Int) {
    RESERVED(241, 715),
    BASE(240, 712),
    EMPTY(241, 715),
    RIFT(240, 716),
    WALL(241, 716),
    FOG(240, 715),
    LARGE_ROCK(242, 716),
    ALTAR(241, 714),
    KILN(240, 714),
    ROCK(247, 715),
    TREE(247, 712),
    POOL(247, 714),
    SWARM(247, 713);

    val isSkill get() = this in SKILL_PLOTS

    companion object {
        val SKILL_PLOTS = setOf(ROCK, TREE, POOL, SWARM)
        private val RESERVED_TEMPLATES = arrayOf(intArrayOf(240, 713), intArrayOf(241, 712), intArrayOf(241, 713))

        fun template(type: PlotType, tier: Int): IntArray = when {
            type == RESERVED -> RESERVED_TEMPLATES.getOrElse(tier) { intArrayOf(EMPTY.templateChunkX, EMPTY.templateChunkY) }
            type.isSkill && tier > 0 -> intArrayOf(type.templateChunkX - tier, type.templateChunkY)
            type.isSkill -> intArrayOf(EMPTY.templateChunkX, EMPTY.templateChunkY)
            else -> intArrayOf(type.templateChunkX, type.templateChunkY)
        }
    }
}

private val BASE_ENTRANCE_BLUE = intArrayOf(4, 5)
private val BASE_ENTRANCE_RED = intArrayOf(4, 2)
private val PLOT_ENTRANCES = arrayOf(intArrayOf(2, 2), intArrayOf(2, 3), intArrayOf(2, 4), intArrayOf(3, 2), intArrayOf(3, 4), intArrayOf(4, 2), intArrayOf(4, 4), intArrayOf(5, 2), intArrayOf(5, 3), intArrayOf(5, 4))
private val BLUE_MANAGER = intArrayOf(4, 2)
private val RED_MANAGER = intArrayOf(3, 5)
val MANAGER_NPCS = setOf(Npc.sc_mystic_ingame_human, Npc.sc_mystic_ingame_human_2, Npc.sc_mystic_ingame_goblin, Npc.npc_8238)
private const val SKILL_PLOT_SHARE = 0.3
private const val OBSTACLE_SHARE = 0.2
private const val SKILL_PLOT_DEGRADATION = 60
private const val PLACEMENT_ATTEMPTS = 100

private class BaseDoor(val id: Int, val rotation: Int, val localX: Int, val localY: Int)

private val BLUE_DOORS = listOf(
    BaseDoor(Loc.loc_39766, 1, 3, 7), BaseDoor(Loc.loc_39768, 1, 4, 7),
    BaseDoor(Loc.loc_39766, 2, 7, 4), BaseDoor(Loc.loc_39768, 2, 7, 3),
)
private val RED_DOORS = listOf(
    BaseDoor(Loc.loc_39767, 3, 4, 0), BaseDoor(Loc.loc_39769, 3, 3, 0),
    BaseDoor(Loc.loc_39767, 0, 0, 3), BaseDoor(Loc.loc_39769, 0, 0, 4),
)

val BLUE_DOOR_IDS = setOf(Loc.loc_39766, Loc.loc_39768)
val RED_DOOR_IDS = setOf(Loc.loc_39767, Loc.loc_39769)

fun teamNumber(red: Boolean) = if (red) 2 else 1

class GameArea(val size: Int) {
    private val flags = Array(size) { IntArray(size) }
    private var region: Instance? = null

    val minX get() = region!!.baseX
    val minY get() = region!!.baseY
    val maxX get() = minX + (size shl 3)
    val maxY get() = minY + (size shl 3)

    fun calculate() {
        flags.forEach { it.fill(PlotType.EMPTY.ordinal) }
        val last = size - 1
        set(0, 0, PlotType.BASE, 0, 0)
        set(0, 1, PlotType.RESERVED, 0, 0)
        set(1, 0, PlotType.RESERVED, 1, 0)
        set(1, 1, PlotType.RESERVED, 2, 0)
        set(last, last, PlotType.BASE, 0, 2)
        set(last, last - 1, PlotType.RESERVED, 0, 2)
        set(last - 1, last, PlotType.RESERVED, 1, 2)
        set(last - 1, last - 1, PlotType.RESERVED, 2, 2)
        setWallTeam(0, 0, teamNumber(false))
        setWallTeam(last, last, teamNumber(true))

        val total = size * size
        var skillPlots = (total * SKILL_PLOT_SHARE).toInt()
        while (skillPlots-- > 0) {
            val type = PlotType.SKILL_PLOTS.random()
            setRandom(0, 0, size, size, type, if (skillPlots == 0) 5 else Utils.random(5), Utils.random(4), SKILL_PLOT_DEGRADATION)
        }
        repeat((total * OBSTACLE_SHARE).toInt()) {
            val type = PlotType.entries[Utils.random(5) + PlotType.RIFT.ordinal]
            setRandom(0, 0, size, size, type, 0, if (type == PlotType.FOG) 3 else Utils.random(4))
        }
        repeat(Utils.random(2) + 1) { setRandom(0, 0, size / 2, size / 2, PlotType.KILN, 0, Utils.random(4)) }
        repeat(Utils.random(2) + 1) { setRandom(size / 2, size / 2, size, size, PlotType.KILN, 0, Utils.random(4)) }
    }

    fun create(onCreated: () -> Unit) {
        check(region == null) { "Area already created." }
        val instance = Instance.of(StealingCreation.EXIT, size, size)
        region = instance
        instance.requestChunkBound().thenAccept {
            val copies = ArrayList<CompletableFuture<Boolean>>()
            for (x in 0 until size) for (y in 0 until size) {
                val template = PlotType.template(getType(x, y), getTier(x, y))
                copies += instance.copyChunk(x, y, 0, template[0], template[1], 0, getRotation(x, y), false)
            }
            CompletableFuture.allOf(*copies.toTypedArray()).thenRun {
                WorldTasks.schedule(0) {
                    spawnBaseFixtures()
                    onCreated()
                }
            }
        }
    }

    private fun spawnBaseFixtures() {
        val redOffset = (size - 1) shl 3
        BLUE_DOORS.forEach { World.spawnObject(GameObject(it.id, ObjectShape.WALL_STRAIGHT, it.rotation, minX + it.localX, minY + it.localY, 0)) }
        RED_DOORS.forEach { World.spawnObject(GameObject(it.id, ObjectShape.WALL_STRAIGHT, it.rotation, minX + redOffset + it.localX, minY + redOffset + it.localY, 0)) }
        World.spawnNPC(MANAGER_NPCS.random(), Tile.of(minX + BLUE_MANAGER[0], minY + BLUE_MANAGER[1], 0), true, true).setRandomWalk(false)
        World.spawnNPC(MANAGER_NPCS.random(), Tile.of(minX + redOffset + RED_MANAGER[0], minY + redOffset + RED_MANAGER[1], 0), true, true).setRandomWalk(false)
    }

    fun destroy() {
        checkNotNull(region) { "Area already destroyed." }.destroy()
        region = null
    }

    fun contains(tile: Tile) = tile.x in minX..maxX && tile.y in minY..maxY

    fun plotX(tile: Tile) = tile.getChunkX() - (minX shr 3)

    fun plotY(tile: Tile) = tile.getChunkY() - (minY shr 3)

    fun inBounds(x: Int, y: Int) = x in 0 until size && y in 0 until size

    fun plotTile(x: Int, y: Int, localX: Int, localY: Int) = Tile.of(minX + (x shl 3) + localX, minY + (y shl 3) + localY, 0)

    fun nearestBase(tile: Tile, red: Boolean): IntArray {
        val px = plotX(tile)
        val py = plotY(tile)
        var best = intArrayOf(0, 0)
        var bestDistance = Double.MAX_VALUE
        for (x in 0 until size) for (y in 0 until size) {
            if (getWallTeam(x, y) != teamNumber(red)) continue
            val distance = hypot((px - x).toDouble(), (py - y).toDouble())
            if (distance < bestDistance) {
                bestDistance = distance
                best = intArrayOf(x, y)
            }
        }
        return best
    }

    fun baseEntrance(tile: Tile, red: Boolean): Tile {
        val base = nearestBase(tile, red)
        val corner = (base[0] == 0 && base[1] == 0) || (base[0] == size - 1 && base[1] == size - 1)
        val entrance = if (!corner) PLOT_ENTRANCES.random() else if (red) BASE_ENTRANCE_RED else BASE_ENTRANCE_BLUE
        return plotTile(base[0], base[1], entrance[0], entrance[1])
    }

    fun respawnPoint(player: Player, red: Boolean): Tile {
        val gates = BarrierTier.gateIds(red)
        return World.getSpawnedObjectsInChunkRange(player.chunkId, 8)
            .filter { it.id in gates }
            .minByOrNull { Utils.getDistance(player.tile, it.tile) }
            ?.let(::gateInside) ?: baseEntrance(player.tile, red)
    }

    fun nearPlotPoint(player: Player, x: Int, y: Int, offsetX: Int, offsetY: Int, distance: Int = 2) =
        player.withinDistance(plotTile(x, y, BarrierTier.PLOT_INSET + offsetX, BarrierTier.PLOT_INSET + offsetY).let { Tile.of(it.x, it.y, player.plane) }, distance)

    fun inSafeArea(player: Player, red: Boolean): Boolean {
        val x = plotX(player.tile)
        val y = plotY(player.tile)
        if (!inBounds(x, y) || getWallTeam(x, y) != teamNumber(red)) return false
        val (low, high) = if (getType(x, y) == PlotType.BASE) 0 to 7 else BarrierTier.PLOT_INSET + 1 to BarrierTier.PLOT_OUTSET - 1
        val min = plotTile(x, y, low, low)
        val max = plotTile(x, y, high, high)
        return player.x in min.x..max.x && player.y in min.y..max.y
    }

    fun inFog(player: Player, x: Int, y: Int): Boolean {
        val min = plotTile(x, y, 1, 1)
        val max = plotTile(x, y, 4, 4)
        return player.withinDistance(Tile.of(min.x, min.y, player.plane), 3) && player.x in min.x..max.x && player.y in min.y..max.y
    }

    private fun setRandom(minX: Int, minY: Int, maxX: Int, maxY: Int, type: PlotType, tier: Int, rotation: Int, degradation: Int = 0): Boolean {
        val spot = (1 until PLACEMENT_ATTEMPTS).asSequence()
            .map { intArrayOf(minX + Utils.random(maxX - minX), minY + Utils.random(maxY - minY)) }
            .firstOrNull { getType(it[0], it[1]) == PlotType.EMPTY }
            ?: (minX until maxX).asSequence().flatMap { x -> (minY until maxY).asSequence().map { y -> intArrayOf(x, y) } }
                .firstOrNull { getType(it[0], it[1]) == PlotType.EMPTY }
            ?: return false
        set(spot[0], spot[1], type, tier, rotation)
        setDegradation(spot[0], spot[1], degradation)
        return true
    }

    private fun set(x: Int, y: Int, type: PlotType, tier: Int, rotation: Int) {
        flags[x][y] = type.ordinal or (tier shl 4) or (rotation shl 8)
    }

    private fun setBits(x: Int, y: Int, shift: Int, mask: Int, value: Int) {
        flags[x][y] = (flags[x][y] and (mask shl shift).inv()) or ((value and mask) shl shift)
    }

    private fun bits(x: Int, y: Int, shift: Int, mask: Int) = (flags[x][y] shr shift) and mask

    fun setDegradation(x: Int, y: Int, value: Int) = setBits(x, y, 10, 0x1FF, value)
    fun setWallTeam(x: Int, y: Int, value: Int) = setBits(x, y, 19, 0x3, value)
    fun setWallTier(x: Int, y: Int, value: Int) = setBits(x, y, 21, 0x7, value)
    fun setWallStatus(x: Int, y: Int, value: Int) = setBits(x, y, 24, 0x3F, value)

    fun getType(x: Int, y: Int): PlotType = PlotType.entries.getOrElse(bits(x, y, 0, 0xF)) { PlotType.EMPTY }
    fun getTier(x: Int, y: Int) = bits(x, y, 4, 0xF)
    fun getRotation(x: Int, y: Int) = bits(x, y, 8, 0x3)
    fun getDegradation(x: Int, y: Int) = bits(x, y, 10, 0x1FF)
    fun getWallTeam(x: Int, y: Int) = bits(x, y, 19, 0x3)
    fun getWallTier(x: Int, y: Int) = bits(x, y, 21, 0x7)
    fun getWallStatus(x: Int, y: Int) = bits(x, y, 24, 0x3F)
}
