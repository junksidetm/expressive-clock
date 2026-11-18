# City glow padding alignment

Read AGENTS.md and inspected main's existing uncommitted work. Modified only `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`; created this report; no deletions.

LazyGrid item offsets are relative to unpadded item space, but the viewport-owned glow draws in padded viewport space. Added the measured beforeContentPadding to its Y coordinate. Centred short lists therefore no longer show a detached glow above the selected card. Existing row extrapolation, offscreen halo continuity, selection fade and autoscroll remain unchanged.

`./gradlew assembleDebug testDebugUnitTest`: successful; 61 tests passed. `git diff --check`: passed. Installed compatible debug update on unfolded Pixel and inspected Sydney, San Francisco and Tokyo selection. Halo now aligns under each selected card, with no detached patch above the centred grid. Left San Francisco selected. No saved locations or display settings changed. Long overflowing lists were not separately retested for this coordinate-only correction.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-glow-alignment-20261118-133040.apk`
Verified San Francisco screenshot: `/Users/sambeckman/Downloads/ExpressiveClock-glow-alignment-20261118-133040.png`

Branch main; existing extensive modified/untracked work preserved. Nothing staged, committed, pushed or published.
