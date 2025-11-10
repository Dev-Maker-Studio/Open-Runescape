package com.jagex.game.runetek5.entity;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Needs better naming...
 *
 */
public class EntityNodeList implements Iterable<EntityNode> {

    public final EntityNode head = new EntityNode();
    public EntityNode pointer;

    public EntityNodeList() {
        this.head.next = this.head;
        this.head.previous = this.head;
    }

    public void getPrevious(EntityNode entityNode) {
        if (entityNode.previous != null) {
            entityNode.unlinkNode();
        }
        entityNode.previous = this.head;
        entityNode.next = this.head.next;
        entityNode.previous.next = entityNode;
        entityNode.next.previous = entityNode;
    }

    public EntityNode get() {
        EntityNode entityNode = this.head.next;
        if (entityNode == this.head) {
            return null;
        } 
        entityNode.unlinkNode();
        return entityNode;
    }

    public void offer(EntityNode entityNode) {
        if (entityNode.previous != null) {
            entityNode.unlinkNode();
        }
        entityNode.previous = this.head.previous;
        entityNode.next = this.head;
        entityNode.previous.next = entityNode;
        entityNode.next.previous = entityNode;
    }

    public EntityNode getNext() {
        EntityNode entity = this.pointer;
        if (entity == this.head) {
            this.pointer = null;
            return null;
        } 
        this.pointer = entity.next;
        return entity;
    }

    public void unlink() {
        while (true) {
            EntityNode entity = this.head.next;
            if (entity == this.head) {
                this.pointer = null;
                return;
            }
            entity.unlinkNode();
        }
    }

    public EntityNode getFirst() {
        EntityNode entity = this.head.next;
        if (entity == this.head) {
            this.pointer = null;
            return null;
        } 
        this.pointer = entity.next;
        return entity;
    }

    public boolean isEmpty() {
        return this.head.next == this.head;
    }

    @Override
    public Iterator<EntityNode> iterator() {
        return new EntityNodeIterator();
    }

    private class EntityNodeIterator implements Iterator<EntityNode> {
        private EntityNode current = head.next;

        @Override
        public boolean hasNext() {
            return current != head;
        }

        @Override
        public EntityNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            EntityNode node = current;
            current = current.next;
            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }
}