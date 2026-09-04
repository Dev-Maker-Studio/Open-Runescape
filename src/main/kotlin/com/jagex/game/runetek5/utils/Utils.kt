package com.jagex.game.runetek5.utils

object Utils {

    @JvmField
    var lastSystemTime: Long = 0

    @JvmField
    var timeOffset: Long = 0

    @JvmStatic
    fun cp1252ToChar(b: Byte): Char {
        var codePoint = b.toInt() and 0xff
        if (codePoint == 0) {
            throw IllegalArgumentException(Integer.toString(0, 16))
        } else {
            if (codePoint >= 128 && codePoint < 160) {
                var charValue = UNICODE_TABLE[codePoint - 128]
                if (charValue.code == 0) {
                    charValue = 63.toChar()
                }
                codePoint = charValue.code
            }
            return codePoint.toChar()
        }
    }

    @JvmStatic
    fun readString(data: ByteArray, offset: Int, length: Int): String {
        val chars = CharArray(length)
        var charIndex = 0
        for (i in 0 until length) {
            var codePoint = data[i + offset].toInt() and 0xff
            if (codePoint != 0) {
                if (codePoint >= 128 && codePoint < 160) {
                    var mappedChar = UNICODE_TABLE[codePoint - 128]
                    if (mappedChar.code == 0) {
                        mappedChar = 63.toChar()
                    }
                    codePoint = mappedChar.code
                }
                chars[charIndex++] = codePoint.toChar()
            }
        }
        return String(chars, 0, charIndex)
    }

