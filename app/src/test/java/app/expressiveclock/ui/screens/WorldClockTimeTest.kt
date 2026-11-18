package app.expressiveclock.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WorldClockTimeTest {
    @Test fun shortestPathAcrossMidnight() {
        assertEquals(-20f, shortestClockDelta(350f, 10f), .001f)
        assertEquals(20f, shortestClockDelta(10f, 350f), .001f)
        assertEquals(0f, shortestClockDelta(720f, 0f), .001f)
    }
    @Test fun fractionalOffsetsAndCalendarRollover() {
        val instant = Instant.parse("2026-11-16T23:30:30Z").toEpochMilli()
        val nepal = clockAngles(instant, ZoneId.of("Asia/Kathmandu"))
        assertEquals(157.75f, nepal.first, .001f) // 05:15:30 next day
        assertEquals(93f, nepal.second, .001f)
    }
    @Test fun daylightSavingTransitionUsesZoneRules() {
        val zone = ZoneId.of("America/New_York")
        val before = clockAngles(Instant.parse("2026-03-08T06:59:00Z").toEpochMilli(), zone)
        val after = clockAngles(Instant.parse("2026-03-08T07:00:00Z").toEpochMilli(), zone)
        assertEquals(59.5f, before.first, .001f)
        assertEquals(90f, after.first, .001f)
    }
}
