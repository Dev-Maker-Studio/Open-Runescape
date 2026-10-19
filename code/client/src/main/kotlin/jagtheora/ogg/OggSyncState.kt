/* OggSyncState - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.ogg

import jagtheora.misc.SimplePeer

class OggSyncState : SimplePeer() {
    init {
        init()
        if (peerPresent())
            throw IllegalStateException()
    }

    private external fun init()

    external fun write(`is`: ByteArray, i: Int): Boolean

    external fun pageSeek(oggpage: OggPage): Long

    external fun pageOut(oggpage: OggPage): Int

    external fun reset(): Boolean

    protected external override fun clear()

    protected external override fun q()

    protected external override fun f()

    protected external override fun m()
}
