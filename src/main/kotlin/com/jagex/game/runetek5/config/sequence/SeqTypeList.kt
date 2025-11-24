package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.sequence.frameset.FrameSet
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class SeqTypeList(
    @JvmField val sequenceResource: Js5ResourceProvider,
    skeletonResource: Js5ResourceProvider,
    baseResource: Js5ResourceProvider
) {

    @JvmField val sequenceCache: LRUCache = LRUCache(64)
    @JvmField val framesetCache: LRUCache = LRUCache(100)

    init {
        if (sequenceResource != null) {
            val lastArchiveId = sequenceResource.getArchiveCount() - 1
            SharedConfigsType.SEQ.archiveFileCapacity()
            sequenceResource.getArchiveFileCount(lastArchiveId)
        }
        FrameSet.initialize(skeletonResource, baseResource)
    }

    fun resetCaches() {
        synchronized(sequenceCache) {
            sequenceCache.reset()
        }
        synchronized(framesetCache) {
            framesetCache.reset()
        }
    }

    fun list(sequenceId: Int): SeqType {
        var seqtype: SeqType?
        synchronized(sequenceCache) {
            seqtype = sequenceCache.get(sequenceId.toLong()) as SeqType?
        }
        if (seqtype != null) {
            return seqtype!!
        }
        val sequenceData: ByteArray?
        synchronized(sequenceResource) {
            sequenceData = sequenceResource.getFile(SharedConfigsType.SEQ.archiveId(sequenceId), SharedConfigsType.SEQ.archiveFileId(sequenceId))
        }
        seqtype = SeqType()
        seqtype!!.id = sequenceId
        seqtype!!.seqtypelist = this
        if (sequenceData != null) {
            seqtype!!.decode(Packet(sequenceData))
        }
        seqtype!!.postDecode()
        synchronized(sequenceCache) {
            sequenceCache.put(seqtype, sequenceId.toLong())
            return seqtype!!
        }
    }

    fun listFrameset(groupId: Int): FrameSet? {
        synchronized(framesetCache) {
            var frameset = framesetCache.get(groupId.toLong()) as FrameSet?
            if (frameset == null) {
                frameset = FrameSet(groupId)
                framesetCache.put(frameset, groupId.toLong())
            }
            return if (!frameset.decodeFrames()) null else frameset
        }
    }

    fun clearQueues() {
        synchronized(sequenceCache) {
            sequenceCache.resize()
        }
        synchronized(framesetCache) {
            framesetCache.resize()
        }
    }

    fun processCacheCycle() {
        synchronized(sequenceCache) {
            sequenceCache.clean(5)
        }
        synchronized(framesetCache) {
            framesetCache.clean(5)
        }
    }

    companion object {
        @JvmField var forceTween: Boolean = false
        @JvmField var seqTypeList: SeqTypeList? = null

        @JvmStatic
        fun initialize(seqtypelist: SeqTypeList) {
            seqTypeList = seqtypelist
        }
    }
}
