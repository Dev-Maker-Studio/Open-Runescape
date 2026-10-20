package com.jagex.game.runetek5.config.mec

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class MECTypeList(
    @JvmField val definitionResource: Js5ResourceProvider,
    @JvmField val spriteResource: Js5ResourceProvider
) {

    @JvmField var definitionCache: LRUCache = LRUCache(128)
    @JvmField var spriteCache: LRUCache = LRUCache(64)

    init {
        definitionResource.getArchiveFileCount(SharedConfigsType.MAP_AREAS.id)
    }

    fun sweepCache() {
        synchronized(definitionCache) {
            definitionCache.clean(5)
        }
        synchronized(spriteCache) {
            spriteCache.clean(5)
        }
    }

    fun clearCacheQueue() {
        synchronized(definitionCache) {
            definitionCache.resize()
        }
        synchronized(spriteCache) {
            spriteCache.resize()
        }
    }

    fun createCache(definitionCapacity: Int, spriteCapacity: Int) {
        definitionCache = LRUCache(definitionCapacity)
        spriteCache = LRUCache(spriteCapacity)
    }

    fun resetCache() {
        synchronized(definitionCache) {
            definitionCache.reset()
        }
        synchronized(spriteCache) {
            spriteCache.reset()
        }
    }

    fun list(id: Int): MECType {
        var mectype: MECType?
        synchronized(definitionCache) {
            mectype = definitionCache.get(id.toLong()) as MECType?
        }

        if (mectype != null) {
            return mectype!!
        }
        val data: ByteArray?
        synchronized(definitionResource) {
            data = definitionResource.getFile(SharedConfigsType.MAP_AREAS.id, id)
        }

        mectype = MECType()
        mectype!!.mectypeId = id
        mectype!!.initialType = this
        if (data != null) {
            mectype!!.decode(Packet(data))
        }

        mectype!!.postDecode()
        synchronized(definitionCache) {
            definitionCache.put(mectype!!, id.toLong())
            return mectype!!
        }
    }
}
