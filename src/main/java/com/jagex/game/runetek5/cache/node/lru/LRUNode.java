package com.jagex.game.runetek5.cache.node.lru;

public class LRUNode extends AbstractLRUNode {

    public final Object value;

    public LRUNode(Object value, int capacity) {
        super(capacity);
        this.value = value;
    }

    @Override
    public boolean isReusable() {
        return false;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

}
