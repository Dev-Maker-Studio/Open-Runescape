/* VorbisInfo - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.vorbis;

import jagtheora.misc.SimplePeer;
import jagtheora.ogg.OggPacket;

public class VorbisInfo extends SimplePeer {
    static {
        initFields();
    }

    public int channels;
    public int rate;

    public VorbisInfo() {
        init();
        if (peerPresent())
            throw new IllegalStateException();
    }

    public static native void initFields();

    public native void init();

    public native int headerIn(VorbisComment vorbiscomment, OggPacket oggpacket);

    @Override
    protected native void clear();

    @Override
    protected native void q();

    @Override
    protected native void f();

    @Override
    protected native void m();
}
