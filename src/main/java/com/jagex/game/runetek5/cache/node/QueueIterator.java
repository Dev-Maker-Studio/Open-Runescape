package com.jagex.game.runetek5.cache.node;

import java.util.Iterator;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

public class QueueIterator implements Iterator {

    public static boolean LOYALTY_ENABLED;
    public CacheableNode aCacheableNode5582;
    public final Queue aClass4775584;
    public CacheableNode aCacheableNode5583;

    public QueueIterator(Queue class4771) {
        this.aClass4775584 = class4771;
        this.aCacheableNode5583 = this.aClass4775584.current.next;
        this.aCacheableNode5582 = null;
    }

    public CacheableNode method7790() {
        method7800();
        return (CacheableNode) next();
    }

    @Override
    public Object next() {
        CacheableNode cacheablenode1 = this.aCacheableNode5583;
        if (cacheablenode1 == this.aClass4775584.current) {
            cacheablenode1 = null;
            this.aCacheableNode5583 = null;
        } else {
            this.aCacheableNode5583 = cacheablenode1.next;
        }

        this.aCacheableNode5582 = cacheablenode1;
        return cacheablenode1;
    }

    @Override
    public boolean hasNext() {
        return this.aClass4775584.current != this.aCacheableNode5583;
    }

    @Override
    public void remove() {
        if (this.aCacheableNode5582 == null) {
            throw new IllegalStateException();
        } else {
            this.aCacheableNode5582.unlinkSub();
            this.aCacheableNode5582 = null;
        }
    }

    public void method7800() {
        this.aCacheableNode5583 = this.aClass4775584.current.next;
        this.aCacheableNode5582 = null;
    }

}
