# Globe outline optical weight

Increased the globe's proportional outline from 2 to 3 units per 24-unit viewport (50% thicker, 3.375dp at 27dp icon size) following visual feedback. Radius continues to reserve half the stroke to avoid clipping. Colours, continent shapes and rotation unchanged.

Modified: `app/src/main/java/app/expressiveclock/ui/RotatingGlobe.kt`. Created: this report. No deletions. Read AGENTS.md and inspected existing changes on main; all pre-existing work preserved.

`./gradlew assembleDebug`: succeeded. `git diff --check`: passed. No device check or unit-test rerun for this drawing adjustment.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-globe-outline-bolder-20261118-104448.apk`

Branch main, working tree retains existing modified/untracked changes. Nothing staged, committed, pushed or published.
