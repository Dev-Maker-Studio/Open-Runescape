package com.jagex.game.runetek5.config.hitbar

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class HitbarTypeList(
    @JvmField val hitbarResource: Js5ResourceProvider,
    @JvmField val spriteResource: Js5ResourceProvider
) {

    @JvmField val hitbarCache = LRUCache(64)
    @JvmField val spriteCache = LRUCache(20)

    init {
        hitbarResource.getArchiveFileCount(SharedConfigsType.HITBARS.id)
    }

    fun list(id: Int): HitbarType {
        var hitbarType: HitbarType?
        synchronized(hitbarCache) {
            hitbarType = hitbarCache.get(id.toLong()) as HitbarType?
        }
        if (hitbarType != null) {
            return hitbarType!!
        }
        val hitbarData: ByteArray?
        synchronized(hitbarResource) {
            hitbarData = hitbarResource.getFile(SharedConfigsType.HITBARS.id, id)
        }
        hitbarType = HitbarType()
        hitbarType!!.hitbarTypeList = this
        if (hitbarData != null) {
            hitbarType!!.decode(Packet(hitbarData))
        }
        synchronized(hitbarCache) {
            hitbarCache.put(hitbarType, id.toLong())
            return hitbarType!!
        }
    }

    fun resetCaches() {
        synchronized(hitbarCache) {
            hitbarCache.reset()
        }
        synchronized(spriteCache) {
            spriteCache.reset()
        }
    }

    fun processCycle() {
        synchronized(hitbarCache) {
            hitbarCache.clean(5)
        }
        synchronized(spriteCache) {
            spriteCache.clean(5)
        }
    }

    fun clearQueues() {
        synchronized(hitbarCache) {
            hitbarCache.resize()
        }
        synchronized(spriteCache) {
            spriteCache.resize()
        }
    }
}
