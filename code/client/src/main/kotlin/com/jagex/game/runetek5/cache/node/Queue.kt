package com.jagex.game.runetek5.cache.node

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

class Queue<T> : Iterable<T> {

    @JvmField val current = CacheableNode()
    @JvmField var next: CacheableNode? = null

    init {
        current.nextCache = current
        current.current = current
    }

    fun reset() {
        while (current.nextCache != current) {
            current.nextCache!!.unlinkSub()
        }
    }

    fun insert(next: CacheableNode) {
        if (next.current != null) {
            next.unlinkSub()
        }
        next.current = current.current
        next.nextCache = current
        next.current!!.nextCache = next
        next.nextCache!!.current = next
    }

    fun getFirst(): CacheableNode? {
        val node = current.nextCache
        return if (node == current) {
            null
        } else {
            node!!.unlinkSub()
            node
        }
    }

    fun size(): Int {
        var size = 0
        var node = current.nextCache
        while (node != current) {
            size++
            node = node!!.nextCache
        }
        return size
    }

    fun getNext(): CacheableNode? {
        return getCurrentNext()
    }

    fun getCurrentNext(): CacheableNode? {
        val node = current.nextCache
        return if (node == current) {
            next = null
            null
        } else {
            next = node!!.nextCache
            node
        }
    }

    fun getDirectNext(): CacheableNode? {
        val node = next
        return if (node == current) {
            next = null
            null
        } else {
            next = node!!.nextCache
            node
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun iterator(): Iterator<T> {
        return QueueIterator(this) as Iterator<T>
    }
}
