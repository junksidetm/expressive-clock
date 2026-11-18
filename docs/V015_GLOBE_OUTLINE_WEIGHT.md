# Globe outline weight

Read AGENTS.md and inspected main's existing working tree. Modified only `app/src/main/java/app/expressiveclock/ui/RotatingGlobe.kt`. Created this report; deleted nothing.

Replaced the fixed 1.6dp outline with the same proportional 2/24 viewport weight used by the neighbouring Material dock icons (2.25dp at the dock's 27dp icon size). Radius reserves half the stroke to prevent edge clipping. Land paths, colouring and 3D rotation remain unchanged.

`./gradlew assembleDebug`: successful. `git diff --check`: passed. Source geometry compared against the 24-unit StopwatchCase outline. No device visual check or unit-test rerun for this drawing-only change.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-globe-outline-20261118-104210.apk`

Branch main; existing extensive modified/untracked work preserved. Nothing staged, committed, pushed or published.
