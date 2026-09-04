/* OggPage - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.ogg

import jagtheora.misc.SimplePeer

class OggPage : SimplePeer() {
    external fun getGranulePos(): Long

    external fun getSerialNumber(): Int

    external fun getPageNumber(): Long

    external fun getCompletedPackets(): Int

    external fun getVersion(): Int

    external fun isContinued(): Boolean

    external fun isBOS(): Boolean

    external fun isEOS(): Boolean

    protected external override fun clear()

    protected external override fun q()

    protected external override fun f()

    protected external override fun m()
}
