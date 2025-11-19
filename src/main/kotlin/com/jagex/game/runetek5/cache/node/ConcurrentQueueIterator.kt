package com.jagex.game.runetek5.cache.node

import java.util.ConcurrentModificationException
import java.util.NoSuchElementException

class ConcurrentQueueIterator(
    @JvmField val concurrentQueue: ConcurrentQueue
) : MutableIterator<Any> {

    @JvmField var currentPosition: Int = 0
    @JvmField val accessingThreadId: Int = concurrentQueue.accessingThreadId

    override fun next(): Any {
        if (this.concurrentQueue.accessingThreadId != this.accessingThreadId) {
            throw ConcurrentModificationException()
        } else if (this.currentPosition < this.concurrentQueue.currentPosition) {
            val nodeObject = this.concurrentQueue.queuedObjectNodes[this.currentPosition].`object`
            this.currentPosition++
            return nodeObject
        } else {
            throw NoSuchElementException()
        }
    }

    override fun hasNext(): Boolean {
        return this.currentPosition < this.concurrentQueue.currentPosition
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }
}
