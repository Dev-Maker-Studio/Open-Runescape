/* Stream - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory

class Stream {
    private var aBuffer2120: Buffer? = null
    private var capacity: Int = 0
    private var anInt2122: Int = 0
    private var currentPosition: Int = 0
    private val buffer: ByteArray

    constructor() : this(4096)

    private constructor(capacity: Int) {
        this.buffer = ByteArray(capacity)
    }

    constructor(buffer: Buffer) : this(buffer, 0, buffer.getSize())

    constructor(buffer: Buffer, i: Int, i0: Int) : this(kotlin.math.min(buffer.getSize(), 4096)) {
        method2917(buffer, i, i0)
    }

    private fun method2917(buffer: Buffer, i: Int, i1: Int) {
        method2925()
        this.aBuffer2120 = buffer
        this.anInt2122 = -1706667277 * i
        this.capacity = (i1 + i) * -946986007
        if (-863312807 * this.capacity > buffer.getSize())
            throw RuntimeException()
    }

    fun written(): Int {
        return this.currentPosition * -781132199 + this.anInt2122 * 1160648251
    }

    fun method2919(i: Int) {
        method2925()
        this.anInt2122 = -1706667277 * i
    }

    fun putByte(i: Int) {
        if (this.currentPosition * -781132199 >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
    }

    fun method2921(i: Int) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2922(i: Int) {
        if (this.currentPosition * -781132199 + 3 >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun putFloatBigEndian(f: Float) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            method2925()
        val i = floatToRawIntBits(f)
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
    }

    fun putFloatLittleEndian(f: Float) {
        if (3 + this.currentPosition * -781132199 >= this.buffer.size)
            method2925()
        val i = floatToRawIntBits(f)
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2925() {
        if (this.currentPosition * -781132199 > 0) {
            if (this.anInt2122 * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
                throw RuntimeException()
            this.aBuffer2120!!.put(this.buffer, 0, this.anInt2122 * 1160648251, -781132199 * this.currentPosition)
            this.anInt2122 += this.currentPosition * 1269928059
            this.currentPosition = 0
        }
    }

    private fun method2927(buffer: Buffer, i: Int, i2: Int) {
        method2925()
        this.aBuffer2120 = buffer
        this.anInt2122 = -1706667277 * i
        this.capacity = (i2 + i) * -946986007
        if (-863312807 * this.capacity > buffer.getSize())
            throw RuntimeException()
    }

    private fun method2928(buffer: Buffer, i: Int, i3: Int) {
        method2925()
        this.aBuffer2120 = buffer
        this.anInt2122 = -1706667277 * i
        this.capacity = (i3 + i) * -946986007
        if (-863312807 * this.capacity > buffer.getSize())
            throw RuntimeException()
    }

    fun method2929(): Int {
        return this.currentPosition * -781132199 + this.anInt2122 * 1160648251
    }

    fun method2930(): Int {
        return this.currentPosition * -781132199 + this.anInt2122 * 1160648251
    }

    fun method2931(i: Int) {
        method2925()
        this.anInt2122 = -1706667277 * i
    }

    fun method2932(i: Int) {
        method2925()
        this.anInt2122 = -1706667277 * i
    }

    fun method2933(i: Int) {
        method2925()
        this.anInt2122 = -1706667277 * i
    }

    fun method2934(i: Int) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2935(i: Int) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2936(i: Int) {
        if (this.currentPosition * -781132199 + 3 >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2937(i: Int) {
        if (this.currentPosition * -781132199 + 3 >= this.buffer.size)
            method2925()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2938(f: Float) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            method2925()
        val i = floatToRawIntBits(f)
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
    }

    fun method2939(f: Float) {
        if (3 + this.currentPosition * -781132199 >= this.buffer.size)
            method2925()
        val i = floatToRawIntBits(f)
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun method2940() {
        if (this.currentPosition * -781132199 > 0) {
            if (this.anInt2122 * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
                throw RuntimeException()
            this.aBuffer2120!!.put(this.buffer, 0, this.anInt2122 * 1160648251, -781132199 * this.currentPosition)
            this.anInt2122 += this.currentPosition * 1269928059
            this.currentPosition = 0
        }
    }

    fun method2941() {
        if (this.currentPosition * -781132199 > 0) {
            if (this.anInt2122 * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
                throw RuntimeException()
            this.aBuffer2120!!.put(this.buffer, 0, this.anInt2122 * 1160648251, -781132199 * this.currentPosition)
            this.anInt2122 += this.currentPosition * 1269928059
            this.currentPosition = 0
        }
    }

    companion object {
        private val bigEndiannessEnabled = getLSB(-65536).toInt() == -1

        @JvmStatic
        fun bigEndianessEnabled(): Boolean {
            return bigEndiannessEnabled
        }

        @JvmStatic
        external fun floatToRawIntBits(f: Float): Int

        @JvmStatic
        private external fun getLSB(i: Int): Byte

        @JvmStatic
        fun bigEndianness(): Boolean {
            return bigEndiannessEnabled
        }
    }
}