    @JvmStatic
    fun stringLengthPlus2(str: String?): Int {
        return (str?.length ?: 0) + 2
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
            val gap = heights[offX++] + widths[offY++]
            if (gap < minKerning) {
                minKerning = gap
            }
        }
        return -minKerning
    }

    @JvmStatic
    @Synchronized
    fun time(): Long {
        val currentTime = System.currentTimeMillis()
        if (currentTime < lastSystemTime) {
            timeOffset += lastSystemTime - currentTime
        }
        lastSystemTime = currentTime
        return currentTime + timeOffset
    }

    @JvmStatic
    fun parseInt(text: CharSequence): Int {
        return parseInt(text, 10)
    }

    @JvmStatic
    fun parseInt(text: CharSequence, radix: Int): Int {
        if (radix >= 2 && radix <= 36) {
            var isNegative = false
            var hasDigits = false
            var result = 0
            val len = text.length
            for (i in 0 until len) {
                val ch = text[i]
                if (i == 0) {
                    if (ch == '-') {
                        isNegative = true
                        continue
                    }
                    if (ch == '+') {
                        continue
                    }
                }
                val digitValue: Int
                if (ch in '0'..'9') {
                    digitValue = ch.code - 48
                } else if (ch in 'A'..'Z') {
                    digitValue = ch.code - 55
                } else {
                    if (ch < 'a' || ch > 'z') {
                        throw NumberFormatException()
                    }
                    digitValue = ch.code - 87
                }
                if (digitValue >= radix) {
                    throw NumberFormatException()
                }
                var signedDigit = digitValue
                if (isNegative) {
                    signedDigit = -digitValue
                }
                val accumulated = signedDigit + result * radix
                if (accumulated / radix != result) {
                    throw NumberFormatException()
                }
                result = accumulated
                hasDigits = true
            }
            if (!hasDigits) {
                throw NumberFormatException()
            } else {
                return result
            }
        } else {
            throw IllegalArgumentException("" + radix)
        }
    }

    @JvmStatic
    fun nameForLong(encoded: Long): String? {
        var remaining = encoded
        if (remaining > 0L && remaining < 6582952005840035281L) {
            if (remaining % 37L == 0L) {
                return null
            } else {
                var charCount = 0
                var temp = remaining
                while (temp != 0L) {
                    temp /= 37L
                    ++charCount
                }
                val builder = StringBuilder(charCount)
                while (remaining != 0L) {
                    val previous = remaining
                    remaining /= 37L
                    builder.append(BASE37_ALPHABET[(previous - 37L * remaining).toInt()])
                }
                return builder.reverse().toString()
            }
        } else {
            return null
        }
    }

    @JvmStatic
    fun countOccurrences(text: CharSequence, target: Char): Int {
        var count = 0
        val len = text.length
        for (i in 0 until len) {
            if (text[i] == target) {
                ++count
            }
        }
        return count
    }

    @JvmStatic
    fun hashAsLong(text: CharSequence): Long {
        val len = text.length
        var hash = 0L
        for (i in 0 until len) {
            hash = (hash shl 5) - hash + text[i].code
        }
        return hash
    }

    @JvmStatic
    fun stringToInt(text: CharSequence): Int {
        val len = text.length
        var hash = 0
        for (i in 0 until len) {
            hash = (hash shl 5) - hash + text[i].code
        }
        return hash
    }

    @JvmStatic
    fun isValidInteger(text: CharSequence): Boolean {
        var isNegative = false
        var hasDigits = false
        var result = 0
        val len = text.length
        for (i in 0 until len) {
            val ch = text[i]
            if (i == 0) {
                if (ch == '-') {
                    isNegative = true
                    continue
                }
                if (ch == '+') {
                    continue
                }
            }
            val digitValue: Int
            if (ch in '0'..'9') {
                digitValue = ch.code - 48
            } else if (ch in 'A'..'Z') {
                digitValue = ch.code - 55
            } else {
                if (ch < 'a' || ch > 'z') {
                    return false
                }
                digitValue = ch.code - 87
            }
            if (digitValue >= 10) {
                return false
            }
            var signedDigit = digitValue
            if (isNegative) {
                signedDigit = -digitValue
            }
            val accumulated = signedDigit + result * 10
            if (accumulated / 10 != result) {
                return false
            }
            result = accumulated
            hasDigits = true
        }
        return hasDigits
    }

    @JvmStatic
    fun nameToLong(text: CharSequence): Long {
        var uid = 0L
        val len = text.length
        for (i in 0 until len) {
            uid *= 37L
            val ch = text[i]
            if (ch in 'A'..'Z') {
                uid += ch.code + 1 - 65
            } else if (ch in 'a'..'z') {
                uid += ch.code + 1 - 97
            } else if (ch in '0'..'9') {
                uid += ch.code + 27 - 48
            }
            if (uid >= 177917621779460413L) {
                break
            }
        }
        while (uid % 37L == 0L && uid != 0L) {
            uid /= 37L
        }
        return uid
    }

    @JvmStatic
    fun getLigatureReplacement(ch: Char): Char {
        return when (ch) {
            198.toChar() -> 'E'
            230.toChar() -> 'e'
            223.toChar() -> 's'
            338.toChar() -> 'E'
            339.toChar() -> 'e'
            else -> '\u0000'
        }
    }

    @JvmStatic
    fun formatRequestParameter(text: CharSequence): String {
        val len = text.length
        val builder = StringBuilder(len)
        for (i in 0 until len) {
            val ch = text[i]
            if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9') && ch != '.' && ch != '-' && ch != '*' && ch != '_') {
                if (ch == ' ') {
                    builder.append('+')
                } else {
                    val encodedByte = byteForChar(ch)
                    builder.append('%')
                    var nibble = encodedByte.toInt() shr 4 and 0xf
                    if (nibble >= 10) {
                        builder.append((nibble + 55).toChar())
                    } else {
                        builder.append((nibble + 48).toChar())
                    }
                    nibble = encodedByte.toInt() and 0xf
                    if (nibble >= 10) {
                        builder.append((nibble + 55).toChar())
                    } else {
                        builder.append((nibble + 48).toChar())
                    }
                }
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    @JvmStatic
    fun getCollationKey(ch: Char): Int {
        var key = ch.code shl 4
        if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
            val lowerCase = Character.toLowerCase(ch)
            key = (lowerCase.code shl 4) + 1
        }
        return key
    }

    @JvmStatic
    fun decodeWithUnicodeTable(data: ByteArray): String {
        return readString(data, 0, data.size)
    }

    @JvmStatic
    fun joinStrings(parts: Array<Any?>, startIndex: Int, count: Int): String {
        if (count == 0) {
            return ""
        } else if (count == 1) {
            val part = parts[startIndex] as CharSequence?
            return part?.toString() ?: "null"
        } else {
            val endIndex = count + startIndex
            var totalLength = 0
            for (i in startIndex until endIndex) {
                val part = parts[i] as CharSequence?
                if (part == null) {
                    totalLength += 4
                } else {
                    totalLength += part.length
                }
            }
            val builder = StringBuilder(totalLength)
            for (i in startIndex until endIndex) {
                val part = parts[i] as CharSequence?
                if (part == null) {
                    builder.append("null")
                } else {
                    builder.append(part)
                }
            }
            return builder.toString()
        }
    }

    @JvmStatic
    fun split(input: String, delimiter: Char): Array<String> {
        val delimiterCount = countOccurrences(input, delimiter)
        val result = Array(delimiterCount + 1) { "" }
        var resultIndex = 0
        var searchStart = 0

        for (i in 0 until delimiterCount) {
            var delimiterPos = searchStart
            while (input[delimiterPos] != delimiter) {
                delimiterPos++
            }
            result[resultIndex++] = input.substring(searchStart, delimiterPos)
            searchStart = delimiterPos + 1
        }

        result[delimiterCount] = input.substring(searchStart)
        return result
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
    fun isValidIntegerDeprecated(text: CharSequence): Boolean {
        return isWrongJava(text)
    }

    @JvmStatic
    fun isWrongJava(text: CharSequence): Boolean {
        return isValidInteger(text)
    }

    @JvmStatic
    fun characterSequenceToBytearray(text: CharSequence): ByteArray {
        val len = text.length
        val bytes = ByteArray(len)
        for (i in 0 until len) {
            val ch = text[i]
            if (ch.code > 0 && ch.code < 128 || ch.code >= 160 && ch.code <= 255) {
                bytes[i] = ch.code.toByte()
            } else when (ch.code) {
                8364 -> bytes[i] = -128
                8218 -> bytes[i] = -126
                402 -> bytes[i] = -125
                8222 -> bytes[i] = -124
                8230 -> bytes[i] = -123
                8224 -> bytes[i] = -122
                8225 -> bytes[i] = -121
                710 -> bytes[i] = -120
                8240 -> bytes[i] = -119
                352 -> bytes[i] = -118
                8249 -> bytes[i] = -117
                338 -> bytes[i] = -116
                381 -> bytes[i] = -114
                8216 -> bytes[i] = -111
                8217 -> bytes[i] = -110
                8220 -> bytes[i] = -109
                8221 -> bytes[i] = -108
                8226 -> bytes[i] = -107
                8211 -> bytes[i] = -106
                8212 -> bytes[i] = -105
                732 -> bytes[i] = -104
                8482 -> bytes[i] = -103
                353 -> bytes[i] = -102
                8250 -> bytes[i] = -101
                339 -> bytes[i] = -100
                382 -> bytes[i] = -98
                376 -> bytes[i] = -97
                else -> bytes[i] = 63
            }
        }
        return bytes
    }

    @JvmStatic
    fun sortAlphabetically(names: Array<String>, ids: ShortArray?) {
        if (ids != null) {
            @Suppress("UNCHECKED_CAST")
            ArrayUtils.quicksort(names as Array<String?>, ids, 0, names.size - 1)
        }
    }

    @JvmStatic
    fun formatScript(source: String, carriageReturn: Char, replacement: String): String {
        val sourceLength = source.length
        val replacementLength = replacement.length
        var resultLength = sourceLength
        val extraCharsPerOccurrence = replacementLength - 1
        if (extraCharsPerOccurrence != 0) {
            var searchPos = 0
            while (true) {
                searchPos = source.indexOf(carriageReturn, searchPos)
                if (searchPos < 0) {
                    break
                }
                ++searchPos
                resultLength += extraCharsPerOccurrence
            }
        }
        val builder = StringBuilder(resultLength)
        var copyStart = 0
        while (true) {
            val matchPos = source.indexOf(carriageReturn, copyStart)
            if (matchPos < 0) {
                builder.append(source.substring(copyStart))
                return builder.toString()
            }
            builder.append(source, copyStart, matchPos)
            builder.append(replacement)
            copyStart = matchPos + 1
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
    val BASE37_ALPHABET = charArrayOf('_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    @JvmStatic
    fun parse_int(text: CharSequence): Int {
        return parseInt(text, 16)
    }

    @JvmStatic
    fun isNumber(character: Char): Boolean {
        return character >= '0' && character <= '9'
    }
}
