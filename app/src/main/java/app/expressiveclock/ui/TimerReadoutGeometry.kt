package app.expressiveclock.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class TimerReadoutGeometry(val digitWidth: Dp, val colonWidth: Dp, val spacing: Dp, val capHeight: Dp)

/** Shared with the Timer setup readout: 1 : .28 cells, .08 gaps, 1.32 cap ratio. */
internal fun timerReadoutGeometry(availableWidth: Dp, text: String): TimerReadoutGeometry {
    val units = text.count { it != ':' } + text.count { it == ':' } * .28f + (text.length - 1).coerceAtLeast(0) * .08f
    val unit = if (units > 0) availableWidth / units else availableWidth
    return TimerReadoutGeometry(unit, unit * .28f, unit * .08f, (unit.value * 1.32f).dp)
}
