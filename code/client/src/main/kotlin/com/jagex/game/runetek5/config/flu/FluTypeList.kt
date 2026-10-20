package com.jagex.game.runetek5.config.flu

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class FluTypeList(fluResource: Js5ResourceProvider) {

    @JvmField val cache = LRUCache(128)
    @JvmField val underlayResource: Js5ResourceProvider = fluResource

    init {
        underlayResource.getArchiveFileCount(SharedConfigsType.UNDERLAYS.id)
    }

    fun list(underlayId: Int): FluType {
        var flutype: FluType?
        synchronized(cache) {
            flutype = cache.get(underlayId.toLong()) as FluType?
        }
        if (flutype != null) {
            return flutype!!
        }
        val fluData: ByteArray?
        synchronized(underlayResource) {
            fluData = underlayResource.getFile(SharedConfigsType.UNDERLAYS.id, underlayId)
        }
        flutype = FluType()
        if (fluData != null) {
            flutype!!.decode(Packet(fluData))
        }
        synchronized(cache) {
            cache.put(flutype, underlayId.toLong())
            return flutype!!
        }
    }

    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    fun processCycle() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun clearQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }
}
