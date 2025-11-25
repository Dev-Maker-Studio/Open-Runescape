package com.jagex.game.runetek5.config.sequence.frameset

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class FrameSet(
    @JvmField val groupId: Int
) : CacheableNode() {

    @JvmField var frameData: Array<ByteArray?>? = null
    @JvmField var frames: Array<AnimFrame?>? = null

    fun modifiesAlpha(frameIndex: Int): Boolean {
        return frames!![frameIndex]!!.modifiesAlpha
    }

    fun modifiesColors(frameIndex: Int): Boolean {
        return frames!![frameIndex]!!.modifiesColor
    }

    fun modifiesBillboards(frameIndex: Int): Boolean {
        return frames!![frameIndex]!!.modifiesBillboard
    }

    fun decodeFrames(): Boolean {
        if (frames != null) {
            return true
        }
        if (frameData == null) {
            synchronized(frameskeletonResource!!) {
                if (!frameskeletonResource!!.loadArchive(groupId)) {
                    return false
                }
                val fileIds = frameskeletonResource!!.getValidFileIds(groupId)
                frameData = arrayOfNulls(fileIds.size)
                for (index in fileIds.indices) {
                    frameData!![index] = frameskeletonResource!!.getFile(groupId, fileIds[index])
                }
            }
        }
        var loaded = true
        for (index in frameData!!.indices) {
            val data = frameData!![index]!!
            val buffer = Packet(data)
            buffer.currentPosition = 1
            val frameId = buffer.readUShort()
            synchronized(framebaseResource!!) {
                loaded = loaded and framebaseResource!!.isFileCached(frameId)
            }
        }
        if (!loaded) {
            return false
        }
        val frameBaseList = LinkedList()
        val fileIds: IntArray
        synchronized(frameskeletonResource!!) {
            val fileCount = frameskeletonResource!!.getArchiveFileCount(groupId)
            frames = arrayOfNulls(fileCount)
            fileIds = frameskeletonResource!!.getValidFileIds(groupId)
        }
        for (fileIndex in fileIds.indices) {
            val data = frameData!![fileIndex]!!
            val buffer = Packet(data)
            buffer.currentPosition = 1
            val frameId = buffer.readUShort()
            var frameBase: AnimBase? = null
            var sequenceBase = frameBaseList.getTail() as AnimBase?
            while (sequenceBase != null) {
                if (frameId == sequenceBase.id) {
                    frameBase = sequenceBase
                    break
                }
                sequenceBase = frameBaseList.getPrevious() as AnimBase?
            }
            if (frameBase == null) {
                synchronized(framebaseResource!!) {
                    frameBase = AnimBase(frameId, framebaseResource!!.getFile(frameId))
                }
                frameBaseList.insertTail(frameBase)
            }
            frames!![fileIds[fileIndex]] = AnimFrame(data, frameBase!!)
        }
        frameData = null
        return true
    }

    companion object {
        @JvmStatic var framebaseResource: Js5ResourceProvider? = null
        @JvmStatic var frameskeletonResource: Js5ResourceProvider? = null

        @JvmStatic
        fun initialize(skeletonResource: Js5ResourceProvider, baseResource: Js5ResourceProvider) {
            frameskeletonResource = skeletonResource
            framebaseResource = baseResource
        }
    }
}
