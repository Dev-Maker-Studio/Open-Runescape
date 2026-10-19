package com.jagex.game.runetek5.config.hitsplat

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages hitsplat types with LRU caching for both hitsplats and sprites.
 */
class HitSplatTypeList(
    @JvmField val hitsplatResource: Js5ResourceProvider,
    @JvmField val spriteResource: Js5ResourceProvider
) {

    @JvmField val spriteCache = LRUCache(20)
    @JvmField val hitsplatTypeCache = LRUCache(64)

    init {
        hitsplatResource.getArchiveFileCount(SharedConfigsType.HITSPLATS.id)
    }

    /**
     * Retrieves a hitsplat type by ID, loading it if not cached.
     *
     * @param typeId The hitsplat type ID
     * @return The hitsplat type
     */
    fun list(typeId: Int): HitSplatType {
        var hitsplattype: HitSplatType?
        synchronized(hitsplatTypeCache) {
            hitsplattype = hitsplatTypeCache.get(typeId.toLong()) as HitSplatType?
        }
        if (hitsplattype != null) {
            return hitsplattype!!
        }
        val hitsplatData: ByteArray?
        synchronized(hitsplatResource) {
            hitsplatData = hitsplatResource.getFile(SharedConfigsType.HITSPLATS.id, typeId)
        }
        hitsplattype = HitSplatType()
        hitsplattype!!.typelist = this
        if (hitsplatData != null) {
            hitsplattype!!.decode(Packet(hitsplatData))
        }
        synchronized(hitsplatTypeCache) {
            hitsplatTypeCache.put(hitsplattype, typeId.toLong())
            return hitsplattype!!
        }
    }

    /**
     * Processes the cache cycle, cleaning up old entries.
     */
    fun processCacheCycle() {
        synchronized(hitsplatTypeCache) {
            hitsplatTypeCache.clean(5)
        }
        synchronized(spriteCache) {
            spriteCache.clean(5)
        }
    }

    /**
     * Resets both the hitsplat and sprite caches.
     */
    fun resetCaches() {
        synchronized(hitsplatTypeCache) {
            hitsplatTypeCache.reset()
        }
        synchronized(spriteCache) {
            spriteCache.reset()
        }
    }

    /**
     * Clears both cache queues by resizing.
     */
    fun clearQueues() {
        synchronized(hitsplatTypeCache) {
            hitsplatTypeCache.resize()
        }
        synchronized(spriteCache) {
            spriteCache.resize()
        }
    }
}
