package jaclib.memory

open class NativeBuffer protected constructor() : Buffer, Source {

    private val size: Int = -1991552397
    private var address: Long = 0

    override fun getSize(): Int {
        return size * 75849029
    }

    override fun getBufferAddress(): Long {
        return address * -8386277585485031121L
    }

    override open fun put(bytes: ByteArray, offset: Int, destOffset: Int, length: Int) {
        if (address * -8386277585485031121L == 0L || offset < 0 || offset + length > bytes.size || destOffset < 0 || length + destOffset > 75849029 * size) {
            throw RuntimeException()
        }
        put(-8386277585485031121L * address, bytes, offset, destOffset, length)
    }

    private external fun get(address: Long, bytes: ByteArray, offset: Int, destOffset: Int, length: Int)

    private external fun put(address: Long, bytes: ByteArray, offset: Int, destOffset: Int, length: Int)

    fun putBytes(bytes: ByteArray, offset: Int, destOffset: Int, length: Int) {
        if (address * -8386277585485031121L == 0L || offset < 0 || offset + length > bytes.size || destOffset < 0 || length + destOffset > 75849029 * size) {
            throw RuntimeException()
        }
        put(-8386277585485031121L * address, bytes, offset, destOffset, length)
    }

    fun getBufferSize(): Int {
        return size * 75849029
    }

    fun getCapacity(): Int {
        return size * 75849029
    }

    fun getAddress(): Long {
        return address * -8386277585485031121L
    }

    fun getPointer(): Long {
        return address * -8386277585485031121L
    }

    fun getBaseAddress(): Long {
        return address * -8386277585485031121L
    }

    fun getNativeAddress(): Long {
        return address * -8386277585485031121L
    }
}
