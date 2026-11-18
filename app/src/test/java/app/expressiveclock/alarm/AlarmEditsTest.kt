package app.expressiveclock.alarm
import app.expressiveclock.core.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class AlarmEditsTest {
    private val skipped=Alarm(5,LocalTime.of(7,0),days=DayOfWeek.entries.toSet(),skippedOccurrenceMillis=1000,skippedOccurrenceDate=LocalDate.of(2026,11,16))
    @Test fun labelAndSoundEditsPreserveDismissal() {
        val edited=skipped.copy(label="New label",soundUri="content://audio/new").preservingDismissal(skipped)
        assertEquals(skipped.skippedOccurrenceMillis,edited.skippedOccurrenceMillis)
        assertEquals(skipped.skippedOccurrenceDate,edited.skippedOccurrenceDate)
    }
    @Test fun changedTimeOrRepeatCreatesNewOccurrence() {
        assertNull(skipped.copy(time=LocalTime.of(8,0)).preservingDismissal(skipped).skippedOccurrenceDate)
        assertNull(skipped.copy(days=Alarm.WEEKDAYS).preservingDismissal(skipped).skippedOccurrenceMillis)
    }
    @Test fun explicitRearmClearsDismissal() {
        val off=skipped.copy(enabled=false)
        assertNull(off.copy(enabled=true).preservingDismissal(off).skippedOccurrenceMillis)
    }
}
