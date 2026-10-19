package com.jagex.game.runetek5.config.sun

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages the loading and caching of Sun configuration types for sky/sun rendering.
 */
class SunTypeList(sunResource: Js5ResourceProvider) {

    @JvmField val cache = LRUCache(16)
    @JvmField val sunResource: Js5ResourceProvider = sunResource

    init {
        this.sunResource.getArchiveFileCount(SharedConfigsType.SUN.id)
    }

    /**
     * Retrieves a Sun type by ID, loading from cache or resource provider.
     *
     * @param id The Sun type ID
     * @return The Sun type configuration
     */
    fun list(id: Int): SunType {
        var sunType: SunType?
        synchronized(cache) {
            sunType = cache.get(id.toLong()) as SunType?
        }
        if (sunType != null) {
            return sunType!!
        }
        val data: ByteArray?
        synchronized(sunResource) {
            data = sunResource.getFile(SharedConfigsType.SUN.id, id)
        }
        sunType = SunType()
        if (data != null) {
            sunType!!.decode(Packet(data))
        }
        synchronized(cache) {
            cache.put(sunType, id.toLong())
            return sunType!!
        }
    }

    /**
     * Resets the cache, clearing all cached Sun types.
     */
    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    /**
     * Sweeps the cache, removing least recently used entries.
     */
    fun sweepCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    /**
     * Clears the cache queue of pending removals.
     */
    fun clearCacheQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }
}
