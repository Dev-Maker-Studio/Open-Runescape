package com.jagex.game.runetek5.cache

import java.io.EOFException
import java.io.IOException

class FileOnDisk(
    @JvmField val fs32: JFS32,
    capacity: Int
) {

    @JvmField
    var currentPosition: Long = 0L

    @JvmField
    var aLong5351: Long = 0L

    @JvmField
    var initialCapacity: Long

    @JvmField
    var anInt5356: Int = 0

    @JvmField
    var aLong5345: Long = -1L

    @JvmField
    var occupiedSize: Long

    @JvmField
    var aLong5347: Long = -1L

    @JvmField
    var aByteArray5350: ByteArray

    @JvmField
    var aByteArray5348: ByteArray

    @JvmField
    var anInt5349: Int = 0

    init {
        initialCapacity = fs32.length()
        occupiedSize = initialCapacity
        aByteArray5350 = ByteArray(capacity)
        aByteArray5348 = ByteArray(0)
        currentPosition = 0L
    }

    fun getInitialSize(): Long {
        return initialCapacity
    }

    @Throws(IOException::class)
    fun close() {
        method7353()
        fs32.close()
    }

    @Throws(IOException::class)
    fun setPosition(position: Long) {
        if (position < 0L) {
            throw IOException()
        }
        currentPosition = position
    }

    @Throws(IOException::class)
    fun load0(buffer: ByteArray, i_2: Int, i_3: Int) {
        var i_2_mod = i_2
        var i_3_mod = i_3
        try {
            if (i_3_mod + i_2_mod > buffer.size) {
                throw ArrayIndexOutOfBoundsException(i_3_mod + i_2_mod - buffer.size)
            }

            if (aLong5345 != -1L && currentPosition >= aLong5345 && currentPosition + i_3_mod <= (anInt5356 * 675653231L) + aLong5345) {
                System.arraycopy(aByteArray5348, (currentPosition - aLong5345).toInt(), buffer, i_2_mod, i_3_mod)
                currentPosition += i_3_mod
                return
            }

            val long_5 = currentPosition
            val i_8 = i_3_mod
            var i_9: Int
            if (currentPosition >= aLong5347 && currentPosition < anInt5349 + aLong5347) {
                i_9 = (anInt5349 - (currentPosition - aLong5347)).toInt()
                if (i_9 > i_3_mod) {
                    i_9 = i_3_mod
                }

                System.arraycopy(aByteArray5350, (currentPosition - aLong5347).toInt(), buffer, i_2_mod, i_9)
                currentPosition += i_9
                i_2_mod += i_9
                i_3_mod -= i_9
            }

            if (i_3_mod > aByteArray5350.size) {
                fs32.seek(currentPosition)

                aLong5351 = currentPosition
                while (i_3_mod > 0) {
                    i_9 = fs32.getSeekOffset(buffer, i_2_mod, i_3_mod)
                    if (i_9 == -1) {
                        break
                    }

                    aLong5351 += i_9
                    currentPosition += i_9
                    i_2_mod += i_9
                    i_3_mod -= i_9
                }
            } else if (i_3_mod > 0) {
                seek()
                i_9 = kotlin.math.min(i_3_mod, anInt5349)

                System.arraycopy(aByteArray5350, 0, buffer, i_2_mod, i_9)
                i_2_mod += i_9
                i_3_mod -= i_9
                currentPosition += i_9
            }

            if (aLong5345 != -1L) {
                if (aLong5345 > currentPosition && i_3_mod > 0) {
                    i_9 = i_2_mod + (aLong5345 - currentPosition).toInt()
                    if (i_9 > i_3_mod + i_2_mod) {
                        i_9 = i_3_mod + i_2_mod
                    }

                    while (i_2_mod < i_9) {
                        buffer[i_2_mod++] = 0
                        --i_3_mod
                        ++currentPosition
                    }
                }

                var long_14 = -1L
                var long_11 = -1L
                if (aLong5345 >= long_5 && aLong5345 < i_8 + long_5) {
                    long_14 = aLong5345
                } else if (long_5 >= aLong5345 && long_5 < (anInt5356 * 675653231L) + aLong5345) {
                    long_14 = long_5
                }

                if ((anInt5356 * 675653231L) + aLong5345 > long_5 && (anInt5356 * 675653231L) + aLong5345 <= i_8 + long_5) {
                    long_11 = (anInt5356 * 675653231L) + aLong5345
                } else if (i_8 + long_5 > aLong5345 && long_5 + i_8 <= aLong5345 + (anInt5356 * 675653231L)) {
                    long_11 = i_8 + long_5
                }

                if (long_14 > -1L && long_11 > long_14) {
                    val i_13 = (long_11 - long_14).toInt()
                    System.arraycopy(aByteArray5348, (long_14 - aLong5345).toInt(), buffer, (long_14 - long_5).toInt() + i_2_mod, i_13)
                    if (long_11 > currentPosition) {
                        i_3_mod = (i_3_mod - (long_11 - currentPosition)).toInt()
                        currentPosition = long_11
                    }
                }
            }
        } catch (ioexception17: IOException) {
            aLong5351 = -1L
            throw ioexception17
        }

        if (i_3_mod > 0) {
            throw EOFException()
        }
    }

    @Throws(IOException::class)
    fun load(bytes_1: ByteArray) {
        load0(bytes_1, 0, bytes_1.size)
    }

    @Throws(IOException::class)
    fun write(bytes_1: ByteArray, i_2: Int, i_3: Int) {
        try {
            var i_2_mod = i_2
            var i_3_mod = i_3
            if (i_3_mod + currentPosition > initialCapacity) {
                initialCapacity = currentPosition + i_3_mod
            }

            if (aLong5345 != -1L && (currentPosition < aLong5345 || currentPosition > (anInt5356 * 675653231L) + aLong5345)) {
                method7353()
            }

            if (aLong5345 != -1L && currentPosition + i_3_mod > aLong5345 + aByteArray5348.size) {
                val i_5 = (aByteArray5348.size - (currentPosition - aLong5345)).toInt()
                System.arraycopy(bytes_1, i_2_mod, aByteArray5348, (currentPosition - aLong5345).toInt(), i_5)
                currentPosition += i_5
                i_2_mod += i_5
                i_3_mod -= i_5
                anInt5356 = aByteArray5348.size * -1070827377
                method7353()
            }

            if (i_3_mod > aByteArray5348.size) {
                if (currentPosition != aLong5351) {
                    fs32.seek(currentPosition)
                    aLong5351 = currentPosition
                }

                fs32.write(bytes_1, i_2_mod, i_3_mod)
                aLong5351 += i_3_mod
                if (aLong5351 > occupiedSize) {
                    occupiedSize = aLong5351
                }

                var long_10 = -1L
                var long_7 = -1L
                if (currentPosition >= aLong5347 && currentPosition < aLong5347 + anInt5349) {
                    long_10 = currentPosition
                } else if (aLong5347 >= currentPosition && aLong5347 < i_3_mod + currentPosition) {
                    long_10 = aLong5347
                }

                if (i_3_mod + currentPosition > aLong5347 && currentPosition + i_3_mod <= aLong5347 + anInt5349) {
                    long_7 = i_3_mod + currentPosition
                } else if (aLong5347 + anInt5349 > currentPosition && aLong5347 + anInt5349 <= currentPosition + i_3_mod) {
                    long_7 = anInt5349 + aLong5347
                }

                if (long_10 > -1L && long_7 > long_10) {
                    val i_9 = (long_7 - long_10).toInt()
                    System.arraycopy(bytes_1, (long_10 + i_2_mod - currentPosition).toInt(), aByteArray5350, (long_10 - aLong5347).toInt(), i_9)
                }

                currentPosition += i_3_mod
            } else if (i_3_mod > 0) {
                if (aLong5345 == -1L) {
                    aLong5345 = currentPosition
                }

                System.arraycopy(bytes_1, i_2_mod, aByteArray5348, (currentPosition - aLong5345).toInt(), i_3_mod)
                currentPosition += i_3_mod
                if (currentPosition - aLong5345 > (anInt5356 * 675653231L)) {
                    anInt5356 = ((currentPosition - aLong5345).toInt() * -1070827377)
                }
            }

        } catch (ioexception13: IOException) {
            aLong5351 = -1L
            throw ioexception13
        }
    }

    @Throws(IOException::class)
    fun method7353() {
        if (aLong5345 != -1L) {
            if (aLong5345 != aLong5351) {
                fs32.seek(aLong5345)
                aLong5351 = aLong5345
            }
            fs32.write(aByteArray5348, 0, anInt5356 * 675653231)
            aLong5351 += 675653231L * anInt5356
            if (aLong5351 > occupiedSize) {
                occupiedSize = aLong5351
            }
            var long_2 = -1L
            var long_4 = -1L
            if (aLong5345 >= aLong5347 && aLong5345 < anInt5349 + aLong5347) {
                long_2 = aLong5345
            } else if (aLong5347 >= aLong5345 && aLong5347 < aLong5345 + (anInt5356 * 675653231L)) {
                long_2 = aLong5347
            }
            if ((anInt5356 * 675653231L) + aLong5345 > aLong5347 && (anInt5356 * 675653231L) + aLong5345 <= anInt5349 + aLong5347) {
                long_4 = (anInt5356 * 675653231L) + aLong5345
            } else if (aLong5347 + anInt5349 > aLong5345 && anInt5349 + aLong5347 <= aLong5345 + (anInt5356 * 675653231L)) {
                long_4 = aLong5347 + anInt5349
            }
            if (long_2 > -1L && long_4 > long_2) {
                val i_6 = (long_4 - long_2).toInt()
                System.arraycopy(aByteArray5348, (long_2 - aLong5345).toInt(), aByteArray5350, (long_2 - aLong5347).toInt(), i_6)
            }
            aLong5345 = -1L
            anInt5356 = 0
        }
    }

    @Throws(IOException::class)
    fun seek() {
        anInt5349 = 0
        if (currentPosition != aLong5351) {
            fs32.seek(currentPosition)
            aLong5351 = currentPosition
        }
        var offset: Int
        aLong5347 = currentPosition
        while (anInt5349 < aByteArray5350.size) {
            var length = aByteArray5350.size - anInt5349
            if (length > 200000000) {
                length = 200000000
            }
            offset = fs32.getSeekOffset(aByteArray5350, anInt5349, length)
            if (offset == -1) {
                break
            }
            aLong5351 += offset
            anInt5349 += offset
        }
    }

    companion object {
        @JvmField
        var anInt5357: Int = 0

        @JvmField
        var cacheFileIndices: Array<FileOnDisk?>? = null
    }
}
