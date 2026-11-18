package app.expressiveclock.ui

import org.junit.Assert.*
import org.junit.Test

class StartupNavigationTest {
    @Test fun everyStartupPageWaitsForSettingsThenHydratesOnce() {
        for (page in Tab.entries) {
            assertNull(resolveStartupNavigation(null, null, 0, null, false, page).tab)
            assertEquals(page, resolveStartupNavigation(null, null, 0, null, true, page).tab)
            assertEquals(Tab.WORLD, resolveStartupNavigation(Tab.WORLD, null, 0, null, true, page).tab)
        }
    }

    @Test fun consumedAlarmLaunchDoesNotOverrideWorldAfterResizeOrRotation() {
        assertEquals(StartupNavigation(Tab.WORLD, 7L),
            resolveStartupNavigation(Tab.WORLD, "alarms", 7, 7, false, Tab.ALARMS))
    }

    @Test fun newDirectIntentOverridesRestoredTabIncludingRepeatedSameDestination() {
        for (requested in Tab.entries) {
            assertEquals(StartupNavigation(requested, 8L),
                resolveStartupNavigation(Tab.WORLD, requested.key, 8, 7, true, Tab.ALARMS))
        }
    }

    @Test fun launcherAndUnknownIntentsPreserveCurrentSelection() {
        for (key in listOf(null, "unknown"))
            assertEquals(Tab.WORLD, resolveStartupNavigation(Tab.WORLD, key, 8, 7, true, Tab.ALARMS).tab)
    }
}
