package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.lru.CacheableNode

/**
 * A mini menu entry container that groups entries by color tag.
 */
class ColoredMiniMenuEntry(
    @JvmField val tagStart: String
) : CacheableNode() {

    @JvmField val nodeQueue = Queue<CacheableNode>()
    @JvmField var queueSize = 0

    /**
     * Gets the click type of the first entry in the queue.
     *
     * @return The click type, or -1 if the queue is empty
     */
    fun getClickType(): Int {
        return if (nodeQueue.current.next != nodeQueue.current) {
            (nodeQueue.current.next as MiniMenuEntry).clickType
        } else {
            -1
        }
    }

    /**
     * Adds a menu entry to the queue, maintaining priority order.
     *
     * @param baseEntry The entry to add
     * @return true if this is the first entry added, false otherwise
     */
    fun addEntry(baseEntry: MiniMenuEntry): Boolean {
        var isFirst = true
        baseEntry.unlinkSub()

        var subEntry = nodeQueue.getNext() as MiniMenuEntry?
        while (subEntry != null) {
            if (MiniMenu.method5334(baseEntry.clickType, subEntry.clickType)) {
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

    /**
     * Removes a menu entry from the queue.
     *
     * @param entry The entry to remove
     * @return true if the click type changed after removal, false otherwise
     */
    fun removeEntry(entry: MiniMenuEntry): Boolean {
        val clickType = getClickType()
        entry.unlinkSub()
        queueSize--
        if (queueSize == 0) {
            unlink()
            unlinkSub()
            MiniMenu.anInt170--
            MiniMenu.aClass229164.put(this, entry.bitpacked)
            return false
        } else {
            return clickType != getClickType()
        }
    }

    companion object {
        /**
         * Inserts nodeA before nodeB in a linked list.
         *
         * @param nodeA The node to insert
         * @param nodeB The node to insert before
         */
        @JvmStatic
        fun push(nodeA: CacheableNode, nodeB: CacheableNode) {
            if (nodeA.current != null) {
                nodeA.unlinkSub()
            }
            nodeA.current = nodeB.current
            nodeA.next = nodeB
            nodeA.current!!.next = nodeA
            nodeA.next!!.current = nodeA
        }
    }
}
