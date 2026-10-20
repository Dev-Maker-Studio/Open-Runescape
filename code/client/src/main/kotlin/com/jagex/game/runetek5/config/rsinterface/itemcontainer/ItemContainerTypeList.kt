package com.jagex.game.runetek5.config.rsinterface.itemcontainer

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class ItemContainerTypeList(
    @JvmField val configResource: Js5ResourceProvider
) {
    @JvmField val cached: LRUCache = LRUCache(64)

    init {
        configResource.getArchiveFileCount(SharedConfigsType.INVENTORIES.id)
    }

    fun list(inventoryId: Int): ItemContainerType {
        synchronized(cached) {
            val containerType = cached.get(inventoryId.toLong()) as? ItemContainerType
            if (containerType != null) {
                return containerType
            }
        }

        val data: ByteArray? = synchronized(configResource) {
            configResource.getFile(SharedConfigsType.INVENTORIES.id, inventoryId)
        }

        val containerType = ItemContainerType()
        if (data != null) {
            containerType.decode(Packet(data))
        }

        synchronized(cached) {
            cached.put(containerType, inventoryId.toLong())
            return containerType
        }
    }

    companion object {
        @JvmField
        var preferenceMask: Int = 0
    }
}
