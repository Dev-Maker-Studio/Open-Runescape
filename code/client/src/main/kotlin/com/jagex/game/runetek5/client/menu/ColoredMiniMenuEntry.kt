package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.lru.CacheableNode

class ColoredMiniMenuEntry(
    @JvmField val tagStart: String
) : CacheableNode() {

    @JvmField val nodeQueue = Queue<CacheableNode>()
    @JvmField var queueSize = 0

    fun getClickType(): Int {
        return if (nodeQueue.current.nextCache != nodeQueue.current) {
            (nodeQueue.current.nextCache as MiniMenuEntry).clickType
        } else {
            -1
        }
    }

    fun addEntry(baseEntry: MiniMenuEntry): Boolean {
        var isFirst = true
        baseEntry.unlinkSub()

        var subEntry = nodeQueue.getNext() as MiniMenuEntry?
        while (subEntry != null) {
            if (MiniMenu.hasHigherPriority(baseEntry.clickType, subEntry.clickType)) {
                push(baseEntry, subEntry)
                queueSize++
                return !isFirst
            }
            isFirst = false
            subEntry = nodeQueue.getDirectNext() as MiniMenuEntry?
        }

        nodeQueue.insert(baseEntry)
        queueSize++
        return isFirst
    }

    fun removeEntry(entry: MiniMenuEntry): Boolean {
        val clickType = getClickType()
        entry.unlinkSub()
        queueSize--
        if (queueSize == 0) {
            unlink()
            unlinkSub()
            MiniMenu.coloredEntryCount--
            MiniMenu.coloredEntryCache.put(this, entry.bitpacked)
            return false
        } else {
            return clickType != getClickType()
        }
    }

    companion object {
        @JvmStatic
        fun push(nodeA: CacheableNode, nodeB: CacheableNode) {
            if (nodeA.current != null) {
                nodeA.unlinkSub()
            }
            nodeA.current = nodeB.current
            nodeA.nextCache = nodeB
            nodeA.current!!.nextCache = nodeA
            nodeA.nextCache!!.current = nodeA
        }
    }
}
