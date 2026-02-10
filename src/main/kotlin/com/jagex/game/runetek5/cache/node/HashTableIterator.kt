package com.jagex.game.runetek5.cache.node

/**
 * Iterator for IterableNodeMap hashtable.
 * Type parameter T is not constrained to match Java's raw type bound flexibility.
 */
class HashTableIterator<T>(
    @JvmField val nodeMap: IterableNodeMap<*>
) : MutableIterator<T?> {

    @JvmField var lastReturnedNode: Node<*>? = null
    @JvmField var currentPosition: Int = 0
    @JvmField var nextNode: Node<*>? = null

    init {
        reset()
    }

    /**
     * Resets to the first node and returns the next one.
     *
     * @return The first node
     */
    fun first(): T? {
        reset()
        return next()
    }

    override fun hasNext(): Boolean {
        if (nodeMap.nodes[currentPosition - 1] != nextNode) {
            return true
        }
        while (currentPosition < nodeMap.size) {
            if (nodeMap.nodes[++currentPosition - 1].next != nodeMap.nodes[currentPosition - 1]) {
                nextNode = nodeMap.nodes[currentPosition - 1].next
                return true
            }
            nextNode = nodeMap.nodes[currentPosition - 1]
        }
        return false
    }

    override fun remove() {
        if (lastReturnedNode == null) {
            throw IllegalStateException()
        }
        lastReturnedNode!!.unlink()
        lastReturnedNode = null
    }

    override fun next(): T? {
        val node: Node<*>
        if (nodeMap.nodes[currentPosition - 1] != nextNode) {
            node = nextNode!!
            nextNode = node.next
            lastReturnedNode = node
            @Suppress("UNCHECKED_CAST")
            return node as T
        }
        while (currentPosition < nodeMap.size) {
            val candidateNode = nodeMap.nodes[++currentPosition - 1].next
            if (candidateNode != nodeMap.nodes[currentPosition - 1]) {
                nextNode = candidateNode!!.next
                lastReturnedNode = candidateNode
                @Suppress("UNCHECKED_CAST")
                return candidateNode as T
            }
        }
        return null
    }

    /**
     * Resets the iterator to the beginning.
     */
    fun reset() {
        nextNode = nodeMap.nodes[0].next
        currentPosition = 1
        lastReturnedNode = null
    }
}
