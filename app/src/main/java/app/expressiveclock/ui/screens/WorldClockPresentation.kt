package app.expressiveclock.ui.screens

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.staticCompositionLocalOf

/** Hoisted above responsive layout branches so fold/unfold preserves the same viewport. */
internal class WorldClockPresentation(val grid: LazyGridState, val collapse: MutableFloatState) {
    val deletionSelection = androidx.compose.runtime.mutableStateOf(emptySet<String>())
    val deletionConfirmation = androidx.compose.runtime.mutableStateOf(false)
}
internal val LocalWorldClockPresentation = staticCompositionLocalOf<WorldClockPresentation> {
    error("World clock presentation must be provided by ClockApp")
}
