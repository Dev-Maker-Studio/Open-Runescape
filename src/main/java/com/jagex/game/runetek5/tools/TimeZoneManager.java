package com.jagex.game.runetek5.tools;

import java.util.*;

public class TimeZoneManager {

    public static final HashMap<String, TimeZone> TIMEZONE_MAP = new HashMap<>();

    static {
        Calendar.getInstance(getTimezone());
    }

    TimeZoneManager() {
        throw new Error();
    }

    public static TimeZone getTimezone2() {
        return getTimezone();
    }

    public static TimeZone getTimezone() {
        synchronized (TIMEZONE_MAP) {
            TimeZone timezone = TIMEZONE_MAP.get("Europe/London");
            if (timezone == null) {
                timezone = TimeZone.getTimeZone("Europe/London");
                TIMEZONE_MAP.put("Europe/London", timezone);
            }
            return timezone;
        }
    }

}
