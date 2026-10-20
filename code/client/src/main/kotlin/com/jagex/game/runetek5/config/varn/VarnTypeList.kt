package com.jagex.game.runetek5.config.varn

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class VarnTypeList(
    @JvmField val configResource: Js5ResourceProvider?
) {

    @JvmField val cache = LRUCache(64)

    init {
        configResource?.getArchiveFileCount(SharedConfigsType.VARN.id)
    }

    fun list(id: Int): VarnType {
        var varnType: VarnType?
        synchronized(cache) {
            varnType = cache.get(id.toLong()) as VarnType?
        }
        if (varnType != null) {
            return varnType!!
        }
        val data = configResource?.getFile(SharedConfigsType.VARN.id, id)
        varnType = VarnType()
        if (data != null) {
            varnType!!.decode(Packet(data))
        }
        synchronized(cache) {
            cache.put(varnType, id.toLong())
            return varnType!!
        }
    }

    fun sweep() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun clearQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }

    fun reset() {
        synchronized(cache) {
            cache.reset()
        }
    }
}
