/* DSPState - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.vorbis

import jagtheora.misc.SimplePeer

class DSPState(vorbisinfo: VorbisInfo) : SimplePeer() {

    init {
        init(vorbisinfo)
        if (peerPresent())
            throw IllegalStateException()
    }

    external fun init(vorbisinfo: VorbisInfo)

    external fun blockIn(vorbisblock: VorbisBlock)

    external fun pcmOut(i: Int): Array<FloatArray>?

    external fun read(i: Int)

    external fun granuleTime(): Double

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()
}
