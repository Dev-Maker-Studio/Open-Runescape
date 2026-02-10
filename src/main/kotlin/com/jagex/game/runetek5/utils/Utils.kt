package com.jagex.game.runetek5.utils

object Utils {

    @JvmField
    var lastSystemTime: Long = 0

    @JvmField
    var timeOffset: Long = 0

    @JvmStatic
    fun cp1252ToChar(b: Byte): Char {
        var `val` = b.toInt() and 0xff
        if (`val` == 0) {
            throw IllegalArgumentException(Integer.toString(0, 16))
        } else {
            if (`val` >= 128 && `val` < 160) {
                var charValue = UNICODE_TABLE[`val` - 128]
                if (charValue.code == 0) {
                    charValue = 63.toChar()
                }
                `val` = charValue.code
            }
            return `val`.toChar()
        }
    }

    @JvmStatic
    fun readString(bytes_0: ByteArray, i_1: Int, i_2: Int): String {
        val arr_4 = CharArray(i_2)
        var i_5 = 0
        for (i_6 in 0 until i_2) {
            var i_7 = bytes_0[i_6 + i_1].toInt() and 0xff
            if (i_7 != 0) {
                if (i_7 >= 128 && i_7 < 160) {
                    var var_8 = UNICODE_TABLE[i_7 - 128]
                    if (var_8.code == 0) {
                        var_8 = 63.toChar()
                    }
                    i_7 = var_8.code
                }
                arr_4[i_5++] = i_7.toChar()
            }
        }
        return String(arr_4, 0, i_5)
    }

    @JvmStatic
    fun stringLengthPlus2(string_0: String?): Int {
        return (string_0?.length ?: 0) + 2
    }

    @JvmStatic
    fun calculateKerning(glyphWidths: Array<ByteArray>, glyphHeights: Array<ByteArray>, glyphX: IntArray, kerning: ByteArray, glyphY: IntArray, start: Int, end: Int): Int {
        val x1 = glyphX[start]
        val x2 = x1 + glyphY[start]
        val x3 = glyphX[end]
        val x4 = x3 + glyphY[end]
        var minX = x1
        if (x3 > x1) {
            minX = x3
        }
        var maxX = x2
        if (x4 < x2) {
            maxX = x4
        }
        var minKerning = kerning[start].toInt() and 0xff
        if ((kerning[end].toInt() and 0xff) < minKerning) {
            minKerning = kerning[end].toInt() and 0xff
        }
        val heights = glyphHeights[start]
        val widths = glyphWidths[end]
        var offX = minX - x1
        var offY = minX - x3
        for (i in minX until maxX) {
            val i_20 = heights[offX++] + widths[offY++]
            if (i_20 < minKerning) {
                minKerning = i_20
            }
        }
        return -minKerning
    }

    @JvmStatic
    @Synchronized
    fun time(): Long {
        val long_1 = System.currentTimeMillis()
        if (long_1 < lastSystemTime) {
            timeOffset += lastSystemTime - long_1
        }
        lastSystemTime = long_1
        return long_1 + timeOffset
    }

    @JvmStatic
    fun parseInt(charsequence_0: CharSequence): Int {
        return parseInt(charsequence_0, 10)
    }

    @JvmStatic
    fun parseInt(charsequence_0: CharSequence, i_1: Int): Int {
        if (i_1 >= 2 && i_1 <= 36) {
            var bool4 = false
            var bool5 = false
            var i_6 = 0
            val i_7 = charsequence_0.length
            for (i_8 in 0 until i_7) {
                val var_9 = charsequence_0[i_8]
                if (i_8 == 0) {
                    if (var_9 == '-') {
                        bool4 = true
                        continue
                    }
                    if (var_9 == '+') {
                        continue
                    }
                }
                val i_11: Int
                if (var_9 in '0'..'9') {
                    i_11 = var_9.code - 48
                } else if (var_9 in 'A'..'Z') {
                    i_11 = var_9.code - 55
                } else {
                    if (var_9 < 'a' || var_9 > 'z') {
                        throw NumberFormatException()
                    }
                    i_11 = var_9.code - 87
                }
                if (i_11 >= i_1) {
                    throw NumberFormatException()
                }
                var i_11_final = i_11
                if (bool4) {
                    i_11_final = -i_11
                }
                val i_10 = i_11_final + i_6 * i_1
                if (i_10 / i_1 != i_6) {
                    throw NumberFormatException()
                }
                i_6 = i_10
                bool5 = true
            }
            if (!bool5) {
                throw NumberFormatException()
            } else {
                return i_6
            }
        } else {
            throw IllegalArgumentException("" + i_1)
        }
    }

