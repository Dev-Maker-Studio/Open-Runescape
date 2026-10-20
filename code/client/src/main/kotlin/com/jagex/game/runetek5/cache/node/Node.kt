package com.jagex.game.runetek5.cache.node

open class Node<T> {
    @JvmField
    var pointer: Long = 0L

    @JvmField
    var next: Node<T>? = null

    @JvmField
    var previous: Node<T>? = null

    fun unlink() {
        if (previous != null) {
            previous!!.next = next
            next!!.previous = previous
            next = null
            previous = null
        }
    }

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
