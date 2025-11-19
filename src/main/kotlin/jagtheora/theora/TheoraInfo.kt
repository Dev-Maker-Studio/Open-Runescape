/* TheoraInfo - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.theora

import jagtheora.misc.SimplePeer

class TheoraInfo : SimplePeer() {
    companion object {
        init {
            initFields()
        }

        @JvmStatic
        external fun initFields()
    }

    @JvmField var versionMajor: Byte = 0
    @JvmField var versionMinor: Byte = 0
    @JvmField var versionSubMinor: Byte = 0
    @JvmField var frameWidth: Int = 0
    @JvmField var frameHeight: Int = 0
    @JvmField var picWidth: Int = 0
    @JvmField var picHeight: Int = 0
    @JvmField var picX: Int = 0
    @JvmField var picY: Int = 0
    @JvmField var fpsNumerator: Int = 0
    @JvmField var fpsDenominator: Int = 0
    @JvmField var aspectNumerator: Int = 0
    @JvmField var aspectDenominator: Int = 0
    @JvmField var colourSpace: Int = 0
    @JvmField var pixelFormat: Int = 0

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
