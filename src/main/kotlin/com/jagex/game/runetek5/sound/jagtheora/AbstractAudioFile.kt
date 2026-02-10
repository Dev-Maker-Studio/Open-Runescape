package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.impl.AudioNode

abstract class AbstractAudioFile : AudioNode() {
    @JvmField
    var start: Int = 0

    @JvmField
    var sampleRate: Int = 0

    @JvmField
    var enableLoop: Boolean = false

    @JvmField
    var end: Int = 0

    abstract fun getTotalSamples(): Int

    abstract fun getStream(var1: Int, var2: Int, var3: Int): AreaSoundStream
}
