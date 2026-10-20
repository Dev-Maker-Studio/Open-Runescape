package com.jagex.game.runetek5.config.identitykit

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class IdkTypeList(
    @JvmField val identityResource: Js5ResourceProvider,
    @JvmField val modelResource: Js5ResourceProvider
) {

    @JvmField val cache = LRUCache(64)

    init {
        identityResource.getArchiveFileCount(SharedConfigsType.IDENTITY_KIT.id)
    }

    fun list(identityId: Int): IdkType {
        var identityType: IdkType?
        synchronized(cache) {
            identityType = cache.get(identityId.toLong()) as IdkType?
        }
        if (identityType != null) {
            return identityType!!
        }
        val data: ByteArray?
        synchronized(identityResource) {
            data = identityResource.getFile(SharedConfigsType.IDENTITY_KIT.id, identityId)
        }
        identityType = IdkType()
        identityType!!.identityTypeList = this
        if (data != null) {
            identityType!!.decode(Packet(data))
        }
        synchronized(cache) {
            cache.put(identityType, identityId.toLong())
            return identityType!!
        }
    }

    fun reset() {
        synchronized(cache) {
            cache.reset()
        }
    }

    fun cycleCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun remove() {
        synchronized(cache) {
            cache.resize()
        }
    }
}
