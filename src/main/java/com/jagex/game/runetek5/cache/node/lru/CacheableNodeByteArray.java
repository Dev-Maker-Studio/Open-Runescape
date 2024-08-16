package com.jagex.game.runetek5.cache.node.lru;

public class CacheableNodeByteArray extends CacheableNode {

    public final byte[] data;

    public CacheableNodeByteArray(byte[] data) {
        this.data = data;
    }
}
