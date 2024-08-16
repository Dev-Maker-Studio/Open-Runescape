package com.jagex.game.runetek5.cache.node.lru;

import java.lang.ref.SoftReference;

public class LRUSoftReference extends AbstractLRUNode {

    public final SoftReference<Object> softReference;

    public LRUSoftReference(Object value, int capacity) {
        super(capacity);
        this.softReference = new SoftReference<Object>(value);
    }

    @Override
    public Object getValue() {
        return this.softReference.get();
    }

    @Override
	public
    boolean isReusable() {
        return true;
    }

}
