package app.expressiveclock.ui.responsive

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import app.expressiveclock.ui.screens.progressiveListEdgeMask

/** Fixed viewport falloffs, with end gutters so the first/last controls can rest in clear space.
 * Whether edges are needed depends on measured content, never the current scroll offset.
 * Fitting content keeps its original geometry and does not incur a blur layer.
 */
@Composable
internal fun Modifier.persistentScrollEdges(state: ScrollState): Modifier {
    var viewport by remember { mutableIntStateOf(0) }
    var overflowing by remember { mutableStateOf(false) }
    return onSizeChanged { viewport = it.height }
        .progressiveListEdgeMask(if (overflowing) 1f else 0f, if (overflowing) 1f else 0f)
        .verticalScroll(state)
        .layout { measurable, constraints ->
            val child = measurable.measure(constraints)
            val needsEdges = viewport > 0 && child.height > viewport
            if (overflowing != needsEdges) overflowing = needsEdges
            val gutter = if (needsEdges) 32.dp.roundToPx() else 0
            layout(child.width, child.height + gutter * 2) { child.placeRelative(0, gutter) }
        }
}
