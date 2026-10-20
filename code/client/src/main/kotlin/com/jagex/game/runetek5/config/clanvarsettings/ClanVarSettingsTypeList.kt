package com.jagex.game.runetek5.config.clanvarsettings

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class ClanVarSettingsTypeList(
    @JvmField val configResource: Js5ResourceProvider?
) {
    @JvmField val cache: LRUCache = LRUCache(64)

    init {
        configResource?.getArchiveFileCount(SharedConfigsType.CLAN_VAR_SETTINGS.id)
    }

    fun list(id: Int): ClanVarSettingsType {
        synchronized(cache) {
            val clanvarSettingsType = cache.get(id.toLong()) as? ClanVarSettingsType
            if (clanvarSettingsType != null) {
                return clanvarSettingsType
            }
        }

        val data: ByteArray? = synchronized(configResource!!) {
            configResource.getFile(SharedConfigsType.CLAN_VAR_SETTINGS.id, id)
        }

        val clanvarSettingsType = ClanVarSettingsType()
        if (data != null) {
            clanvarSettingsType.decode(Packet(data))
        }

        synchronized(cache) {
            cache.put(clanvarSettingsType, id.toLong())
            return clanvarSettingsType
        }
    }

    fun sweepCache() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun resetCache() {
        synchronized(cache) {
            cache.reset()
        }
    }

    fun clearCacheQueue() {
        synchronized(cache) {
            cache.resize()
        }
    }
}
