package com.jagex.game.runetek5.entity

import java.util.NoSuchElementException

class EntityNodeList<N : EntityNode> : Iterable<N> {

    @JvmField
    val head: EntityNode = EntityNode()

    @JvmField
    var pointer: EntityNode? = null

    init {
        head.next = head
        head.previous = head
    }

    fun getPrevious(node: N) {
        if (node.previous != null) {
            node.unlinkNode()
        }
        node.previous = head
        node.next = head.next
        node.previous!!.next = node
        node.next!!.previous = node
    }

    fun get(): N? {
        val node = head.next
        if (node == head) {
            return null
        }
        node!!.unlinkNode()
        @Suppress("UNCHECKED_CAST")
        return node as N
    }

    fun offer(node: N) {
        if (node.previous != null) {
            node.unlinkNode()
        }
        node.previous = head.previous
        node.next = head
        node.previous!!.next = node
        node.next!!.previous = node
    }

    fun getNext(): N? {
        val node = pointer
        if (node == null || node == head) {
            pointer = null
            return null
        }
        pointer = node.next
        @Suppress("UNCHECKED_CAST")
        return node as N
    }

    fun unlink() {
        while (true) {
            val node = head.next
            if (node == head) {
                pointer = null
                return
            }
            node!!.unlinkNode()
        }
    }

    fun getFirst(): N? {
        val node = head.next
        if (node == head) {
            pointer = null
            return null
        }
        pointer = node!!.next
        @Suppress("UNCHECKED_CAST")
        return node as N
    }

    fun isEmpty(): Boolean {
        return head.next == head
    }

    override fun iterator(): Iterator<N> = EntityNodeIterator()

    private inner class EntityNodeIterator : Iterator<N> {
        private var current: EntityNode? = head.next

        override fun hasNext(): Boolean {
            return current != head
        }

        override fun next(): N {
            if (!hasNext()) throw NoSuchElementException()
            val node = current!!
            current = node.next
            @Suppress("UNCHECKED_CAST")
            return node as N
        }
    }
}