    @JvmStatic
    fun getStringFromLong(long_0: Long): String? {
        var long_0_var = long_0
        if (long_0_var > 0L && long_0_var < 6582952005840035281L) {
            if (long_0_var % 37L == 0L) {
                return null
            } else {
                var i_2 = 0
                var long_3 = long_0_var
                while (long_3 != 0L) {
                    long_3 /= 37L
                    ++i_2
                }
                val stringbuilder6 = StringBuilder(i_2)
                while (long_0_var != 0L) {
                    val long_4 = long_0_var
                    long_0_var /= 37L
                    stringbuilder6.append(aCharArray5825[(long_4 - 37L * long_0_var).toInt()])
                }
                return stringbuilder6.reverse().toString()
            }
        } else {
            return null
        }
    }

    @JvmStatic
    fun countOccurrences(charsequence_0: CharSequence, var_1: Char): Int {
        var i_3 = 0
        val i_4 = charsequence_0.length
        for (i_5 in 0 until i_4) {
            if (charsequence_0[i_5] == var_1) {
                ++i_3
            }
        }
        return i_3
    }

    @JvmStatic
    fun hashAsLong(charsequence_0: CharSequence): Long {
        val i_2 = charsequence_0.length
        var long_3 = 0L
        for (i_5 in 0 until i_2) {
            long_3 = (long_3 shl 5) - long_3 + charsequence_0[i_5].code
        }
        return long_3
    }

    @JvmStatic
    fun stringToInt(charsequence_0: CharSequence): Int {
        val i_2 = charsequence_0.length
        var i_3 = 0
        for (i_4 in 0 until i_2) {
            i_3 = (i_3 shl 5) - i_3 + charsequence_0[i_4].code
        }
        return i_3
    }

    @JvmStatic
    fun isValidInteger(charsequence_0: CharSequence): Boolean {
        var bool4 = false
        var bool5 = false
        var i_6 = 0
        val i_7 = charsequence_0.length
        for (i_8 in 0 until i_7) {
            val var_9 = charsequence_0[i_8]
            if (i_8 == 0) {
                if (var_9 == '-') {
                    bool4 = true
                    continue
                }
                if (var_9 == '+') {
                    continue
                }
            }
            val i_11: Int
            if (var_9 in '0'..'9') {
                i_11 = var_9.code - 48
            } else if (var_9 in 'A'..'Z') {
                i_11 = var_9.code - 55
            } else {
                if (var_9 < 'a' || var_9 > 'z') {
                    return false
                }
                i_11 = var_9.code - 87
            }
            if (i_11 >= 10) {
                return false
            }
            var i_11_final = i_11
            if (bool4) {
                i_11_final = -i_11
            }
            val i_10 = i_11_final + i_6 * 10
            if (i_10 / 10 != i_6) {
                return false
            }
            i_6 = i_10
            bool5 = true
        }
        return bool5
    }

    @JvmStatic
    fun charseqTouid(charsequence_0: CharSequence): Long {
        var long_2 = 0L
        val i_4 = charsequence_0.length
        for (i_5 in 0 until i_4) {
            long_2 *= 37L
            val var_6 = charsequence_0[i_5]
            if (var_6 in 'A'..'Z') {
                long_2 += var_6.code + 1 - 65
            } else if (var_6 in 'a'..'z') {
                long_2 += var_6.code + 1 - 97
            } else if (var_6 in '0'..'9') {
                long_2 += var_6.code + 27 - 48
            }
            if (long_2 >= 177917621779460413L) {
                break
            }
        }
        while (long_2 % 37L == 0L && long_2 != 0L) {
            long_2 /= 37L
        }
        return long_2
    }

    @JvmStatic
    fun getLigatureReplacement(var_0: Char): Char {
        return when (var_0) {
            198.toChar() -> 'E'
            230.toChar() -> 'e'
            223.toChar() -> 's'
            338.toChar() -> 'E'
            339.toChar() -> 'e'
            else -> '\u0000'
        }
    }

