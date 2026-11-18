package app.expressiveclock.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

internal fun Modifier.featheredVerticalViewport(
    active: Boolean,
    fadeHeightFraction: Float = 0.18f,
): Modifier = if (!active) this else this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val fadePx = size.height * fadeHeightFraction
        val topFadeFraction = (fadePx / size.height).coerceIn(0f, 0.45f)
        val bottomFadeFraction = 1f - topFadeFraction

        val maskBrush = Brush.verticalGradient(
            0.0f to Color.Transparent,
            topFadeFraction to Color.Black,
            bottomFadeFraction to Color.Black,
            1.0f to Color.Transparent,
        )

        drawRect(
            brush = maskBrush,
            blendMode = BlendMode.DstIn,
        )
    }
