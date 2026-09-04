package com.jagex.game.runetek5.tools

import java.util.*

class TimeZoneManager private constructor() {
    init {
        throw Error()
    }

    companion object {
        @JvmField
        val TIMEZONE_MAP: HashMap<String, TimeZone> = HashMap()

        init {
            Calendar.getInstance(getTimezone())
        }

        @JvmStatic
        fun getTimezone2(): TimeZone {
            return getTimezone()
        }

        @JvmStatic
        fun getTimezone(): TimeZone {
            synchronized(TIMEZONE_MAP) {
                var timezone = TIMEZONE_MAP["Europe/London"]
                if (timezone == null) {
                    timezone = TimeZone.getTimeZone("Europe/London")
                    TIMEZONE_MAP["Europe/London"] = timezone
                }
                return timezone
            }
        }
    }
}
