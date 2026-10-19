package com.jagex.game.runetek5.config.npc

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText

class NpcTypeList(
    @JvmField val game: Game,
    @JvmField val lang: Language,
    @JvmField val configResource: Js5ResourceProvider?,
    @JvmField val modelResource: Js5ResourceProvider
) {

    @JvmField var preferenceFlags = 0
    @JvmField val typeCache = LRUCache(64)
    @JvmField val bodymodelCache = LRUCache(50)
    @JvmField val headmodelCache = LRUCache(5)
    @JvmField var isMember = false
    @JvmField val defaultActions: Array<String?>

    init {
        isMember = true
        if (configResource != null) {
            val lastArchive = configResource.getArchiveCount() - 1
            SharedConfigsType.NPCS.archiveFileCapacity()
            configResource.getArchiveFileCount(lastArchive)
        }
        defaultActions = if (game == Game.RUNESCAPE) {
            arrayOf(null, null, null, null, null, LocalizedText.EXAMINE.translate(lang))
        } else {
            arrayOfNulls(6)
        }
    }

    fun list(id: Int): NPCType {
        val npcType: NPCType?
        synchronized(typeCache) {
            npcType = typeCache.get(id.toLong()) as NPCType?
        }
        if (npcType != null) {
            return npcType
        }
        val data: ByteArray?
        synchronized(configResource!!) {
            data = configResource.getFile(SharedConfigsType.NPCS.archiveId(id), SharedConfigsType.NPCS.archiveFileId(id))
        }
        val newNpcType = NPCType()
        newNpcType.type = id
        newNpcType.npcTypeList = this
        newNpcType.actions = defaultActions.clone()
        if (data != null) {
            newNpcType.decodeType(Packet(data))
        }
        newNpcType.setGameType()
        synchronized(typeCache) {
            typeCache.put(newNpcType, id.toLong())
            return newNpcType
        }
    }

    fun setPreferenceFlags(prefFlags: Int) {
        preferenceFlags = prefFlags
        synchronized(bodymodelCache) {
            bodymodelCache.reset()
        }
        synchronized(headmodelCache) {
            headmodelCache.reset()
        }
    }

    fun resetBodyCaches() {
        synchronized(bodymodelCache) {
            bodymodelCache.reset()
        }
        synchronized(headmodelCache) {
            headmodelCache.reset()
        }
    }

    fun sweepCaches() {
        synchronized(typeCache) {
            typeCache.clean(5)
        }
        synchronized(bodymodelCache) {
            bodymodelCache.clean(5)
        }
        synchronized(headmodelCache) {
            headmodelCache.clean(5)
        }
    }

    fun clearCacheQueues() {
        synchronized(typeCache) {
            typeCache.resize()
        }
        synchronized(bodymodelCache) {
            bodymodelCache.resize()
        }
        synchronized(headmodelCache) {
            headmodelCache.resize()
        }
    }

    fun resetCaches() {
        synchronized(typeCache) {
            typeCache.reset()
        }
        synchronized(bodymodelCache) {
            bodymodelCache.reset()
        }
        synchronized(headmodelCache) {
            headmodelCache.reset()
        }
    }

    fun setMember(isMember: Boolean) {
        if (this.isMember != isMember) {
            this.isMember = isMember
            resetCaches()
        }
    }
}
