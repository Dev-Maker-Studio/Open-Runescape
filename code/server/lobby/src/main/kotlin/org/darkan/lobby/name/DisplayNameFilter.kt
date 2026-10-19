package org.darkan.lobby.name

enum class DisplayNameVerdict { PROFANE, RESERVED }

data class DisplayNameMatch(val verdict: DisplayNameVerdict, val term: String)

/**
 * Matches a display name against `display-name-vocabulary.txt` through three forms, each closing
 * an evasion the others leave open: every word on its own, every word with repeated letters
 * squeezed out ("Niiigggerr"), and the whole name with its separators dropped ("F u c k"). The
 * squeezed form of a word is only used when the word actually had a repeat, so "As" never
 * collapses into "Ass".
 *
 * The separator-stripped form is scanned with the long terms only, because two innocent words run
 * together can spell a short one; the short terms reach it only when the name has a one or
 * two-letter word, which is the only way to split a short term across words.
 *
 * A word of digits alone is a number, not disguised text, so it is dropped rather than read through
 * the leet table - otherwise "Player 455" spells one of the terms.
 */
object DisplayNameFilter {

    private const val COMPACT_MIN_LENGTH = 6
    private const val SHORT_WORD_LENGTH = 2
    private const val SQUEEZE_MIN_LENGTH = 4
    private const val VOCABULARY = "/display-name-vocabulary.txt"

    private val leet = mapOf(
        '0' to 'o', '3' to 'e', '4' to 'a', '5' to 's', '6' to 'g', '7' to 't', '8' to 'b', '9' to 'g',
        '@' to 'a', '$' to 's', '!' to 'i', '+' to 't', '£' to 'e', '(' to 'c', '<' to 'c',
    )
    private val separators = Regex("[^A-Za-z0-9@$!+£(<]+")

    private val vocabulary = readVocabulary()
    private val inWord = vocabulary.getValue("in-word")
    private val wholeWord = vocabulary.getValue("whole-word")
    private val allowed = vocabulary.getValue("allow")
    private val reserved = vocabulary.getValue("reserved")

    private val longInWord = inWord.filterTo(HashSet()) { it.length >= COMPACT_MIN_LENGTH }
    private val squeezedInWord = squeezedIndex(inWord)
    private val squeezedWholeWord = squeezedIndex(wholeWord)

    fun isAcceptable(name: String): Boolean = match(name) == null

    fun match(name: String): DisplayNameMatch? = matchIn(name, oneAsL = false) ?: matchIn(name, oneAsL = true)

    private fun matchIn(name: String, oneAsL: Boolean): DisplayNameMatch? {
        val words = name.split(separators).mapNotNull { raw ->
            val read = raw.normalize(oneAsL)
            if (read.isEmpty()) null else Word(read, raw.lettersOnly())
        }
        if (words.isEmpty()) return null

        for (word in words) {
            if (word.allowed) continue
            if (word.read in reserved) return DisplayNameMatch(DisplayNameVerdict.RESERVED, word.read)
            wholeWordMatch(word.read)?.let { return it }
            inWordMatch(word.read, inWord)?.let { return it }
        }

        val scannable = words.filterNot { it.allowed }.map { it.read }
        if (scannable.size < 2) return null
        val compact = scannable.joinToString("")
        if (compact in allowed) return null
        if (compact in reserved) return DisplayNameMatch(DisplayNameVerdict.RESERVED, compact)
        wholeWordMatch(compact)?.let { return it }
        val hasShortWord = scannable.any { it.length <= SHORT_WORD_LENGTH }
        return inWordMatch(compact, if (hasShortWord) inWord else longInWord)
    }

    private fun wholeWordMatch(word: String): DisplayNameMatch? {
        if (word in wholeWord) return profane(word)
        val squeezed = word.squeeze()
        if (squeezed == word) return null
        return squeezedWholeWord[squeezed]?.let { profane(it) }
    }

    private fun inWordMatch(word: String, terms: Set<String>): DisplayNameMatch? {
        terms.firstOrNull { word.contains(it) }?.let { return profane(it) }
        val squeezed = word.squeeze()
        if (squeezed == word) return null
        return squeezedInWord.entries
            .firstOrNull { (key, term) -> term in terms && squeezed.contains(key) }
            ?.let { profane(it.value) }
    }

    private fun profane(term: String) = DisplayNameMatch(DisplayNameVerdict.PROFANE, term)

    /**
     * [read] is the word with its leetspeak resolved; [bare] is the same word with digits simply
     * dropped, which is what rescues "Lollipop1994" - resolving its digits spells a word no
     * allow-list entry covers, while dropping them leaves the ordinary word behind.
     */
    private class Word(val read: String, val bare: String) {
        val allowed: Boolean get() = read in DisplayNameFilter.allowed || bare in DisplayNameFilter.allowed
    }

    private fun squeezedIndex(terms: Set<String>): Map<String, String> = terms
        .filter { it.squeeze().length >= SQUEEZE_MIN_LENGTH }
        .associateBy { it.squeeze() }

    private fun readVocabulary(): Map<String, Set<String>> {
        val text = DisplayNameFilter::class.java.getResourceAsStream(VOCABULARY)
            ?.bufferedReader()?.use { it.readText() }
            ?: error("Missing $VOCABULARY")
        val sections = mutableMapOf<String, MutableSet<String>>()
        var current: MutableSet<String>? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() || line.startsWith("#") -> Unit
                line.startsWith("[") && line.endsWith("]") ->
                    current = sections.getOrPut(line.trim('[', ']')) { mutableSetOf() }
                else -> current?.add(line)
            }
        }
        return sections
    }

    private fun String.normalize(oneAsL: Boolean): String {
        val lowered = lowercase()
        if (lowered.none { it in 'a'..'z' }) return ""
        val out = StringBuilder(length)
        for (raw in lowered) {
            val ch = if (raw == '1') (if (oneAsL) 'l' else 'i') else leet[raw] ?: raw
            if (ch in 'a'..'z') out.append(ch)
        }
        return out.toString()
    }

    private fun String.lettersOnly(): String = lowercase().filter { it in 'a'..'z' }

    private fun String.squeeze(): String {
        val out = StringBuilder(length)
        for (ch in this) if (out.isEmpty() || out.last() != ch) out.append(ch)
        return out.toString()
    }
}
