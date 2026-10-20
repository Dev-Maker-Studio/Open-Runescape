package com.jagex.game.runetek5.cache.node

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
