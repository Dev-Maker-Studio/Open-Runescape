package com.jagex.game.runetek5.cache.node;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ConcurrentQueueIterator implements Iterator {

    public int currentPosition;

    public final int accessingThreadId;

    public ConcurrentQueue concurrentQueue;

    public ConcurrentQueueIterator(ConcurrentQueue abstractqueueSub11) {
        this.accessingThreadId = this.concurrentQueue.accessingThreadId;
        this.concurrentQueue = abstractqueueSub11;
    }

    @Override
    public Object next() {
        if (this.concurrentQueue.accessingThreadId != this.accessingThreadId) {
            throw new ConcurrentModificationException();
        } else if (this.currentPosition < this.concurrentQueue.currentPosition) {
            Object nodeObject = this.concurrentQueue.queuedObjectNodes[this.currentPosition].object;
            this.currentPosition++;
            return nodeObject;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public boolean hasNext() {
        return this.currentPosition < this.concurrentQueue.currentPosition;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
