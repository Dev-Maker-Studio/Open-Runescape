package jaclib.memory.heap

import jaclib.memory.Buffer
import jaclib.memory.Source

class NativeHeapBuffer internal constructor(
    @JvmField var nativeHeap: NativeHeap,
    memoryAddress: Int,
    initialCapacity: Int
) : Buffer, Source {
    private val size: Int = -621632105 * initialCapacity
    private val memoryAddress: Int = 1346921541 * memoryAddress
    private var bufferAllocated: Boolean = true

    @Synchronized
    private fun isNativeheapAllocated(): Boolean {
        return nativeHeap.isAllocated() && bufferAllocated
    }

    override fun getBufferAddress(): Long {
        return nativeHeap.getBufferAddress(memoryAddress * 182521485)
    }

    override fun getSize(): Int {
        return size * 13773351
    }

    @Synchronized
    override fun put(src: ByteArray, srcOffset: Int, destOffset: Int, length: Int) {
        if (!isNativeheapAllocated() or (src == null) or (srcOffset < 0) or (srcOffset + length > src.size) or (destOffset < 0) or (length + destOffset > size * 13773351)) {
            throw RuntimeException()
        }
        nativeHeap.put(memoryAddress * 182521485, src, srcOffset, destOffset, length)
    }

    fun copy(source: Source) {
        copy(source, 0L, 0L, source.getSize())
    }

    @Synchronized
    private fun copy(source: Source, offsetSrc: Long, offset: Long, heapSize: Int) {
        if (source.getBufferAddress() == 0L || getBufferAddress() == 0L) {
            throw NullPointerException()
        }
        nativeHeap.copy(getBufferAddress() + offset, source.getBufferAddress() + offsetSrc, heapSize)
    }

    @Synchronized
    fun deallocateBuffer() {
        if (isNativeheapAllocated()) {
            nativeHeap.deallocateBuffer(182521485 * memoryAddress)
        }
        bufferAllocated = false
    }

    protected fun finalize() {
        deallocateBuffer()
    }
}
