package com.jagex.game.runetek5.utils

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.inputsubscriber.NameType
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText

object TextUtils {

    @JvmField
    val ALLOWED_SPECIAL_CHARS = charArrayOf(
        ' ', '\u00a0', '_', '-', '\u00e0', '\u00e1', '\u00e2', '\u00e4', '\u00e3', '\u00c0', '\u00c1', '\u00c2',
        '\u00c4', '\u00c3', '\u00e8', '\u00e9', '\u00ea', '\u00eb', '\u00c8', '\u00c9', '\u00ca', '\u00cb',
        '\u00ed', '\u00ee', '\u00ef', '\u00cd', '\u00ce', '\u00cf', '\u00f2', '\u00f3', '\u00f4', '\u00f6',
        '\u00f5', '\u00d2', '\u00d3', '\u00d4', '\u00d6', '\u00d5', '\u00f9', '\u00fa', '\u00fb', '\u00fc',
        '\u00d9', '\u00da', '\u00db', '\u00dc', '\u00e7', '\u00c7', '\u00ff', '\u0178', '\u00f1', '\u00d1', '\u00df'
    )

    @JvmField
    val OTHER_ALLOWED_CHARS = charArrayOf('[', ']', '#')

    @JvmStatic
    fun createNumberCenteredWithinText(number: Int, dynamicRadix: Boolean): String {
        return if (dynamicRadix && number >= 0) centerIntegerInString(number, true) else number.toString()
    }

    @JvmStatic
    fun centerIntegerInString(number: Int, dynamicRadix: Boolean): String {
        var num = number
        if (dynamicRadix && num >= 0) {
            var characterCount = 2
            var temp = num / 10
            while (temp != 0) {
                characterCount++
                temp /= 10
            }
            val characters = CharArray(characterCount)
            characters[0] = '+'
            for (countDescending in characterCount - 1 downTo 1) {
                val cachedNumber = num
                num /= 10
                val digit = cachedNumber - num * 10
                characters[countDescending] = if (digit >= 10) {
                    (digit + 87).toChar()
                } else {
                    (digit + 48).toChar()
                }
            }
            return String(characters)
        } else {
            return num.toString(10)
        }
    }

    @JvmStatic
    fun matches(findRaw: String?, findFormatted: String?, currentRaw: String?, currentFormatted: String?, garbageValue: Byte): Boolean {
        if (findRaw != null) {
            if (currentRaw != null) {
                if (!findRaw.startsWith("#")) {
                    if (garbageValue >= -1) {
                        throw IllegalStateException()
                    }
                    if (!currentRaw.startsWith("#")) {
                        return findFormatted == currentFormatted
                    }
                }
                return findRaw == currentRaw
            }
            if (garbageValue >= -1) {
                throw IllegalStateException()
            }
        }
        return false
    }

    @JvmStatic
    fun decodeUidName(uid: Long): String? {
        var remaining = uid
        if (remaining > 0L && remaining < 6582952005840035281L) {
            if (remaining % 37L == 0L) {
                return null
            } else {
                var length = 0
                var temp = remaining
                while (temp != 0L) {
                    length++
                    temp /= 37L
                }
                val stringBuilder = StringBuilder(length)
                while (remaining != 0L) {
                    val previous = remaining
                    remaining /= 37L
                    var ch = Utils.BASE37_ALPHABET[(previous - 37L * remaining).toInt()]
                    if (ch == '_') {
                        val lastIndex = stringBuilder.length - 1
                        stringBuilder.setCharAt(lastIndex, Character.toUpperCase(stringBuilder[lastIndex]))
                        ch = '\u00a0'
                    }
                    stringBuilder.append(ch)
                }
                stringBuilder.reverse()
                stringBuilder.setCharAt(0, Character.toUpperCase(stringBuilder[0]))
                return stringBuilder.toString()
            }
        } else {
            return null
        }
    }

    @JvmStatic
    fun stripDiacritics(character: Char, language: Language): Char {
        if (character in '\u00c0'..'\u00ff') {
            when {
                character <= '\u00c6' -> return 'A'
                character == '\u00c7' -> return 'C'
                character <= '\u00cb' -> return 'E'
                character <= '\u00cf' -> return 'I'
                character == '\u00d1' && language != Language.SPANISH -> return 'N'
                character in '\u00d2'..'\u00d6' -> return 'O'
                character in '\u00d9'..'\u00dc' -> return 'U'
                character == '\u00dd' -> return 'Y'
                character == '\u00df' -> return 's'
                character in '\u00e0'..'\u00e6' -> return 'a'
                character == '\u00e7' -> return 'c'
                character in '\u00e8'..'\u00eb' -> return 'e'
                character in '\u00ec'..'\u00ef' -> return 'i'
                character == '\u00f1' && language != Language.SPANISH -> return 'n'
                character in '\u00f2'..'\u00f6' -> return 'o'
                character in '\u00f9'..'\u00fc' -> return 'u'
                character == '\u00fd' || character == '\u00ff' -> return 'y'
            }
        }
        return when (character) {
            '\u0152' -> 'O'
            '\u0153' -> 'o'
            '\u0178' -> 'Y'
            else -> character
        }
    }

