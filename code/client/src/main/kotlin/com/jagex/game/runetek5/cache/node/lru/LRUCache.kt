package com.jagex.game.runetek5.cache.node.lru

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Queue

@Suppress("UNCHECKED_CAST")
class LRUCache {
    @JvmField
    val queue: Queue<AbstractLRUNode>

    @JvmField
    val capacity: Int

    @JvmField
    var capacityLeft: Int

    @JvmField
    val nodeMap: IterableNodeMap<AbstractLRUNode>

    constructor(big: Int, small: Int) {
        queue = Queue()
        capacity = big
        capacityLeft = big
        var size = 1
        while (size + size < big && size < small) {
            size += size
        }
        nodeMap = IterableNodeMap(size)
    }

    constructor(capacity: Int) : this(capacity, capacity)

    fun put(obj: Any?, uid: Long) {
        put(obj, uid, 1)
    }

    fun put(obj: Any?, uid: Long, capacityTaken: Int) {
        if (capacityTaken > capacity) {
            throw IllegalStateException()
        }
        removeByUid(uid)
        capacityLeft -= capacityTaken

        while (capacityLeft < 0) {
            val cacheableNode = queue.getFirst() as AbstractLRUNode
            remove(cacheableNode)
        }

        val replacingNode = LRUNode(obj, capacityTaken)
        nodeMap.put(replacingNode, uid)
        queue.insert(replacingNode)
        replacingNode.indexReference = 0L
    }

    fun clean(max: Int) {
        var node = queue.getNext() as? AbstractLRUNode
        while (node != null) {
            if (node.isReusable()) {
                if (node.getValue() == null) {
                    node.unlink()
                    node.unlinkSub()
                    capacityLeft += node.capacityRequirement
                }
            } else if (++node.indexReference > max) {
                val softReference = LRUSoftReference(node.getValue(), node.capacityRequirement)
                nodeMap.put(softReference as AbstractLRUNode, node.pointer)
                CacheableNode.rebuild(softReference, node)
                node.unlink()
                node.unlinkSub()
            }
            node = queue.getDirectNext() as? AbstractLRUNode
        }
    }

    fun reset() {
        queue.reset()
        nodeMap.clear()
        capacityLeft = capacity
    }

    fun getCapacity(): Int = capacity

    fun getCapacityLeft(): Int = capacityLeft

    fun count(): Int {
        var count = 0
        var node = queue.getNext() as? AbstractLRUNode
        while (node != null) {
            if (!node.isReusable()) {
                count++
            }
            node = queue.getDirectNext() as? AbstractLRUNode
        }
        return count
    }

    fun resize() {
        var cacheableNode = queue.getNext() as? AbstractLRUNode
        while (cacheableNode != null) {
            if (cacheableNode.isReusable()) {
                cacheableNode.unlink()
                cacheableNode.unlinkSub()
                capacityLeft += cacheableNode.capacityRequirement
            }
            cacheableNode = queue.getDirectNext() as? AbstractLRUNode
        }
    }

    fun remove(cacheableNode: AbstractLRUNode?) {
        if (cacheableNode != null) {
            cacheableNode.unlink()
            cacheableNode.unlinkSub()
            capacityLeft += cacheableNode.capacityRequirement
        }
    }

    operator fun get(uid: Long): Any? {
        val cachedNode = nodeMap[uid] as? AbstractLRUNode ?: return null
        val obj = cachedNode.getValue()

        if (obj == null) {
            cachedNode.unlink()
            cachedNode.unlinkSub()
            capacityLeft += cachedNode.capacityRequirement
            return null
        }

        if (cachedNode.isReusable()) {
            val newNode = LRUNode(obj, cachedNode.capacityRequirement)
            nodeMap.put(newNode as AbstractLRUNode, cachedNode.pointer)
            queue.insert(newNode)
            newNode.indexReference = 0L
            cachedNode.unlink()
            cachedNode.unlinkSub()
        } else {
            queue.insert(cachedNode)
            cachedNode.indexReference = 0L
        }

        return obj
    }

    fun getFirst(): Any? {
        var cachedNode = nodeMap.first() as? AbstractLRUNode
        while (cachedNode != null) {
            val obj = cachedNode.getValue()
            if (obj != null) {
                return obj
            }
            val lastNode = cachedNode
            cachedNode = nodeMap.next() as? AbstractLRUNode
            lastNode.unlink()
            lastNode.unlinkSub()
            capacityLeft += lastNode.capacityRequirement
        }
        return null
    }

    fun getNext(): Any? {
        var node = nodeMap.next() as? AbstractLRUNode
        while (node != null) {
            val obj = node.getValue()
            if (obj != null) {
                return obj
            }
            val lastNode = node
            node = nodeMap.next() as? AbstractLRUNode
            lastNode.unlink()
            lastNode.unlinkSub()
            capacityLeft += lastNode.capacityRequirement
        }
        return null
    }

    fun removeByUid(uid: Long) {
        val cacheableNode = nodeMap[uid] as? AbstractLRUNode
        remove(cacheableNode)
    }
}
