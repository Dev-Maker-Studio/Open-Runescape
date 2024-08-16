package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation;

public class PositionedGfx extends CacheableNode {

    public final SpotAnimation objectGfx;

    public PositionedGfx(SpotAnimation objectGfx) {
        this.objectGfx = objectGfx;
    }

}
