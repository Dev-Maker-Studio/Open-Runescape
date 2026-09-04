package com.jagex.game.runetek5.cache.node.lru

import com.jagex.game.runetek5.cache.node.Node

open class CacheableNode : Node<CacheableNode>() {
    @JvmField var indexReference: Long = 0
    @JvmField var nextCache: CacheableNode? = null
    @JvmField var current: CacheableNode? = null

    fun unlinkSub() {
        if (current != null) {
            current!!.nextCache = nextCache
            nextCache!!.current = current
            nextCache = null
            current = null
        }
    }

    companion object {
        @JvmStatic
        fun rebuild(currentNode: CacheableNode, nextNode: CacheableNode) {
            if (currentNode.current != null) {
                currentNode.unlinkSub()
            }
            currentNode.current = nextNode
            currentNode.nextCache = nextNode.nextCache
            currentNode.current!!.nextCache = currentNode
            currentNode.nextCache!!.current = currentNode
        }
    }
}
