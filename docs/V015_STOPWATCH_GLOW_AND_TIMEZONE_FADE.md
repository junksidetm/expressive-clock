# Stopwatch lighting, dock hand and timezone fade

Changes:
- StopwatchScreen.kt uses the existing shared clockLighting draw modifier outside the face's circular clip. Both portrait and wide stopwatch layouts render that same face, so they share the World Clock's theme-aware static halo/elevation treatment without changing stopwatch layout, timing or controls.
- ClockDock.kt and new StopwatchDockIcon.kt keep Material Outlined.Timer's exact case geometry stationary while rotating only its inner hand around the dial pivot. Normal taps complete one revolution in 420ms; interrupted taps continue from the current angle and settle at twelve. Existing selected highlight/scale/semantics remain. Reduced-motion disables the spin. The static outline is adapted from Apache-2.0 Material Icons with attribution in source.
- WorldClockHeader.kt provides a shared WorldClockSubtitle for phone and WideClockPages.kt. One shaped text line fades out for 140ms, changes only while transparent, then fades in for 180ms. Rapid switches cancel and continue from current opacity. Reduced motion updates immediately. Existing analogue/digital/header/city-name mechanics remain unchanged.

Modified existing files: ui/ClockDock.kt, ui/screens/StopwatchScreen.kt, ui/screens/WorldClockHeader.kt and ui/screens/WideClockPages.kt. Created: ui/StopwatchDockIcon.kt and this report. No files deleted, dependencies or versions changed. All source paths are under app/src/main/java/app/expressiveclock unless noted.

Verification: `./gradlew assembleDebug testDebugUnitTest` succeeded; 55 tests, zero failures/errors. `git diff --check` passed. Reviewed scoped changes against pre-task copies and checked the split icon geometry against bundled Material icon source. No connected-device or rendered animation tests run, per the user's request; visual appearance/timing remain for owner review.

APK: [ExpressiveClock-debug-stopwatch-glow-20261117-204024.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-stopwatch-glow-20261117-204024.apk); copied without overwriting an existing APK, SHA-256 matches build output. Installer Helper delivery remains inactive; no upload or notification attempted.

Branch main, existing modified/untracked v0.1.5 work preserved. No staging, commits, pushes, merges or publication.

## Follow-up: timezone cross-dissolve

Replaced the shared subtitle's sequential fade-out/replace/fade-in with Compose Crossfade: outgoing and incoming timezone/date lines animate simultaneously over 240ms with linear opacity. No blank handover interval, text-colour animation or black overlay. Each layer renders its own captured string; Compose handles interrupted transitions. Reduced motion switches immediately. Shared by phone and unfolded World Clock layouts.

Only WorldClockHeader.kt and this report modified; no files created/deleted. `./gradlew assembleDebug testDebugUnitTest` succeeded: 55 tests, 0 failures, 0 errors. `git diff --check` passed. No device tests performed per user request.

APK: [ExpressiveClock-debug-timezone-dissolve-20261117-205341.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-timezone-dissolve-20261117-205341.apk), hash verified against build output, no existing APK overwritten. Main branch; prior working-tree changes preserved. No staging, commits, pushes or publication.
