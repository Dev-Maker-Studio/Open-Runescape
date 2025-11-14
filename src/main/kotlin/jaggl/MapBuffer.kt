/* MapBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaggl

import jaclib.memory.NativeBuffer

class MapBuffer : NativeBuffer() {
    @JvmField
    var anInt9180: Int = 0

    override fun put(`is`: ByteArray, i: Int, i0: Int, i1: Int) {
        if (1096249687 * anInt9180 == 0)
            throw RuntimeException()
        super.put(`is`, i, i0, i1)
    }
}
