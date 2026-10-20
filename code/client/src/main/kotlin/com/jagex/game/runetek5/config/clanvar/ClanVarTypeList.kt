package com.jagex.game.runetek5.config.clanvar

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class ClanVarTypeList(
    @JvmField val configProvider: Js5ResourceProvider?
) {

    @JvmField val size: Int
    @JvmField val clanvarCache = LRUCache(64)

    init {
        size = if (configProvider != null) {
            configProvider.getArchiveFileCount(SharedConfigsType.CLAN_VAR.id)
        } else {
            0
        }
    }

    fun list(clanvarId: Int): ClanVarType {
        var clanvarType: ClanVarType?
        synchronized(clanvarCache) {
            clanvarType = clanvarCache.get(clanvarId.toLong()) as ClanVarType?
        }
        if (clanvarType != null) {
            return clanvarType!!
        }
        val data: ByteArray?
        synchronized(configProvider!!) {
            data = configProvider.getFile(SharedConfigsType.CLAN_VAR.id, clanvarId)
        }
        clanvarType = ClanVarType()
        if (data != null) {
            clanvarType!!.decodeType(Packet(data))
        }
        synchronized(clanvarCache) {
            clanvarCache.put(clanvarType, clanvarId.toLong())
            return clanvarType!!
        }
    }

    fun reset() {
        synchronized(clanvarCache) {
            clanvarCache.reset()
        }
    }

    fun sweep() {
        synchronized(clanvarCache) {
            clanvarCache.clean(5)
        }
    }

    fun clearQueue() {
        synchronized(clanvarCache) {
            clanvarCache.resize()
        }
    }

    companion object {
        @JvmField var CLAN_VARS: Array<Any?>? = null
    }
}
