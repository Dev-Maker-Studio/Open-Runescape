package jagtheora.ogg

import jagtheora.misc.SimplePeer

class OggStreamState(i: Int) : SimplePeer() {

    init {
        if (!init(i)) {
            throw IllegalStateException()
        }
    }

    external fun init(i: Int): Boolean

    external fun isEOS(): Boolean

    external fun pageIn(oggpage: OggPage): Boolean

    external fun packetOut(oggpacket: OggPacket): Int

    external fun packetOut(): Int

    external fun packetPeek(oggpacket: OggPacket): Int

    external fun packetPeek(): Int

    external fun resetSerialNo(i: Int): Boolean

    external fun reset(): Boolean

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()
}
