package com.jagex.game.runetek5.config.clanvarsettings

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Manages loading and caching of clan variable settings type configurations.
 * Clan variable settings define the structure and properties of clan variables.
 */
class ClanVarSettingsTypeList(
    @JvmField val configResource: Js5ResourceProvider?
) {
    @JvmField val cache: LRUCache = LRUCache(64)

    init {
        configResource?.getArchiveFileCount(SharedConfigsType.CLAN_VAR_SETTINGS.id)
    }

    /**
     * Loads a clan variable settings type by ID, using cache if available
     * @param id The settings type ID
     * @return The loaded settings type
     */
    fun list(id: Int): ClanVarSettingsType {
        // Check cache first
        synchronized(cache) {
            val clanvarSettingsType = cache.get(id.toLong()) as? ClanVarSettingsType
            if (clanvarSettingsType != null) {
                return clanvarSettingsType
            }
        }

        // Load from resource
        val data: ByteArray? = synchronized(configResource!!) {
            configResource.getFile(SharedConfigsType.CLAN_VAR_SETTINGS.id, id)
        }

        // Decode settings type
        val clanvarSettingsType = ClanVarSettingsType()
        if (data != null) {
            clanvarSettingsType.decode(Packet(data))
        }

        // Cache and return
        synchronized(cache) {
            cache.put(clanvarSettingsType, id.toLong())
            return clanvarSettingsType
        }
    }

    /**
     * Sweeps the cache, cleaning old entries
     */
    fun sweepCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    /**
     * Resets the entire cache, clearing all entries
     */
    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    /**
     * Clears the cache queue, resizing it to remove unused entries
     */
    fun clearCacheQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }
}
