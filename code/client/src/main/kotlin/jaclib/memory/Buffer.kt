package jaclib.memory

interface Buffer {
    fun getSize(): Int
    fun getBufferAddress(): Long
    fun put(data: ByteArray, i: Int, i0: Int, size: Int)
}
