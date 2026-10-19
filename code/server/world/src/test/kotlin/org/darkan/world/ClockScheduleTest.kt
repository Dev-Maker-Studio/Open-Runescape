package org.darkan.world

import com.rs.game.tasks.nextClockBoundary
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ClockScheduleTest {
    private val twoHours = 2 * 60 * 60_000L
    private val halfHour = 30 * 60_000L

    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun `a two-hourly boundary is the next even utc hour on the dot`() {
        assertEquals(utc("2026-10-01T06:00:00Z"), nextClockBoundary(utc("2026-10-01T05:59:59.999Z"), twoHours))
        assertEquals(utc("2026-10-01T08:00:00Z"), nextClockBoundary(utc("2026-10-01T06:00:00Z"), twoHours))
        assertEquals(0, Instant.ofEpochMilli(nextClockBoundary(utc("2026-10-01T23:17:42Z"), twoHours)).atOffset(ZoneOffset.UTC).hour)
    }

    @Test
    fun `a late tick still lands the following run on the dot`() {
        assertEquals(utc("2026-10-01T07:00:00Z"), nextClockBoundary(utc("2026-10-01T06:30:04.200Z"), halfHour))
    }
}
