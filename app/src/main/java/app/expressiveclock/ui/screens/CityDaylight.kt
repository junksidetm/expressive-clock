package app.expressiveclock.ui.screens

import app.expressiveclock.core.WorldCity
import app.expressiveclock.ui.GeographicPoint
import app.expressiveclock.ui.ZoneCoordinates
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.*

/** Uses existing tzdb representative coordinates; never requests device location or a network lookup. */
internal fun cityCoordinates(city: WorldCity): GeographicPoint? {
    if (city.zone.id == "America/Los_Angeles" && city.city == "San Francisco")
        return GeographicPoint(37.7749, -122.4194)
    val canonical = android.icu.util.TimeZone.getCanonicalID(city.zone.id) ?: city.zone.id
    return ZoneCoordinates.byZone[canonical] ?: ZoneCoordinates.byZone[city.zone.id]
}

/** NOAA fractional-year solar position approximation, using UTC so DST cannot shift the sun.
 * https://gml.noaa.gov/grad/solcalc/solareqns.PDF
 * Sunrise/sunset use the conventional -0.833 degree solar-centre horizon correction.
 * Representative coordinates and atmospheric/horizon differences make this a visual cue, not ephemeris data.
 */
internal fun solarDaylight(utcMillis: Long, latitude: Double, longitude: Double): Boolean {
    val utc = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC)
    val hours = utc.hour + utc.minute / 60.0 + utc.second / 3600.0
    val gamma = 2 * PI / utc.toLocalDate().lengthOfYear() * (utc.dayOfYear - 1 + (hours - 12) / 24)
    val equation = 229.18 * (.000075 + .001868 * cos(gamma) - .032077 * sin(gamma) -
        .014615 * cos(2 * gamma) - .040849 * sin(2 * gamma))
    val declination = .006918 - .399912 * cos(gamma) + .070257 * sin(gamma) -
        .006758 * cos(2 * gamma) + .000907 * sin(2 * gamma) -
        .002697 * cos(3 * gamma) + .00148 * sin(3 * gamma)
    val solarMinutes = ((hours * 60 + equation + 4 * longitude) % 1440 + 1440) % 1440
    val hourAngle = Math.toRadians(solarMinutes / 4 - 180)
    val lat = Math.toRadians(latitude)
    val cosZenith = sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle)
    return cosZenith >= cos(Math.toRadians(90.833))
}
