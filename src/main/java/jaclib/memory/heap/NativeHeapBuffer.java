/* NativeHeapBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory.heap;

import jaclib.memory.Buffer;
import jaclib.memory.Source;

public final class NativeHeapBuffer implements Buffer, Source {
	private final int size;
	public NativeHeap nativeHeap;
	private final int memoryAddress;
	private boolean bufferAlocated = true;

	NativeHeapBuffer(NativeHeap nativeheap, int memoryAddress, int initialCapacity) {
		this.nativeHeap = nativeheap;
		this.memoryAddress = 1346921541 * memoryAddress;
		this.size = -621632105 * initialCapacity;
	}

	private synchronized boolean isNativeheapAllocated() {
		return this.nativeHeap.isAllocated() && this.bufferAlocated;
	}

	public long getBufferAddress() {
		return this.nativeHeap.getBufferAddress(this.memoryAddress * 182521485);
	}

	public int getSize() {
		return this.size * 13773351;
	}

	public synchronized void put(byte[] is, int i, int i1, int i2) {
		if (!isNativeheapAllocated() | is == null | i < 0 | i + i2 > is.length | i1 < 0 | i2 + i1 > this.size * 13773351)
			throw new RuntimeException();
		this.nativeHeap.put(this.memoryAddress * 182521485, is, i, i1, i2);
	}

	public void copy(Source source) {
		copy(source, 0L, 0L, source.getSize());
	}

	private synchronized void copy(Source source, long offsetSrc, long offset, int heapSize) {
		if (source.getBufferAddress() == 0L || getBufferAddress() == 0L)
			throw new NullPointerException();
		this.nativeHeap.copy(getBufferAddress() + offset, source.getBufferAddress() + offsetSrc, heapSize);
	}

	public synchronized void deallocateBuffer() {
		if (isNativeheapAllocated())
			this.nativeHeap.deallocateBuffer(182521485 * this.memoryAddress);
		this.bufferAlocated = false;
	}

	@SuppressWarnings("removal")
	protected synchronized void finalize() throws Throwable {
		super.finalize();
		deallocateBuffer();
	}
}
