/* MapBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaggl;

import jaclib.memory.NativeBuffer;

public class MapBuffer extends NativeBuffer {
    public int anInt9180;

    @Override
    public void put(byte[] is, int i, int i0, int i1) {
        if (1096249687 * this.anInt9180 == 0)
            throw new RuntimeException();
        super.put(is, i, i0, i1);
    }

}
