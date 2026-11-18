package app.expressiveclock.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldClockSnapTest {
    @Test fun slowReleaseUsesHalfwayPoint() {
        assertEquals(0f, worldClockSnapTarget(.49f, 0f), 0f)
        assertEquals(1f, worldClockSnapTarget(.5f, 0f), 0f)
        assertEquals(1f, worldClockSnapTarget(.51f, 0f), 0f)
    }
    @Test fun intentionalFlickOverridesPositionInEitherDirection() {
        assertEquals(1f, worldClockSnapTarget(.1f, 800f), 0f)
        assertEquals(0f, worldClockSnapTarget(.9f, -800f), 0f)
        assertEquals(0f, worldClockSnapTarget(.4f, 300f), 0f)
    }
    @Test fun stableEndpointsAllowListFlingToContinue() {
        assertEquals(0f, worldClockSnapTarget(0f, 1000f), 0f)
        assertEquals(1f, worldClockSnapTarget(1f, -1000f), 0f)
    }
}
