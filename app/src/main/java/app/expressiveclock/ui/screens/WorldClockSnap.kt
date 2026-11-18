package app.expressiveclock.ui.screens

internal data class WorldClockHeaderVisibility(val analogue: Float, val digital: Float)

/** Pure scroll-derived handover: no independent alpha animations can outlive a reversal. */
internal fun worldClockHeaderVisibility(progress: Float): WorldClockHeaderVisibility {
    val collapse = progress.coerceIn(0f, 1f)
    return WorldClockHeaderVisibility(
        analogue = (1f - collapse / .45f).coerceIn(0f, 1f),
        digital = ((collapse - .55f) / .45f).coerceIn(0f, 1f),
    )
}

/** Velocity is in dp/s, positive toward collapse. Slow releases use the halfway threshold. */
internal fun worldClockSnapTarget(progress: Float, velocity: Float): Float = when {
    progress <= 0f -> 0f
    progress >= 1f -> 1f
    velocity >= 600f -> 1f
    velocity <= -600f -> 0f
    progress >= .5f -> 1f
    else -> 0f
}
