package app.expressiveclock.ui

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private data class MechanicalGlyph(val text: String, val index: Int, val glyph: String)

/** Grapheme cells preserve spaces, accents and supplementary characters. */
internal fun mechanicalCells(text: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
    return buildList {
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            add(text.substring(start, end)); start = end; end = iterator.next()
        }
    }
}

internal fun mechanicalChanges(previous: String, next: String): List<Int> {
    val old = mechanicalCells(previous)
    val updated = mechanicalCells(next)
    return updated.indices.filter { updated[it] != old.getOrNull(it) }
}

internal fun mechanicalStagger(index: Int, selection: Boolean): Int =
    (index * if (selection) 22 else 18).coerceAtMost(if (selection) 180 else 72)

/** Fixed outer bounds and smoothly positioned cells prevent name-length changes from jumping. */
@Composable
internal fun MechanicalText(
    text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier,
    digits: Boolean = false, selectionOnly: Boolean = false, selectionKey: Any = text, active: Boolean = true,
    progressiveBlur: Boolean = false,
    colorAt: (Int) -> Color = { color },
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val cells = remember(text) { mechanicalCells(text) }
    var previousText by remember { mutableStateOf(text) }
    var previousKey by remember { mutableStateOf(selectionKey) }
    var retainedCount by remember { mutableIntStateOf(cells.size) }
    val previousCells = remember(previousText) { mechanicalCells(previousText) }
    val selection = remember(text) { previousKey != selectionKey || !digits }
    val changed = remember(text) { mechanicalChanges(previousText, text) }
    SideEffect { previousText = text; previousKey = selectionKey; retainedCount = maxOf(retainedCount, cells.size) }
    val animate = active && ValueAnimator.areAnimatorsEnabled()
    val viewportHeight = with(density) { (style.lineHeight.takeIf { it.value.isFinite() } ?: style.fontSize * 1.3f).toDp() }
    BoxWithConstraints(modifier.height(viewportHeight)
        .clearAndSetSemantics { contentDescription = text }) {
        // Paragraph caret advances preserve kerning and actual word spaces. Measuring each
        // letter (or a space's ink box) separately can collapse spaces and crowd narrow glyphs.
        val rawWidth = measurer.measure(text, style).size.width.toFloat()
        val fit = minOf(1f, with(density) { maxWidth.toPx() } / rawWidth.coerceAtLeast(1f))
        val fitted = style.copy(fontSize = style.fontSize * fit, lineHeight = style.lineHeight * fit)
        if (!animate) {
            Text(buildAnnotatedString { cells.forEachIndexed { i, char -> withStyle(SpanStyle(color = colorAt(i))) { append(char) } } }, style = fitted, color = color, maxLines = 1,
                modifier = Modifier.align(Alignment.Center))
        } else {
            val digitWidth = (0..9).maxOf { measurer.measure(it.toString(), fitted).size.width }.toFloat()
            val paragraph = measurer.measure(text, fitted)
            var offset = 0
            val widths = cells.map { glyph ->
                val start = paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                offset += glyph.length
                val end = paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                if (digits && glyph.firstOrNull()?.isDigit() == true) digitWidth else abs(end - start)
            }
            val fullWidth = widths.sum()
            val measuredCellWidth = maxOf(digitWidth, (cells + previousCells + "W").maxOfOrNull {
                measurer.measure(it, fitted).size.width.toFloat()
            } ?: 1f)
            var widestCell by remember(fitted) { mutableFloatStateOf(measuredCellWidth) }
            val cellWidth = maxOf(widestCell, measuredCellWidth)
            SideEffect { widestCell = cellWidth }
            repeat(maxOf(retainedCount, cells.size)) { index ->
                val char = cells.getOrElse(index) { " " }
                // Remember this policy per target glyph: a normal seconds update must not
                // interrupt an unchanged hour/minute reel already moving to a selected city.
                val animateThisChange = remember(char) { !selectionOnly || selection }
                val before = widths.take(index).sum()
                val centre = (with(density) { maxWidth.toPx() } - fullWidth) / 2f + before +
                    widths.getOrElse(index) { 0f } / 2f
                val position by animateFloatAsState(centre - cellWidth / 2f,
                    tween(if (digits) 250 else 420, easing = FastOutSlowInEasing), label = "reelPosition")
                // New slots in a longer title must begin with the previous paragraph's
                // (empty) strip, not the target glyph, until their staggered turn arrives.
                var staged by remember {
                    mutableStateOf(MechanicalGlyph(previousText, index, previousCells.getOrElse(index) { "" }))
                }
                LaunchedEffect(text, char) {
                    val next = MechanicalGlyph(text, index, char)
                    if (next != staged) {
                        val rank = if (digits) changed.indexOf(index).coerceAtLeast(0) else index
                        if (animateThisChange) delay(mechanicalStagger(rank, selection).toLong())
                        staged = next
                    }
                }
                AnimatedContent(staged,
                    modifier = Modifier.width(with(density) { cellWidth.toDp() }).fillMaxHeight()
                        .graphicsLayer { translationX = position }
                        .then(if (progressiveBlur) Modifier.wrapContentHeight(unbounded = true)
                            .requiredHeight(viewportHeight + 16.dp)
                            .featheredVerticalViewport(active = true, fadeHeightFraction = .18f)
                        else Modifier.clipToBounds()),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        if (!animateThisChange) EnterTransition.None togetherWith ExitTransition.None
                        else ((slideInVertically(spring(dampingRatio = 1f, stiffness = 420f)) { it } + fadeIn(tween(100))) togetherWith
                            (slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(100))))
                            .using(SizeTransform(clip = !progressiveBlur))
                    }, label = "mechanicalCell") { reel ->
                    val tilt = if (animateThisChange) {
                        val angle by transition.animateFloat(transitionSpec = { tween(230) }, label = "reelTilt") { state ->
                            when (state) { EnterExitState.PreEnter -> -35f; EnterExitState.Visible -> 0f; EnterExitState.PostExit -> 35f }
                        }
                        angle
                    } else 0f
                    val blur = if (progressiveBlur && animateThisChange) {
                        val travel by transition.animateFloat(transitionSpec = {
                            if (targetState == EnterExitState.Visible) spring(dampingRatio = 1f, stiffness = 420f)
                            else tween(180, easing = FastOutSlowInEasing)
                        }, label = "nameEdgeBlur") { state ->
                            when (state) {
                                EnterExitState.PreEnter -> 1f
                                EnterExitState.Visible -> 0f
                                EnterExitState.PostExit -> -1f
                            }
                        }
                        val edge = (abs(travel) / .75f).coerceIn(0f, 1f)
                        edge * edge * (3f - 2f * edge) * 4f
                    } else 0f
                    val glyphModifier = Modifier.blur(blur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded).graphicsLayer {
                            rotationX = tilt; cameraDistance = size.height * 8f
                            transformOrigin = TransformOrigin.Center
                        }
                    if (digits) Text(reel.glyph, style = fitted, color = colorAt(index), maxLines = 1, modifier = glyphModifier)
                    else {
                        // Reassemble clipped strips of the shaped paragraph, rather than laying out
                        // isolated letters. Narrow glyphs now keep their true bearings and kerning.
                        val source = remember(reel.text, fitted) { measurer.measure(reel.text, fitted) }
                        val sourceCells = remember(reel.text) { mechanicalCells(reel.text) }
                        val startOffset = sourceCells.take(reel.index).sumOf { it.length }
                        val endOffset = startOffset + (sourceCells.getOrNull(reel.index)?.length ?: 0)
                        val startX = source.getHorizontalPosition(startOffset, true)
                        val endX = source.getHorizontalPosition(endOffset, true)
                        val advance = abs(endX - startX)
                        Canvas(glyphModifier.width(with(density) { cellWidth.toDp() }).height(viewportHeight)) {
                            if (advance > 0f) {
                                val left = (size.width - advance) / 2f
                                clipRect(left = left, right = left + advance) {
                                    drawText(source, color = colorAt(index), topLeft = Offset(
                                        left - min(startX, endX), (size.height - source.size.height) / 2f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
