package com.jagex.game.runetek5.cache.node

/**
 * Iterator for traversing nodes in a NodeCollection.
 * Implements Java's Iterator interface for compatibility.
 */
class NodeIterator(collection: NodeCollection) : MutableIterator<Node<*>?> {

    @JvmField var collection: NodeCollection = collection

    /** Current node being pointed to */
    @JvmField var current: Node<*>? = collection.tail.next

    /** Last node returned by next(), used for remove() */
    @JvmField var lastReturned: Node<*>? = null

    /**
     * Gets the next node and resets to the beginning
     * @return The next node in the collection
     */
    @JvmName("method7683")
    fun nextAndReset(): Node<*>? {
        reset()
        return next()
    }

    override fun next(): Node<*>? {
        var node = current
        if (node == collection.tail) {
            node = null
            current = null
        } else {
            current = node?.next
        }
        lastReturned = node
        return node
    }

    override fun hasNext(): Boolean {
        return collection.tail != current
    }

    /**
     * Reinitializes the iterator with a new collection
     * @param collection The new collection to iterate
     */
    @JvmName("method7684")
    fun reinitialize(collection: NodeCollection) {
        this.collection = collection
        this.current = collection.tail.next
        this.lastReturned = null
    }

    override fun remove() {
        val node = lastReturned ?: throw IllegalStateException()
        node.unlink()
        lastReturned = null
    }

    /**
     * Resets the iterator to the beginning of the collection
     */
    fun reset() {
        current = collection.tail.next
        lastReturned = null
    }
}
