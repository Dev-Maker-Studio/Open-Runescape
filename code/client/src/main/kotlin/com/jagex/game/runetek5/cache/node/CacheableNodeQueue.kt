package com.jagex.game.runetek5.cache.node

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

class CacheableNodeQueue(size: Int) {

    @JvmField var capacityLeft: Int
    @JvmField var head = CacheableNode()
    @JvmField val queue = Queue<CacheableNode>()
    @JvmField val initialCapacity: Int = size
    @JvmField val nodeMap: IterableNodeMap<CacheableNode>

    init {
        capacityLeft = size
        var capacity = 1
        while (capacity + capacity < size) {
            capacity += capacity
        }
        nodeMap = IterableNodeMap(capacity)
    }

    fun get(key: Long): CacheableNode? {
        val node = nodeMap.get(key) as CacheableNode?
        if (node != null) {
            queue.insert(node)
        }
        return node
    }

    fun put(value: CacheableNode, key: Long) {
        if (capacityLeft == 0) {
            var first = queue.getFirst()!!
            first.unlink()
            first.unlinkSub()
            if (first == head) {
                first = queue.getFirst()!!
                first.unlink()
                first.unlinkSub()
            }
        } else {
            --capacityLeft
        }
        nodeMap.put(value, key)
        queue.insert(value)
    }

    fun reset() {
        queue.reset()
        nodeMap.clear()
        head = CacheableNode()
        capacityLeft = initialCapacity
    }
}
