package app.expressiveclock.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import java.time.ZoneId

/** One immutable motion snapshot: target zone and both hand corrections change atomically. */
internal data class ClockHandMotion(
    val zone: ZoneId,
    val startedAt: Long = 0L,
    val durationMillis: Long = 0L,
    val hourCorrection: Float = 0f,
    val minuteCorrection: Float = 0f,
) {
    fun anglesAt(millis: Long): Pair<Float, Float> {
        val target = clockAngles(millis, zone)
        val progress = if (durationMillis <= 0L) 1f else
            ((millis - startedAt).toFloat() / durationMillis).coerceIn(0f, 1f)
        val remaining = 1f - settling.transform(progress)
        return target.first + hourCorrection * remaining to target.second + minuteCorrection * remaining
    }

    fun retarget(next: ZoneId, millis: Long, duration: Long = 680L): ClockHandMotion {
        val displayed = anglesAt(millis)
        val target = clockAngles(millis, next)
        return ClockHandMotion(next, millis, duration,
            shortestClockDelta(displayed.first, target.first),
            shortestClockDelta(displayed.second, target.second))
    }

    private companion object {
        val settling = CubicBezierEasing(.2f, .85f, .25f, 1.04f)
    }
}
