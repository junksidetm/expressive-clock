package app.expressiveclock.ui.screens

import org.junit.Assert.*
import org.junit.Test

class WorldClockHeaderVisibilityTest {
    @Test fun clocksNeverOverlapAcrossFullDragAndRapidReversals() {
        val progress = (0..10000).map { it / 10000f }
        for (p in progress + progress.reversed() + listOf(.2f, .9f, .44f, .56f, .5f, 0f, 1f)) {
            val state = worldClockHeaderVisibility(p)
            assertTrue("overlap at $p", state.analogue == 0f || state.digital == 0f)
            assertTrue(state.analogue in 0f..1f && state.digital in 0f..1f)
        }
    }

    @Test fun endpointsAndHandoverRemainDeliberate() {
        assertEquals(WorldClockHeaderVisibility(1f, 0f), worldClockHeaderVisibility(0f))
        assertEquals(WorldClockHeaderVisibility(0f, 1f), worldClockHeaderVisibility(1f))
        for (p in listOf(.45f, .5f, .55f))
            assertEquals(WorldClockHeaderVisibility(0f, 0f), worldClockHeaderVisibility(p))
        assertTrue(worldClockHeaderVisibility(.01f).analogue < 1f)
        assertTrue(worldClockHeaderVisibility(.56f).digital > 0f)
    }
}
