/* NativeBuffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory;

public class NativeBuffer implements Buffer, Source {
	private final int anInt4084 = -1991552397;
	private long aLong4085;

	protected NativeBuffer() {
		/* empty */
	}

	public final int getSize() {
		return this.anInt4084 * 75849029;
	}

	public final long getBufferAddress() {
		return this.aLong4085 * -8386277585485031121L;
	}

	public void put(byte[] is, int i, int i0, int i1) {
		if (this.aLong4085 * -8386277585485031121L == 0L | null == is | i < 0 | i + i1 > is.length | i0 < 0 | i1 + i0 > 75849029 * this.anInt4084)
			throw new RuntimeException();
		put(-8386277585485031121L * this.aLong4085, is, i, i0, i1);
	}

	private final native void get(long l, byte[] is, int i, int i2, int i3);

	private final native void put(long l, byte[] is, int i, int i4, int i5);

	public void method117(byte[] is, int i, int i6, int i7) {
		if (this.aLong4085 * -8386277585485031121L == 0L | null == is | i < 0 | i + i7 > is.length | i6 < 0 | i7 + i6 > 75849029 * this.anInt4084)
			throw new RuntimeException();
		put(-8386277585485031121L * this.aLong4085, is, i, i6, i7);
	}

	public final int method3() {
		return this.anInt4084 * 75849029;
	}

	public final int method4() {
		return this.anInt4084 * 75849029;
	}

	public final long method5() {
		return this.aLong4085 * -8386277585485031121L;
	}

	public final long method6() {
		return this.aLong4085 * -8386277585485031121L;
	}

	public final long method7() {
		return this.aLong4085 * -8386277585485031121L;
	}

	public final long method8() {
		return this.aLong4085 * -8386277585485031121L;
	}
}