    @JvmStatic
    fun compareLocaleAware(str1: CharSequence, str2: CharSequence, language: Language): Int {
        val length1 = str1.length
        val length2 = str2.length
        var index1 = 0
        var index2 = 0
        var pending1: Char = 0.toChar()
        var pending2: Char = 0.toChar()

        while (index1 - pending1.code < length1 || index2 - pending2.code < length2) {
            if (index1 - pending1.code >= length1) {
                return -1
            }
            if (index2 - pending2.code >= length2) {
                return 1
            }
            val c1 = if (pending1.code != 0) pending1 else str1[index1++]
            val c2 = if (pending2.code != 0) pending2 else str2[index2++]

            pending1 = Utils.getLigatureReplacement(c1)
            pending2 = Utils.getLigatureReplacement(c2)
            val stripped1 = stripDiacritics(c1, language)
            val stripped2 = stripDiacritics(c2, language)

            if (stripped1 != stripped2 && Character.toUpperCase(stripped1) != Character.toUpperCase(stripped2)) {
                val lower1 = Character.toLowerCase(stripped1)
                val lower2 = Character.toLowerCase(stripped2)
                if (lower2 != lower1) {
                    return Utils.getCollationKey(lower1) - Utils.getCollationKey(lower2)
                }
            }
        }

        val minLength = minOf(length1, length2)
        for (i in 0 until minLength) {
            val idx1: Int
            val idx2: Int
            if (language == Language.FRENCH) {
                idx1 = length1 - 1 - i
                idx2 = length2 - 1 - i
            } else {
                idx2 = i
                idx1 = i
            }
            val c1 = str1[idx1]
            val c2 = str2[idx2]
            if (c2 != c1 && Character.toUpperCase(c1) != Character.toUpperCase(c2)) {
                val lower1 = Character.toLowerCase(c1)
                val lower2 = Character.toLowerCase(c2)
                if (lower1 != lower2) {
                    return Utils.getCollationKey(lower1) - Utils.getCollationKey(lower2)
                }
            }
        }

        val lengthDiff = length1 - length2
        if (lengthDiff != 0) {
            return lengthDiff
        } else {
            for (i in 0 until minLength) {
                val c1 = str1[i]
                val c2 = str2[i]
                if (c1 != c2) {
                    return Utils.getCollationKey(c1) - Utils.getCollationKey(c2)
                }
            }
            return 0
        }
    }

    @JvmStatic
    fun isAlphabeticalOrNumeric(character: Char): Boolean {
        return character in '0'..'9' || character in 'A'..'Z' || character in 'a'..'z'
    }

    @JvmStatic
    fun formatDisplayname(charSequence: CharSequence?): String? {
        return format(charSequence)
    }

    @JvmStatic
    fun isSpaceCharacter(character: Char): Boolean {
        return character == '\u00a0' || character == ' ' || character == '_' || character == '-'
    }

