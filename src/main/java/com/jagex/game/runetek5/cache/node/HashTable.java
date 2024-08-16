package com.jagex.game.runetek5.cache.node;

public class HashTable {

    public long retrievalKey;
    public Node retrievableNode;
    public final int size;
    public final Node[] buckets;

    public HashTable(int size) {
        this.size = size;
        this.buckets = new Node[size];
        for (int i = 0; i < size; i++) {
            Node node = this.buckets[i] = new Node();
            node.next = node;
            node.previous = node;
        }
    }

    public Node get(long key) {
        this.retrievalKey = key;
        Node head = this.buckets[(int) (key & (this.size - 1))];
        for (this.retrievableNode = head.next; head != this.retrievableNode; this.retrievableNode = this.retrievableNode.next) {
            if (key == this.retrievableNode.pointer) {
                Node value = this.retrievableNode;
                this.retrievableNode = this.retrievableNode.next;
                return value;
            }
        }
        this.retrievableNode = null;
        return null;
    }

    public int values(Node[] values) {
        int count = 0;
        for (int i = 0; i < this.size; i++) {
            Node head = this.buckets[i];
            for (Node next = head.next; head != next; next = next.next) {
                values[count++] = next;
            }
        }
        return count;
    }

    public void put(Node value, long key) {
        if (value.previous != null) {
            value.unlink();
        }
        Node node = this.buckets[(int) (key & (this.size - 1))];
        value.previous = node.previous;
        value.next = node;
        value.previous.next = value;
        value.next.previous = value;
        value.pointer = key;
    }

    public int nodeCount() {
        int i_2 = 0;
        for (int i_3 = 0; i_3 < this.size; i_3++) {
            Node node4 = this.buckets[i_3];
            for (Node node5 = node4.next; node4 != node5; node5 = node5.next) {
                ++i_2;
            }
        }
        return i_2;
    }

    public Node nextInBucket() {
        if (this.retrievableNode != null) {
            for (Node node = this.buckets[(int) (this.retrievalKey & (this.size - 1))]; node != this.retrievableNode; this.retrievableNode = this.retrievableNode.next) {
                if (this.retrievableNode.pointer == this.retrievalKey) {
                    Node value = this.retrievableNode;
                    this.retrievableNode = this.retrievableNode.next;
                    return value;
                }
            }
            this.retrievableNode = null;
        }
        return null;
    }
}
