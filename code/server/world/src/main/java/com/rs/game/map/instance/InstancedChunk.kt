package com.rs.game.map.instance

import com.rs.game.map.Chunk
import com.rs.game.map.ChunkManager
import com.rs.game.model.gameobject.GameObject
import com.rs.utils.Areas
import com.rs.utils.spawns.NPCSpawns
import org.darkan.core.map.ClipFlag
import org.darkan.core.map.CollisionWriter
import org.darkan.core.map.WorldCollision
import org.darkan.core.map.pathfinder.Direction
import org.darkan.core.model.WorldObject
import org.darkan.core.util.MapUtils
import org.darkan.core.util.Structure
import world.gregs.voidps.type.Tile

private val COPIED_FLOOR_FLAGS = ClipFlag.PFBW_FLOOR.flag or ClipFlag.UNDER_ROOF.flag

class InstancedChunk(val fromChunkId: Int, toChunkId: Int, private val mapRotation: Int) : Chunk(toChunkId) {
    private val fromChunkX: Int
    private val fromChunkY: Int
    private val fromPlane: Int

    val originalBaseX get() = fromChunkX shl 3
    val originalBaseY get() = fromChunkY shl 3

    init {
        loadedMapData = true
        loadedSpawnData = true
        val coords = MapUtils.decode(Structure.CHUNK, fromChunkId)
        fromChunkX = coords[0]
        fromChunkY = coords[1]
        fromPlane = coords[2]
    }

    override fun checkLoaded() {}

    fun loadMap(copyNpcs: Boolean, copyMulti: Boolean) {
        val template = InstanceBuilder.template(fromChunkId)
        setMapDataLoaded()
        if (copyMulti) copyMulticombat()
        WorldCollision.initChunk(id)
        copyFloor(WorldCollision.live, template.floor(fromChunkId))
        for (obj in transformObjects(template.objects(fromChunkId)))
            ChunkManager.getChunk(obj.tile.getChunkId(), false).addBaseObject(obj)
        if (copyNpcs) spawnTemplateNpcs()
    }

    fun copyFloor(writer: CollisionWriter, sourceFlags: (Int, Int) -> Int) {
        for (x in 0 until 8) for (y in 0 until 8) {
            val coords = transform(x, y, mapRotation)
            writer.add(baseX + coords[0], baseY + coords[1], plane, sourceFlags(x, y) and COPIED_FLOOR_FLAGS)
        }
    }

    fun transformObjects(objects: List<WorldObject>): List<GameObject> = objects.map { orig ->
        val defs = orig.definitions
        val coords = transform(orig.tile.getXInChunk(), orig.tile.getYInChunk(), mapRotation, defs.sizeX, defs.sizeY, orig.rotation)
        GameObject(orig.id, orig.type, (orig.rotation + mapRotation) and 0x3, Tile.of(baseX + coords[0], baseY + coords[1], plane))
    }

    fun placeBaseObject(obj: GameObject) {
        baseObjects[obj.tile.getXInChunk()][obj.tile.getYInChunk()][obj.slot] = obj
    }

    fun copyMulticombat() = setMulticombat(Areas.withinArea("multi", fromChunkId))

    fun spawnTemplateNpcs() {
        for (spawn in NPCSpawns.getSpawnsForChunk(fromChunkId) ?: return) {
            val coords = transform(spawn.tile.getXInChunk(), spawn.tile.getYInChunk(), mapRotation, spawn.defs.size, spawn.defs.size, 0)
            spawn.spawnAtCoords(Tile.of(baseX + coords[0], baseY + coords[1], plane), spawn.dir?.let { Direction.rotateClockwise(it, mapRotation * 2) })
        }
    }

    override fun getRotation() = mapRotation

    override fun getRenderChunkX() = fromChunkX

    override fun getRenderChunkY() = fromChunkY

    override fun getRenderPlane() = fromPlane

    companion object {
        @JvmStatic
        fun transform(localX: Int, localY: Int, rotation: Int): IntArray = when (rotation) {
            1 -> intArrayOf(localY, 7 - localX)
            2 -> intArrayOf(7 - localX, 7 - localY)
            3 -> intArrayOf(7 - localY, localX)
            else -> intArrayOf(localX, localY)
        }

        @JvmStatic
        fun transform(localX: Int, localY: Int, mapRotation: Int, sizeX: Int, sizeY: Int, objectRotation: Int): IntArray {
            val (width, length) = if (objectRotation and 0x1 == 1) sizeY to sizeX else sizeX to sizeY
            return when (mapRotation) {
                1 -> intArrayOf(localY, 7 - localX - (width - 1))
                2 -> intArrayOf(7 - localX - (width - 1), 7 - localY - (length - 1))
                3 -> intArrayOf(7 - localY - (length - 1), localX)
                else -> intArrayOf(localX, localY)
            }
        }
    }
}
