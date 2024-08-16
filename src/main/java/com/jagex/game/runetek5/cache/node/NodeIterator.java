package com.jagex.game.runetek5.cache.node;

import java.util.Iterator;

public class NodeIterator implements Iterator {

    public Node aNode5536;

    public Node aNode5537;

    public NodeCollection collection;

    public NodeIterator(NodeCollection nodecollection1) {
        this.collection = nodecollection1;
        this.aNode5536 = this.collection.tail.next;
        this.aNode5537 = null;
    }

    public Node method7683() {
        method7697();
        return (Node) next();
    }

    @Override
    public Object next() {
        Node node1 = this.aNode5536;
        if (node1 == this.collection.tail) {
            node1 = null;
            this.aNode5536 = null;
        } else {
            this.aNode5536 = node1.next;
        }
        this.aNode5537 = node1;
        return node1;
    }

    @Override
    public boolean hasNext() {
        return this.collection.tail != this.aNode5536;
    }

    public void method7684(NodeCollection nodecollection1) {
        this.collection = nodecollection1;
        this.aNode5536 = this.collection.tail.next;
        this.aNode5537 = null;
    }

    @Override
    public void remove() {
        if (this.aNode5537 == null) {
            throw new IllegalStateException();
        } else {
            this.aNode5537.unlink();
            this.aNode5537 = null;
        }
    }

    public void method7697() {
        this.aNode5536 = this.collection.tail.next;
        this.aNode5537 = null;
    }
}
