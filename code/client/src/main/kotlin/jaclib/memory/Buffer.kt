/* Buffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory

interface Buffer {
    fun getSize(): Int
    fun getBufferAddress(): Long
    fun put(data: ByteArray, i: Int, i0: Int, size: Int)
}
