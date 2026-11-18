package app.expressiveclock.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Same header controls and typography as New/Edit Alarm, with immediate selection instead of Save. */
@Composable
internal fun SelectionSheetHeader(title: String, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        SheetHeaderButton("Cancel", primary = false, onClick = onCancel)
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 2)
        Spacer(Modifier.width(80.dp))
    }
}
