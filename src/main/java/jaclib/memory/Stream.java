/* Stream - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory;

public final class Stream {
	private Buffer aBuffer2120;
	private int capacity;
	private int anInt2122;
	private int currentPosition;
	private final byte[] buffer;
	private static final boolean bigEndiannessEnabled = getLSB(-65536) == -1;

	public Stream() {
		this(4096);
	}

	private Stream(int capacity) {
		this.buffer = new byte[capacity];
	}

	public Stream(Buffer buffer) {
		this(buffer, 0, buffer.getSize());
	}

	public Stream(Buffer buffer, int i, int i0) {
		this(buffer.getSize() < 4096 ? buffer.getSize() : 4096);
		method2917(buffer, i, i0);
	}

	private void method2917(Buffer buffer, int i, int i1) {
		method2925();
		this.aBuffer2120 = buffer;
		this.anInt2122 = -1706667277 * i;
		this.capacity = (i1 + i) * -946986007;
		if (-863312807 * this.capacity > buffer.getSize())
			throw new RuntimeException();
	}

	public int written() {
		return this.currentPosition * -781132199 + this.anInt2122 * 1160648251;
	}

	public void method2919(int i) {
		method2925();
		this.anInt2122 = -1706667277 * i;
	}

	public void putByte(int i) {
		if (this.currentPosition * -781132199 >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
	}

	public void method2921(int i) {
		if (3 + -781132199 * this.currentPosition >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2922(int i) {
		if (this.currentPosition * -781132199 + 3 >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void putFloatBigEndian(float f) {
		if (3 + -781132199 * this.currentPosition >= this.buffer.length)
			method2925();
		int i = floatToRawIntBits(f);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
	}

	public void putFloatLittleEndian(float f) {
		if (3 + this.currentPosition * -781132199 >= this.buffer.length)
			method2925();
		int i = floatToRawIntBits(f);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2925() {
		if (this.currentPosition * -781132199 > 0) {
			if (this.anInt2122 * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
				throw new RuntimeException();
			this.aBuffer2120.put(this.buffer, 0, this.anInt2122 * 1160648251, -781132199 * this.currentPosition);
			this.anInt2122 += this.currentPosition * 1269928059;
			this.currentPosition = 0;
		}
	}

	public static boolean bigEndianessEnabled() {
		return bigEndiannessEnabled;
	}

	public static native int floatToRawIntBits(float f);

	private static native byte getLSB(int i);

	private void method2927(Buffer buffer, int i, int i2) {
		method2925();
		this.aBuffer2120 = buffer;
		this.anInt2122 = -1706667277 * i;
		this.capacity = (i2 + i) * -946986007;
		if (-863312807 * this.capacity > buffer.getSize())
			throw new RuntimeException();
	}

	private void method2928(Buffer buffer, int i, int i3) {
		method2925();
		this.aBuffer2120 = buffer;
		this.anInt2122 = -1706667277 * i;
		this.capacity = (i3 + i) * -946986007;
		if (-863312807 * this.capacity > buffer.getSize())
			throw new RuntimeException();
	}

	public int method2929() {
		return this.currentPosition * -781132199 + this.anInt2122 * 1160648251;
	}

	public int method2930() {
		return this.currentPosition * -781132199 + this.anInt2122 * 1160648251;
	}

	public void method2931(int i) {
		method2925();
		this.anInt2122 = -1706667277 * i;
	}

	public void method2932(int i) {
		method2925();
		this.anInt2122 = -1706667277 * i;
	}

	public void method2933(int i) {
		method2925();
		this.anInt2122 = -1706667277 * i;
	}

	public void method2934(int i) {
		if (3 + -781132199 * this.currentPosition >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2935(int i) {
		if (3 + -781132199 * this.currentPosition >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2936(int i) {
		if (this.currentPosition * -781132199 + 3 >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2937(int i) {
		if (this.currentPosition * -781132199 + 3 >= this.buffer.length)
			method2925();
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2938(float f) {
		if (3 + -781132199 * this.currentPosition >= this.buffer.length)
			method2925();
		int i = floatToRawIntBits(f);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
	}

	public void method2939(float f) {
		if (3 + this.currentPosition * -781132199 >= this.buffer.length)
			method2925();
		int i = floatToRawIntBits(f);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) i;
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 8);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 16);
		this.buffer[(this.currentPosition += 1787333097) * -781132199 - 1] = (byte) (i >> 24);
	}

	public void method2940() {
		if (this.currentPosition * -781132199 > 0) {
			if (this.anInt2122 * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
				throw new RuntimeException();
			this.aBuffer2120.put(this.buffer, 0, this.anInt2122 * 1160648251, -781132199 * this.currentPosition);
			this.anInt2122 += this.currentPosition * 1269928059;
			this.currentPosition = 0;
		}
	}

	public void method2941() {
		if (this.currentPosition * -781132199 > 0) {
			if (this.anInt2122 * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
				throw new RuntimeException();
			this.aBuffer2120.put(this.buffer, 0, this.anInt2122 * 1160648251, -781132199 * this.currentPosition);
			this.anInt2122 += this.currentPosition * 1269928059;
			this.currentPosition = 0;
		}
	}

	public static boolean bigEndianness() {
		return bigEndiannessEnabled;
	}
}
