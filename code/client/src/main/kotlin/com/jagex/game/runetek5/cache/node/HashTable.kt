package com.jagex.game.runetek5.cache.node

/**
 * Hashtable implementation using chained buckets.
 */
class HashTable(
    @JvmField val size: Int
) {

    @JvmField var retrievalKey: Long = 0L
    @JvmField var retrievableNode: Node<*>? = null
    @JvmField val buckets: Array<Node<*>>

    init {
        buckets = Array(size) {
            val node = Node<Any?>()
            node.next = node
            node.previous = node
            node
        }
    }

    /**
     * Gets a node by key.
     *
     * @param key The key to search for
     * @return The node with this key, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun get(key: Long): Node<*>? {
        retrievalKey = key
        val head = buckets[(key and (size - 1).toLong()).toInt()]
        retrievableNode = head.next as Node<*>?
        while (head != retrievableNode) {
            if (key == retrievableNode!!.pointer) {
                val value = retrievableNode
                retrievableNode = retrievableNode!!.next as Node<*>?
                return value
            }
            retrievableNode = retrievableNode!!.next as Node<*>?
        }
        retrievableNode = null
        return null
    }

    /**
     * Gets all values into an array.
     *
     * @param values The output array
     * @return The number of values retrieved
     */
    @Suppress("UNCHECKED_CAST")
    fun values(values: Array<Node<*>?>): Int {
        var count = 0
        for (i in 0 until size) {
            val head = buckets[i]
            var next = head.next as Node<*>?
            while (head != next) {
                values[count++] = next
                next = next!!.next as Node<*>?
            }
        }
        return count
    }

    /**
     * Puts a node with the given key.
     *
     * @param value The node to put
     * @param key The key to associate
     */
    @Suppress("UNCHECKED_CAST")
    fun put(value: Node<*>, key: Long) {
        if (value.previous != null) {
            value.unlink()
        }
        val node = buckets[(key and (size - 1).toLong()).toInt()]
        (value as Node<Any?>).previous = node.previous as Node<Any?>?
        value.next = node as Node<Any?>?
        value.previous!!.next = value
        value.next!!.previous = value
        value.pointer = key
    }

    /**
     * Counts the number of nodes in the table.
     *
     * @return The node count
     */
    @Suppress("UNCHECKED_CAST")
    fun nodeCount(): Int {
        var count = 0
        for (i in 0 until size) {
            val head = buckets[i]
            var node = head.next as Node<*>?
            while (head != node) {
                count++
                node = node!!.next as Node<*>?
            }
        }
        return count
    }

    /**
     * Gets the next node in the current bucket with the same key.
     *
     * @return The next node, or null if none
     */
    @Suppress("UNCHECKED_CAST")
    fun nextInBucket(): Node<*>? {
        if (retrievableNode != null) {
            val head = buckets[(retrievalKey and (size - 1).toLong()).toInt()]
            while (head != retrievableNode) {
                if (retrievableNode!!.pointer == retrievalKey) {
                    val value = retrievableNode
                    retrievableNode = retrievableNode!!.next as Node<*>?
                    return value
                }
                retrievableNode = retrievableNode!!.next as Node<*>?
            }
            retrievableNode = null
        }
        return null
    }
}
