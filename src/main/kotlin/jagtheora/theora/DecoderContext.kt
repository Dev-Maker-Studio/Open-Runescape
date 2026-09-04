/* DecoderContext - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.theora

import jagtheora.misc.SimplePeer
import jagtheora.ogg.OggPacket

class DecoderContext(theorainfo: TheoraInfo, setupinfo: SetupInfo) : SimplePeer() {

    init {
        init(theorainfo, setupinfo)
        if (peerPresent()) {
            throw IllegalArgumentException("")
        }
    }

    external fun init(theorainfo: TheoraInfo, setupinfo: SetupInfo)

    external fun decodePacketIn(oggpacket: OggPacket, granulepos: GranulePos): Int

    external fun decodeFrame(frame: Frame): Int

    external fun granuleFrame(granulepos: GranulePos): Long

    external fun granuleTime(granulepos: GranulePos): Double

    external fun getMaxPostProcessingLevel(): Int

    external fun setPostProcessingLevel(i: Int): Int

    external fun setGranulePosition(l: Long): Int

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()
}
