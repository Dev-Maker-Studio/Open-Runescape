package com.jagex.game.runetek5.entity

import java.util.NoSuchElementException

/**
 * Needs better naming...
 *
 */
class EntityNodeList : Iterable<EntityNode> {

    @JvmField val head = EntityNode()
    @JvmField var pointer: EntityNode? = null

    init {
        head.next = head
        head.previous = head
    }

    fun getPrevious(entityNode: EntityNode) {
        if (entityNode.previous != null) {
            entityNode.unlinkNode()
        }
        entityNode.previous = head
        entityNode.next = head.next
        entityNode.previous!!.next = entityNode
        entityNode.next!!.previous = entityNode
    }

    fun get(): EntityNode? {
        val entityNode = head.next
        if (entityNode == head) {
            return null
        }
        entityNode!!.unlinkNode()
        return entityNode
    }

    fun offer(entityNode: EntityNode) {
        if (entityNode.previous != null) {
            entityNode.unlinkNode()
        }
        entityNode.previous = head.previous
        entityNode.next = head
        entityNode.previous!!.next = entityNode
        entityNode.next!!.previous = entityNode
    }

    fun getNext(): EntityNode? {
        val entity = pointer
        if (entity == head) {
            pointer = null
            return null
        }
        pointer = entity!!.next
        return entity
    }

    fun unlink() {
        while (true) {
            val entity = head.next
            if (entity == head) {
                pointer = null
                return
            }
            entity!!.unlinkNode()
        }
    }

    fun getFirst(): EntityNode? {
        val entity = head.next
        if (entity == head) {
            pointer = null
            return null
        }
        pointer = entity!!.next
        return entity
    }

    fun isEmpty(): Boolean {
        return head.next == head
    }

    override fun iterator(): Iterator<EntityNode> {
        return EntityNodeIterator()
    }

    private inner class EntityNodeIterator : Iterator<EntityNode> {
        private var current = head.next

        override fun hasNext(): Boolean {
            return current != head
        }

        override fun next(): EntityNode {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            val node = current!!
            current = current!!.next
            return node
        }
    }
}
