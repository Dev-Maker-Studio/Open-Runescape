package com.jagex.game.runetek5.cache.node.lru;

public abstract class AbstractLRUNode extends CacheableNode {

    public final int capacityRequirement;

    public AbstractLRUNode(int capacityRequirement) {
        this.capacityRequirement = capacityRequirement;
    }

    public abstract boolean isReusable();

    public abstract Object getValue();

}
