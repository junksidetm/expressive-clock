# Running timer display-size protection

Read AGENTS.md and inspected main's existing uncommitted diff. Modified only `app/src/main/java/app/expressiveclock/ui/screens/TimersScreen.kt`; created this report; no files deleted.

The non-auto-sized ticker previously scaled a row after fixed-width children had already been measured under narrow constraints. The trailing cell could therefore be clipped before graphics scaling. Digit/colon cell widths, spacing and numeral cap heights now fit before measurement. Existing ticker direction, stagger, blur and countdown logic remain unchanged. Running faces in both phone and wide layouts reserve 40dp horizontal clearance inside the ring. Phone circle sizing now uses the actual available width rather than requesting an extra 24dp that parent constraints removed. Original nominal numeral dimensions remain when they fit.

`./gradlew assembleDebug testDebugUnitTest`: successful, 61 tests passed. `git diff --check`: passed. Installed compatible debug APK on Pixel at its existing density override 540/font scale 1.0. Started an 11m11s temporary test timer from an empty setup, inspected ticking countdown clearance, then cancelled it. Display/font settings were not changed. Wide layout built but was not separately device-tested; no exhaustive font-size matrix.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-running-timer-fit-20261118-103337.apk`
Screenshot: `/Users/sambeckman/Downloads/ExpressiveClock-running-timer-fit-20261118-103337.png`

Branch main; all existing modified/untracked v0.1.5 work remains local and uncommitted. No staging, pushing or publishing.
