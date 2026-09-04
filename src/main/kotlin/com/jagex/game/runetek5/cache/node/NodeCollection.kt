package com.jagex.game.runetek5.cache.node

@Suppress("UNCHECKED_CAST")
class NodeCollection {

    @JvmField val tail: Node<*> = Node<Any>()
    @JvmField var current: Node<*>? = null

    init {
        (tail as Node<Any>).next = tail
        (tail as Node<Any>).previous = tail
    }

    fun addFirst(node: Node<*>) {
        if (node.previous != null) {
            node.unlink()
        }
        (node as Node<Any>).previous = tail.previous as Node<Any>
        (node as Node<Any>).next = tail as Node<Any>
        (node.previous as Node<Any>).next = node
        (node.next as Node<Any>).previous = node
    }

    fun popHead(): Node<*>? {
        val node = tail.next
        return if (node == tail) {
            null
        } else {
            node!!.unlink()
            node
        }
    }

    fun spliceInto(destination: NodeCollection, from: Node<*>?) {
        val last = tail.previous
        (tail as Node<Any>).previous = (from as Node<Any>).previous
        (from.previous as Node<Any>).next = tail
        if (from != tail) {
            (from as Node<Any>).previous = destination.tail.previous as Node<Any>
            (from.previous as Node<Any>).next = from
            (last as Node<Any>).next = destination.tail as Node<Any>
            (destination.tail as Node<Any>).previous = last
        }
    }

    fun transferAllTo(destination: NodeCollection) {
        if (tail.next != tail) {
            spliceInto(destination, tail.next)
        }
    }

    operator fun iterator(): Iterator<Node<*>> {
        return NodeIterator(this) as Iterator<Node<*>>
    }

    fun first(): Node<*>? {
        return getLast()
    }

    fun getLast(): Node<*>? {
        val node = tail.previous
        return if (node == tail) {
            current = null
            null
        } else {
            current = node!!.previous
            node
        }
    }

    fun next(): Node<*>? {
        val node = current
        return if (node == tail) {
            current = null
            null
        } else {
            current = node!!.next
            node
        }
    }

    fun getNext(): Node<*>? {
        val node = current
        return if (node == tail) {
            current = null
            null
        } else {
            current = node!!.previous
            node
        }
    }

    fun size(): Int {
        var n = 0
        var node = tail.next
        while (node != tail) {
            ++n
            node = node!!.next
        }
        return n
    }

    fun add(element: Node<*>): Boolean {
        addFirst(element)
        return true
    }

    fun toNodeArray(): Array<Node<*>?> {
        val nodes = arrayOfNulls<Node<*>>(size())
        var i = 0
        var node = tail.next
        while (node != tail) {
            nodes[i++] = node
            node = node!!.next
        }
        return nodes
    }

    fun clear() {
        remove_all()
    }

    fun isEmpty(): Boolean {
        return hasNext()
    }

    fun contains(element: Node<*>): Boolean {
        throw RuntimeException()
    }

    fun toArray(): Array<Any> {
        return toNodeArray() as Array<Any>
    }

    fun <T> toArray(array: Array<T>): Array<T> {
        var i = 0
        var node = tail.next
        while (node != tail) {
            (array as Array<Node<*>?>)[i++] = node
            node = node!!.next
        }
        return array
    }

    fun containsAll(elements: Collection<Node<*>>): Boolean {
        throw RuntimeException()
    }

    fun addAll(elements: Collection<Node<*>>): Boolean {
        throw RuntimeException()
    }

    fun removeAll(elements: Collection<Node<*>>): Boolean {
        throw RuntimeException()
    }

    fun retainAll(elements: Collection<Node<*>>): Boolean {
        throw RuntimeException()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    fun getFirstNode(): Node<*>? {
        val node = tail.next
        return if (node == tail) {
            current = null
            null
        } else {
            current = node!!.next
            node
        }
    }

    fun hasNext(): Boolean {
        return tail.next == tail
    }

    fun getFirst(): Node<*>? {
        return getFirstNode()
    }

    fun addLast(node: Node<*>) {
        if (node.previous != null) {
            node.unlink()
        }
        (node as Node<Any>).previous = tail as Node<Any>
        (node as Node<Any>).next = tail.next as Node<Any>
        (node.previous as Node<Any>).next = node
        (node.next as Node<Any>).previous = node
    }

    fun remove_all() {
        while (tail.next != tail) {
            tail.next!!.unlink()
        }
    }

    fun remove(element: Node<*>): Boolean {
        throw RuntimeException()
    }
}
