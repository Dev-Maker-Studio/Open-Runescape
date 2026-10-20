package com.jagex.game.runetek5.config.varp

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class VarpbitTypeList(
    @JvmField val varbitResource: Js5ResourceProvider?
) {

    @JvmField var cache = LRUCache(64)

    init {
        if (varbitResource != null) {
            val lastArchiveId = varbitResource.getArchiveCount() - 1
            SharedConfigsType.VARPBIT.archiveFileCapacity()
            varbitResource.getArchiveFileCount(lastArchiveId)
        }
    }

    fun list(id: Int): VarpbitType {
        var varbit: VarpbitType?
        synchronized(cache) {
            varbit = cache.get(id.toLong()) as VarpbitType?
        }
        if (varbit != null) {
            return varbit!!
        }
        val varbitData: ByteArray?
        synchronized(varbitResource!!) {
            varbitData = varbitResource.getFile(
                SharedConfigsType.VARPBIT.archiveId(id),
                SharedConfigsType.VARPBIT.archiveFileId(id)
            )
        }
        varbit = VarpbitType()
        if (varbitData != null) {
            varbit!!.decode(Packet(varbitData))
        }
        synchronized(cache) {
            cache.put(varbit, id.toLong())
            return varbit!!
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

    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    fun resetCache(capacity: Int) {
        synchronized(cache) {
            cache.reset()
            cache = LRUCache(capacity)
        }
    }
}
