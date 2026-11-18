package app.expressiveclock.ui.screens

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class WideClockPagesTest {
    @Test fun windowDimensionsChooseLayoutWithoutDeviceIdentity() {
        assertFalse(useWideClockPages(420f, 900f, 1f))
        assertFalse(useWideClockPages(900f, 420f, 1f))
        assertFalse(useWideClockPages(680f, 900f, 1f))
        assertTrue(useWideClockPages(814f, 898f, 1f))
        assertTrue(useWideClockPages(898f, 814f, 1f))
    }

    @Test fun largeFontsFallBackBeforeCityColumnsBecomeTooNarrow() {
        assertFalse(useWideClockPages(814f, 898f, 2f))
        assertTrue(useWideClockPages(1300f, 898f, 2f))
    }

    @Test fun interruptedHandMovementSurvivesConfigurationStateRestore() {
        val now = 1_800_000_000_000L
        val motion = ClockHandMotion(ZoneId.of("Australia/Sydney"))
            .retarget(ZoneId.of("America/Los_Angeles"), now)
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(ClockHandMotionSaver) { scope.save(motion) }!!
        val restored = ClockHandMotionSaver.restore(saved)!!
        assertEquals(motion, restored)
        assertEquals(motion.anglesAt(now + 230), restored.anglesAt(now + 230))
        val back = restored.retarget(ZoneId.of("Australia/Sydney"), now + 230)
        assertEquals(0f, shortestClockDelta(restored.anglesAt(now + 230).first, back.anglesAt(now + 230).first), .001f)
    }
}
