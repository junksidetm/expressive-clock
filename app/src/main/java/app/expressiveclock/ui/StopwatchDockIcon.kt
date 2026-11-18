/*
 * Outline adapted from Material Icons Outlined.Timer.
 * Copyright 2025 The Android Open Source Project. Licensed under Apache License 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package app.expressiveclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Same 24-unit outline and hand geometry as the existing icon; only the hand moves.
private val StopwatchCase = ImageVector.Builder("StopwatchCase", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(15f, 1f); horizontalLineTo(9f); verticalLineToRelative(2f)
        horizontalLineToRelative(6f); verticalLineTo(1f); close()
        moveTo(19.03f, 7.39f); lineToRelative(1.42f, -1.42f)
        curveToRelative(-.43f, -.51f, -.9f, -.99f, -1.41f, -1.41f)
        lineToRelative(-1.42f, 1.42f)
        curveTo(16.07f, 4.74f, 14.12f, 4f, 12f, 4f)
        curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
        reflectiveCurveToRelative(4.02f, 9f, 9f, 9f)
        reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
        curveTo(21f, 10.88f, 20.26f, 8.93f, 19.03f, 7.39f); close()
        moveTo(12f, 20f)
        curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
        reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
        reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
        reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f); close()
    }
}.build()

@Composable
internal fun StopwatchDockIcon(phase: () -> Float, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        Icon(StopwatchCase, contentDescription = null, tint = tint, modifier = Modifier.matchParentSize())
        Canvas(Modifier.matchParentSize()) {
            withTransform({
                scale(size.width / 24f, size.height / 24f, Offset.Zero)
                rotate(phase(), Offset(12f, 13f))
            }) {
                drawRect(tint, topLeft = Offset(11f, 8f), size = Size(2f, 6f))
            }
        }
    }
}
