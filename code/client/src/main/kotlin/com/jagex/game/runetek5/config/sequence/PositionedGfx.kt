package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation

class PositionedGfx(
    @JvmField val objectGfx: SpotAnimation
) : CacheableNode()
