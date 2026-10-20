package com.jagex.game.runetek5.cache.node

class IterableNodeMap<V : Node<*>>(capacity: Int) : Iterable<V> {

    @JvmField var nodes: Array<Node<Any?>>
    @JvmField var currentPosition = 0
    @JvmField var iterationNode: Node<Any?>? = null
    @JvmField var retrievalKey: Long = 0
    @JvmField var retrievalNode: Node<Any?>? = null
    @JvmField var size: Int

    init {
        size = capacity
        @Suppress("UNCHECKED_CAST")
        nodes = Array(capacity) {
            val linkable = Node<Any?>()
            linkable.next = linkable
            linkable.previous = linkable
            linkable
        }
    }

    fun clear() {
        for (index in 0 until size) {
            val bucket = nodes[index]
            while (true) {
                val next = bucket.next!!
                if (bucket == next) {
                    break
                }
                next.unlink()
            }
        }
        retrievalNode = null
        iterationNode = null
    }

    fun first(): V? {
        currentPosition = 0
        return next()
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(key: Long): V? {
        retrievalKey = key
        val head = nodes[(key and (size - 1).toLong()).toInt()]
        retrievalNode = head.next
        while (retrievalNode != head) {
            if (key == retrievalNode!!.pointer) {
                val value = retrievalNode
                retrievalNode = retrievalNode!!.next
                return value as V
            }
            retrievalNode = retrievalNode!!.next
        }
        retrievalNode = null
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun iterator(): Iterator<V> {
        return HashTableIterator<V>(this) as Iterator<V>
    }

    @Suppress("UNCHECKED_CAST")
    operator fun next(): V? {
        if (currentPosition > 0 && nodes[currentPosition - 1] != iterationNode) {
            val value = iterationNode
            iterationNode = value!!.next
            return value as V
        }
        while (currentPosition < size) {
            val value = nodes[++currentPosition - 1].next
            if (value != nodes[currentPosition - 1]) {
                iterationNode = value!!.next
                return value as V
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun nextInBucket(): V? {
        if (retrievalNode == null) {
            return null
        }
        val linkable = nodes[(retrievalKey and (size - 1).toLong()).toInt()]
        while (linkable != retrievalNode) {
            if (retrievalNode!!.pointer == retrievalKey) {
                val value = retrievalNode
                retrievalNode = retrievalNode!!.next
                return value as V
            }
            retrievalNode = retrievalNode!!.next
        }
        retrievalNode = null
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun put(value: V, key: Long) {
        val node = value as Node<Any?>
        if (node.previous != null) {
            node.unlink()
        }
        val linkable = nodes[(key and (size - 1).toLong()).toInt()]
        node.previous = linkable.previous
        node.next = linkable
        node.previous!!.next = node
        node.next!!.previous = node
        node.pointer = key
    }

    fun size(): Int {
        var count = 0
        for (index in 0 until size) {
            val linkable = nodes[index]
            var next = linkable.next
            while (linkable != next) {
                count++
                next = next!!.next
            }
        }
        return count
    }

    @Suppress("UNCHECKED_CAST")
    fun values(values: Array<V>): Int {
        var count = 0
        for (index in 0 until size) {
            val head = nodes[index]
            var next = head.next
            while (head != next) {
                values[count++] = next as V
                next = (next as Node<Any?>).next
            }
        }
        return count
    }
}
