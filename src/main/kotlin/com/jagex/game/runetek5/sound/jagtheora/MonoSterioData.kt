package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class MonoSterioData(
    @JvmField val aShortArrayArray8003: Array<ShortArray>,
    @JvmField var aDouble8004: Double
) : Node<Any?>() {
    fun method13294(): Long {
        return (aShortArrayArray8003.size or aShortArrayArray8003[0].size).toLong()
    }
}
