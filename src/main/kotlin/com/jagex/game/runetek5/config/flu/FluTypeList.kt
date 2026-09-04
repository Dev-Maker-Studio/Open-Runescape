package com.jagex.game.runetek5.config.flu

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages the loading and caching of Flu (floor underlay) configuration types.
 */
class FluTypeList(fluResource: Js5ResourceProvider) {

    @JvmField val cache = LRUCache(128)
    @JvmField val underlayResource: Js5ResourceProvider = fluResource

    init {
        underlayResource.getArchiveFileCount(SharedConfigsType.UNDERLAYS.id)
    }

    /**
     * Retrieves a Flu (floor underlay) type by ID.
     *
     * @param underlayId The underlay ID
     * @return The Flu type configuration
     */
    fun list(underlayId: Int): FluType {
        var flutype: FluType?
        synchronized(cache) {
            flutype = cache.get(underlayId.toLong()) as FluType?
        }
        if (flutype != null) {
            return flutype!!
        }
        val fluData: ByteArray?
        synchronized(underlayResource) {
            fluData = underlayResource.getFile(SharedConfigsType.UNDERLAYS.id, underlayId)
        }
        flutype = FluType()
        if (fluData != null) {
            flutype!!.decode(Packet(fluData))
        }
        synchronized(cache) {
            cache.put(flutype, underlayId.toLong())
            return flutype!!
        }
    }

    /**
     * Resets the cache, clearing all cached Flu types.
     */
    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    /**
     * Processes a cache cycle, cleaning up old entries.
     */
    fun processCycle() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    /**
     * Clears the cache queue of pending removals.
     */
    fun clearQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }
}
