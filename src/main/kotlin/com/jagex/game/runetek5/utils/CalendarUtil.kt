package com.jagex.game.runetek5.utils

import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.TimeZoneManager
import java.util.*

object CalendarUtil {

    @JvmField val MONTHS_BY_LANGUAGE = arrayOf(
        arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        arrayOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"),
        arrayOf("jan", "fév", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc"),
        arrayOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"),
        arrayOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec")
    )
    @JvmField val LOCAL_CALENDAR: Calendar = Calendar.getInstance()
    @JvmField val GMT_CALENDAR: Calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

    @JvmStatic
    fun getLocalYear(milliseconds: Long): Int {
        setLocalTime(milliseconds)
        return LOCAL_CALENDAR.get(Calendar.YEAR)
    }

    @JvmStatic
    fun setGmtTime(milliseconds: Long) {
        GMT_CALENDAR.time = Date(milliseconds)
    }

    @JvmStatic
    fun getLocalDdmmyyyy(daysSinceEpoch: Int): IntArray {
        val ddmmyyyy = IntArray(3)
        setLocalTime(convertDaysToMilliseconds(daysSinceEpoch))
        ddmmyyyy[0] = LOCAL_CALENDAR.get(Calendar.DATE)
        ddmmyyyy[1] = LOCAL_CALENDAR.get(Calendar.MONTH)
        ddmmyyyy[2] = LOCAL_CALENDAR.get(Calendar.YEAR)
        return ddmmyyyy
    }

    @JvmStatic
    fun setLocalTime(milliseconds: Long) {
        LOCAL_CALENDAR.time = Date(milliseconds)
    }

    @JvmStatic
    fun formatWithLanguage(milliseconds: Long): String {
        return formatWithLanguage(Date(milliseconds), "EEE dd-MMM-yyyy HH:mm:ss zzz")
    }

    @JvmStatic
    fun getGmtNoon(day: Int, month: Int, year: Int): Long {
        GMT_CALENDAR.clear()
        GMT_CALENDAR.set(year, month, day, 12, 0, 0)
        return GMT_CALENDAR.time.time
    }

    @JvmStatic
    fun getGmtTimestring(milliseconds: Long): String {
        setGmtTime(milliseconds)
        val gmt = GMT_CALENDAR
        val date = gmt.get(Calendar.DATE)
        val nextMonth = gmt.get(Calendar.MONTH) + 1
        val year = gmt.get(Calendar.YEAR)
        val hour = gmt.get(Calendar.HOUR_OF_DAY)
        val minute = gmt.get(Calendar.MINUTE)
        return (date / 10).toString() + date % 10 + "/" + nextMonth / 10 + nextMonth % 10 + "/" + year % 100 / 10 + year % 10 + " " + hour / 10 + hour % 10 + ":" + minute / 10 + minute % 10
    }

    @JvmStatic
    fun getLocalTimestring(milliseconds: Long): String {
        setLocalTime(milliseconds)
        val date = LOCAL_CALENDAR.get(Calendar.DATE)
        val month = LOCAL_CALENDAR.get(Calendar.MONTH) + 1
        val year = LOCAL_CALENDAR.get(Calendar.YEAR)
        return (date / 10).toString() + date % 10 + "/" + month / 10 + month % 10 + "/" + year % 100 / 10 + year % 10
    }

    @JvmStatic
    fun getLocalDateString(milliseconds: Long, language: Int): String {
        setLocalTime(milliseconds)
        val date = LOCAL_CALENDAR.get(Calendar.DATE)
        val month = LOCAL_CALENDAR.get(Calendar.MONTH)
        val year = LOCAL_CALENDAR.get(Calendar.YEAR)
        return if (language == 3) getLocalTimestring(milliseconds) else (date / 10).toString() + date % 10 + "-" + MONTHS_BY_LANGUAGE[language][month] + "-" + year
    }

    @JvmStatic
    fun getLanguageTimestring(milliseconds: Long, language: Int): String {
        setGmtTime(milliseconds)
        val gmt = GMT_CALENDAR

        val date = gmt.get(Calendar.DATE)
        val month = gmt.get(Calendar.MONTH)
        val year = gmt.get(Calendar.YEAR)
        val hour = gmt.get(Calendar.HOUR_OF_DAY)
        val minute = gmt.get(Calendar.MINUTE)
        return if (language == 3) getGmtTimestring(milliseconds) else (date / 10).toString() + date % 10 + "-" + MONTHS_BY_LANGUAGE[language][month] + "-" + year + " " + hour / 10 + hour % 10 + ":" + minute / 10 + minute % 10
    }

    @JvmStatic
    fun formatWithLanguage(date: Date, format: String): String {
        return Language.getFormattedDatestring(date, format, TimeZoneManager.getTimezone2())
    }

    @JvmStatic
    fun convertDaysToMilliseconds(daysSinceEpoch: Int): Long {
        return (daysSinceEpoch + 11745) * 86400000L
    }
}
