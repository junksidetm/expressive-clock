package app.expressiveclock.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobeRotationTest {
    @Test fun australiaIsTheRestingMeridian() {
        assertEquals(-135f, globeRotationDegrees(0f), 0f)
    }
    @Test fun fullTurnsHaveIdenticalProjectionAngles() {
        for (phase in listOf(0f, 15.5f, 135f, 270f)) {
            assertEquals(globeRotationDegrees(phase), globeRotationDegrees(phase + 360f), 0f)
            assertEquals(globeRotationDegrees(phase), globeRotationDegrees(phase - 360f), 0f)
        }
    }
}
