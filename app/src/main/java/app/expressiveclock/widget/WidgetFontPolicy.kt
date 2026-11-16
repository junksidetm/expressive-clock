package app.expressiveclock.widget

import app.expressiveclock.data.WidgetFontFamily

/** Presentation choice only: requested preferences are never rewritten by capability checks. */
internal enum class NativeFontKind { GOOGLE_FLEX, CONDENSED, SANS, SERIF, MONOSPACE, CURSIVE }

internal object WidgetFontPolicy {
    fun choose(requested: WidgetFontFamily, exactGoogle: Boolean, condensedAvailable: Boolean): NativeFontKind =
        when (requested) {
            WidgetFontFamily.GOOGLE_SANS -> when {
                exactGoogle -> NativeFontKind.GOOGLE_FLEX
                condensedAvailable -> NativeFontKind.CONDENSED
                else -> NativeFontKind.SANS
            }
            WidgetFontFamily.GOOGLE_SANS_NORMAL -> if (exactGoogle) NativeFontKind.GOOGLE_FLEX else NativeFontKind.SANS
            WidgetFontFamily.SERIF -> NativeFontKind.SERIF
            WidgetFontFamily.MONOSPACE -> NativeFontKind.MONOSPACE
            WidgetFontFamily.CURSIVE -> NativeFontKind.CURSIVE
        }
}
