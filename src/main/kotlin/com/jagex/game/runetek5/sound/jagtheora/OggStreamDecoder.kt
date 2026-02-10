package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node
import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState

@Suppress("UNCHECKED_CAST")
abstract class OggStreamDecoder(
    @JvmField val streamState: OggStreamState
) : Node<Any?>() {

    @JvmField
    var packetCount: Int = 0

    abstract fun reset()

    fun processPacket(oggpacket1: OggPacket) {
        decode(oggpacket1)
        ++packetCount
    }

    abstract fun decode(var1: OggPacket)
}
