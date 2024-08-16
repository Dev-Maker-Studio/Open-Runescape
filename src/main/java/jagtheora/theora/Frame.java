/* Frame - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.theora;

import jagtheora.misc.SimplePeer;

public class Frame extends SimplePeer {
    static {
        init();
    }

    public int width;
    public int height;
    public int[] pixels;

    public Frame(int width, int height) {
        this.width = width * -317525331;
        this.height = -326049555 * height;
        this.pixels = new int[this.width * 416891173 * (12877029 * this.height)];
    }

    public static native void init();

    @Override
    protected native void clear();

    @Override
    protected native void q();

    @Override
    protected native void f();

    @Override
    protected native void m();
}