    @JvmStatic
    fun allowedCharacter(character: Char): Boolean {
        if (Character.isISOControl(character)) {
            return false
        }
        if (isAlphabeticalOrNumeric(character)) {
            return true
        }
        for (specialChar in ALLOWED_SPECIAL_CHARS) {
            if (character == specialChar) {
                return true
            }
        }
        for (otherChar in OTHER_ALLOWED_CHARS) {
            if (character == otherChar) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun format(charSequence: CharSequence?): String? {
        if (charSequence == null) {
            return null
        }
        var start = 0
        var end = charSequence.length
        while (start < end && isSpaceCharacter(charSequence[start])) {
            start++
        }
        while (end > start && isSpaceCharacter(charSequence[end - 1])) {
            end--
        }
        val length = end - start
        if (length >= 1 && length <= getMaxLength(null)) {
            val sb = StringBuilder(length)
            for (index in start until end) {
                val character = charSequence[index]
                if (allowedCharacter(character)) {
                    val formatted = normalizeCharacter(character)
                    if (formatted.code != 0) {
                        sb.append(formatted)
                    }
                }
            }
            if (sb.isEmpty()) {
                return null
            }
            return sb.toString()
        }
        return null
    }

    @JvmStatic
    fun getMaxLength(nameType: NameType?): Int {
        return if (nameType == null) {
            12
        } else {
            when (nameType.type) {
                5 -> 20
                else -> 12
            }
        }
    }

    @JvmStatic
    fun isValidCp1252Byte(b: Byte): Boolean {
        val value = b.toInt() and 0xff
        return value != 0 && (value < 128 || value >= 160 || Utils.UNICODE_TABLE[value - 128].code != 0)
    }

    @JvmStatic
    fun decodeUrl(encodedUrl: String): String {
        val sb = StringBuilder()
        val length = encodedUrl.length
        var charIndex = 0
        while (charIndex < length) {
            var character = encodedUrl[charIndex]
            if (character == '%' && length > charIndex + 2) {
                character = encodedUrl[charIndex + 1]
                val cp1252 = when {
                    character in '0'..'9' -> character - '0'
                    character in 'a'..'f' -> character.code + 10 - 'a'.code
                    character in 'A'..'F' -> character.code + 10 - 'A'.code
                    else -> {
                        sb.append('%')
                        charIndex++
                        continue
                    }
                } * 16

                val nextChar = encodedUrl[charIndex + 2]
                val fullCp1252 = cp1252 + when {
                    nextChar in '0'..'9' -> nextChar - '0'
                    nextChar in 'a'..'f' -> nextChar.code + 10 - 'a'.code
                    nextChar in 'A'..'F' -> nextChar.code + 10 - 'A'.code
                    else -> {
                        sb.append('%')
                        charIndex++
                        continue
                    }
                }

                if (fullCp1252 != 0 && isValidCp1252Byte(fullCp1252.toByte())) {
                    sb.append(Utils.cp1252ToChar(fullCp1252.toByte()))
                }
                charIndex += 2
            } else if (character == '+') {
                sb.append(' ')
            } else {
                sb.append(character)
            }
            charIndex++
        }
        return sb.toString()
    }

    @JvmStatic
    fun getLength(text: String): Int {
        return text.length + 1
    }

    @JvmStatic
    fun setTextColor(col: Int): String {
        return "<col=${Integer.toHexString(col)}>"
    }

    @JvmStatic
    fun calculateCombatBasedColor(targetCombatLevel: Int, myCombatLevel: Int): String {
        val diff = myCombatLevel - targetCombatLevel
        return when {
            diff < -9 -> setTextColor(16711680)
            diff < -6 -> setTextColor(16723968)
            diff < -3 -> setTextColor(16740352)
            diff < 0 -> setTextColor(16756736)
            diff > 9 -> setTextColor(65280)
            diff > 6 -> setTextColor(4259584)
            diff > 3 -> setTextColor(8453888)
            diff > 0 -> setTextColor(12648192)
            else -> setTextColor(16776960)
        }
    }

    @JvmStatic
    fun getCrown(image: Int): String {
        return "<img=$image>"
    }

    @JvmStatic
    fun atLtGtTags(text: String): String {
        val length = text.length
        var extraLength = 0
        for (index in 0 until length) {
            val chr = text[index]
            if (chr == '<' || chr == '>') {
                extraLength += 3
            }
        }
        val sb = StringBuilder(extraLength + length)
        for (index in 0 until length) {
            val chr = text[index]
            when (chr) {
                '<' -> sb.append("<lt>")
                '>' -> sb.append("<gt>")
                else -> sb.append(chr)
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun formatStackSize(stackSize: Int): String {
        var unformatted = stackSize.toString()
        var charIndex = unformatted.length - 3
        while (charIndex > 0) {
            unformatted = unformatted.substring(0, charIndex) + "," + unformatted.substring(charIndex)
            charIndex -= 3
        }
        return when {
            unformatted.length > 9 -> " ${setTextColor(65408)}${unformatted.substring(0, unformatted.length - 8)}${LocalizedText.M.translate(Client.CURRENT_LANGUAGE!!)} ($unformatted)</col>"
            unformatted.length > 6 -> " ${setTextColor(16777215)}${unformatted.substring(0, unformatted.length - 4)}${LocalizedText.K.translate(Client.CURRENT_LANGUAGE!!)} ($unformatted)</col>"
            else -> " ${setTextColor(16776960)}$unformatted</col>"
        }
    }

    @JvmStatic
    fun normalizeCharacter(c: Char): Char {
        return when (c) {
            '\u00d2', '\u00d3', '\u00d4', '\u00d5', '\u00d6', '\u00f2', '\u00f3', '\u00f4', '\u00f5', '\u00f6' -> 'o'
            '\u00c7', '\u00e7' -> 'c'
            '\u00df' -> 'b'
            '\u00ff', '\u0178' -> 'y'
            '#', '[', ']' -> c
            '\u00c8', '\u00c9', '\u00ca', '\u00cb', '\u00e8', '\u00e9', '\u00ea', '\u00eb' -> 'e'
            '\u00d1', '\u00f1' -> 'n'
            '\u00c0', '\u00c1', '\u00c2', '\u00c3', '\u00c4', '\u00e0', '\u00e1', '\u00e2', '\u00e3', '\u00e4' -> 'a'
            ' ', '-', '_', '\u00a0' -> '_'
            '\u00d9', '\u00da', '\u00db', '\u00dc', '\u00f9', '\u00fa', '\u00fb', '\u00fc' -> 'u'
            '\u00cd', '\u00ce', '\u00cf', '\u00ed', '\u00ee', '\u00ef' -> 'i'
            else -> Character.toLowerCase(c)
        }
    }

    @JvmStatic
    fun formatAmount(amount: Int, language: Language): String {
        return when {
            amount < 100000 -> "<col=ffff00>$amount</col>"
            amount < 10000000 -> "<col=ffffff>${amount / 1000}${LocalizedText.K2.translate(language)}</col>"
            else -> "<col=00ff80>${amount / 1000000}${LocalizedText.M2.translate(language)}</col>"
        }
    }
}
