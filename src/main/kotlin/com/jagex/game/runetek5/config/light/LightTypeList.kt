package com.jagex.game.runetek5.config.light

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * < Mostly refactored >
 * @author Brainpower - 30/05/2020
 *
 * Manages LightType configurations
 */
class LightTypeList(
    @JvmField val lightResource: Js5ResourceProvider
) {

    @JvmField val lightCache = LRUCache(64)

    init {
        lightResource.getArchiveFileCount(SharedConfigsType.LIGHT_INTENSITIES.id)
    }

    /**
     * Retrieves a light type by ID, loading from cache or resource provider.
     *
     * @param lightId The light ID
     * @return The light type configuration
     */
    fun list(lightId: Int): LightType {
        var lightType: LightType?
        synchronized(lightCache) {
            lightType = lightCache.get(lightId.toLong()) as LightType?
        }
        if (lightType != null) {
            return lightType!!
        }
        val lightData: ByteArray?
        synchronized(lightResource) {
            lightData = lightResource.getFile(SharedConfigsType.LIGHT_INTENSITIES.id, lightId)
        }
        lightType = LightType()
        if (lightData != null) {
            lightType!!.decode(Packet(lightData))
        }
        synchronized(lightCache) {
            lightCache.put(lightType, lightId.toLong())
            return lightType!!
        }
    }

    /**
     * Resets the light cache, clearing all cached light types.
     */
    fun cacheReset() {
        synchronized(lightCache) {
            lightCache.reset()
        }
    }

    /**
     * Cleans the cache, removing least recently used entries.
     */
    fun cacheClean() {
        synchronized(lightCache) {
            lightCache.clean(5)
        }
    }

    /**
     * Resizes the cache queue.
     */
    fun cacheResize() {
        synchronized(lightCache) {
            lightCache.resize()
        }
    }
}
