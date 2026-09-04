package com.jagex.game.runetek5.cache.node

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

/**
 * A queue-based cache for cacheable nodes with automatic eviction when capacity is reached.
 */
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

    /**
     * Retrieves a node by key, moving it to the front of the queue if found.
     *
     * @param key The key to look up
     * @return The cached node, or null if not found
     */
    fun get(key: Long): CacheableNode? {
        val node = nodeMap.get(key) as CacheableNode?
        if (node != null) {
            queue.insert(node)
        }
        return node
    }

    /**
     * Adds a node to the cache, evicting the least recently used node if at capacity.
     *
     * @param value The node to cache
     * @param key The key to associate with the node
     */
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

    /**
     * Resets the cache, clearing all entries and restoring initial capacity.
     */
    fun reset() {
        queue.reset()
        nodeMap.clear()
        head = CacheableNode()
        capacityLeft = initialCapacity
    }
}
