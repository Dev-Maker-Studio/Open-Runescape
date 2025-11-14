package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.impl.Node_Sub26

abstract class AbstractAudioFile : Node_Sub26() {
    @JvmField
    var start: Int = 0

    @JvmField
    var sampleRate: Int = 0

    @JvmField
    var enableLoop: Boolean = false

    @JvmField
    protected var end: Int = 0

    abstract fun getTotalSamples(): Int

    abstract fun getStream(var1: Int, var2: Int, var3: Int): AreaSoundStream
}
