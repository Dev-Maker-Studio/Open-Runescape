package com.jagex.game.runetek5.tools

/**
 * ISAAC (Indirection, Shift, Accumulate, Add, and Count) random number generator
 *
 * @author Steven - 29/07/2023
 * Reference: https://gist.github.com/bl00dshooter/995695
 */
class Isaac(seed: IntArray) {

    @JvmField
    var count: Int = 0

    @JvmField
    var counter: Int = 0

    @JvmField
    var lastResult: Int = 0

    @JvmField
    var accumulator: Int = 0

    @JvmField
    val memory = IntArray(256)

    @JvmField
    val results = IntArray(256)

    init {
        System.arraycopy(seed, 0, results, 0, seed.size)
        init()
    }

    fun init() {
        var a: Int
        var b: Int
        var c: Int
        var d: Int
        var e: Int
        var f: Int
        var g: Int
        var h: Int
        h = RATIO
        g = h
        f = g
        e = f
        d = e
        c = d
        b = c
        a = b

        for (i in 0 until 4) {
            h = h xor (g shl 11)
            e += h
            g += f
            g = g xor (f ushr 2)
            d += g
            f += e
            f = f xor (e shl 8)
            c += f
            e += d
            e = e xor (d ushr 16)
            b += e
            d += c
            d = d xor (c shl 10)
            a += d
            c += b
            c = c xor (b ushr 4)
            h += c
            b += a
            b = b xor (a shl 8)
            g += b
            a += h
            a = a xor (h ushr 9)
            f += a
            h += g
        }

        var i = 0
        while (i < 256) {
            h += results[i]
            g += results[i + 1]
            f += results[i + 2]
            e += results[i + 3]
            d += results[i + 4]
            c += results[i + 5]
            b += results[i + 6]
            a += results[i + 7]
            h = h xor (g shl 11)
            e += h
            g += f
            g = g xor (f ushr 2)
            d += g
            f += e
            f = f xor (e shl 8)
            c += f
            e += d
            e = e xor (d ushr 16)
            b += e
            d += c
            d = d xor (c shl 10)
            a += d
            c += b
            c = c xor (b ushr 4)
            h += c
            b += a
            b = b xor (a shl 8)
            g += b
            a += h
            a = a xor (h ushr 9)
            f += a
            h += g
            memory[i] = h
            memory[i + 1] = g
            memory[i + 2] = f
            memory[i + 3] = e
            memory[i + 4] = d
            memory[i + 5] = c
            memory[i + 6] = b
            memory[i + 7] = a
            i += 8
        }

        i = 0
        while (i < 256) {
            h += memory[i]
            g += memory[i + 1]
            f += memory[i + 2]
            e += memory[i + 3]
            d += memory[i + 4]
            c += memory[i + 5]
            b += memory[i + 6]
            a += memory[i + 7]
            h = h xor (g shl 11)
            e += h
            g += f
            g = g xor (f ushr 2)
            d += g
            f += e
            f = f xor (e shl 8)
            c += f
            e += d
            e = e xor (d ushr 16)
            b += e
            d += c
            d = d xor (c shl 10)
            a += d
            c += b
            c = c xor (b ushr 4)
            h += c
            b += a
            b = b xor (a shl 8)
            g += b
            a += h
            a = a xor (h ushr 9)
            f += a
            h += g
            memory[i] = h
            memory[i + 1] = g
            memory[i + 2] = f
            memory[i + 3] = e
            memory[i + 4] = d
            memory[i + 5] = c
            memory[i + 6] = b
            memory[i + 7] = a
            i += 8
        }

        isaac()
        count = 256
    }

    fun getNextKey(): Int {
        if (count == 0) {
            isaac()
            count = 256
        }
        count -= 1
        return results[count]
    }

    fun nextKey(): Int {
        if (count == 0) {
            isaac()
            count = 256
        }
        return results[count - 1]
    }

    fun isaac() {
        lastResult += ++counter
        for (i in 0 until 256) {
            val j = memory[i]
            if ((i and 0x2) == 0) {
                if ((i and 0x1) == 0) {
                    accumulator = accumulator xor (accumulator shl 13)
                } else {
                    accumulator = accumulator xor (accumulator ushr 6)
                }
            } else if ((i and 0x1) == 0) {
                accumulator = accumulator xor (accumulator shl 2)
            } else {
                accumulator = accumulator xor (accumulator ushr 16)
            }
            accumulator += memory[128 + i and 0xff]
            val k: Int
            memory[i] = run {
                k = memory[(j and 0x3fc) shr 2] + lastResult + accumulator
                k
            }
            results[i] = run {
                lastResult = memory[(k shr 8 and 0x3fc) shr 2] + j
                lastResult
            }
        }
    }

    companion object {
        private const val RATIO = -1640531527
    }
}
