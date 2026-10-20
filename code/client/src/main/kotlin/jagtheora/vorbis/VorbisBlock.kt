package jagtheora.vorbis

import jagtheora.misc.SimplePeer
import jagtheora.ogg.OggPacket

class VorbisBlock(dspstate: DSPState) : SimplePeer() {
    init {
        init(dspstate)
        if (peerPresent())
            throw IllegalStateException()
    }

    external fun init(dspstate: DSPState)

    external fun synthesis(oggpacket: OggPacket): Int

    protected external override fun clear()

    protected external override fun q()

    protected external override fun f()

    protected external override fun m()
}
