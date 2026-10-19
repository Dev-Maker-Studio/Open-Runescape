package com.rs.game.map.instance

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.map.MapWorkers
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.darkan.core.Logger
import org.darkan.core.map.CollisionBuffer
import org.darkan.core.map.MapRegion
import org.darkan.core.map.RegionCollision
import org.darkan.core.map.WorldCollision
import org.darkan.core.model.WorldObject
import org.darkan.core.util.MapUtils
import org.darkan.core.util.Structure
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object InstanceBuilder {
    const val NORTH = 0
    const val EAST = 1
    const val SOUTH = 2
    const val WEST = 3

    private const val MAX_REGION_X = 127
    private const val MAX_REGION_Y = 255
    private const val REGION_CHUNKS = 8
    private const val MIN_REGION_SPAN = 2
    private const val FIRST_REGION = 2
    private const val TEMPLATE_2X2 = 2
    private val ALL_PLANES = intArrayOf(0, 1, 2, 3)
    private val EMPTY_CHUNK = MapUtils.encode(Structure.CHUNK, 0, 0, 0)

    private val reservedRegions = IntOpenHashSet()
    private val templates = ConcurrentHashMap<Int, Template>()

    internal class Template(private val region: MapRegion) {
        private val objectsByChunk: Map<Int, List<WorldObject>> = region.getObjects().groupBy { it.tile.getChunkId() }
        private val clipFlags = region.getClipFlags()!!

        fun objects(chunkId: Int) = objectsByChunk[chunkId].orEmpty()

        fun floor(chunkId: Int): (Int, Int) -> Int {
            val (chunkX, chunkY, plane) = MapUtils.decode(Structure.CHUNK, chunkId)
            val localX = (chunkX shl 3) - region.baseX
            val localY = (chunkY shl 3) - region.baseY
            return { x, y -> clipFlags[plane][localX + x][localY + y] }
        }
    }

    private class ChunkCopy(val from: Int, val to: Int, val rotation: Int)

    private class BuiltMap(val chunks: List<InstancedChunk>, val collision: CollisionBuffer)

    @JvmStatic
    fun findEmptyChunkBound(ref: Instance, future: CompletableFuture<Boolean>) {
        runCatching { ref.setChunkBase(reserveEmptyChunkBound(ref.width, ref.height)) }
            .onSuccess {
                ref.chunkIds.clear()
                for (plane in ALL_PLANES)
                    for (x in 0 until ref.width)
                        for (y in 0 until ref.height)
                            ref.chunkIds.add(chunk(ref.chunkBase[0] + x, ref.chunkBase[1] + y, plane))
                future.complete(true)
            }
            .onFailure { fail(future, it) }
    }

    @JvmStatic
    fun copyChunk(ref: Instance, localChunkX: Int, localChunkY: Int, plane: Int, fromChunkX: Int, fromChunkY: Int, fromPlane: Int, rotation: Int, reinitCollision: Boolean, future: CompletableFuture<Boolean>) =
        build(ref, listOf(ChunkCopy(chunk(fromChunkX, fromChunkY, fromPlane), chunk(ref.baseChunkX + localChunkX, ref.baseChunkY + localChunkY, plane), rotation)), ref.copyNpcs, ref.copyMulti, future)

    @JvmStatic
    fun copy2x2ChunkSquare(ref: Instance, chunkX: Int, chunkY: Int, fromChunkX: Int, fromChunkY: Int, rotation: Int, planes: IntArray, future: CompletableFuture<Boolean>) {
        val offsets = rotatedOffsets(TEMPLATE_2X2, TEMPLATE_2X2, rotation)
        val copies = planes.flatMap { plane ->
            (0 until TEMPLATE_2X2).flatMap { y ->
                (0 until TEMPLATE_2X2).map { x ->
                    val offset = offsets[y][x]
                    ChunkCopy(chunk(fromChunkX + x, fromChunkY + y, plane), chunk(ref.baseChunkX + chunkX + offset % TEMPLATE_2X2, ref.baseChunkY + chunkY + offset / TEMPLATE_2X2, plane), rotation)
                }
            }
        }
        build(ref, copies, ref.copyNpcs, ref.copyMulti, future)
    }

    @JvmStatic
    fun clearChunk(ref: Instance, localChunkX: Int, localChunkY: Int, plane: Int, reinitCollision: Boolean, future: CompletableFuture<Boolean>) =
        build(ref, listOf(ChunkCopy(EMPTY_CHUNK, chunk(ref.baseChunkX + localChunkX, ref.baseChunkY + localChunkY, plane), 0)), copyNpcs = false, copyMulti = true, future = future)

    @JvmStatic
    fun clearMap(ref: Instance, localChunkX: Int, localChunkY: Int, width: Int, height: Int, planes: IntArray, future: CompletableFuture<Boolean>) {
        val copies = planes.flatMap { plane ->
            (0 until width).flatMap { x -> (0 until height).map { y -> ChunkCopy(EMPTY_CHUNK, chunk(ref.baseChunkX + localChunkX + x, ref.baseChunkY + localChunkY + y, plane), 0) } }
        }
        build(ref, copies, copyNpcs = false, copyMulti = true, future = future)
    }

    @JvmStatic
    fun copyMap(ref: Instance, localChunkX: Int, localChunkY: Int, fromChunkX: Int, fromChunkY: Int, size: Int, future: CompletableFuture<Boolean>) =
        copyMap(ref, localChunkX, localChunkY, ALL_PLANES, fromChunkX, fromChunkY, ALL_PLANES, size, size, ref.copyNpcs, ref.copyMulti, future)

    @JvmStatic
    fun copyMap(ref: Instance, localChunkX: Int, localChunkY: Int, planes: IntArray, fromChunkX: Int, fromChunkY: Int, fromPlanes: IntArray, width: Int, height: Int, copyNpcs: Boolean, copyMulti: Boolean, future: CompletableFuture<Boolean>) {
        require(planes.size == fromPlanes.size) { "Copying ${fromPlanes.size} planes onto ${planes.size}" }
        val copies = planes.indices.flatMap { index ->
            (0 until width).flatMap { x ->
                (0 until height).map { y -> ChunkCopy(chunk(fromChunkX + x, fromChunkY + y, fromPlanes[index]), chunk(ref.baseChunkX + localChunkX + x, ref.baseChunkY + localChunkY + y, planes[index]), 0) }
            }
        }
        build(ref, copies, copyNpcs, copyMulti, future)
    }

    @JvmStatic
    fun destroyMap(ref: Instance, future: CompletableFuture<Boolean>) {
        val base = ref.chunkBase
        if (base != null) {
            for (plane in ALL_PLANES)
                for (x in 0 until ref.width)
                    for (y in 0 until ref.height)
                        ChunkManager.removeChunk(chunk(base[0] + x, base[1] + y, plane))?.destroy()
            releaseRegions(base[0] / REGION_CHUNKS, base[1] / REGION_CHUNKS, regionSpan(ref.width), regionSpan(ref.height))
        }
        future.complete(true)
    }

    private fun build(ref: Instance, copies: List<ChunkCopy>, copyNpcs: Boolean, copyMulti: Boolean, future: CompletableFuture<Boolean>) {
        MapWorkers.launch {
            val built = runCatching { buildChunks(copies) }
            WorldTasks.schedule(0) {
                runCatching { publish(ref, built.getOrThrow(), copyNpcs, copyMulti) }
                    .onSuccess(future::complete)
                    .onFailure { fail(future, it) }
            }
        }
    }

    private suspend fun buildChunks(copies: List<ChunkCopy>): BuiltMap = coroutineScope {
        val collision = CollisionBuffer(copies.map { it.to })
        val built = copies.map { copy -> async { buildChunk(copy, collision) } }.awaitAll()
        val chunks = built.associate { (chunk, _) -> chunk.id to chunk }
        for ((_, objects) in built)
            for (obj in objects) {
                val owner = chunks[obj.tile.getChunkId()] ?: continue
                owner.placeBaseObject(obj)
                RegionCollision.clip(obj, collision)
            }
        BuiltMap(built.map { it.first }, collision)
    }

    private fun buildChunk(copy: ChunkCopy, collision: CollisionBuffer): Pair<InstancedChunk, List<GameObject>> {
        val template = template(copy.from)
        val chunk = InstancedChunk(copy.from, copy.to, copy.rotation)
        chunk.copyFloor(collision, template.floor(copy.from))
        return chunk to chunk.transformObjects(template.objects(copy.from))
    }

    internal fun template(chunkId: Int): Template =
        templates.computeIfAbsent(MapUtils.chunkToRegionId(chunkId)) { Template(MapRegion(it).apply { loadRegionMap(false) }) }

    private fun publish(ref: Instance, built: BuiltMap, copyNpcs: Boolean, copyMulti: Boolean): Boolean {
        if (ref.isDestroyed) return false
        val displaced = IntOpenHashSet()
        for (chunk in built.chunks) {
            ChunkManager.removeChunk(chunk.id)?.let { old ->
                displaced.addAll(old.playerIndexes)
                old.destroy(false)
            }
            ChunkManager.putChunk(chunk.id, chunk)
            WorldCollision.allFlags[chunk.id] = built.collision.flags(chunk.id)
            if (copyMulti) chunk.copyMulticombat()
            if (copyNpcs) chunk.spawnTemplateNpcs()
        }
        built.collision.writeOverflow(WorldCollision.live)
        ChunkManager.restoreNeighbourEdges(built.chunks.mapTo(IntOpenHashSet()) { it.id })
        for (index in displaced) {
            val player = World.players[index] ?: continue
            if (!player.hasStarted() || player.hasFinished()) continue
            player.setForceNextMapLoadRefresh(true)
            player.loadMapRegions()
        }
        return true
    }

    private fun fail(future: CompletableFuture<Boolean>, e: Throwable) {
        Logger.handle(InstanceBuilder::class.java, "build", e)
        future.completeExceptionally(e)
    }

    private fun chunk(x: Int, y: Int, plane: Int) = MapUtils.encode(Structure.CHUNK, x, y, plane)

    private fun regionSpan(chunks: Int) = MIN_REGION_SPAN + (chunks - 1).coerceAtLeast(0) / REGION_CHUNKS

    private fun reserveEmptyChunkBound(widthChunks: Int, heightChunks: Int): IntArray {
        val spanX = regionSpan(widthChunks)
        val spanY = regionSpan(heightChunks)
        synchronized(reservedRegions) {
            for (regionX in FIRST_REGION..MAX_REGION_X - spanX)
                for (regionY in FIRST_REGION..MAX_REGION_Y - spanY) {
                    val free = (regionX - 1..regionX + spanX).none { x -> (regionY - 1..regionY + spanY).any { y -> region(x, y) in reservedRegions } }
                    if (!free) continue
                    for (x in regionX until regionX + spanX) for (y in regionY until regionY + spanY) reservedRegions.add(region(x, y))
                    Logger.info(InstanceBuilder::class.java, "reserveRegions", "Reserved ${spanX}x$spanY regions at $regionX, $regionY (${reservedRegions.size} reserved)")
                    return intArrayOf(regionX * REGION_CHUNKS, regionY * REGION_CHUNKS)
                }
        }
        throw IllegalStateException("No free region for a ${widthChunks}x$heightChunks chunk instance")
    }

    private fun releaseRegions(regionX: Int, regionY: Int, spanX: Int, spanY: Int) = synchronized(reservedRegions) {
        for (x in regionX until regionX + spanX) for (y in regionY until regionY + spanY) reservedRegions.remove(region(x, y))
        Logger.info(InstanceBuilder::class.java, "releaseRegions", "Released ${spanX}x$spanY regions at $regionX, $regionY (${reservedRegions.size} reserved)")
    }

    private fun region(x: Int, y: Int) = MapUtils.encode(Structure.REGION, x, y)

    private fun rotatedOffsets(width: Int, height: Int, rotation: Int): Array<IntArray> {
        val offsets = Array(height) { IntArray(width) }
        for (y in 0 until height)
            for (x in 0 until width) {
                val index = y * width + x
                when (rotation) {
                    1 -> offsets[x][height - 1 - y] = index
                    2 -> offsets[height - 1 - y][width - 1 - x] = index
                    3 -> offsets[width - 1 - x][y] = index
                    else -> offsets[y][x] = index
                }
            }
        return offsets
    }
}
