package app.expressiveclock.widget

/** Fit the widest possible time, reserving a small ink margin for every native text view. */
internal object DigitalTextFit {
    fun scale(
        width: Float,
        hours: Float,
        colon: Float,
        minutes: Float,
        stacked: Boolean,
        gutter: Float,
    ): Float {
        val text = if (stacked) maxOf(hours, minutes) else hours + colon + minutes
        val margins = gutter * if (stacked) 1 else 3
        return ((width - margins).coerceAtLeast(0f) / text.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }
}
