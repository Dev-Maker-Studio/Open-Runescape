/* GranulePos - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.theora

import jagtheora.misc.SimplePeer

class GranulePos : SimplePeer() {
    @JvmField
    var position: Long = 0

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()

    companion object {
        init {
            init()
        }

        @JvmStatic
        external fun init()
    }
}
