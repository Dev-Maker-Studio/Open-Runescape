package com.jagex.game.runetek5.cache.node;

import java.util.Iterator;

public class HashTableIterator<T extends Node> implements Iterator<T> {

    public Node aNode5441;

    public final IterableNodeMap nodeMap;

    public int currentPosition;

    public Node aNode5440;

    public HashTableIterator(IterableNodeMap iterablenodemap1) {
        this.nodeMap = iterablenodemap1;
        method7512();
    }

    public Node first() {
        method7512();
        return next();
    }

    @Override
    public boolean hasNext() {
        if (this.nodeMap.nodes[this.currentPosition - 1] != this.aNode5440) {
            return true;
        } 
        while (this.currentPosition < this.nodeMap.size) {
            if (this.nodeMap.nodes[++this.currentPosition - 1].next != this.nodeMap.nodes[this.currentPosition - 1]) {
                this.aNode5440 = this.nodeMap.nodes[this.currentPosition - 1].next;
                return true;
            }
            this.aNode5440 = this.nodeMap.nodes[this.currentPosition - 1];
        }
        return false;
    }

    @Override
    public void remove() {
        if (this.aNode5441 == null) {
            throw new IllegalStateException();
        } 
        this.aNode5441.unlink();
        this.aNode5441 = null;
    }

    @Override
    public T next() {
        Node node1;
        if (this.nodeMap.nodes[this.currentPosition - 1] != this.aNode5440) {
            node1 = this.aNode5440;
            this.aNode5440 = node1.next;
            this.aNode5441 = node1;
            return (T) node1;
        } 
        while (this.currentPosition < this.nodeMap.size) {
            node1 = this.nodeMap.nodes[++this.currentPosition - 1].next;
            if (node1 != this.nodeMap.nodes[this.currentPosition - 1]) {
                this.aNode5440 = node1.next;
                this.aNode5441 = node1;
                return (T) node1;
            }
        }
        return null;
    }

    public void method7512() {
        this.aNode5440 = this.nodeMap.nodes[0].next;
        this.currentPosition = 1;
        this.aNode5441 = null;
    }
}
