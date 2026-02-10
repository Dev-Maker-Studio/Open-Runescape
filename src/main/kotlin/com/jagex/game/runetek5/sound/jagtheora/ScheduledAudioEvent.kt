package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
abstract class ScheduledAudioEvent : Node<Any?>() {
    @JvmField
    internal var scheduledTime: Int = 0

    init {
        throw Error()
    }

    abstract fun processEvent(var1: PcmStreamScheduler): Int

    abstract fun onRemove()

    abstract fun method12356()

    abstract fun method12357()

    abstract fun method12358(var1: PcmStreamScheduler): Int
}
