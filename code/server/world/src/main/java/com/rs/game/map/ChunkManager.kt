package com.rs.game.map

import com.rs.Settings
import com.rs.engine.pathfinder.GameObjectCollision
import com.rs.engine.pathfinder.NpcCollision
import com.rs.game.World
import com.rs.game.map.instance.InstancedChunk
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.EnterChunkEvent
import com.rs.utils.music.Music
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSets
import kotlinx.coroutines.launch
import org.darkan.core.Logger
import org.darkan.core.map.CollisionWriter
import org.darkan.core.map.MapRegion
import org.darkan.core.map.RegionCollision
import org.darkan.core.map.WorldCollision
import org.darkan.core.model.RegionSize
import org.darkan.core.util.MapUtils
import org.darkan.core.util.Structure
import world.gregs.voidps.type.Tile
import java.util.concurrent.ConcurrentHashMap

private const val REGION_CHUNKS = 8
private const val REGION_TILES = 64
private const val CHUNK_TILES = 8
private const val PLANES = 4
private const val ZONE_TILES = 64
private const val UNLOADED_TILE = -1
private const val MAX_REGION_ID = 0xFFFF
private const val COLLISION_HEAP = 1024L * 1024 * 1024
private const val OBJECTS_HEAP = 3072L * 1024 * 1024
// The citadel map decodes partially with its xteas but fails the gzip format checks.
private const val UNDECODABLE_REGION = 18754

@ServerStartupEvent(ServerStartupEvent.Priority.FILE_IO)
fun loadAllMapData() {
    val maxMemory = Runtime.getRuntime().maxMemory()
    Logger.info(WorldCollision::class.java, "loadAllMapData", "${maxMemory / (1024 * 1024)}mb heap space available. For better performance, allocate at least 1024mb or 3072mb")
    val optimize = Settings.getConfig().isAllowHighMemUseOptimizations && maxMemory != Int.MAX_VALUE.toLong()
    if (!optimize || maxMemory <= COLLISION_HEAP) return
    val preloadObjects = maxMemory > OBJECTS_HEAP
    for (regionId in 0 until MAX_REGION_ID) {
        if (regionId == UNDECODABLE_REGION) continue
        val region = MapRegion(regionId)
        region.loadRegionMap(false)
        if (!region.hasData()) continue
        val published = ChunkManager.publishCollision(region)
        if (!preloadObjects) continue
        ChunkManager.publishObjects(region)
        ChunkManager.restoreNeighbourEdges(published)
    }
}

