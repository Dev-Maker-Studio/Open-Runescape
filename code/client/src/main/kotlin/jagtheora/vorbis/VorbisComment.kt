package jagtheora.vorbis

import jagtheora.misc.SimplePeer

class VorbisComment : SimplePeer() {
    init {
        init()
        if (peerPresent())
            throw IllegalStateException()
    }

    external fun init()

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()
}
