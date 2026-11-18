# Adaptive UI refinement: second pass

Reviewed AGENTS.md and existing changes on main. Extended the existing uncommitted work in place.

- New/Edit Alarm: compact layouts place the title above the Cancel/Save action row. The scrolling form has progressive blur/fade at overflowing edges, and the repeating time wheels have progressive falloff at both ends.
- Settings dialogs: compact/enlarged layouts enforce 24dp external side gutters instead of allowing Material minimum-width constraints to consume the screen. Normal-size dialogs retain platform sizing. Choice lists can scroll.
- Timer: compact/enlarged layouts reserve 16dp between the scrolling body/presets and controls. Normal layout spacing is unchanged.
- Pill controls: measure real label width and available room after icon/padding, fitting the label only when needed. This prevents clipping of Stopwatch Start/Stop and other shared pill labels without globally overriding accessibility scaling.

## Verification

`./gradlew assembleDebug testDebugUnitTest` succeeded (61 tests, zero failures). `git diff --check` passed.

Installed compatible debug APK on connected Pixel without uninstalling or clearing data. Inspected enlarged New Alarm and Edit Alarm headers/wheels, Settings alarm-format dialog gutters, Timer body falloff and Stopwatch Start label. Device density was already overridden to 540 and font scale 1.0 at task start; neither setting was changed in this pass. No alarm was saved or deleted for these checks. Not an exhaustive audit of every accessibility size, theme, folded/unfolded state or manufacturer.

## Files

Modified:
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/SettingsScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/TimersScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/StackedDigits.kt`

Created: this report. No source files created or deleted; no dependencies/version changes.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-adaptive-ui-20261118-093003.apk`
Screenshots: `/Users/sambeckman/Downloads/ExpressiveClock-adaptive-ui-checks-20261118-093003`

Branch: main. Extensive existing modified/untracked work remains local; nothing staged, committed, pushed or published.
