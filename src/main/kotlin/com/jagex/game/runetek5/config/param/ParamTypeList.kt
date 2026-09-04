package com.jagex.game.runetek5.config.param

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages loading and caching of parameter type configurations.
 * Parameters are key-value pairs that can be attached to various game objects.
 */
class ParamTypeList(
    @JvmField val paramResource: Js5ResourceProvider?
) {
    @JvmField val cache: LRUCache = LRUCache(64)

    init {
        paramResource?.getArchiveFileCount(SharedConfigsType.PARAMS.id)
    }

    /**
     * Clears the cache queue, resizing it to remove unused entries
     */
    fun clearCacheQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }

    /**
     * Sweeps the cache, cleaning old entries
     */
    fun sweepCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    /**
     * Loads a parameter type by ID, using cache if available
     * @param id The parameter type ID
     * @return The loaded parameter type
     */
    fun list(id: Int): ParamType {
        // Check cache first
        synchronized(cache) {
            val paramType = cache.get(id.toLong()) as? ParamType
            if (paramType != null) {
                return paramType
            }
        }

        // Load from resource
        val data: ByteArray? = synchronized(paramResource!!) {
            paramResource.getFile(SharedConfigsType.PARAMS.id, id)
        }

        // Decode parameter type
        val paramType = ParamType()
        if (data != null) {
            paramType.decode(Packet(data))
        }

        // Cache and return
        synchronized(cache) {
            cache.put(paramType, id.toLong())
            return paramType
        }
    }

    /**
     * Resets the entire cache, clearing all entries
     */
    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }
}
