package com.jagex.game.runetek5.cache.node

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

/**
 * Iterator for traversing cacheable nodes in a Queue.
 */
class QueueIterator(queue: Queue<*>) : MutableIterator<CacheableNode?> {

    @JvmField var lastReturned: CacheableNode? = null
    @JvmField val queue: Queue<*> = queue
    @JvmField var current: CacheableNode? = queue.current.nextCache

    init {
        lastReturned = null
    }

    /**
     * Resets the iterator and returns the next node.
     *
     * @return The next node after reset
     */
    fun nextAndReset(): CacheableNode? {
        reset()
        return next() as CacheableNode?
    }

    override fun next(): CacheableNode? {
        var cacheablenode1 = current
        if (cacheablenode1 == queue.current) {
            cacheablenode1 = null
            current = null
        } else {
            current = cacheablenode1?.nextCache
        }
        lastReturned = cacheablenode1
        return cacheablenode1
    }

    override fun hasNext(): Boolean {
        return queue.current != current
    }

    override fun remove() {
        val node = lastReturned ?: throw IllegalStateException()
        node.unlinkSub()
        lastReturned = null
    }

    /**
     * Resets the iterator to the beginning of the queue.
     */
    fun reset() {
        current = queue.current.nextCache
        lastReturned = null
    }

    companion object {
        @JvmField var LOYALTY_ENABLED = false
    }
}
