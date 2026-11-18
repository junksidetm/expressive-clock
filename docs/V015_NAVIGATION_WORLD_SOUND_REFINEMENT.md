# v0.1.5 navigation, World Clock and sound refinement

Implemented locally on `main`, preserving the pre-existing uncommitted work.

## Implemented

- Four icons in the original frosted dock: Alarm, World Clock, Timer, Stopwatch. The pill grows from 204dp to 252dp, with equal spacing, the existing circular selection spring and responsive width constraints. At 320dp the four tab targets calculate to 55.5dp each. Plus remains separate; Timer/Stopwatch retain their existing in-content actions and hidden dock Plus.
- Removed Alarm/World horizontal paging, dots, visibility setting and discovery animation. Removed the obsolete settings model/read/write fields; old stored keys are harmless and ignored. Alarm card time formatting remains independent.
- World selects a separate single-pane screen. In two-pane mode its content resolves to the existing Alarm-left/World-right view, without recreating a duplicate screen when switching between Alarm and World. Dock selection still determines whether Plus opens New Alarm or Add Location.
- Removed the large Add Location content button. Location search uses the Alarm sheet shape, drag handle, Cancel control, typography, padding, rounded results and keyboard insets. Selecting a result immediately adds it without a Save step.
- Fixed hand retargeting by replacing separately updated zone/correction animatables with one immutable motion snapshot. Drawing retains the prior motion until both new corrections and the target zone are published atomically. Rapid retargets sample in-flight positions; live ZoneId/DST calculations and shortest paths remain intact. System animation duration scaling is respected.
- Analogue/digital crossfade begins at scroll onset and completes over the first 38% of collapse. The digital clock follows the shrinking analogue centre upward, then remains pinned when collapsed. The hidden analogue renderer stops its frame ticker but retains its motion state. Hidden clock semantics are cleared during the crossfade.
- Reused the established progressive edge blur/fade for the scrolling city list, with a short fade-out at the end of the list and bottom clearance so the final row remains readable. Corrected new-city entrance scroll estimates after removing the Add button.
- Replaced loose editor sound buttons with a shared Alarm Sound sheet used by New, Edit and inline editors. Includes system alarm sounds, explicit Default and Silent choices, OpenDocument custom files, selected filename, Preview/Stop and Done/Cancel. Persistent read grants, timeout/lifecycle preview cleanup and the existing AlarmAudio fallback/audio-focus implementation are retained.
- Added explicit dock Tab/Button accessibility roles, selected state and Plus accessibility actions.

## Files changed in this pass

Modified:

- `app/src/main/java/app/expressiveclock/ui/ClockApp.kt`
- `app/src/main/java/app/expressiveclock/ui/ClockDock.kt`
- `app/src/main/java/app/expressiveclock/data/Settings.kt`
- `app/src/main/java/app/expressiveclock/data/ClockStore.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/SettingsScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockHeader.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AddCitySheet.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmEditSheet.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/NewAlarmPickerSheet.kt`

Created:

- `app/src/main/java/app/expressiveclock/ui/screens/ClockHandMotion.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmSoundSheet.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/SelectionSheetHeader.kt`
- `app/src/test/java/app/expressiveclock/ui/screens/ClockHandMotionTest.kt`
- This report.

Deleted, as explicitly superseded by this refinement (both were previously untracked):

- `app/src/main/java/app/expressiveclock/ui/AlarmWorldPages.kt` — retained its alarm-card composition local in ClockApp.
- `app/src/main/java/app/expressiveclock/ui/sheets/CustomAlarmSoundControls.kt` — retained its sound-title resolution and persistence/playback behavior in the new selector.

## Verified

- Final `./gradlew assembleDebug testDebugUnitTest`: **BUILD SUCCESSFUL**, all **31 unit tests passed**. Four new tests cover initial continuity, rapid in-flight retargeting, live settled targets and disabled motion. Existing timezone/DST, alarm/audio, notification and widget tests pass.
- `git diff --check` passed.
- Installed debug APK on Pixel 11 Pro Fold and OPPO Find N6.
- OPPO portrait: four-icon dock appearance, contextual New Alarm/World Plus, Add Location search with keyboard visible, shared sound sheet, default preview/stop, explicit Silent indication, system sound enumeration, custom-file selection and filename in the Sound row. Unsaved editor draft cancelled.
- OPPO OpenDocument probe: persisted custom-file grant and playback survived process restart. Probe released the test grant; temporary WAV removed.
- Existing `scripts/adb_alarm_probe.py --serial ...` passed on **both phones**: custom WAV URI playback, invalid URI and bundled fallback, audio-focus loss/restoration, exact scheduled alarm/ringer, stale start protection, native timer expiry, upcoming notification action and persisted occurrence skip. The probe cleans up its own test state; speaker audibility is not asserted.
- Both phones: two-pane Alarm-left/World-right arrangement and contextual Plus. OPPO inline location picker and alarm editor use the same sound selector. Pixel partial/collapsed header and downward reversal observed through UI hierarchy; city and collapsed state retained when returning to portrait.
- Temporary Android device-state overrides restored to none; Pixel display-size override restored. The 320dp device check was inconclusive due to system-surface interference; width/touch sizing was checked from layout constraints, not claimed as a passed device test.
- Compared protected source files with the starting snapshot: all 38 alarm/audio, widget, timer, stopwatch and ViewModel files are byte-for-byte unchanged.

## Remaining review limits

No reliable frame-by-frame motion recording or performance profile was obtained. Rapid-selection continuity is unit-tested; animation feel still merits hands-on review. Pixel screenshots can contain stale Recents surfaces; OPPO sheet captures are more reliable. Exhaustive dark/pure-black, large-font and rotation combinations were not visually tested in this pass. Narrow-width device capture was inconclusive. No unrelated tests or release operations were performed.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug.apk`

Screenshots, app UI hierarchies, build and device-probe logs: `/Users/sambeckman/Downloads/ExpressiveClock-refinement-review-20261117-105744`.

Final Git branch: `main`. Working tree remains dirty with pre-existing modified/untracked work plus this pass. Nothing staged, committed, pushed, merged, tagged or published.
