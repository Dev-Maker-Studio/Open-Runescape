package com.jagex.game.runetek5.cache.node;

import java.util.Collection;
import java.util.Iterator;

public class NodeCollection implements Collection {

    public final Node tail = new Node();
    public Node current;

    public NodeCollection() {
        this.tail.next = this.tail;
        this.tail.previous = this.tail;
    }

    public void addFirst(Node node1) {
        if (node1.previous != null) {
            node1.unlink();
        }
        node1.previous = this.tail.previous;
        node1.next = this.tail;
        node1.previous.next = node1;
        node1.next.previous = node1;
    }

    public Node popHead() {
        Node node2 = this.tail.next;
        if (node2 == this.tail) {
            return null;
        } else {
            node2.unlink();
            return node2;
        }
    }

    public void method8062(NodeCollection nodecollection1, Node node2) {
        Node node4 = this.tail.previous;
        this.tail.previous = node2.previous;
        node2.previous.next = this.tail;
        if (node2 != this.tail) {
            node2.previous = nodecollection1.tail.previous;
            node2.previous.next = node2;
            node4.next = nodecollection1.tail;
            nodecollection1.tail.previous = node4;
        }
    }

    public void method8063(NodeCollection nodecollection1) {
        if (this.tail.next != this.tail) {
            method8062(nodecollection1, this.tail.next);
        }
    }

    @Override
    public Iterator iterator() {
        return new NodeIterator(this);
    }

    public Node first() {
        return method8066();
    }

    public Node method8066() {
        Node node3;
        node3 = this.tail.previous;
        if (node3 == this.tail) {
            this.current = null;
            return null;
        } else {
            this.current = node3.previous;
            return node3;
        }
    }

    public Node next() {
        Node node2 = this.current;
        if (node2 == this.tail) {
            this.current = null;
            return null;
        } else {
            this.current = node2.next;
            return node2;
        }
    }

    public Node getNext() {
        Node node2 = this.current;
        if (node2 == this.tail) {
            this.current = null;
            return null;
        } else {
            this.current = node2.previous;
            return node2;
        }
    }

    @Override
    public int size() {
        int i_2 = 0;
        for (Node node3 = this.tail.next; node3 != this.tail; node3 = node3.next) {
            ++i_2;
        }
        return i_2;
    }

    @Override
    public boolean add(Object object1) {
        return add((Node) object1);
    }

    public Node[] method8070() {
        Node[] arr_2 = new Node[size()];
        int i_3 = 0;
        for (Node node4 = this.tail.next; node4 != this.tail; node4 = node4.next) {
            arr_2[i_3++] = node4;
        }
        return arr_2;
    }

    @Override
    public void clear() {
        remove_all();
    }

    @Override
    public boolean isEmpty() {
        return hasNext();
    }

    @Override
    public boolean contains(Object object1) {
        throw new RuntimeException();
    }

    @Override
    public Object[] toArray() {
        return method8070();
    }

    @Override
    public Object[] toArray(Object[] arr_1) {
        int i_2 = 0;
        for (Node node3 = this.tail.next; node3 != this.tail; node3 = node3.next) {
            arr_1[i_2++] = node3;
        }
        return arr_1;
    }

    public boolean add(Node node1) {
        addFirst(node1);
        return true;
    }

    @Override
    public boolean containsAll(Collection collection1) {
        throw new RuntimeException();
    }

    @Override
    public boolean addAll(Collection collection1) {
        throw new RuntimeException();
    }

    @Override
    public boolean removeAll(Collection collection1) {
        throw new RuntimeException();
    }

    @Override
    public boolean retainAll(Collection collection1) {
        throw new RuntimeException();
    }

    public boolean equals(Object object1) {
        return super.equals(object1);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public Node method8076() {
        Node node3;
        node3 = this.tail.next;
        if (node3 == this.tail) {
            this.current = null;
            return null;
        } else {
            this.current = node3.next;
            return node3;
        }
    }

    public boolean hasNext() {
        return this.tail.next == this.tail;
    }

    public Node getFirst() {
        return method8076();
    }

    public void method8098(Node node1) {
        if (node1.previous != null) {
            node1.unlink();
        }
        node1.previous = this.tail;
        node1.next = this.tail.next;
        node1.previous.next = node1;
        node1.next.previous = node1;
    }

    public void remove_all() {
        while (this.tail.next != this.tail) {
            this.tail.next.unlink();
        }
    }

    @Override
    public boolean remove(Object object1) {
        throw new RuntimeException();
    }
}
