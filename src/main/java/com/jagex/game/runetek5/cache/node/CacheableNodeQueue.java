package com.jagex.game.runetek5.cache.node;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

public class CacheableNodeQueue {

    public int capacityLeft;

    public CacheableNode head = new CacheableNode();

    public final Queue queue = new Queue();

    public final int initialCapacity;

    public final IterableNodeMap nodeMap;

    public CacheableNodeQueue(int size) {
        this.initialCapacity = size;
        this.capacityLeft = size;
        int capacity;
        for (capacity = 1; capacity + capacity < size; capacity += capacity) {
        }
        this.nodeMap = new IterableNodeMap(capacity);
    }

    public CacheableNode get(long key) {
        CacheableNode node = (CacheableNode) this.nodeMap.get(key);
        if (node != null) {
            this.queue.insert(node);
        }
        return node;
    }

    public void put(CacheableNode value, long key) {
        if (this.capacityLeft == 0) {
            CacheableNode first = this.queue.getFirst();
            first.unlink();
            first.unlinkSub();
            if (first == this.head) {
                first = this.queue.getFirst();
                first.unlink();
                first.unlinkSub();
            }
        } else {
            --this.capacityLeft;
        }
        this.nodeMap.put(value, key);
        this.queue.insert(value);
    }

    public void reset() {
        this.queue.reset();
        this.nodeMap.clear();
        this.head = new CacheableNode();
        this.capacityLeft = this.initialCapacity;
    }
}
