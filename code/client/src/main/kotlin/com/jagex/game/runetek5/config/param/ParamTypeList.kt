package com.jagex.game.runetek5.config.param

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class ParamTypeList(
    @JvmField val paramResource: Js5ResourceProvider?
) {
    @JvmField val cache: LRUCache = LRUCache(64)

    init {
        paramResource?.getArchiveFileCount(SharedConfigsType.PARAMS.id)
    }

    fun clearCacheQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }

    fun sweepCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun list(id: Int): ParamType {
        synchronized(cache) {
            val paramType = cache.get(id.toLong()) as? ParamType
            if (paramType != null) {
                return paramType
            }
        }

        val data: ByteArray? = synchronized(paramResource!!) {
            paramResource.getFile(SharedConfigsType.PARAMS.id, id)
        }

        val paramType = ParamType()
        if (data != null) {
            paramType.decode(Packet(data))
        }

        synchronized(cache) {
            cache.put(paramType, id.toLong())
            return paramType
        }
    }

    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }
}
