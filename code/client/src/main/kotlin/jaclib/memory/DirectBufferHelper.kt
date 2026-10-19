/* DirectBufferHelper - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory

class DirectBufferHelper private constructor() {
    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        external fun getDirectBufferAddress(`object`: Any): Long
    }
}
