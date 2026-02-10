package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node
import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState

@Suppress("UNCHECKED_CAST")
abstract class OggStreamDecoder(
    @JvmField val anOggStreamState8032: OggStreamState
) : Node<Any?>() {

    @JvmField
    var anInt8031: Int = 0

    abstract fun reset()

    fun method13361(oggpacket1: OggPacket) {
        decode(oggpacket1)
        ++anInt8031
    }

    abstract fun decode(var1: OggPacket)
}
