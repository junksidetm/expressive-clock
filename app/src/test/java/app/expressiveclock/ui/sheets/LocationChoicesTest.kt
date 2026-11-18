package app.expressiveclock.ui.sheets

import org.junit.Assert.*
import org.junit.Test

class LocationChoicesTest {
    private val cities = listOf("Asia/Tokyo", "Europe/London", "America/New_York", "Asia/Kathmandu").map(::prettify)
    @Test fun suggestionsAreRealAvailableCitiesAndExcludeSavedZones() {
        val choices = locationChoices(cities, setOf("Europe/London"), "")
        assertEquals("Asia/Tokyo", choices.first().zone.id)
        assertFalse(choices.any { it.zone.id == "Europe/London" })
        assertTrue(choices.all { it in cities })
    }
    @Test fun searchMatchesCityAndRegionCaseInsensitively() {
        assertEquals("America/New_York", locationChoices(cities, emptySet(), " new YORK ").single().zone.id)
        assertEquals(2, locationChoices(cities, emptySet(), "ASIA").size)
    }
    @Test fun matchingSavedCityIsExcludedAndUnknownSearchIsEmpty() {
        assertTrue(locationChoices(cities, setOf("Asia/Tokyo"), "tokyo").isEmpty())
        assertTrue(locationChoices(cities, emptySet(), "not a city").isEmpty())
    }
    @Test fun regionNamesPreserveMultiWordCities() {
        val city = prettify("America/Argentina/Buenos_Aires")
        assertEquals("Buenos Aires", city.city)
        assertEquals("Argentina", city.region)
    }
}
