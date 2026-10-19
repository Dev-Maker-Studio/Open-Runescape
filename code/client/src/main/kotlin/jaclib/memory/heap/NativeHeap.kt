/* NativeHeap - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory.heap

class NativeHeap(size: Int) {
    @JvmField var peer: Long = 0
    private val heapSize: Int = size * -1278684715
    private var allocated: Boolean

    init {
        allocateHeap(heapSize * -217718915)
        allocated = true
    }

    @Synchronized
    fun isAllocated(): Boolean {
        return allocated
    }

    fun createBuffer(initialCapacity: Int, allocateDirect: Boolean): NativeHeapBuffer {
        if (!allocated) {
            throw IllegalStateException()
        }
        return NativeHeapBuffer(this, allocateBuffer(initialCapacity, allocateDirect), initialCapacity)
    }

    @Synchronized
    fun deallocate() {
        if (allocated) {
            deallocateHeap()
        }
        allocated = false
    }

    private external fun allocateHeap(size: Int)

    private external fun deallocateHeap()

    @Synchronized
    external fun allocateBuffer(initialCapacity: Int, allocateDirect: Boolean): Int

    @Synchronized
    external fun getBufferAddress(handle: Int): Long

    @Synchronized
    external fun get(handle: Int, dest: ByteArray, srcOffset: Int, destOffset: Int, length: Int)

    @Synchronized
    external fun put(handle: Int, src: ByteArray, srcOffset: Int, destOffset: Int, length: Int)

    @Synchronized
    external fun deallocateBuffer(handle: Int)

    @Synchronized
    external fun copy(destAddress: Long, srcAddress: Long, size: Int)
}
