# v0.1.5 regression workflow

Build only the debug variant (application version metadata is unchanged):

```sh
./gradlew assembleDebug testDebugUnitTest
python3 -m unittest discover -s tests
```

Inspect `adb devices -l`, then supply a serial for every device operation:

```sh
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
python3 scripts/adb_alarm_probe.py --serial SERIAL
python3 scripts/adb_alarm_probe.py --serial SERIAL --deferred
python3 scripts/adb_app_smoke.py --serial SERIAL --editor
```

Use the SDK platform-tools adb path when adb is not on PATH. Python scripts also
read local.properties. Do not uninstall or clear app data to work around failures.

## Native alarm probe

AlarmProbe is instrumentation declared only in the debug source set, with no new
production exported component or permission. It briefly plays audio at existing
alarm volume. Its temporary media player exercises focus loss and restoration;
custom WAV, invalid URI and packaged fallback paths exercise actual MediaPlayer.
A short snooze deadline exercises AlarmManager → receiver → foreground service.
It tests a temporary timer only if no existing timer is present. A missing-ID start
checks that a stale event does not stop the active test ringer.

The deferred option schedules its temporary alarm 32 minutes ahead, waits roughly
1–2 minutes for the native 30-minute callback, and invokes the real notification
PendingIntent. No device time changes or production polling are used. Notification
permission denial is reported as a skipped notification-action check.

Only its allocated alarm and app-owned WAV are removed in finally cleanup. Existing
alarms and nonempty timer state are preserved. It skips ringing when an existing
alarm is imminent. It consumes a monotonic test ID, which is intentionally not
reused. Securely locked devices are skipped (exit 2); do not bypass authentication.
Outputs are placed in uniquely named temporary folders; inspect instrumentation
results, not just adb's exit code. A killed instrumentation process can interrupt
cleanup; inspect only the named ADB fixture before rerunning, never wipe data.

## UI and document checks

App smoke captures all four destinations and optionally opens, types into and
cancels an unsaved alarm. It does not assert pixel-perfect layout correctness.
Review screenshots for digits, weekday/switch separation, presets, dock clearance,
keyboard placement and current-device font scaling. A wrong/stale physical display
capture is not a UI pass. See ADB_TESTING.md for display limitations.

For persisted document playback, create a uniquely named WAV fixture in Downloads
without overwriting an existing file. In a new unsaved alarm choose it with Android
OpenDocument, preview, stop and cancel. Recreate the app process and verify access.
The debug probe supports this post-selection check:

```sh
adb -s SERIAL shell am instrument -w -e document EXACT_FIXTURE_FILENAME.wav app.expressiveclock.debug/app.expressiveclock.AlarmProbe
```

It checks a persisted local document grant and playback of that exact URI, then
releases its test grant. Remove only the uploaded test file afterward. This is a
process-restart check, not proof of reboot or every cloud provider's behavior.

## Power settings and limits

Before temporary stay-awake/brightness changes record each device's original
stay_on_while_plugged_in (global), screen_brightness and screen_brightness_mode
(system). `adb -s SERIAL shell svc power stayon usb` is permitted for an authorised
session. Restore recorded originals in final cleanup, including failure paths.
Never change PINs, biometrics, DND, alarm volume, clock, rotation, fold state or
unrelated app settings merely to get a passing test.

Speaker audibility needs listening; player start and non-silent PCM are distinct
checks. Locked-boot storage is credential protected: current code defers rescheduling
to BOOT_COMPLETED after first unlock and does not implement pre-unlock alarm delivery.
Test final Pixel UI/live system-format changes when unlocked; Samsung and Android
12–15 need separate devices. Do not repeat completed widget matrices unless widget
code changes. Do not publish or create release builds as part of this workflow.

## Additional static analysis

Run `./gradlew lintDebug` separately from the debug build. At the final session
check it reports four errors in unchanged WidgetColours.kt, values/themes.xml
and LaunchTileServices.kt, plus 400 warnings and six hints. See
V015_IMPLEMENTATION_REPORT.md for the exact findings. A successful assembleDebug
is not a successful lint check; do not silently baseline or suppress these errors.
