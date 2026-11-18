# World Clock globe resting view

Modified RotatingGlobe.kt to center its resting longitude on 135°E (Australia region), retaining the existing sphere projection, icon size/colour and continent geometry. Normalize phase before trigonometry so the beginning and end of complete rotations yield identical projection inputs. ClockDock.kt now targets the next complete revolution and rests at zero even after rapid interrupted taps; the existing 600ms World animation and 420ms stopwatch animation are preserved. Reduced motion still skips spinning.

Created GlobeRotationTest.kt with two tests for the Australia meridian and exact positive/negative full-turn periodicity, plus this report. No files deleted. All source paths under app/src/main/java/app/expressiveclock/ui and test under app/src/test/java/app/expressiveclock/ui.

`./gradlew assembleDebug testDebugUnitTest` succeeded: 57 tests, zero failures/errors. `git diff --check` passed. No connected-device visual tests performed, per user preference; resting appearance remains for owner review.

APK: [ExpressiveClock-debug-australia-globe-20261117-205909.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-australia-globe-20261117-205909.apk); copy hash matches build output, no existing APK overwritten. Branch main; existing modified/untracked changes preserved. Nothing staged, committed, pushed or published.
