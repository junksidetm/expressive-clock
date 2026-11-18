# Persistent scroll falloffs

Root cause: scroll boundaries drove blur strength through canScrollForward/canScrollBackward booleans. At an endpoint the blur layer switched off even though the visible controls had barely moved.

A shared persistentScrollEdges modifier now measures unpadded content versus viewport height. Overflowing content gets stationary top/bottom 32dp falloffs and matching scrollable end gutters. No blur strength depends on scroll offset. Fitting content retains original geometry with no blur. Applied to Timer body, wide scroll panes, onboarding and New/Edit Alarm forms. The World Clock grid uses fixed bands with 40dp end padding (top increased from 10dp), keeping badges and final rows clear at rest. Existing blur renderer and older-Android fade fallback retained. No global scaling overrides, continuous glow work, dependencies or version changes.

## Verification

`./gradlew assembleDebug testDebugUnitTest`: successful; 61 tests, zero failures. `git diff --check`: passed. Installed compatible debug update on Pixel without clearing data/uninstalling. Inspected enlarged Timer end and near-end states, reversed scrolling and World Clock grid falloff. Pixel already had density override 540/font scale 1.0; no display settings changed. Other shared consumers were build-verified but not each separately exercised on device. No exhaustive unfolded/dark-mode matrix in this pass.

## Files

Modified:
- `app/src/main/java/app/expressiveclock/ui/screens/TimersScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/OnboardingScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt`
- `app/src/main/java/app/expressiveclock/ui/responsive/ScrollPane.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`

Created: `app/src/main/java/app/expressiveclock/ui/responsive/PersistentScrollEdges.kt`, this report. No files deleted.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-persistent-blur-20261118-093743.apk`
Screenshots: `/Users/sambeckman/Downloads/ExpressiveClock-persistent-blur-checks-20261118-093743`

Branch main; extensive existing modified/untracked work preserved. Nothing staged, committed, pushed or published.
