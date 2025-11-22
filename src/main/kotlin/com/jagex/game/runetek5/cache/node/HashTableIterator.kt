package com.jagex.game.runetek5.cache.node

/**
 * Iterator for IterableNodeMap hashtable.
 * Type parameter T is not constrained to match Java's raw type bound flexibility.
 */
class HashTableIterator<T>(
    @JvmField val nodeMap: IterableNodeMap<*>
) : MutableIterator<T?> {

    @JvmField var aNode5441: Node<*>? = null
    @JvmField var currentPosition: Int = 0
    @JvmField var aNode5440: Node<*>? = null

    init {
        method7512()
    }

    /**
     * Resets to the first node and returns the next one.
     *
     * @return The first node
     */
    fun first(): T? {
        method7512()
        return next()
    }

    override fun hasNext(): Boolean {
        if (nodeMap.nodes[currentPosition - 1] != aNode5440) {
            return true
        }
        while (currentPosition < nodeMap.size) {
            if (nodeMap.nodes[++currentPosition - 1].next != nodeMap.nodes[currentPosition - 1]) {
                aNode5440 = nodeMap.nodes[currentPosition - 1].next
                return true
            }
            aNode5440 = nodeMap.nodes[currentPosition - 1]
        }
        return false
    }

    override fun remove() {
        if (aNode5441 == null) {
            throw IllegalStateException()
        }
        aNode5441!!.unlink()
        aNode5441 = null
    }

    override fun next(): T? {
        val node: Node<*>
        if (nodeMap.nodes[currentPosition - 1] != aNode5440) {
            node = aNode5440!!
            aNode5440 = node.next
            aNode5441 = node
            @Suppress("UNCHECKED_CAST")
            return node as T
        }
        while (currentPosition < nodeMap.size) {
            val candidateNode = nodeMap.nodes[++currentPosition - 1].next
            if (candidateNode != nodeMap.nodes[currentPosition - 1]) {
                aNode5440 = candidateNode!!.next
                aNode5441 = candidateNode
                @Suppress("UNCHECKED_CAST")
                return candidateNode as T
            }
        }
        return null
    }

    /**
     * Resets the iterator to the beginning.
     */
    fun method7512() {
        aNode5440 = nodeMap.nodes[0].next
        currentPosition = 1
        aNode5441 = null
    }
}