object ChunkManager {
    private val chunks: MutableMap<Int, Chunk> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap())
    private val activeChunks: MutableSet<Int> = IntSets.synchronize(IntOpenHashSet())
    private val unloadableRegions: MutableSet<Int> = IntSets.synchronize(IntOpenHashSet())
    private val permanentlyLoadedRegions: MutableSet<Int> = IntSets.synchronize(IntOpenHashSet())
    private val requestedRegions: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val updateZones = HashMap<Long, UpdateZone>()
    private val updateZonesByRegion = HashMap<Int, MutableList<UpdateZone>>()

    @JvmStatic
    fun updateChunks(entity: Entity) {
        if (entity is NPC) NpcCollision.clipNPC(entity)
        if (entity.hasFinished()) {
            if (entity is Player) {
                getChunk(entity.lastChunkId).removePlayerIndex(entity.index)
                getUpdateZone(entity.sceneBaseChunkId, entity.mapSize).removePlayerWatcher(entity.index)
            } else {
                getChunk(entity.lastChunkId).removeNPCIndex(entity.index)
                if (entity is NPC && entity.isLoadsUpdateZones) getUpdateZone(entity.sceneBaseChunkId, entity.mapSize).removeNPCWatcher(entity.index)
            }
            return
        }
        val chunkId = MapUtils.encode(Structure.CHUNK, entity.chunkX, entity.chunkY, entity.plane)
        if (entity.lastChunkId == chunkId && !entity.isForceUpdateEntityRegion) {
            if (entity is Player) entity.controllerManager.moved()
            return
        }
        if (entity !is Player || entity.hasStarted()) PluginManager.handle(EnterChunkEvent(entity, chunkId))
        entity.checkMultiArea()
        if (entity is Player) enterChunk(entity, chunkId)
        else {
            if (entity.lastChunkId > 0) getChunk(entity.lastChunkId).removeNPCIndex(entity.index)
            getChunk(chunkId).addNPCIndex(entity.index)
        }
        entity.isForceUpdateEntityRegion = false
        entity.lastChunkId = chunkId
    }

    private fun enterChunk(player: Player, chunkId: Int) {
        if (Settings.getConfig().isDebug && player.hasStarted() && Music.getGenre(player) == null && getChunk(player.chunkId) !is InstancedChunk)
            player.sendMessage("$chunkId has no music genre!", true)
        if (player.lastChunkId > 0) getChunk(player.lastChunkId).removePlayerIndex(player.index)
        val chunk = getChunk(chunkId)
        chunk.addPlayerIndex(player.index)
        if (player.hasStarted())
            chunk.musicIds?.filterNot(player.musicsManager::hasMusic)?.forEach(player.musicsManager::unlockMusic)
        val playing = player.musicsManager.playingGenre
        if (player.hasStarted() && (Music.getGenre(player) == null || playing == null || !playing.matches(Music.getGenre(player)))) {
            val controller = player.controllerManager.controller
            if (controller == null) player.musicsManager.nextAmbientSong()
            else if (controller.playAmbientOnControllerRegionEnter() && !player.dungManager.isInsideDungeon && (playing == null || !playing.matches(controller.genre)))
                player.musicsManager.nextAmbientSong()
        }
        player.controllerManager.moved()
    }

    @JvmStatic
    fun loadRegionMapDataByChunkId(chunkId: Int) {
        val regionId = MapUtils.chunkToRegionId(chunkId)
        if (!requestedRegions.add(regionId)) return
        val region = MapRegion(regionId)
        forEachZone(region) { zone, _, _, _ ->
            if (chunks[zone] !is InstancedChunk) WorldCollision.allFlags[zone] = IntArray(ZONE_TILES) { UNLOADED_TILE }
        }
        MapWorkers.launch {
            val loaded = runCatching { region.apply { loadRegionMap(false) } }
            WorldTasks.schedule(0) {
                loaded.onSuccess(::publishRegion).onFailure {
                    requestedRegions.remove(regionId)
                    Logger.handle(ChunkManager::class.java, "loadRegionMapDataByChunkId", it)
                }
            }
        }
    }

    private fun publishRegion(region: MapRegion) {
        val published = publishCollision(region)
        publishObjects(region)
        forEachZone(region) { zone, _, _, _ ->
            val chunk = chunks[zone] ?: return@forEachZone
            chunk.spawnedObjects.toList().forEach(GameObjectCollision::clip)
            for (index in chunk.getNPCsIndexes().toList()) {
                val npc = World.NPCs[index] ?: continue
                if (npc.blocksOtherNpcs() && !npc.hasFinished()) NpcCollision.fillNPCClip(npc.tile, npc.size, true)
            }
        }
        restoreNeighbourEdges(published)
    }

    internal fun publishCollision(region: MapRegion): IntOpenHashSet {
        val published = IntOpenHashSet()
        val flags = region.getClipFlags() ?: return published
        forEachZone(region) { zone, plane, chunkX, chunkY ->
            val chunk = chunks[zone]
            if (chunk is InstancedChunk) return@forEachZone
            WorldCollision.allFlags[zone] = IntArray(ZONE_TILES) { flags[plane][chunkX * CHUNK_TILES + (it and 0x7)][chunkY * CHUNK_TILES + (it shr 3)] }
            chunk?.setMapDataLoaded()
            published.add(zone)
        }
        return published
    }

    fun restoreNeighbourEdges(published: IntOpenHashSet) {
        val intoPublished = CollisionWriter { x, y, level, flag -> if (MapUtils.encode(Structure.CHUNK, x shr 3, y shr 3, level) in published) WorldCollision.addFlag(x, y, level, flag) }
        val neighbours = IntOpenHashSet()
        for (id in published) {
            val (chunkX, chunkY, plane) = MapUtils.decode(Structure.CHUNK, id)
            for (dx in -1..1) for (dy in -1..1) {
                val neighbour = MapUtils.encode(Structure.CHUNK, chunkX + dx, chunkY + dy, plane)
                if (neighbour !in published) neighbours.add(neighbour)
            }
        }
        for (id in neighbours) {
            val loaded = findChunk(id) ?: continue
            loaded.getAllBaseObjects(false)?.forEach { RegionCollision.clip(it, intoPublished) }
            loaded.spawnedObjects.forEach { RegionCollision.clip(it, intoPublished) }
        }
    }

    internal fun publishObjects(region: MapRegion) {
        for (obj in region.getObjects()) {
            val chunk = getChunk(obj.tile.getChunkId(), false)
            if (chunk is InstancedChunk) continue
            chunk.addBaseObject(GameObject(obj))
            chunk.setMapDataLoaded()
        }
        requestedRegions.add(MapUtils.encode(Structure.REGION, region.baseX / REGION_TILES, region.baseY / REGION_TILES))
    }

    private inline fun forEachZone(region: MapRegion, action: (zone: Int, plane: Int, chunkX: Int, chunkY: Int) -> Unit) {
        val baseChunkX = region.baseX / CHUNK_TILES
        val baseChunkY = region.baseY / CHUNK_TILES
        for (plane in 0 until PLANES)
            for (chunkX in 0 until REGION_CHUNKS)
                for (chunkY in 0 until REGION_CHUNKS)
                    action(MapUtils.encode(Structure.CHUNK, baseChunkX + chunkX, baseChunkY + chunkY, plane), plane, chunkX, chunkY)
    }

    @JvmStatic
    @JvmOverloads
    fun getChunk(id: Int, load: Boolean = true): Chunk = synchronized(chunks) {
        val chunk = chunks.getOrPut(id) { Chunk(id) }
        if (load) chunk.checkLoaded()
        chunk
    }

    fun findChunk(id: Int): Chunk? = synchronized(chunks) { chunks[id] }

    @JvmStatic
    fun putChunk(id: Int, chunk: Chunk): Chunk? = synchronized(chunks) { chunks.put(id, chunk) }

    @JvmStatic
    fun removeChunk(id: Int): Chunk? = synchronized(chunks) { chunks.remove(id) }

    @JvmStatic
    fun removeUpdateZone(baseChunkId: Int, size: RegionSize): UpdateZone? = synchronized(updateZones) {
        val zone = updateZones.remove(UpdateZone.getId(baseChunkId, size)) ?: return null
        for (chunkId in zone.chunkIds) getChunk(chunkId).removeUpdateZone(zone)
        for (regionId in zone.regionIds) {
            val zones = updateZonesByRegion[regionId] ?: continue
            zones.remove(zone)
            if (zones.isEmpty()) {
                updateZonesByRegion.remove(regionId)
                markRegionUnloadable(regionId)
            }
        }
        zone
    }

    @JvmStatic
    fun getUpdateZone(baseChunkId: Int, size: RegionSize): UpdateZone = synchronized(updateZones) {
        val id = UpdateZone.getId(baseChunkId, size)
        updateZones[id]?.let { return it }
        val zone = UpdateZone(baseChunkId, size)
        updateZones[id] = zone
        for (regionId in zone.regionIds)
            if (regionId !in updateZonesByRegion) updateZonesByRegion[regionId] = mutableListOf(zone)
        for (chunkId in zone.chunkIds) getChunk(chunkId).addUpdateZone(zone)
        zone
    }

    @JvmStatic
    fun markRegionUnloadable(regionId: Int) {
        if (regionId !in permanentlyLoadedRegions) unloadableRegions.add(regionId)
    }

    @JvmStatic
    fun unmarkRegionUnloadable(regionId: Int) {
        if (regionId !in permanentlyLoadedRegions) unloadableRegions.remove(regionId)
    }

    @JvmStatic
    fun permanentlyPreloadRegions(vararg regionIds: Int) = permanentlyPreloadRegions(regionIds.toSet())

    @JvmStatic
    fun permanentlyPreloadRegions(regionIds: Set<Int>) {
        for (regionId in regionIds) {
            if (regionId in permanentlyLoadedRegions) continue
            getChunk(Tile.of((regionId shr 8) * REGION_TILES + REGION_TILES / 2, (regionId and 0xff) * REGION_TILES + REGION_TILES / 2, 0).getChunkId(), true)
            permanentlyLoadedRegions.add(regionId)
        }
    }

    @JvmStatic
    fun removePermanentlyPreloadedRegions(regionIds: Set<Int>) = permanentlyLoadedRegions.removeAll(regionIds)

    @JvmStatic
    fun processChunks() = synchronized(chunks) {
        try {
            for (chunkId in IntOpenHashSet(activeChunks)) getChunk(chunkId).process()
        } catch (e: Throwable) {
            Logger.handle(ChunkManager::class.java, "processChunks", e)
        }
    }

    @JvmStatic
    fun markChunkActive(chunkId: Int) = activeChunks.add(chunkId)

    @JvmStatic
    fun markChunkInactive(chunkId: Int) = activeChunks.remove(chunkId)

    @JvmStatic
    fun processUpdateZones() = synchronized(updateZones) {
        for (zone in updateZones.values) zone.update()
    }

    @JvmStatic
    fun clearUnusedMemory() = synchronized(chunks) {
        val iterator = unloadableRegions.iterator()
        while (iterator.hasNext()) {
            val regionId = iterator.next()
            val region = MapRegion(regionId)
            val destroy = IntOpenHashSet()
            var active = false
            forEachZone(region) { zone, _, _, _ ->
                if (zone in activeChunks) active = true
                val chunk = chunks[zone]
                if (chunk != null && chunk !is InstancedChunk) destroy.add(zone)
            }
            if (active) {
                Logger.debug(ChunkManager::class.java, "clearUnusedMemory", "Skipping active region: $regionId")
                continue
            }
            Logger.debug(ChunkManager::class.java, "clearUnusedMemory", "Clearing region: $regionId")
            destroy.forEach { chunks[it]?.destroy() }
            requestedRegions.remove(regionId)
            iterator.remove()
        }
    }
}