    @JvmStatic
    fun formatRequestParameter(charsequence_0: CharSequence): String {
        val i_2 = charsequence_0.length
        val stringbuilder3 = StringBuilder(i_2)
        for (i_4 in 0 until i_2) {
            val var_5 = charsequence_0[i_4]
            if ((var_5 < 'a' || var_5 > 'z') && (var_5 < 'A' || var_5 > 'Z') && (var_5 < '0' || var_5 > '9') && var_5 != '.' && var_5 != '-' && var_5 != '*' && var_5 != '_') {
                if (var_5 == ' ') {
                    stringbuilder3.append('+')
                } else {
                    val b_6 = byteForChar(var_5)
                    stringbuilder3.append('%')
                    var i_7 = b_6.toInt() shr 4 and 0xf
                    if (i_7 >= 10) {
                        stringbuilder3.append((i_7 + 55).toChar())
                    } else {
                        stringbuilder3.append((i_7 + 48).toChar())
                    }
                    i_7 = b_6.toInt() and 0xf
                    if (i_7 >= 10) {
                        stringbuilder3.append((i_7 + 55).toChar())
                    } else {
                        stringbuilder3.append((i_7 + 48).toChar())
                    }
                }
            } else {
                stringbuilder3.append(var_5)
            }
        }
        return stringbuilder3.toString()
    }

    @JvmStatic
    fun getCollationKey(var_0: Char): Int {
        var i_3 = var_0.code shl 4
        if (Character.isUpperCase(var_0) || Character.isTitleCase(var_0)) {
            val var_4 = Character.toLowerCase(var_0)
            i_3 = (var_4.code shl 4) + 1
        }
        return i_3
    }

    @JvmStatic
    fun decodeWithUnicodeTable(bytes_0: ByteArray): String {
        return readString(bytes_0, 0, bytes_0.size)
    }

    @JvmStatic
    fun joinStrings(arr_0: Array<Any?>, i_1: Int, i_2: Int): String {
        if (i_2 == 0) {
            return ""
        } else if (i_2 == 1) {
            val charsequence_4 = arr_0[i_1] as CharSequence?
            return charsequence_4?.toString() ?: "null"
        } else {
            val i_9 = i_2 + i_1
            var i_5 = 0
            for (i_6 in i_1 until i_9) {
                val charsequence_11 = arr_0[i_6] as CharSequence?
                if (charsequence_11 == null) {
                    i_5 += 4
                } else {
                    i_5 += charsequence_11.length
                }
            }
            val stringbuilder10 = StringBuilder(i_5)
            for (i_7 in i_1 until i_9) {
                val charsequence_8 = arr_0[i_7] as CharSequence?
                if (charsequence_8 == null) {
                    stringbuilder10.append("null")
                } else {
                    stringbuilder10.append(charsequence_8)
                }
            }
            return stringbuilder10.toString()
        }
    }

    @JvmStatic
    fun split(string_0: String, var_1: Char): Array<String> {
        val i_3 = countOccurrences(string_0, var_1)
        val arr_4 = Array(i_3 + 1) { "" }
        var i_5 = 0
        var i_6 = 0

        for (i_7 in 0 until i_3) {
            var i_8 = i_6
            while (string_0[i_8] != var_1) {
                i_8++
            }
            arr_4[i_5++] = string_0.substring(i_6, i_8)
            i_6 = i_8 + 1
        }

        arr_4[i_3] = string_0.substring(i_6)
        return arr_4
    }

    @JvmStatic
    fun byteForChar(c: Char): Byte {
        val res: Byte
        if ((c.code <= 0 || c.code >= 128) && (c.code < 160 || c.code > 255)) {
            res = when (c.code) {
                8364 -> -128
                8218 -> -126
                402 -> -125
                8222 -> -124
                8230 -> -123
                8224 -> -122
                8225 -> -121
                710 -> -120
                8240 -> -119
                352 -> -118
                8249 -> -117
                338 -> -116
                381 -> -114
                8216 -> -111
                8217 -> -110
                8220 -> -109
                8221 -> -108
                8226 -> -107
                8211 -> -106
                8212 -> -105
                732 -> -104
                8482 -> -103
                353 -> -102
                8250 -> -101
                339 -> -100
                382 -> -98
                376 -> -97
                else -> 63
            }
        } else {
            res = c.code.toByte()
        }
        return res
    }

