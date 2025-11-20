package com.jagex.game.runetek5.config.varn

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages the loading and caching of VarnBit configuration types.
 * VarnBits are used to extract specific bit ranges from Varn variables.
 *
 * @author Brainpower - 25/03/2023
 */
class VarnBitTypeList(
    @JvmField val varnbitProvider: Js5ResourceProvider?
) {

    @JvmField val cache = LRUCache(64)

    init {
        varnbitProvider?.getArchiveFileCount(SharedConfigsType.VARNBIT.id)
    }

    /**
     * Resets the cache, clearing all cached VarnBit types.
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

    /**
     * Retrieves a VarnBit type by ID, loading from cache or resource provider.
     *
     * @param id The VarnBit ID
     * @return The VarnBit type configuration
     */
    fun list(id: Int): VarnBitType {
        var varnbitType: VarnBitType?
        synchronized(cache) {
            varnbitType = cache.get(id.toLong()) as VarnBitType?
        }
        if (varnbitType != null) {
            return varnbitType!!
        }
        val data = varnbitProvider?.getFile(SharedConfigsType.VARNBIT.id, id)
        varnbitType = VarnBitType()
        if (data != null) {
            varnbitType!!.decode(Packet(data))
        }
        synchronized(cache) {
            cache.put(varnbitType, id.toLong())
            return varnbitType!!
        }
    }
}
