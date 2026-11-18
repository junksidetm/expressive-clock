# v0.1.5 implementation and verification — 2026-11-17

## Outcome

All requested implementation areas have local changes: alarm audio focus and file
playback/preview/recovery, native upcoming notifications with single-occurrence
dismissal, responsive alarm/timer/stopwatch fixes, primary-palette alarm colours,
post-save duration confirmation, portrait World Clock, and consistent/live time
format handling. The hybrid widget implementation is unchanged.

Audio uses one shared, lifecycle-controlled native player engine with alarm audio
attributes, transient focus, asynchronous descriptor acquisition/preparation,
fallback timeout and an original packaged PCM tone. It never changes stream volume
or DND. Explicit Silent remains available. MediaPlayer's wake lock is held only for
actual playback and released with the player. Foreground starts are serialized;
stale/deleted alarm starts do not replace the active ringer. Timer expiry rejects
paused or not-yet-due callbacks.

Upcoming notifications use AlarmManager events, not polling. Immutable occurrence
actions and atomic DataStore edits preserve repeat rules and reject stale events.
Consumed/skipped timestamps plus local date prevent same-day replay after zone
rescheduling. Label/sound-only edits preserve dismissal; time/repeat changes and
explicit re-arming clear it. Notification permissions and channels are respected.

PR #5 was reviewed selectively; useful sizing/scroll ideas were adapted. No merge,
signing change or unrelated proposed visual restyling was adopted.

## Verification

- ./gradlew assembleDebug testDebugUnitTest: SUCCESS; 24 Kotlin tests, zero failures.
- python3 -m unittest discover -s tests: 12 checks passed, including non-silent,
  unclipped packaged PCM validation.
- git diff --check: clean.
- ./gradlew lintDebug: FAILED with four pre-existing errors in unchanged files
  (WidgetColours.kt:94, two API-31 colour calls; values/themes.xml:19, API-27
  cutout attribute; quicksettings/LaunchTileServices.kt:36, deprecated tile launch).
  The report also contains 400 warnings and six hints. Relevant composition,
  observable-locale and notification-permission findings in touched code were
  resolved. The unrelated baseline findings were not suppressed or changed.
- Debug builds installed with install -r, preserving app data on both devices.

### Pixel

Serial 66231FDDJ003QS, Pixel_11_Pro_Fold/yogi_beta, Android 17.
Earlier unlocked session builds passed actual custom WAV playback, invalid-URI
recovery, media focus loss/restoration, exact alarm callback/foreground ringer,
deferred 30-minute notification callback, actual dismiss action and repeat retention.
Pixel later locked; final build is installed but later audio/state refinements,
new live-format listener and final UI were not rerun on an unlocked Pixel.
Do not count the locked app-smoke attempt as a visual pass.

### OPPO

Serial 3B161P00QM400000, CPH2765/OP6271L1 (Find N6), Android 16.
Unlocked probes passed actual custom WAV URI playback, invalid-URI fallback,
packaged resource playback, transient media-focus loss and restoration, exact
snooze callback, one-shot disarming, native timer expiry/foreground ringer,
deferred upcoming notification, actual notification PendingIntent and persisted
single-occurrence skip retaining future repeat.

Real OpenDocument choice → UI preview/stop → cancelled draft → process restart →
playback of the exact persisted URI passed. The grant and uploaded fixture were
cleaned up. System alarm_alert checked unchanged after picker exploration.

Alarms, World Clock, timers, stopwatch and keyboard layouts captured and inspected.
Weekday strip/switch, keypad/presets, controls/dock and keyboard/name separation
were observed. Stopwatch text bounds remained identical across changing digits;
the initially zero stopwatch was paused/reset to zero. All original three alarms
remained after deleting the own ADB confirmation fixture. Typed label was preserved
and “Alarm in 6 h 39 min” was captured above the dock. World rows were corrected for
contrast and for a swipe-delete icon showing through behind the time at rest.

OPPO also locked before the very last rerun. The final live-format observer and the
new missing-ID stale-start negative case are compiled but that last runtime attempt
was explicitly skipped. Previously passed probes are evidence for their tested
builds, not an assertion that every final change was retested on both devices.

## Cleanup and limits

Original power settings restored and read back exactly:
Pixel stay-on=0, brightness=22, brightness-mode=1;
OPPO stay-on=0, brightness=1492, brightness-mode=1.
No app data wipe, uninstall, security change, system clock change, DND/volume change,
release build, publication or release-asset modification. Existing untracked files,
including app/release and historical reports/caches, remain untouched.

Owner checks still needed: unlocked final-build Pixel/OPPO smoke (especially live
12/24-format changes), speaker audibility and longer field use. No pre-first-unlock
cold-boot alarm delivery: credential-protected records are re-armed after unlock.
No Samsung, Android 12–15, extreme accessibility scaling or all OEM background
restriction coverage is claimed. No widget matrix/fold/lock cycle was repeated.

## APK

/Users/sambeckman/Downloads/ExpressiveClock-v015-20261117-review-debug.apk

SHA-256: 6113e84e5410d0fb6a6313e58f0d0c01783bfc3cf1ee147ccd1529b69889dabb

Application version metadata is unchanged; this is a debug development build.

## Files

Modified tracked sources:

- app/src/main/java/app/expressiveclock/alarm/AlarmReceiver.kt
- app/src/main/java/app/expressiveclock/alarm/AlarmRingActivity.kt
- app/src/main/java/app/expressiveclock/alarm/AlarmScheduler.kt
- app/src/main/java/app/expressiveclock/alarm/AlarmService.kt
- app/src/main/java/app/expressiveclock/alarm/BootReceiver.kt
- app/src/main/java/app/expressiveclock/alarm/ClockActionReceiver.kt
- app/src/main/java/app/expressiveclock/alarm/Notifications.kt
- app/src/main/java/app/expressiveclock/core/Model.kt
- app/src/main/java/app/expressiveclock/data/ClockStore.kt
- app/src/main/java/app/expressiveclock/ui/ClockApp.kt
- app/src/main/java/app/expressiveclock/ui/ClockDock.kt
- app/src/main/java/app/expressiveclock/ui/ClockViewModel.kt
- app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt
- app/src/main/java/app/expressiveclock/ui/screens/StopwatchScreen.kt
- app/src/main/java/app/expressiveclock/ui/screens/TimersScreen.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockScreen.kt
- app/src/main/java/app/expressiveclock/ui/sheets/AlarmEditSheet.kt
- app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt
- app/src/main/java/app/expressiveclock/ui/sheets/NewAlarmPickerSheet.kt

Created production sources: AlarmAudio.kt, SoundPlayback.kt, UpcomingAlarm.kt,
core/AlarmEdits.kt, ui/TimeFormat.kt, ui/sheets/CustomAlarmSoundControls.kt and
res/raw/alarm_fallback.wav. Created debug instrumentation manifest/AlarmProbe.kt;
SoundPlaybackTest/UpcomingAlarmTest/AlarmEditsTest; tests/test_alarm_fallback.py;
ADB alarm/app smoke scripts and tone-generation script; testing/report documents.
No tracked files deleted. See V015_TESTING.md for reusable commands.

## Git

Branch main, baseline 5686e9e. Implementation remains modified/untracked locally;
index empty. No commits, pushes, merges, tags or branch changes were made.
