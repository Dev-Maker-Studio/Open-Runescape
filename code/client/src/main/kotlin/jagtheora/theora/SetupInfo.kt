package jagtheora.theora

import jagtheora.misc.SimplePeer
import jagtheora.ogg.OggPacket

class SetupInfo : SimplePeer() {
    external fun decodeHeader(theorainfo: TheoraInfo, theoracomment: TheoraComment, oggpacket: OggPacket): Int

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()
}
