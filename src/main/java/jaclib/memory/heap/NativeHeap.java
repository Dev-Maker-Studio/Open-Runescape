/* NativeHeap - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory.heap;

public final class NativeHeap {
	long peer;
	private final int heapSize;
	private boolean allocated;

	public NativeHeap(int size) {
		this.heapSize = size * -1278684715;
		allocateHeap(this.heapSize * -217718915);
		this.allocated = true;
	}

	synchronized boolean isAllocated() {
		return this.allocated;
	}

	public NativeHeapBuffer createBuffer(int initialCapacity, boolean allocateDirect) {
		if (!this.allocated)
			throw new IllegalStateException();
		return new NativeHeapBuffer(this, allocateBuffer(initialCapacity, allocateDirect), initialCapacity);
	}

	public synchronized void deallocate() {
		if (this.allocated)
			deallocateHeap();
		this.allocated = false;
	}

	private native void allocateHeap(int i);

	private native void deallocateHeap();

	synchronized native int allocateBuffer(int initialCapacity, boolean allocateDirect);

	synchronized native long getBufferAddress(int i);

	synchronized native void get(int i, byte[] is, int i0, int i1, int i2);

	synchronized native void put(int i, byte[] is, int i3, int i4, int i5);

	synchronized native void deallocateBuffer(int i);

	public synchronized native void copy(long l, long l6, int i);

	@SuppressWarnings("removal")
	protected synchronized void finalize() throws Throwable {
		super.finalize();
		deallocate();
	}
}
