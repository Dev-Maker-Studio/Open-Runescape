package com.jagex.game.runetek5.tools

import com.jagex.game.runetek5.config.Identifiable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

enum class Language(
    @JvmField val uppercaseAbbreviation: String,
    val lowercaseAbbreviation: String,
    private val value: Int,
    country: String?
) : Identifiable {
    ENGLISH("EN", "en", 0, "GB"),
    GERMAN("DE", "de", 1, "DE"),
    FRENCH("FR", "fr", 2, "FR"),
    PORTUGESE("PT", "pt", 3, "BR"),
    DUTCH("NL", "nl", 4, "NL"),
    SPANISH("ES", "es", 5, "ES");

    @JvmField val locale: Locale

    init {
        var tempLocale: Locale? = null
        if (country != null) {
            tempLocale = Locale(lowercaseAbbreviation, country)
        }
        locale = tempLocale ?: Locale(lowercaseAbbreviation)
    }

    override fun getValue(): Int {
        return value
    }

    override fun toString(): String {
        return lowercaseAbbreviation.lowercase(Locale.ENGLISH)
    }

    fun getLocale(): Locale {
        return locale
    }

    companion object {
        @JvmField val LANGUAGES: Array<Language?>
        @JvmField var dateformatByLanguage: MutableMap<Language, ConcurrentLinkedQueue<SimpleDateFormat>>? = null

        init {
            val langs = values()
            LANGUAGES = arrayOfNulls(langs.size)
            for (language in langs) {
                if (LANGUAGES[language.value] != null) {
                    throw IllegalStateException()
                }
                LANGUAGES[language.value] = language
            }
        }

        @JvmStatic
        fun getLanguage(index: Int): Language? {
            return if (index >= 0 && index < LANGUAGES.size) LANGUAGES[index] else null
        }

        @JvmStatic
        fun getFormattedDatestring(date: Date, format: String, timezone: TimeZone): String {
            if (dateformatByLanguage == null) {
                dateformatByLanguage = HashMap(6)
                val languages = values()
                for (index in languages.indices) {
                    val language = languages[index]
                    dateformatByLanguage!![language] = ConcurrentLinkedQueue()
                }
            }
            val dateformats = dateformatByLanguage!![ENGLISH]!!
            var dateformat = dateformats.poll()
            if (dateformat == null) {
                dateformat = SimpleDateFormat(format, ENGLISH.getLocale())
            } else {
                dateformat.applyPattern(format)
            }
            dateformat.timeZone = timezone
            val formattedDate = dateformat.format(date)
            dateformats.add(dateformat)
            return formattedDate
        }
    }
}
