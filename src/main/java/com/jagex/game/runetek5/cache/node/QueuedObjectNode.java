package com.jagex.game.runetek5.cache.node;

public class QueuedObjectNode {

    public final Object object;

    /**
     * The index in the direct cache
     */
    public int slot;

    public QueuedObjectNode(Object object, int slot) {
        this.object = object;
        this.slot = slot;
    }

}
