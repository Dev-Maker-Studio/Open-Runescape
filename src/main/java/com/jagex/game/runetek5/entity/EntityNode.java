package com.jagex.game.runetek5.entity;

/**
 *
 * @author Brainpower - 01/05/2022
 *
 */
public class EntityNode {

    public EntityNode next;
    public EntityNode previous;

    public static void replace(EntityNode dst, EntityNode src) {
        if (dst.previous != null) {
            dst.unlinkNode();
        }
        dst.previous = src;
        dst.next = src.next;
        dst.previous.next = dst;
        dst.next.previous = dst;
    }

    public void unlinkNode() {
        if (this.previous != null) {
            this.previous.next = this.next;
            this.next.previous = this.previous;
            this.next = null;
            this.previous = null;
        }
    }
}
