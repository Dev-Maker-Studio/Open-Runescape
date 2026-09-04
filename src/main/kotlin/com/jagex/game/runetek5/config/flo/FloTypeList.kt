package com.jagex.game.runetek5.config.flo

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * < Fully refactored >
 * @author Brainpower - 28/05/2020
 *
 * Manages overlay floor files
 *
 */
class FloTypeList(val floResource: Js5ResourceProvider) {

    /**
     * The last decoded overlay
     */
    @JvmField var lastLoadedOverlayId: Int = 0

    /**
     * The total amount of overlay floors in the cache
     */
    @JvmField val totalFileCount: Int = floResource.getArchiveFileCount(SharedConfigsType.OVERLAYS.id)

    /**
     * Cached floors by most used
     */
    @JvmField val overlayCache = LRUCache(64)

    /**
     * Decode a flo file by it's id
     */
    fun list(overlayId: Int): FloType {
        var floType: FloType?
        synchronized(overlayCache) {
            floType = overlayCache.get(overlayId.toLong()) as FloType?
        }
        if (floType != null) {
            return floType!!
        }
        val floData: ByteArray?
        synchronized(floResource) {
            floData = floResource.getFile(SharedConfigsType.OVERLAYS.id, overlayId)
        }
        floType = FloType()
        floType!!.floTypeList = this
        floType!!.overlayId = overlayId
        if (floData != null) {
            floType!!.decode(Packet(floData))
        }
        floType!!.postDecode()
        synchronized(overlayCache) {
            overlayCache.put(floType, overlayId.toLong())
            return floType!!
        }
    }

    /**
     * Reset the type cache
     */
    fun reset() {
        synchronized(overlayCache) {
            overlayCache.reset()
        }
    }

    /**
     * Increase cycle on cache entries
     * to keep track of how long each one has existed
     */
    fun cycle() {
        synchronized(overlayCache) {
            overlayCache.clean(5)
        }
    }

    /**
     * Remove all entries from the queue
     */
    fun clear() {
        synchronized(overlayCache) {
            overlayCache.resize()
        }
    }
}
