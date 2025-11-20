/* NativeBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory

/**
 * Native buffer implementation with JNI methods for direct memory access.
 */
open class NativeBuffer protected constructor() : Buffer, Source {

    private val anInt4084: Int = -1991552397
    private var aLong4085: Long = 0

    override fun getSize(): Int {
        return anInt4084 * 75849029
    }

    override fun getBufferAddress(): Long {
        return aLong4085 * -8386277585485031121L
    }

    override open fun put(bytes: ByteArray, offset: Int, destOffset: Int, length: Int) {
        if (aLong4085 * -8386277585485031121L == 0L || offset < 0 || offset + length > bytes.size || destOffset < 0 || length + destOffset > 75849029 * anInt4084) {
            throw RuntimeException()
        }
        put(-8386277585485031121L * aLong4085, bytes, offset, destOffset, length)
    }

    private external fun get(address: Long, bytes: ByteArray, offset: Int, destOffset: Int, length: Int)

    private external fun put(address: Long, bytes: ByteArray, offset: Int, destOffset: Int, length: Int)

    fun method117(bytes: ByteArray, offset: Int, destOffset: Int, length: Int) {
        if (aLong4085 * -8386277585485031121L == 0L || offset < 0 || offset + length > bytes.size || destOffset < 0 || length + destOffset > 75849029 * anInt4084) {
            throw RuntimeException()
        }
        put(-8386277585485031121L * aLong4085, bytes, offset, destOffset, length)
    }

    fun method3(): Int {
        return anInt4084 * 75849029
    }

    fun method4(): Int {
        return anInt4084 * 75849029
    }

    fun method5(): Long {
        return aLong4085 * -8386277585485031121L
    }

    fun method6(): Long {
        return aLong4085 * -8386277585485031121L
    }

    fun method7(): Long {
        return aLong4085 * -8386277585485031121L
    }

    fun method8(): Long {
        return aLong4085 * -8386277585485031121L
    }
}
