package com.jagex.game.runetek5.utils

object CalenderUtil {
    @JvmStatic
    fun getLocalYear(milliseconds: Long): Int = CalendarUtil.getLocalYear(milliseconds)

    @JvmStatic
    fun setGmtTime(milliseconds: Long) = CalendarUtil.setGmtTime(milliseconds)

    @JvmStatic
    fun getLocalDdmmyyyy(daysSinceEpoch: Int): IntArray = CalendarUtil.getLocalDdmmyyyy(daysSinceEpoch)

    @JvmStatic
    fun setLocalTime(milliseconds: Long) = CalendarUtil.setLocalTime(milliseconds)

    @JvmStatic
    fun formatWithLanguage(milliseconds: Long): String = CalendarUtil.formatWithLanguage(milliseconds)

    @JvmStatic
    fun getGmtNoon(day: Int, month: Int, year: Int): Long = CalendarUtil.getGmtNoon(day, month, year)

    @JvmStatic
    fun getGmtTimestring(milliseconds: Long): String = CalendarUtil.getGmtTimestring(milliseconds)

    @JvmStatic
    fun getLocalTimestring(milliseconds: Long): String = CalendarUtil.getLocalTimestring(milliseconds)

    @JvmStatic
    fun getLocalDateString(milliseconds: Long, language: Int): String = CalendarUtil.getLocalDateString(milliseconds, language)

    @JvmStatic
    fun getLanguageTimestring(milliseconds: Long, language: Int): String = CalendarUtil.getLanguageTimestring(milliseconds, language)

    @JvmStatic
    fun convertDaysToMilliseconds(daysSinceEpoch: Int): Long = CalendarUtil.convertDaysToMilliseconds(daysSinceEpoch)
}
