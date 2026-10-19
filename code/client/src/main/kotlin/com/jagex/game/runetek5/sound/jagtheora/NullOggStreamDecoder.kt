package com.jagex.game.runetek5.sound.jagtheora

import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState

class NullOggStreamDecoder(oggstreamstate1: OggStreamState) : OggStreamDecoder(oggstreamstate1) {
    override fun decode(oggpacket1: OggPacket) {
    }

    override fun reset() {
    }
}
