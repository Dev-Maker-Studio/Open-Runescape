package com.jagex.game.runetek5.config.mapsprite

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class MapSpriteTypeList(
    @JvmField val mapspriteTypeResource: Js5ResourceProvider,
    @JvmField val mapspriteResource: Js5ResourceProvider
) {

    /**
     * Definitions about map sprites
     */
    @JvmField var mapspriteTypeCache: LRUCache = LRUCache(64)

    /**
     * The drawable map sprite data
     */
    @JvmField var mapspriteCache: LRUCache = LRUCache(64)

    init {
        mapspriteTypeResource.getArchiveFileCount(SharedConfigsType.MAP_SPRITES.id)
    }

    fun list(mapspriteId: Int): MapSpriteType {
        var mapspriteType: MapSpriteType?
        synchronized(mapspriteTypeCache) {
            mapspriteType = mapspriteTypeCache.get(mapspriteId.toLong()) as MapSpriteType?
        }
        if (mapspriteType != null) {
            return mapspriteType!!
        }
        val data: ByteArray?
        synchronized(mapspriteTypeResource) {
            data = mapspriteTypeResource.getFile(SharedConfigsType.MAP_SPRITES.id, mapspriteId)
        }
        mapspriteType = MapSpriteType()
        mapspriteType!!.mapspriteTypeList = this
        if (data != null) {
            mapspriteType!!.decode(Packet(data))
        }
        synchronized(mapspriteTypeCache) {
            mapspriteTypeCache.put(mapspriteType, mapspriteId.toLong())
            return mapspriteType!!
        }
    }

    fun sweepCaches() {
        synchronized(mapspriteTypeCache) {
            mapspriteTypeCache.clean(5)
        }
        synchronized(mapspriteCache) {
            mapspriteCache.clean(5)
        }
    }

    fun resetCaches() {
        synchronized(mapspriteTypeCache) {
            mapspriteTypeCache.reset()
        }
        synchronized(mapspriteCache) {
            mapspriteCache.reset()
        }
    }

    fun resetCaches(typeCapacity: Int, spriteCapacity: Int) {
        mapspriteTypeCache = LRUCache(typeCapacity)
        mapspriteCache = LRUCache(spriteCapacity)
    }

    fun clearCacheQueues() {
        synchronized(mapspriteTypeCache) {
            mapspriteTypeCache.resize()
        }
        synchronized(mapspriteCache) {
            mapspriteCache.resize()
        }
    }

    companion object {
        @JvmStatic
        fun isCached(gameObject: GameObject): Boolean {
            val locType = Resource.mapRegion!!.getLoctypeList().list(gameObject.id)
            if (locType.mapIconId == -1) {
                return true
            }
            val mapSpriteType = TypeLists.MAP_SPRITE!!.list(locType.mapIconId)
            return mapSpriteType.fileId == -1 || mapSpriteType.isCached()
        }
    }
}
