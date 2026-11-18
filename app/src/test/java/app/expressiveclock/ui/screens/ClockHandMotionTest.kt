package app.expressiveclock.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ClockHandMotionTest {
    private val now = Instant.parse("2026-11-17T00:15:30.500Z").toEpochMilli()
    private val sydney = ZoneId.of("Australia/Sydney")
    private val sanFrancisco = ZoneId.of("America/Los_Angeles")

    private fun sameAngles(a: Pair<Float, Float>, b: Pair<Float, Float>) {
        assertEquals(0f, shortestClockDelta(a.first, b.first), .001f)
        assertEquals(0f, shortestClockDelta(a.second, b.second), .001f)
    }

    @Test fun firstFrameRetainsDisplayedHands() {
        val old = ClockHandMotion(sydney)
        val next = old.retarget(sanFrancisco, now)
        sameAngles(old.anglesAt(now), next.anglesAt(now))
        assertTrue(kotlin.math.abs(next.hourCorrection) <= 180f)
        assertTrue(kotlin.math.abs(next.minuteCorrection) <= 180f)
    }

    @Test fun rapidSelectionsRetargetFromInFlightPositions() {
        var motion = ClockHandMotion(sydney)
        var time = now
        val zones = listOf("America/Los_Angeles", "Asia/Kathmandu", "Australia/Adelaide", "America/New_York", "Australia/Sydney")
        for ((i, id) in zones.withIndex()) {
            time += listOf(1L, 16L, 180L, 5L, 300L)[i]
            val displayed = motion.anglesAt(time)
            motion = motion.retarget(ZoneId.of(id), time)
            sameAngles(displayed, motion.anglesAt(time))
        }
        sameAngles(clockAngles(time + 680, sydney), motion.anglesAt(time + 680))
    }

    @Test fun settledHandsKeepFollowingLiveTarget() {
        val motion = ClockHandMotion(sydney).retarget(sanFrancisco, now)
        sameAngles(clockAngles(now + 10_000, sanFrancisco), motion.anglesAt(now + 10_000))
    }

    @Test fun disabledMotionUsesExactTargetImmediately() {
        val motion = ClockHandMotion(sydney).retarget(sanFrancisco, now, duration = 0)
        sameAngles(clockAngles(now, sanFrancisco), motion.anglesAt(now))
    }
}
