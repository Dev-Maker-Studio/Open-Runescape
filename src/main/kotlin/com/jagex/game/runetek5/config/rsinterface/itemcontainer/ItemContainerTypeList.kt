package com.jagex.game.runetek5.config.rsinterface.itemcontainer

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages loading and caching of item container (inventory) type configurations.
 *
 * @author Brainpower - 06/05/2022
 */
class ItemContainerTypeList(
    /** The resource provider for shared config files */
    @JvmField val configResource: Js5ResourceProvider
) {
    /** Cached container types */
    @JvmField val cached: LRUCache = LRUCache(64)

    init {
        configResource.getArchiveFileCount(SharedConfigsType.INVENTORIES.id)
    }

    /**
     * Loads an item container type by ID, using cache if available
     * @param inventoryId The inventory/container ID
     * @return The loaded container type
     */
    fun list(inventoryId: Int): ItemContainerType {
        // Check cache first
        synchronized(cached) {
            val containerType = cached.get(inventoryId.toLong()) as? ItemContainerType
            if (containerType != null) {
                return containerType
            }
        }

        // Load from resource
        val data: ByteArray? = synchronized(configResource) {
            configResource.getFile(SharedConfigsType.INVENTORIES.id, inventoryId)
        }

        // Decode container type
        val containerType = ItemContainerType()
        if (data != null) {
            containerType.decode(Packet(data))
        }

        // Cache and return
        synchronized(cached) {
            cached.put(containerType, inventoryId.toLong())
            return containerType
        }
    }

    companion object {
        /**
         * The preferences the player has selected (e.g. textures off).
         * Bitmask of enabled preferences.
         */
        @JvmField
        var preferenceMask: Int = 0
    }
}
