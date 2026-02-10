package com.jagex.game.runetek5.cache.node

@Suppress("UNCHECKED_CAST")
class NodeCollection {

    @JvmField val tail: Node<*> = Node<Any>()
    @JvmField var current: Node<*>? = null

    init {
        (tail as Node<Any>).next = tail
        (tail as Node<Any>).previous = tail
    }

    fun addFirst(node1: Node<*>) {
        if (node1.previous != null) {
            node1.unlink()
        }
        (node1 as Node<Any>).previous = tail.previous as Node<Any>
        (node1 as Node<Any>).next = tail as Node<Any>
        (node1.previous as Node<Any>).next = node1
        (node1.next as Node<Any>).previous = node1
    }

    fun popHead(): Node<*>? {
        val node2 = tail.next
        return if (node2 == tail) {
            null
        } else {
            node2!!.unlink()
            node2
        }
    }

    fun spliceInto(nodecollection1: NodeCollection, node2: Node<*>?) {
        val node4 = tail.previous
        (tail as Node<Any>).previous = (node2 as Node<Any>).previous
        (node2.previous as Node<Any>).next = tail
        if (node2 != tail) {
            (node2 as Node<Any>).previous = nodecollection1.tail.previous as Node<Any>
            (node2.previous as Node<Any>).next = node2
            (node4 as Node<Any>).next = nodecollection1.tail as Node<Any>
            (nodecollection1.tail as Node<Any>).previous = node4
        }
    }

    fun transferAllTo(nodecollection1: NodeCollection) {
        if (tail.next != tail) {
            spliceInto(nodecollection1, tail.next)
        }
    }

    operator fun iterator(): Iterator<Node<*>> {
        return NodeIterator(this) as Iterator<Node<*>>
    }

    fun first(): Node<*>? {
        return getLast()
    }

    fun getLast(): Node<*>? {
        val node3 = tail.previous
        return if (node3 == tail) {
            current = null
            null
        } else {
            current = node3!!.previous
            node3
        }
    }

    fun next(): Node<*>? {
        val node2 = current
        return if (node2 == tail) {
            current = null
            null
        } else {
            current = node2!!.next
            node2
        }
    }

    fun getNext(): Node<*>? {
        val node2 = current
        return if (node2 == tail) {
            current = null
            null
        } else {
            current = node2!!.previous
            node2
        }
    }

    fun size(): Int {
        var i_2 = 0
        var node3 = tail.next
        while (node3 != tail) {
            ++i_2
            node3 = node3!!.next
        }
        return i_2
    }

    fun add(element: Node<*>): Boolean {
        addFirst(element)
        return true
    }

    fun toNodeArray(): Array<Node<*>?> {
        val arr_2 = arrayOfNulls<Node<*>>(size())
        var i_3 = 0
        var node4 = tail.next
        while (node4 != tail) {
            arr_2[i_3++] = node4
            node4 = node4!!.next
        }
        return arr_2
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

    fun <T> toArray(arr_1: Array<T>): Array<T> {
        var i_2 = 0
        var node3 = tail.next
        while (node3 != tail) {
            (arr_1 as Array<Node<*>?>)[i_2++] = node3
            node3 = node3!!.next
        }
        return arr_1
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
        val node3 = tail.next
        return if (node3 == tail) {
            current = null
            null
        } else {
            current = node3!!.next
            node3
        }
    }

    fun hasNext(): Boolean {
        return tail.next == tail
    }

    fun getFirst(): Node<*>? {
        return getFirstNode()
    }

    fun addLast(node1: Node<*>) {
        if (node1.previous != null) {
            node1.unlink()
        }
        (node1 as Node<Any>).previous = tail as Node<Any>
        (node1 as Node<Any>).next = tail.next as Node<Any>
        (node1.previous as Node<Any>).next = node1
        (node1.next as Node<Any>).previous = node1
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
