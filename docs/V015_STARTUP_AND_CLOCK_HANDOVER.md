# v0.1.5 Startup page and World Clock handover

Implemented:
- Added WORLD (World Clock) to DefaultStartupPage and mapped it to the existing World tab. The existing radio dialog automatically lists the fourth destination in navigation order: Alarms, World Clock, Timer, Stopwatch.
- Persistence remains the existing DataStore name-based preference; no migration or resetting of existing choices. Explicit intent routing and one-time startup hydration are unchanged. The existing expanded-layout contentTab mapping reuses the combined Alarm/World Clock screen while retaining World navigation selection.
- Header opacity is derived directly from the same collapse progress: analogue fades from 1 to 0 over 0–45%; both clocks are hidden over 45–55%; digital fades from 0 to 1 over 55–100%. No separate opacity animations can lag behind or overlap during reversal. Header geometry, coordinated scroll movement, subtitle positioning, snapping, live time and mechanical transitions are preserved. Hidden analogue rendering is suspended as before.

Files modified this task:
- app/src/main/java/app/expressiveclock/data/Settings.kt
- app/src/main/java/app/expressiveclock/ui/ClockApp.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockHeader.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockSnap.kt

Files created:
- app/src/test/java/app/expressiveclock/ui/StartupPageTest.kt
- app/src/test/java/app/expressiveclock/ui/screens/WorldClockHeaderVisibilityTest.kt
- docs/V015_STARTUP_AND_CLOCK_HANDOVER.md

No files deleted.

Verification:
- ./gradlew assembleDebug testDebugUnitTest: BUILD SUCCESSFUL; 48 tests, zero failures/errors.
- Added tests cover compatibility of persisted names/navigation keys, visibility endpoints/handover, every 0.0001 progress step forward/backward and abrupt progress reversals. At every tested progress at least one clock opacity is zero. Existing snap tests continue passing.
- git diff --check passed. Compared all app/src and docs files against a task-start SHA256 manifest: only the four intended existing files changed; none missing.
- Installed on OPPO Find N6. Confirmed World Clock, Timer and Stopwatch preferences each survive force-stop/relaunch with the correct content/navigation. Restored original Alarms preference, checked its DataStore value and relaunched with Alarm selected.
- Confirmed a cold-launch Timer intent overrides the saved World Clock default.
- Used the OPPO supported OPENED device state to check World startup in the combined two-pane layout: one Alarm pane and one World pane, World selected, no duplicate screen. Device-state override removed afterward.
- Captured a held intermediate drag showing both clocks hidden while timezone/date and continuous header/list layout remain. Exercised quick collapse/expand/collapse gestures. Screenshot evidence and build log: /Users/sambeckman/Downloads/ExpressiveClock-startup-handover-20261117-133920

Limits: Pixel device checks were not repeated. OPPO accessibility snapshots became stale after changing display state; unfolded outcomes were verified from actual screenshots and window configuration. No frame-by-frame video or frame-rate profiling was performed; exhaustive overlap protection is verified by the pure visibility mapping tests rather than screenshot sampling alone. No other notification/alarm intent categories were individually exercised; their routing code remains unchanged.

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-startup-handover-20261117-133920.apk

Final branch: main. Working tree remains dirty with existing uncommitted v0.1.5 work preserved. Nothing staged, committed, pushed, merged or published. Existing Downloads APKs were not overwritten; the standard ExpressiveClock-debug.apk path was absent and was also populated with this build.
