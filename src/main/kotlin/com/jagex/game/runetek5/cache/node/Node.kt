package com.jagex.game.runetek5.cache.node

/**
 * The most basic entry used by any data structure in the engine. By default it operates
 * as a circular doubly linked list and is capable of storing ids, packed bitsets, hashs, and more.
 * When extended, it is able to function as a multi-purpose entry in most common data structures such
 * as HashMaps, Deques, and Caches (specifically LRUCaches in RuneTek)
 *
 * @param T The type of data contained by the Node.
 *          This restoration greatly enhances the iterator and general type inference abilities.
 */
open class Node<T> {
    /**
     * Often used as an id, a bitset, a hash, and also serves dozens of other purposes.
     * Named 'pointer' because it serves to point/refer to many objects in various situations.
     */
    @JvmField
    var pointer: Long = 0L

    @JvmField
    var next: Node<T>? = null

    @JvmField
    var previous: Node<T>? = null

    /**
     * Detaches this Node from one LinkedList to be discarded or inserted elsewhere.
     */
    fun unlink() {
        if (previous != null) {
            previous!!.next = next
            next!!.previous = previous
            next = null
            previous = null
        }
    }

    /**
     * Checks whether or not this node is connected to other nodes and has formed a valid list.
     *
     * @return true if the previous node isn't null, otherwise false
     */
    fun linked(): Boolean {
        return previous != null
    }

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> replace(newNode: Node<T>, oldNode: Node<T>) {
            if (newNode.previous != null) {
                newNode.unlink()
            }
            newNode.previous = oldNode.previous
            newNode.next = oldNode
            (newNode.previous as Node<T>).next = newNode
            (newNode.next as Node<T>).previous = newNode
        }
    }
}
