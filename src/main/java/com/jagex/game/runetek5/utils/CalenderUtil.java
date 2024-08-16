package com.jagex.game.runetek5.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.TimeZoneManager;

public class CalenderUtil {

    public static final String[][] MONTHS_BY_LANGUAGE = {{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}, {"Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"}, {"jan", "fév", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc"}, {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"}, {"jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec"}};
    public static final Calendar LOCAL_CALENDER = Calendar.getInstance();
    public static final Calendar GMT_CALENDER = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    CalenderUtil() {
        throw new Error();
    }

    public static int getLocalYear(long long_0) {
        setLocalTime(long_0);
        return LOCAL_CALENDER.get(Calendar.YEAR);
    }

    public static void setGmtTime(long milliseconds) {
        GMT_CALENDER.setTime(new Date(milliseconds));
    }

    public static int[] getLocalDdmmyyyy(int i_0) {
        int[] ddmmyyyy = new int[3];
        setLocalTime(CalenderUtil.method5363(i_0));
        ddmmyyyy[0] = LOCAL_CALENDER.get(Calendar.DATE);
        ddmmyyyy[1] = LOCAL_CALENDER.get(Calendar.MONTH);
        ddmmyyyy[2] = LOCAL_CALENDER.get(Calendar.YEAR);
        return ddmmyyyy;
    }

    public static void setLocalTime(long milliseconds) {
        LOCAL_CALENDER.setTime(new Date(milliseconds));
    }

    public static String formatWithLanguage(long milliseconds) {
        return formatWithLanguage(new Date(milliseconds), "EEE dd-MMM-yyyy HH:mm:ss zzz");
    }

    public static long getGmtNoon(int day, int month, int year) {
        GMT_CALENDER.clear();
        GMT_CALENDER.set(year, month, day, 12, 0, 0);
        return GMT_CALENDER.getTime().getTime();
    }

    public static String getGmtTimestring(long milliseconds) {
        Calendar gmt;
        setGmtTime(milliseconds);
        gmt = GMT_CALENDER;
        int date = gmt.get(Calendar.DATE);
        int nextMonth = gmt.get(Calendar.MONTH) + 1;
        int year = gmt.get(Calendar.YEAR);
        int hour = gmt.get(Calendar.HOUR_OF_DAY);
        int minute = gmt.get(Calendar.MINUTE);
        return Integer.toString(date / 10) + date % 10 + "/" + nextMonth / 10 + nextMonth % 10 + "/" + year % 100 / 10 + year % 10 + " " + hour / 10 + hour % 10 + ":" + minute / 10 + minute % 10;
    }

    public static String getLocalTimestring(long milliseconds) {
        setLocalTime(milliseconds);
        int date = LOCAL_CALENDER.get(Calendar.DATE);
        int month = LOCAL_CALENDER.get(Calendar.MONTH) + 1;
        int year = LOCAL_CALENDER.get(Calendar.YEAR);
        return Integer.toString(date / 10) + date % 10 + "/" + month / 10 + month % 10 + "/" + year % 100 / 10 + year % 10;
    }

    public static String method13045(long long_0, int i_2) {
        setLocalTime(long_0);
        int i_4 = LOCAL_CALENDER.get(Calendar.DATE);
        int i_5 = LOCAL_CALENDER.get(Calendar.MONTH);
        int i_6 = LOCAL_CALENDER.get(Calendar.YEAR);
        return i_2 == 3 ? getLocalTimestring(long_0) : Integer.toString(i_4 / 10) + i_4 % 10 + "-" + MONTHS_BY_LANGUAGE[i_2][i_5] + "-" + i_6;
    }

    public static String getLanguageTimestring(long milliseconds, int language) {
        setGmtTime(milliseconds);
        Calendar gmt = GMT_CALENDER;

        int date = gmt.get(Calendar.DATE);
        int month = gmt.get(Calendar.MONTH);
        int year = gmt.get(Calendar.YEAR);
        int hour = gmt.get(Calendar.HOUR_OF_DAY);
        int minute = gmt.get(Calendar.MINUTE);
        return language == 3 ? getGmtTimestring(milliseconds) : Integer.toString(date / 10) + date % 10 + "-" + MONTHS_BY_LANGUAGE[language][month] + "-" + year + " " + hour / 10 + hour % 10 + ":" + minute / 10 + minute % 10;
    }

	public static String formatWithLanguage(Date date, String format) {
	    return Language.getFormattedDatestring(date, format, TimeZoneManager.getTimezone2());
	}

	public static long method5363(int i_0) {
	    return (i_0 + 11745) * 86400000L;
	}
}
