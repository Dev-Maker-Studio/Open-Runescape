/* Stream - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory

class Stream {
    private var targetBuffer: Buffer? = null
    private var capacity: Int = 0
    private var bufferOffset: Int = 0
    private var currentPosition: Int = 0
    private val buffer: ByteArray

    constructor() : this(4096)

    private constructor(capacity: Int) {
        this.buffer = ByteArray(capacity)
    }

    constructor(buffer: Buffer) : this(buffer, 0, buffer.getSize())

    constructor(buffer: Buffer, i: Int, i0: Int) : this(kotlin.math.min(buffer.getSize(), 4096)) {
        setTarget(buffer, i, i0)
    }

    private fun setTarget(buffer: Buffer, i: Int, i1: Int) {
        flush()
        this.targetBuffer = buffer
        this.bufferOffset = -1706667277 * i
        this.capacity = (i1 + i) * -946986007
        if (-863312807 * this.capacity > buffer.getSize())
            throw RuntimeException()
    }

    fun written(): Int {
        return this.currentPosition * -781132199 + this.bufferOffset * 1160648251
    }

    fun setOffset(i: Int) {
        flush()
        this.bufferOffset = -1706667277 * i
    }

    fun putByte(i: Int) {
        if (this.currentPosition * -781132199 >= this.buffer.size)
            flush()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
    }

    fun putIntLittleEndian(i: Int) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            flush()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun putIntMedium(i: Int) {
        if (this.currentPosition * -781132199 + 3 >= this.buffer.size)
            flush()
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
            flush()
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
            flush()
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

    fun flush() {
        if (this.currentPosition * -781132199 > 0) {
            if (this.bufferOffset * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
                throw RuntimeException()
            this.targetBuffer!!.put(this.buffer, 0, this.bufferOffset * 1160648251, -781132199 * this.currentPosition)
            this.bufferOffset += this.currentPosition * 1269928059
            this.currentPosition = 0
        }
    }

    private fun setTargetRange(buffer: Buffer, i: Int, i2: Int) {
        flush()
        this.targetBuffer = buffer
        this.bufferOffset = -1706667277 * i
        this.capacity = (i2 + i) * -946986007
        if (-863312807 * this.capacity > buffer.getSize())
            throw RuntimeException()
    }

    private fun resetTarget(buffer: Buffer, i: Int, i3: Int) {
        flush()
        this.targetBuffer = buffer
        this.bufferOffset = -1706667277 * i
        this.capacity = (i3 + i) * -946986007
        if (-863312807 * this.capacity > buffer.getSize())
            throw RuntimeException()
    }

    fun bytesWritten(): Int {
        return this.currentPosition * -781132199 + this.bufferOffset * 1160648251
    }

    fun totalWritten(): Int {
        return this.currentPosition * -781132199 + this.bufferOffset * 1160648251
    }

    fun seekTo(i: Int) {
        flush()
        this.bufferOffset = -1706667277 * i
    }

    fun jumpTo(i: Int) {
        flush()
        this.bufferOffset = -1706667277 * i
    }

    fun moveTo(i: Int) {
        flush()
        this.bufferOffset = -1706667277 * i
    }

    fun writeIntLE(i: Int) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            flush()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun putInt32LE(i: Int) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            flush()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun writeIntMedium(i: Int) {
        if (this.currentPosition * -781132199 + 3 >= this.buffer.size)
            flush()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun putIntMed(i: Int) {
        if (this.currentPosition * -781132199 + 3 >= this.buffer.size)
            flush()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 16).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 8).toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = i.toByte()
        this.currentPosition += 1787333097
        this.buffer[this.currentPosition * -781132199 - 1] = (i shr 24).toByte()
    }

    fun writeFloatBE(f: Float) {
        if (3 + -781132199 * this.currentPosition >= this.buffer.size)
            flush()
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

    fun writeFloatLE(f: Float) {
        if (3 + this.currentPosition * -781132199 >= this.buffer.size)
            flush()
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

    fun flushBuffer() {
        if (this.currentPosition * -781132199 > 0) {
            if (this.bufferOffset * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
                throw RuntimeException()
            this.targetBuffer!!.put(this.buffer, 0, this.bufferOffset * 1160648251, -781132199 * this.currentPosition)
            this.bufferOffset += this.currentPosition * 1269928059
            this.currentPosition = 0
        }
    }

    fun flushToTarget() {
        if (this.currentPosition * -781132199 > 0) {
            if (this.bufferOffset * 1160648251 + -781132199 * this.currentPosition > -863312807 * this.capacity)
                throw RuntimeException()
            this.targetBuffer!!.put(this.buffer, 0, this.bufferOffset * 1160648251, -781132199 * this.currentPosition)
            this.bufferOffset += this.currentPosition * 1269928059
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
