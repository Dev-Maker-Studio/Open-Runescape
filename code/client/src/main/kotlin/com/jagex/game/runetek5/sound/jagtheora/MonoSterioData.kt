package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class MonoSterioData(
    @JvmField val samples: Array<ShortArray>,
    @JvmField var timestamp: Double
) : Node<Any?>() {
    fun getCacheKey(): Long {
        return (samples.size or samples[0].size).toLong()
    }
}
