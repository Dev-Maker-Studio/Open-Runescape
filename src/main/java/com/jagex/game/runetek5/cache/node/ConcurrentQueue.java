package com.jagex.game.runetek5.cache.node;

import java.util.*;

public class ConcurrentQueue extends AbstractQueue {

    public QueuedObjectNode[] queuedObjectNodes;
    public int accessingThreadId;
    public final Map<Object, QueuedObjectNode> referenceMap;
    public final Comparator queuedObjectComporator;
    public int currentPosition;

    public ConcurrentQueue(int capacity) {
        this.accessingThreadId = 0;
        this.queuedObjectNodes = new QueuedObjectNode[capacity];
        this.referenceMap = new HashMap<Object, QueuedObjectNode>();
        this.queuedObjectComporator = null;
    }

    /**
     * Doubles the size of the cache
     */
    public void expand() {
        int newSize = (this.queuedObjectNodes.length << 1) + 1;
        this.queuedObjectNodes = Arrays.copyOf(this.queuedObjectNodes, newSize);
    }

    @Override
    public int size() {
        return this.currentPosition;
    }

    @Override
    public Object peek() {
        return this.currentPosition == 0 ? null : this.queuedObjectNodes[0].object;
    }

    @Override
    public boolean remove(Object object) {
        QueuedObjectNode queuedObjectNode = this.referenceMap.remove(object);
        if (queuedObjectNode == null) {
            return false;
        } 
        this.accessingThreadId++;
        this.currentPosition--;
        if (queuedObjectNode.slot == this.currentPosition) {
            this.queuedObjectNodes[this.currentPosition] = null;
        } else {
            QueuedObjectNode nodeAtCurrentPosition = this.queuedObjectNodes[this.currentPosition];
            this.queuedObjectNodes[this.currentPosition] = null;
            this.queuedObjectNodes[queuedObjectNode.slot] = nodeAtCurrentPosition;
            this.queuedObjectNodes[queuedObjectNode.slot].slot = queuedObjectNode.slot;
            shiftForward(queuedObjectNode.slot);
            if (nodeAtCurrentPosition == this.queuedObjectNodes[queuedObjectNode.slot]) {
                shiftBackwards(queuedObjectNode.slot);
            }

        }
        return true;
    }

    public void shiftBackwards(int slot) {
        QueuedObjectNode queuedNode;
        int nextSlot;
        for (queuedNode = this.queuedObjectNodes[slot]; slot > 0; slot = nextSlot) {
            nextSlot = slot - 1 >>> 1;
            QueuedObjectNode nextQueuedNode = this.queuedObjectNodes[nextSlot];
            if (this.queuedObjectComporator != null) {
                if (this.queuedObjectComporator.compare(queuedNode.object, nextQueuedNode.object) >= 0) {
                    break;
                }
            } else if (((Comparable) queuedNode.object).compareTo(nextQueuedNode.object) >= 0) {
                break;
            }

            this.queuedObjectNodes[slot] = nextQueuedNode;
            this.queuedObjectNodes[slot].slot = slot;
        }

        this.queuedObjectNodes[slot] = queuedNode;
        this.queuedObjectNodes[slot].slot = slot;
    }

    public void shiftForward(int slot) {
        QueuedObjectNode nodeAtCurrentSlot = this.queuedObjectNodes[slot];

        int nextSlot;
        for (int i_5 = this.currentPosition >>> 1; slot < i_5; slot = nextSlot) {
            int forward1 = (slot << 1) + 1;
            QueuedObjectNode queuedNodeForward1 = this.queuedObjectNodes[forward1];
            int forward2 = (slot << 1) + 2;
            QueuedObjectNode queuedNodeForward2 = this.queuedObjectNodes[forward2];
            if (this.queuedObjectComporator != null) {
                if (forward2 < this.currentPosition && this.queuedObjectComporator.compare(queuedNodeForward1.object, queuedNodeForward2.object) > 0) {
                    nextSlot = forward2;
                } else {
                    nextSlot = forward1;
                }
            } else if (forward2 < this.currentPosition && ((Comparable) queuedNodeForward1.object).compareTo(queuedNodeForward2.object) > 0) {
                nextSlot = forward2;
            } else {
                nextSlot = forward1;
            }

            if (this.queuedObjectComporator != null) {
                if (this.queuedObjectComporator.compare(nodeAtCurrentSlot.object, this.queuedObjectNodes[nextSlot].object) <= 0) {
                    break;
                }
            } else if (((Comparable) nodeAtCurrentSlot.object).compareTo(this.queuedObjectNodes[nextSlot].object) <= 0) {
                break;
            }

            this.queuedObjectNodes[slot] = this.queuedObjectNodes[nextSlot];
            this.queuedObjectNodes[slot].slot = slot;
        }

        this.queuedObjectNodes[slot] = nodeAtCurrentSlot;
        this.queuedObjectNodes[slot].slot = slot;
    }

    @Override
    public boolean contains(Object object1) {
        return this.referenceMap.containsKey(object1);
    }

    @Override
    public Object[] toArray() {
        Object[] objects = super.toArray();
        if (this.queuedObjectComporator != null) {
            Arrays.sort(objects, this.queuedObjectComporator);
        } else {
            Arrays.sort(objects);
        }

        return objects;
    }

    @Override
    public Iterator iterator() {
        return new ConcurrentQueueIterator(this);
    }

    @Override
    public boolean offer(Object object) {
        if (this.referenceMap.containsKey(object)) {
            throw new IllegalArgumentException("");
        } 
        this.accessingThreadId++;
        int position = this.currentPosition;
        if (position >= this.queuedObjectNodes.length) {
            expand();
        }

        this.currentPosition++;
        if (position == 0) {
            this.queuedObjectNodes[0] = new QueuedObjectNode(object, 0);
            this.referenceMap.put(object, this.queuedObjectNodes[0]);
        } else {
            this.queuedObjectNodes[position] = new QueuedObjectNode(object, position);
            this.referenceMap.put(object, this.queuedObjectNodes[position]);
            shiftBackwards(position);
        }

        return true;
    }

    @Override
    public Object poll() {
        if (this.currentPosition == 0) {
            return null;
        } 
        this.accessingThreadId++;
        Object object1 = this.queuedObjectNodes[0].object;
        this.referenceMap.remove(object1);
        this.currentPosition--;
        if (this.currentPosition == 0) {
            this.queuedObjectNodes[this.currentPosition] = null;
        } else {
            this.queuedObjectNodes[0] = this.queuedObjectNodes[this.currentPosition];
            this.queuedObjectNodes[0].slot = 0;
            this.queuedObjectNodes[this.currentPosition] = null;
            shiftForward(0);
        }
        return object1;
    }

}
