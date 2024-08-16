package com.jagex.game.runetek5.cache.node;

import com.jagex.game.runetek5.network.packet.PacketEncoder;

public class LinkedList {

    public final Node head = new Node();
    public Node current;

    public LinkedList() {
        this.head.next = this.head;
        this.head.previous = this.head;
    }

    public void clear() {
        while (true) {
            Node node1 = this.head.next;
            if (node1 == this.head) {
                this.current = null;
                return;
            }
            node1.unlink();
        }
    }

    public Node getPrevious() {
        Node node1 = this.current;
        if (node1 == this.head) {
            this.current = null;
            return null;
        } else {
            this.current = node1.next;
            return node1;
        }
    }

    public Node popTail() {
        Node node1 = this.head.next;
        if (node1 == this.head) {
            return null;
        } else {
            node1.unlink();
            return node1;
        }
    }

    public Node getTail() {
        Node node1 = this.head.next;
        if (node1 == this.head) {
            this.current = null;
            return null;
        } else {
            this.current = node1.next;
            return node1;
        }
    }

    public boolean isEmpty() {
        return this.head.next == this.head;
    }

    public Node getNext() {
        Node node1 = this.head.previous;
        if (node1 == this.head) {
            this.current = null;
            return null;
        } else {
            this.current = node1.previous;
            return node1;
        }
    }

    public void insertHead(Node node1) {
        if (node1.previous != null) {
            node1.unlink();
        }
        node1.previous = this.head;
        node1.next = this.head.next;
        node1.previous.next = node1;
        node1.next.previous = node1;
    }

    public void insertTail(Node node1) {
        if (node1.previous != null) {
            node1.unlink();
        }
        node1.previous = this.head.previous;
        node1.next = this.head;
        node1.previous.next = node1;
        node1.next.previous = node1;
    }
}
