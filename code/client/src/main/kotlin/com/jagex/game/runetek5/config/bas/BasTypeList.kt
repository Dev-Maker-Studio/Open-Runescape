package com.jagex.game.runetek5.config.bas

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class BasTypeList(
    @JvmField val basResource: Js5ResourceProvider,
    @JvmField val equipmentDefaults: EquipmentDefaultsType
) {

    @JvmField val basCache = LRUCache(64)

    init {
        basResource.getArchiveFileCount(SharedConfigsType.RENDER_ANIMS.id)
    }

    fun list(basFileId: Int): BasType {
        var bastype: BasType?
        synchronized(basCache) {
            bastype = basCache.get(basFileId.toLong()) as BasType?
        }
        if (bastype != null) {
            return bastype!!
        }
        val basData: ByteArray?
        synchronized(basResource) {
            basData = basResource.getFile(SharedConfigsType.RENDER_ANIMS.id, basFileId)
        }
        bastype = BasType()
        bastype!!.basTypeList = this
        if (basData != null) {
            bastype!!.decode(Packet(basData))
        }
        synchronized(basCache) {
            basCache.put(bastype, basFileId.toLong())
            return bastype!!
        }
    }

    fun cleanCache() {
        synchronized(basCache) {
            basCache.clean(5)
        }
    }

    fun resetCache() {
        synchronized(basCache) {
            basCache.reset()
        }
    }

    fun resizeCache() {
        synchronized(basCache) {
            basCache.resize()
        }
    }
}
