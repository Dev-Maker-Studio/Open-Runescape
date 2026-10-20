package com.jagex.game.runetek5.config.flo

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class FloTypeList(val floResource: Js5ResourceProvider) {

    @JvmField var lastLoadedOverlayId: Int = 0

    @JvmField val totalFileCount: Int = floResource.getArchiveFileCount(SharedConfigsType.OVERLAYS.id)

    @JvmField val overlayCache = LRUCache(64)

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

    fun reset() {
        synchronized(overlayCache) {
            overlayCache.reset()
        }
    }

    fun cycle() {
        synchronized(overlayCache) {
            overlayCache.clean(5)
        }
    }

    fun clear() {
        synchronized(overlayCache) {
            overlayCache.resize()
        }
    }
}
