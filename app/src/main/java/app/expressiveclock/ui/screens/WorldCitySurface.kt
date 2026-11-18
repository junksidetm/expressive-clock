package app.expressiveclock.ui.screens

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** Size-cached curves; only selection changes their light/tint. No bitmaps or running animations. */
internal fun Modifier.worldCitySurface(
    surface: Color,
    accent: Color,
    dark: Boolean,
    selection: () -> Float,
): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val waves = listOf(
        Path().apply {
            moveTo(w * .14f, h * 1.04f)
            cubicTo(w * .65f, h * 1.12f, w * .72f, h * .66f, w * 1.06f, h * .60f)
            lineTo(w * 1.06f, h * 1.06f); close()
        },
        Path().apply {
            moveTo(w * .37f, h * 1.04f)
            cubicTo(w * .74f, h * 1.06f, w * .77f, h * .80f, w * 1.05f, h * .78f)
            lineTo(w * 1.05f, h * 1.05f); close()
        },
        Path().apply {
            moveTo(w * .61f, h * 1.03f)
            cubicTo(w * .88f, h * 1.02f, w * .90f, h * .89f, w * 1.03f, h * .87f)
            lineTo(w * 1.03f, h * 1.03f); close()
        },
    )
    onDrawBehind {
        val selected = selection()
        val light = Color.White
        val surfaceBase = if (dark) surface else lerp(surface, Color.White, .60f)
        val base = lerp(surfaceBase, accent, selected * (if (dark) .075f else .065f))
        val top = lerp(base, accent, (if (dark) .025f else .018f) + selected * (if (dark) .055f else .045f))
        val bottom = lerp(base, accent, (if (dark) .045f else .035f) + selected * .025f)
        drawRect(Brush.linearGradient(listOf(top, base, bottom),
            start = Offset(w * .08f, 0f), end = Offset(w, h * 1.2f)))
        drawRect(Brush.radialGradient(
            listOf(accent.copy(alpha = selected * if (dark) .035f else .08f), Color.Transparent),
            center = Offset(w * .94f, h * .95f), radius = w * .75f))
        drawRect(Brush.radialGradient(
            listOf(light.copy(alpha = if (dark) .025f else .55f), Color.Transparent),
            center = Offset(w * .22f, -h * .18f), radius = w * .98f))
        val strength = .025f + selected * (if (dark) .075f else .115f)
        waves.forEachIndexed { i, path ->
            drawPath(path, Brush.linearGradient(
                listOf(accent.copy(alpha = 0f), accent.copy(alpha = strength * (1f - i * .17f))),
                start = Offset(w * .28f, h * .72f), end = Offset(w, h)))
        }
        // Broad reflected light softens the left of the curves rather than outlining them.
        drawRect(Brush.radialGradient(
            listOf(light.copy(alpha = if (dark) .015f else .24f), Color.Transparent),
            center = Offset(w * .05f, h * .88f), radius = w * .70f))
        val inset = .9.dp.toPx()
        drawRoundRect(Brush.verticalGradient(listOf(
            Color.White.copy(alpha = if (dark) .10f else .85f), Color.Transparent),
            endY = h * .65f), topLeft = Offset(inset, inset),
            size = Size((w - inset * 2).coerceAtLeast(0f), (h - inset * 2).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(25.dp.toPx()), style = Stroke(.65.dp.toPx()))
    }
}

/** Local reflected light only; leaves the app's themed background in place. */
internal fun Modifier.worldClockAmbient(accent: Color, dark: Boolean): Modifier = drawWithCache {
    val strength = if (dark) .022f else .055f
    // A tall panel's maxDimension radius still has colour at its rectangular edges.
    // Draw in normalized ellipse space instead, with a transparent perimeter fully
    // inside the viewport. The softened outer stops also approach zero gently.
    val light = Brush.radialGradient(
        0f to accent.copy(alpha = strength),
        .5f to accent.copy(alpha = strength * .6f),
        .82f to accent.copy(alpha = strength * .12f),
        1f to Color.Transparent,
        center = Offset.Zero, radius = 1f,
    )
    val centre = Offset(size.width * .5f, size.height * .46f)
    val radiusX = size.width * .49f
    val radiusY = size.height * .45f
    onDrawBehind {
        if (radiusX > 0f && radiusY > 0f) withTransform({
            translate(centre.x, centre.y)
            scale(radiusX, radiusY, Offset.Zero)
        }) { drawCircle(light, radius = 1f, center = Offset.Zero) }
    }
}

/** Low-frequency light outside the card; no blur pass or continuously animated shader. */
internal fun Modifier.cityCardLighting(accent: Color, dark: Boolean, selection: () -> Float): Modifier = drawWithCache {
    val centre = Offset(size.width * .52f, size.height * .88f)
    val radius = size.width * .68f
    val glow = Brush.radialGradient(listOf(accent.copy(alpha = if (dark) .045f else .15f), Color.Transparent), centre, radius)
    onDrawBehind {
        withTransform({ scale(1f, .55f, centre) }) {
            drawCircle(glow, radius, centre, alpha = selection())
        }
    }
}

/** Layered blue-white illumination follows the physical clock, including shared-element motion. */
internal fun Modifier.clockLighting(accent: Color, dark: Boolean): Modifier = drawWithCache {
    val r = size.minDimension / 2f - 4.dp.toPx()
    val centre = Offset(size.width / 2f, size.height / 2f)
    val below = centre + Offset(0f, r * .15f)
    val ambient = Brush.radialGradient(
        0f to Color.Transparent,
        .60f to accent.copy(alpha = if (dark) .035f else .11f),
        .82f to accent.copy(alpha = if (dark) .018f else .045f),
        1f to Color.Transparent, center = below, radius = (r * 1.30f).coerceAtLeast(1f))
    val shadow = Brush.radialGradient(
        0f to Color.Transparent,
        .62f to Color.Black.copy(alpha = if (dark) .025f else .035f),
        .84f to Color.Black.copy(alpha = if (dark) .008f else .012f),
        1f to Color.Transparent, center = below + Offset(0f, r * .03f), radius = (r * 1.23f).coerceAtLeast(1f))
    val halo = Brush.radialGradient(
        0f to Color.Transparent, .85f to Color.Transparent,
        .94f to Color.White.copy(alpha = if (dark) .025f else .30f), 1f to Color.Transparent,
        center = centre, radius = (r * 1.07f).coerceAtLeast(1f))
    onDrawBehind {
        if (r > 0f) {
            drawCircle(shadow, r * 1.23f, below + Offset(0f, r * .03f))
            drawCircle(ambient, r * 1.30f, below)
            drawCircle(halo, r * 1.07f, centre)
        }
    }
}
