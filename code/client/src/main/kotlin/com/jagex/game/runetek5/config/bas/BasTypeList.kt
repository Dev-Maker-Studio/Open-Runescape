package com.jagex.game.runetek5.config.bas

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages the loading and caching of BAS (Base Animation Set) configuration types.
 * BAS types define animation sets for equipped items and character rendering.
 */
class BasTypeList(
    @JvmField val basResource: Js5ResourceProvider,
    @JvmField val equipmentDefaults: EquipmentDefaultsType
) {

    @JvmField val basCache = LRUCache(64)

    init {
        basResource.getArchiveFileCount(SharedConfigsType.RENDER_ANIMS.id)
    }

    /**
     * Retrieves a BAS type by ID, loading from cache or resource provider.
     *
     * @param basFileId The BAS file ID
     * @return The BAS type configuration
     */
    fun list(basFileId: Int): BasType {
        var bastype: BasType?
        synchronized(basCache) {
            bastype = basCache.get(basFileId.toLong()) as BasType?
        }
        if (bastype != null) {
            return bastype!!
        }
        val basData: ByteArray?
        synchronized(basResource) {
            basData = basResource.getFile(SharedConfigsType.RENDER_ANIMS.id, basFileId)
        }
        bastype = BasType()
        bastype!!.basTypeList = this
        if (basData != null) {
            bastype!!.decode(Packet(basData))
        }
        synchronized(basCache) {
            basCache.put(bastype, basFileId.toLong())
            return bastype!!
        }
    }

    /**
     * Cleans the cache, removing least recently used entries.
     */
    fun cleanCache() {
        synchronized(basCache) {
            basCache.clean(5)
        }
    }

    /**
     * Resets the cache, clearing all cached BAS types.
     */
    fun resetCache() {
        synchronized(basCache) {
            basCache.reset()
        }
    }

    /**
     * Resizes the cache queue.
     */
    fun resizeCache() {
        synchronized(basCache) {
            basCache.resize()
        }
    }
}
