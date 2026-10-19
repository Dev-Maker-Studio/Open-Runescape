/* OggPacket - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.ogg

import jagtheora.misc.SimplePeer

class OggPacket : SimplePeer() {
    external fun isHeader(): Int

    external fun isKeyFrame(): Int

    external fun isTheora(): Boolean

    external fun isVorbis(): Boolean

    external fun getData(): ByteArray

    override external fun clear()

    override external fun q()

    override external fun f()

    override external fun m()
}
