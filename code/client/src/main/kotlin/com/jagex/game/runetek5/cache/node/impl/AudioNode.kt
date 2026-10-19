package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
abstract class AudioNode : @Suppress("UNCHECKED_CAST") Node<Any?>() {
    @JvmField
    var activeVoiceCount: Int = 0
}
