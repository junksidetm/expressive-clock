# Status-bar icon contrast fix

Modified `app/src/main/java/app/expressiveclock/ui/theme/Theme.kt` only. AndroidX Activity's existing edge-to-edge implementation retains its first configuration listener. Fixed the theme detector to read `rememberUpdatedState(dark)` so later configuration callbacks respect app-selected dark/light/system mode after saved preferences load. Also restore current icon appearance when the Activity window regains focus, using a registered/disposed window-focus listener. No polling, delays or settings changes. Pure-black uses the existing effective dark flag. Main navigation and dialog layouts are unchanged.

Inspected bundled Activity 1.13.0 and SplashScreen 1.0.1 sources for system-bar handling. Existing uncommitted MainActivity and other v0.1.5 changes inspected and preserved. No Installer Helper changes in this task.

Verification: `./gradlew assembleDebug testDebugUnitTest` succeeded, 55 tests, zero failures/errors. `git diff --check` passed. Phones were securely locked when physical visual validation was attempted; white/black icon appearance has not been device-verified in this pass. No device settings or installed Clock versions changed during this task.

APK: [ExpressiveClock-debug.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug.apk), copy hash matches build output. No existing APK overwritten.

Files modified: Theme.kt. Created: this report. Deleted: none. Final branch main; existing modified/untracked work remains local. Nothing staged, committed, pushed or published.