    /**
     * @deprecated Use [isWrongJava] instead
     */
    @JvmStatic
    fun isValidIntegerDeprecated(charsequence_0: CharSequence): Boolean {
        return isWrongJava(charsequence_0)
    }

    @JvmStatic
    fun isWrongJava(charsequence_0: CharSequence): Boolean {
        return isValidInteger(charsequence_0)
    }

    @JvmStatic
    fun characterSequenceToBytearray(charsequence_0: CharSequence): ByteArray {
        val i_2 = charsequence_0.length
        val bytes_3 = ByteArray(i_2)
        for (i_4 in 0 until i_2) {
            val var_5 = charsequence_0[i_4]
            if (var_5.code > 0 && var_5.code < 128 || var_5.code >= 160 && var_5.code <= 255) {
                bytes_3[i_4] = var_5.code.toByte()
            } else when (var_5.code) {
                8364 -> bytes_3[i_4] = -128
                8218 -> bytes_3[i_4] = -126
                402 -> bytes_3[i_4] = -125
                8222 -> bytes_3[i_4] = -124
                8230 -> bytes_3[i_4] = -123
                8224 -> bytes_3[i_4] = -122
                8225 -> bytes_3[i_4] = -121
                710 -> bytes_3[i_4] = -120
                8240 -> bytes_3[i_4] = -119
                352 -> bytes_3[i_4] = -118
                8249 -> bytes_3[i_4] = -117
                338 -> bytes_3[i_4] = -116
                381 -> bytes_3[i_4] = -114
                8216 -> bytes_3[i_4] = -111
                8217 -> bytes_3[i_4] = -110
                8220 -> bytes_3[i_4] = -109
                8221 -> bytes_3[i_4] = -108
                8226 -> bytes_3[i_4] = -107
                8211 -> bytes_3[i_4] = -106
                8212 -> bytes_3[i_4] = -105
                732 -> bytes_3[i_4] = -104
                8482 -> bytes_3[i_4] = -103
                353 -> bytes_3[i_4] = -102
                8250 -> bytes_3[i_4] = -101
                339 -> bytes_3[i_4] = -100
                382 -> bytes_3[i_4] = -98
                376 -> bytes_3[i_4] = -97
                else -> bytes_3[i_4] = 63
            }
        }
        return bytes_3
    }

    @JvmStatic
    fun sortAlphabetically(names: Array<String>, ids: ShortArray?) {
        if (ids != null) {
            @Suppress("UNCHECKED_CAST")
            ArrayUtils.quicksort(names as Array<String?>, ids, 0, names.size - 1)
        }
    }

    @JvmStatic
    fun formatScript(fullProgramString: String, carriageReturn: Char, string_2: String): String {
        val fullLength = fullProgramString.length
        val i_5 = string_2.length
        var i_6 = fullLength
        val i_7 = i_5 - 1
        if (i_7 != 0) {
            var i_8 = 0
            while (true) {
                i_8 = fullProgramString.indexOf(carriageReturn, i_8)
                if (i_8 < 0) {
                    break
                }
                ++i_8
                i_6 += i_7
            }
        }
        val stringbuilder11 = StringBuilder(i_6)
        var i_9 = 0
        while (true) {
            val i_10 = fullProgramString.indexOf(carriageReturn, i_9)
            if (i_10 < 0) {
                stringbuilder11.append(fullProgramString.substring(i_9))
                return stringbuilder11.toString()
            }
            stringbuilder11.append(fullProgramString, i_9, i_10)
            stringbuilder11.append(string_2)
            i_9 = i_10 + 1
        }
    }

    @JvmField
    val UNICODE_TABLE = charArrayOf('\u20ac', '\u0000', '\u201a', '\u0192', '\u201e', '\u2026', '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160', '\u2039', '\u0152', '\u0000', '\u017d', '\u0000', '\u0000', '\u2018', '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014', '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u0000', '\u017e', '\u0178')

    @JvmStatic
    fun belowZero(integer: Int): Boolean {
        return integer < 0
    }

    @JvmStatic
    fun equalOrAboveZero(integer: Int): Boolean {
        return integer >= 0
    }

    @JvmField
    val aCharArray5825 = charArrayOf('_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    @JvmStatic
    fun parse_int(charsequence_0: CharSequence): Int {
        return parseInt(charsequence_0, 16)
    }

    @JvmStatic
    fun isNumber(character: Char): Boolean {
        return character >= '0' && character <= '9'
    }
}
