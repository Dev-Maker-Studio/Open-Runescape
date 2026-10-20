package com.jagex.game.runetek5.config.varp

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class VarpTypeList(
    @JvmField val resourceProvider: Js5ResourceProvider?
) {

    @JvmField val size: Int
    @JvmField val vardefinitionCache = LRUCache(64)

    init {
        size = if (resourceProvider != null) {
            resourceProvider.getArchiveFileCount(SharedConfigsType.VARS.id)
        } else {
            0
        }
    }

    fun list(id: Int): VarpType {
        var varType: VarpType?
        synchronized(vardefinitionCache) {
            varType = vardefinitionCache.get(id.toLong()) as VarpType?
        }
        if (varType != null) {
            return varType!!
        }
        val data: ByteArray?
        synchronized(resourceProvider!!) {
            data = resourceProvider.getFile(SharedConfigsType.VARS.id, id)
        }
        varType = VarpType()
        if (data != null) {
            varType!!.decode(Packet(data))
        }
        synchronized(vardefinitionCache) {
            vardefinitionCache.put(varType, id.toLong())
            return varType!!
        }
    }

    fun sweepCache() {
        synchronized(vardefinitionCache) {
            vardefinitionCache.clean(5)
        }
    }

    fun resetCache() {
        synchronized(vardefinitionCache) {
            vardefinitionCache.reset()
        }
    }

    fun clearCacheQueue() {
        synchronized(vardefinitionCache) {
            vardefinitionCache.resize()
        }
    }
}
