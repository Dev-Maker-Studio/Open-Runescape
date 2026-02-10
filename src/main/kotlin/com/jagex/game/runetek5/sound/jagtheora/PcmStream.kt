package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.AudioNode

abstract class PcmStream : Node<PcmStream>() {

    @JvmField var nextStream: PcmStream? = null
    @JvmField var streamPriority: Int = 0
    @JvmField var buffer: AudioNode? = null
    @Volatile
    @JvmField var fill: Boolean = true

    open fun getPriority(): Int {
        return 255
    }

    abstract fun getFirstStream(): PcmStream?

    abstract fun getActiveVoiceCount(): Int

    abstract fun fill(buffer: IntArray, offset: Int, length: Int)

    abstract fun skip(length: Int)

    fun process(buffer: IntArray, offset: Int, length: Int) {
        if (this.fill) {
            fill(buffer, offset, length)
        } else {
            skip(length)
        }
    }

    abstract fun getNextStream(): PcmStream?
}
