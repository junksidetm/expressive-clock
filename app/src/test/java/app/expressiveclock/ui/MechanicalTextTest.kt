package app.expressiveclock.ui

import org.junit.Assert.*
import org.junit.Test

class MechanicalTextTest {
    @Test fun multiWordNamesPreserveSpacesAndGraphemes() {
        assertEquals("San Francisco", mechanicalCells("San Francisco").joinToString(""))
        assertEquals(6, mechanicalCells("Que\u0301bec").size)
        assertEquals(listOf("🌍"), mechanicalCells("🌍"))
    }
    @Test fun normalTickChangesOnlyTheLastDigit() {
        assertEquals(listOf(7), mechanicalChanges("09:34:38", "09:34:39"))
        assertEquals(emptyList<Int>(), mechanicalChanges("09:34:39", "09:34:39"))
    }
    @Test fun rolloverKeepsSeparatorsStationary() {
        assertEquals(listOf(0, 1, 3, 4, 6, 7), mechanicalChanges("09:59:59", "10:00:00"))
    }
    @Test fun longNamesHaveBoundedSequentialDelay() {
        assertTrue(mechanicalStagger(1, true) > mechanicalStagger(0, true))
        assertEquals(180, mechanicalStagger(50, true))
        assertEquals(72, mechanicalStagger(50, false))
    }
}
