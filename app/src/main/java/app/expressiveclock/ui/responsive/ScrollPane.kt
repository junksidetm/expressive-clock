package app.expressiveclock.ui.responsive

import app.expressiveclock.ui.responsive.persistentScrollEdges
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.expressiveclock.ui.screens.progressiveListEdgeMask

/** Centre unchanged when content fits; scroll with soft edges when the window becomes too short. */
@Composable
internal fun ScrollPane(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()
            .persistentScrollEdges(state).heightIn(min = maxHeight),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            content = content)
    }
}
