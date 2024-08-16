package com.jagex.game.runetek5.cache.node;

import java.util.Iterator;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

public class Queue<T> implements Iterable<T> {
	public final CacheableNode current = new CacheableNode();
    public CacheableNode next;

    public Queue() {
        this.current.next = this.current;
        this.current.current = this.current;
    }

    public void reset() {
        while (this.current.next != this.current) {
            this.current.next.unlinkSub();
        }
    }

    public void insert(CacheableNode next) {
        if (next.current != null) {
            next.unlinkSub();
        }
        next.current = this.current.current;
        next.next = this.current;
        next.current.next = next;
        next.next.current = next;
    }

    public CacheableNode getFirst() {
        CacheableNode node = this.current.next;
        if (node == this.current) {
            return null;
        } else {
            node.unlinkSub();
            return node;
        }
    }

    public int size() {
        int size = 0;
        for (CacheableNode cacheablenode3 = this.current.next; cacheablenode3 != this.current; cacheablenode3 = cacheablenode3.next) {
            ++size;
        }
        return size;
    }

    public CacheableNode getNext() {
        return getCurrentNext();
    }

    public CacheableNode getCurrentNext() {
        CacheableNode node;
        node = this.current.next;
        if (node == this.current) {
            this.next = null;
            return null;
        } else {
            this.next = node.next;
            return node;
        }
    }

    public CacheableNode getDirectNext() {
        CacheableNode node = this.next;
        if (node == this.current) {
            this.next = null;
            return null;
        } else {
            this.next = node.next;
            return node;
        }
    }

    @Override
    public Iterator iterator() {
        return new QueueIterator(this);
    }
}
