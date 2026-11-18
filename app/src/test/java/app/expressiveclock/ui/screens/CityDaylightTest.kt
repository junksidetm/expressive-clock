package app.expressiveclock.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CityDaylightTest {
    private fun daylight(instant: String, lat: Double, lon: Double) =
        solarDaylight(Instant.parse(instant).toEpochMilli(), lat, lon)

    @Test fun equatorialNoonAndMidnight() {
        assertTrue(daylight("2026-03-20T12:00:00Z", 0.0, 0.0))
        assertFalse(daylight("2026-03-20T00:00:00Z", 0.0, 0.0))
    }
    @Test fun summerEveningCanBeDaylightAfterEightPmInLondon() {
        assertTrue(daylight("2026-06-21T19:30:00Z", 51.5072, -.1276))
        assertFalse(daylight("2026-12-21T19:30:00Z", 51.5072, -.1276))
    }
    @Test fun polarDayAndNightDoNotAssumeClockHours() {
        assertTrue(daylight("2026-06-21T23:00:00Z", 78.0, 16.0))
        assertFalse(daylight("2026-12-21T12:00:00Z", 78.0, 16.0))
    }
    @Test fun southernSummerMorningBeforeSixCanBeDaylight() {
        assertTrue(daylight("2026-12-20T18:55:00Z", -33.8688, 151.2093))
    }
}
