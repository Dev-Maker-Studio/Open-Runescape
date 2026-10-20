package com.jagex.game.runetek5.config.varn

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class VarnBitTypeList(
    @JvmField val varnbitProvider: Js5ResourceProvider?
) {

    @JvmField val cache = LRUCache(64)

    init {
        varnbitProvider?.getArchiveFileCount(SharedConfigsType.VARNBIT.id)
    }

    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    fun sweepCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun clearCacheQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }

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
