package com.jagex.game.runetek5.cache.node

class NodeIterator(collection: NodeCollection) : MutableIterator<Node<*>?> {

    @JvmField var collection: NodeCollection = collection

    @JvmField var current: Node<*>? = collection.tail.next

    @JvmField var lastReturned: Node<*>? = null

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

    fun reset() {
        current = collection.tail.next
        lastReturned = null
    }
}
