/* VorbisInfo - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.vorbis

import jagtheora.misc.SimplePeer
import jagtheora.ogg.OggPacket

class VorbisInfo : SimplePeer() {

    @JvmField var channels: Int = 0
    @JvmField var rate: Int = 0

    init {
        init()
        if (peerPresent()) throw IllegalStateException()
    }

    external fun init()

    external fun headerIn(vorbiscomment: VorbisComment, oggpacket: OggPacket): Int

    protected external override fun clear()

    protected external override fun q()

    protected external override fun f()

    protected external override fun m()

    companion object {
        init {
            initFields()
        }

        @JvmStatic
        external fun initFields()
    }
}
