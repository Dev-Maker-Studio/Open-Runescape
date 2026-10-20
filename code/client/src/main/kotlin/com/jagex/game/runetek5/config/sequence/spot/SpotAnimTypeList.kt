package com.jagex.game.runetek5.config.sequence.spot

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class SpotAnimTypeList(
    @JvmField val gfxResource: Js5ResourceProvider,
    @JvmField val modelResource: Js5ResourceProvider
) {

    @JvmField val definitionCache: LRUCache = LRUCache(64)
    @JvmField val modelCache: LRUCache = LRUCache(60)
    @JvmField var preferenceMask: Int = 0

    init {
        val archiveCount = gfxResource.getArchiveCount() - 1
        SharedConfigsType.GFX.archiveFileCapacity()
        gfxResource.getArchiveFileCount(archiveCount)
    }

    fun list(id: Int): SpotAnimType {
        var gfxType: SpotAnimType?
        synchronized(definitionCache) {
            gfxType = definitionCache.get(id.toLong()) as SpotAnimType?
        }
        if (gfxType != null) {
            return gfxType!!
        }
        val gfxData: ByteArray?
        synchronized(gfxResource) {
            gfxData = gfxResource.getFile(SharedConfigsType.GFX.archiveId(id), SharedConfigsType.GFX.archiveFileId(id))
        }
        gfxType = SpotAnimType()
        gfxType!!.gfxTypeList = this
        gfxType!!.currentId = id
        if (gfxData != null) {
            gfxType!!.decode(Packet(gfxData))
        }
        synchronized(definitionCache) {
            definitionCache.put(gfxType!!, id.toLong())
            return gfxType!!
        }
    }

    fun setPreferenceMask(preferenceMask: Int) {
        this.preferenceMask = preferenceMask
        synchronized(modelCache) {
            modelCache.reset()
        }
    }

    fun sweepCache() {
        synchronized(definitionCache) {
            definitionCache.clean(5)
        }
        synchronized(modelCache) {
            modelCache.clean(5)
        }
    }

    fun resetCache() {
        synchronized(definitionCache) {
            definitionCache.reset()
        }
        synchronized(modelCache) {
            modelCache.reset()
        }
    }

    fun clearCacheQueue() {
        synchronized(definitionCache) {
            definitionCache.resize()
        }
        synchronized(modelCache) {
            modelCache.resize()
        }
    }
}
