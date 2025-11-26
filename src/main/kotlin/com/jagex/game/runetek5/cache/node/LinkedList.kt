package com.jagex.game.runetek5.cache.node

class LinkedList {

    @JvmField
    @Suppress("UNCHECKED_CAST")
    val head: Node<Any> = Node<Any>()

    @JvmField
    @Suppress("UNCHECKED_CAST")
    var current: Node<Any>? = null

    init {
        head.next = head
        head.previous = head
    }

    fun clear() {
        while (true) {
            val node = head.next
            if (node == head) {
                current = null
                return
            }
            node!!.unlink()
        }
    }

    fun getPrevious(): Node<Any>? {
        val node = current
        return if (node == head) {
            current = null
            null
        } else {
            current = node!!.next as Node<Any>?
            node
        }
    }

    fun popTail(): Node<Any>? {
        val node = head.next
        return if (node == head) {
            null
        } else {
            node!!.unlink()
            node as Node<Any>
        }
    }

    fun getTail(): Node<Any>? {
        val node = head.next
        return if (node == head) {
            current = null
            null
        } else {
            current = node!!.next as Node<Any>?
            node as Node<Any>
        }
    }

    fun isEmpty(): Boolean {
        return head.next == head
    }

    fun getNext(): Node<Any>? {
        val node = head.previous
        return if (node == head) {
            current = null
            null
        } else {
            current = node!!.previous as Node<Any>?
            node as Node<Any>
        }
    }

    fun insertHead(node: Node<*>?) {
        if (node == null) return
        if (node.previous != null) {
            node.unlink()
        }
        @Suppress("UNCHECKED_CAST")
        val n = node as Node<Any>
        n.previous = head
        n.next = head.next
        n.previous!!.next = n
        n.next!!.previous = n
    }

    fun insertTail(node: Node<*>?) {
        if (node == null) return
        if (node.previous != null) {
            node.unlink()
        }
        @Suppress("UNCHECKED_CAST")
        val n = node as Node<Any>
        n.previous = head.previous
        n.next = head
        n.previous!!.next = n
        n.next!!.previous = n
    }
}
