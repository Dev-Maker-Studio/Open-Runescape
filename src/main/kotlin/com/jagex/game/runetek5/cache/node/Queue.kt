package com.jagex.game.runetek5.cache.node

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

/**
 * Circular doubly-linked queue implementation.
 */
class Queue<T> : Iterable<T> {

    @JvmField val current = CacheableNode()
    @JvmField var next: CacheableNode? = null

    init {
        current.next = current
        current.current = current
    }

    /**
     * Removes all nodes from the queue.
     */
    fun reset() {
        while (current.next != current) {
            current.next!!.unlinkSub()
        }
    }

    /**
     * Inserts a node at the end of the queue.
     *
     * @param next The node to insert
     */
    fun insert(next: CacheableNode) {
        if (next.current != null) {
            next.unlinkSub()
        }
        next.current = current.current
        next.next = current
        next.current!!.next = next
        next.next!!.current = next
    }

    /**
     * Removes and returns the first node.
     *
     * @return The first node, or null if empty
     */
    fun getFirst(): CacheableNode? {
        val node = current.next
        return if (node == current) {
            null
        } else {
            node!!.unlinkSub()
            node
        }
    }

    /**
     * Gets the size of the queue.
     *
     * @return The number of nodes
     */
    fun size(): Int {
        var size = 0
        var node = current.next
        while (node != current) {
            size++
            node = node!!.next
        }
        return size
    }

    /**
     * Gets the next node (wrapper for getCurrentNext).
     *
     * @return The next node
     */
    fun getNext(): CacheableNode? {
        return getCurrentNext()
    }

    /**
     * Gets the first node without removing it.
     *
     * @return The first node, or null if empty
     */
    fun getCurrentNext(): CacheableNode? {
        val node = current.next
        return if (node == current) {
            next = null
            null
        } else {
            next = node!!.next
            node
        }
    }

    /**
     * Gets the next node after the current iterator position.
     *
     * @return The next node, or null if at the end
     */
    fun getDirectNext(): CacheableNode? {
        val node = next
        return if (node == current) {
            next = null
            null
        } else {
            next = node!!.next
            node
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun iterator(): Iterator<T> {
        return QueueIterator(this) as Iterator<T>
    }
}
