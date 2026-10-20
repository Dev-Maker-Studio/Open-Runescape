package com.jagex.game.runetek5.config.sun

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class SunTypeList(sunResource: Js5ResourceProvider) {

    @JvmField val cache = LRUCache(16)
    @JvmField val sunResource: Js5ResourceProvider = sunResource

    init {
        this.sunResource.getArchiveFileCount(SharedConfigsType.SUN.id)
    }

    fun list(id: Int): SunType {
        var sunType: SunType?
        synchronized(cache) {
            sunType = cache.get(id.toLong()) as SunType?
        }
        if (sunType != null) {
            return sunType!!
        }
        val data: ByteArray?
        synchronized(sunResource) {
            data = sunResource.getFile(SharedConfigsType.SUN.id, id)
        }
        sunType = SunType()
        if (data != null) {
            sunType!!.decode(Packet(data))
        }
        synchronized(cache) {
            cache.put(sunType, id.toLong())
            return sunType!!
        }
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
}
