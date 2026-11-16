package app.expressiveclock.widget

import app.expressiveclock.data.WidgetFontFamily
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetFontPolicyTest {
    @Test fun exactGoogleIsPreferredForBothRequestedGoogleStyles() {
        listOf(WidgetFontFamily.GOOGLE_SANS, WidgetFontFamily.GOOGLE_SANS_NORMAL).forEach {
            assertEquals(NativeFontKind.GOOGLE_FLEX, WidgetFontPolicy.choose(it, true, true))
        }
    }
    @Test fun unavailableGoogleCondensedFallsBackThroughBothLevels() {
        assertEquals(NativeFontKind.CONDENSED, WidgetFontPolicy.choose(WidgetFontFamily.GOOGLE_SANS, false, true))
        assertEquals(NativeFontKind.SANS, WidgetFontPolicy.choose(WidgetFontFamily.GOOGLE_SANS, false, false))
    }
    @Test fun flexDoesNotAccidentallyBecomeCondensed() {
        assertEquals(NativeFontKind.SANS, WidgetFontPolicy.choose(WidgetFontFamily.GOOGLE_SANS_NORMAL, false, true))
    }
    @Test fun nonGoogleStylesDoNotDependOnGoogleCapability() {
        listOf(false, true).forEach { available ->
            assertEquals(NativeFontKind.SERIF, WidgetFontPolicy.choose(WidgetFontFamily.SERIF, available, available))
            assertEquals(NativeFontKind.MONOSPACE, WidgetFontPolicy.choose(WidgetFontFamily.MONOSPACE, available, available))
            assertEquals(NativeFontKind.CURSIVE, WidgetFontPolicy.choose(WidgetFontFamily.CURSIVE, available, available))
        }
    }
}
