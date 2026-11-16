package app.expressiveclock.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitalTextFitTest {
    @Test fun keepsOriginalSizeWhenItFits() {
        assertEquals(1f, DigitalTextFit.scale(500f, 100f, 20f, 100f, false, 4f), 0f)
    }

    @Test fun horizontalReservesRoomForLastDigitAndMargins() {
        val scale = DigitalTextFit.scale(200f, 100f, 20f, 100f, false, 4f)
        assertEquals(200f, scale * 220f + 12f, .001f)
    }

    @Test fun secondsFitAndStackedUsesOnlyWidestLine() {
        val horizontal = DigitalTextFit.scale(250f, 100f, 20f, 220f, false, 4f)
        val stacked = DigitalTextFit.scale(250f, 100f, 20f, 220f, true, 4f)
        assertTrue(horizontal < 1f)
        assertEquals(1f, stacked, 0f)
    }

    @Test fun tinyHostCannotProduceNegativeSize() {
        assertEquals(0f, DigitalTextFit.scale(1f, 100f, 20f, 100f, false, 4f), 0f)
    }
}
