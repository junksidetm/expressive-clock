package app.expressiveclock.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Explicit cuts of the bundled Google Sans Flex; weights are baked into the variable instance. */
internal object WorldClockType {
    fun city(size: TextUnit = 18.sp) = cut(size, 94f, 560, 1.12f)
    fun secondary(size: TextUnit = 13.sp) = cut(size, 98f, 400, 1.2f)
    fun time(size: TextUnit = 32.sp, width: Float = 82f, weight: Int = 430) = cut(size, width, weight, 1.08f).copy(fontFeatureSettings = "tnum, lnum")

    // Card cuts are independent of the mechanical header/readout typography.
    fun cardCity() = cut(19.sp, 96f, 580, 1.10f, roundness = 35f)
    fun cardDate() = cut(13.5.sp, 100f, 400, 1.18f)
    fun cardTime(size: TextUnit = 34.sp) = cut(size, 92f, 480, 1.06f, roundness = 25f)
        .copy(fontFeatureSettings = "tnum, lnum")

    private fun cut(size: TextUnit, width: Float, weight: Int, leading: Float, roundness: Float = 0f) = TextStyle(
        fontFamily = ClockFace.family(opticalSize = size.value, width = width, weight = weight, roundness = roundness),
        fontWeight = FontWeight(weight),
        fontSynthesis = FontSynthesis.None,
        fontSize = size,
        lineHeight = size * leading,
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}
