# v0.1.5 autonomous session checkpoint

The first-batch notes below are historical. See **Resumed session** at the end for current progress.

## Repository safety

Started on main at 5686e9e, equal to origin/main after fetch. AGENTS.md read.
Hybrid widgets unchanged. No commits, pushes, merges, tags, release builds or
release-asset changes. All pre-existing untracked files, including app/release,
were preserved. No app data cleared. This is a partial implementation batch.

## Implemented locally

- AlarmService: transient audio-focus request after entering foreground, alarm
  audio attributes for every playback candidate, focus abandonment on destruction,
  focus loss pause/gain resume, resource release on synchronous/asynchronous failure,
  asynchronous preparation and ordered default-sound recovery. Serialized service
  callbacks on Main; prior silence timeout cancelled on a new ringing request.
- AlarmScheduler: explicit MainActivity show intent replaces nullable package-launch
  lookup forced unwrap (previous device crash).
- CustomAlarmSoundControls: OpenDocument audio selection, persistable read grant,
  readable-file check, preview and stop, preview cleanup on dispose/change.
  Wired to active alarm editor and legacy create/edit sheets. Stored sound URI and
  all unrelated alarm settings remain unchanged unless the user selects a file.
- AlarmPickerUi: scrollable content with IME padding.
- StopwatchScreen: always scrollable, additional bottom spacing, tabular numeral
  feature on circular readout. Other stopwatch digit renderers need investigation.

## PR #5 review

Downloaded https://github.com/samuelbeckman/expressive-clock/pull/5.diff to
/tmp/expressive-pr5.diff. gh is unavailable. Selectively reused always-scrollable
stopwatch content and extra bottom spacing. Did not merge. Did not adopt signing
changes, Sunday recolouring, broad compact-button typography changes or other
layout patches without validation. Remaining alarm weekday fitting, timer layout
and alarm-picker proposals still need evaluation.

## Devices and verification

All ADB calls used explicit -s serial:
- Pixel 66231FDDJ003QS, model Pixel_11_Pro_Fold / yogi_beta, Android 17.
- OPPO 3B161P00QM400000, model CPH2765 / OP6271L1, Android 16.
Both authorised and unlocked at checks. Debug install -r and MainActivity launch
succeeded on both (warm launch Status ok). UI XML captured to
/tmp/expressive-overnight-smoke. Recent AndroidRuntime error snapshots empty.
These are launch smoke checks, NOT alarm/audio/layout correctness passes.
Final audio async-preparation build installed on both after smoke checks.

Original device settings recorded /tmp/expressive-overnight-device-settings.json.
Applied svc power stayon usb and manual brightness 25 for test window; restored
all originals successfully in finally blocks:
Pixel stay_on_while_plugged_in=0, brightness=22, brightness_mode=1.
OPPO stay_on_while_plugged_in=0, brightness=1492, brightness_mode=1.
No security settings changed, no media apps modified, no time changes.

./gradlew assembleDebug testDebugUnitTest passed after final source changes.
Existing 8 Kotlin tests and 11 Python tests passed. No new behavioural audio tests
added yet; this is a verification gap. git diff --check passed.
APK: ~/Downloads/ExpressiveClock-v015-overnight-final-debug.apk.
Earlier batch APK also preserved. No release/version/SDK/dependency changes.

## Outstanding and release risks

- Verify real alarm playback, focus takeover against background media, volume/DND
  respect, focus restoration, failed URI fallback, process/reboot persistence on
  both phones. Never infer audibility from MediaPlayer success alone.
- Fallback tries system defaults; a device with no playable system sounds still
  needs a packaged licensed last-resort sound. Current terminal failure logs and
  vibration are not sufficient to promise audible recovery on every device.
- Preview uses Ringtone and does not yet share playback fallback/focus lifecycle;
  null/failed ringtone reporting and preview duration need refinement.
- Existing active editor sound-row callback is a no-op; separate new document
  controls work, but wire system ringtone selection here as well.
- Potential multiple start races and focus denial handling deserve regression tests.
- Keyboard/clock control clipping and added audio-row wrapping on compact screens
  require screenshot inspection; source changes alone are not validated fixes.
- Weekday/toggle overlap, timer presets, theme reverting green not fixed yet.
- Upcoming alarm notification/dismiss-single-occurrence NOT implemented. Must
  design around persisted occurrence identity, exact alarms, edits/cancellation,
  boot/timezone, permission denial and repeat rescheduling. No partial schedule
  or manifest components were added.
- Confirmation duration, portrait World Clock and 12/24 consistency deferred.
- No new widget matrices, secure unlock, fold or rotation testing repeated.

## Exact next action

Read this checkpoint and git diff (including new CustomAlarmSoundControls.kt).
Recheck both device serials/lock state; record fresh settings before any temporary
changes. First add behavioural audio tests and validate playback via a temporary
alarm through actual UI without editing existing alarms. Verify teardown/fallback
before broadening UI changes. Fix active editor system-sound callback and preview
error reporting. Then implement upcoming occurrence scheduling with pure temporal
regression tests, preserving recurring alarms. Finish remaining PR layout fixes
only with preview/device evidence. Rerun builds and restore device settings.

This session does not establish Samsung or universal Android compatibility.
Automatic continuation has not been configured; resume from this file when a
subsequent session is available. No scheduling or usage limits were altered.

References: https://developer.android.com/media/optimize/audio-focus
and https://developer.android.com/reference/android/content/Intent#ACTION_OPEN_DOCUMENT


## Resumed session — 2026-11-17

### Completed implementation (local only)

- Shared AlarmAudio/SoundPlayback for preview and foreground ringing. Transient alarm
  focus, delayed gain handling, loss pause/gain resume, stale callback guards, FD
  acquisition on IO, asynchronous preparation with a five-second source timeout,
  scoped MediaPlayer partial wake lock and teardown on stop/replacement/destruction.
  Invalid custom/system sources fall through to an original synthesized bundled
  WAV. Explicit Silent remains silent. No stream-volume or DND changes.
- Custom file selection via OpenDocument, retained read permission, asynchronous
  display-name resolution, preview/stop and lifecycle cleanup. Explicit in-app
  Default/Silent choices avoid OEM picker inconsistencies. Active inline editor's
  system picker callback wired; existing legacy picker retained.
- Upcoming alarm: native exact callback approximately 30 minutes before due time,
  silent low-importance channel, permission-aware posting, alarm time/label and
  immutable dismiss action. Atomic persisted skip of just that occurrence; repeat
  rules preserved. Timestamp plus local calendar-date marker survives timezone
  rescheduling. Stale callbacks are rejected; edits, disable, deletion, firing and
  rescheduling clear/replace notifications. Pure edit policy retains dismissal for
  label/audio-only changes, clears it for time/repeat changes or re-arming. Atomic
  DataStore transforms used by alarm UI, fire, skip and snooze paths.
- AlarmScheduler explicit MainActivity intent fixes the prior nullable launch-intent
  crash; locked-boot guard avoids credential-storage access before first unlock.
- Responsive alarm grid and constrained weekday strip, weighted wrapping editor
  header, 48dp AM/PM buttons, IME-aware scrolling and controlled name text field
  (removed competing asynchronous text state). Long sound names ellipsize.
- Timer setup always scrolls; stopwatch content scrolls with bottom clearance,
  portrait face scales as one unit, circular digits use tabular features.
- Enabled alarm cards use selected primary-container/on-primary colours instead
  of tertiary colours that can remain green/purple independently of chosen primary.
  Ringing UI waits for persisted theme rather than rendering a default-theme flash.
- World Clock added to portrait dock, existing city list reused, Add City wired,
  theme-aware row contrast corrected. Expanded world panel honors saved hour-format
  setting. Alarm tiles now follow system 12/24-hour format like their editor.
- Save confirmation is emitted after persistence/scheduling and placed above dock.
- No widget renderer/provider changes, generated palette edits, versions, SDK or
  dependency changes. No commits, pushes, merges, tags, releases or publication.

### Verification completed during resumption

Primary Pixel: 66231FDDJ003QS (Pixel_11_Pro_Fold/yogi_beta, Android 17).
OPPO: 3B161P00QM400000 (CPH2765/OP6271L1, Android 16).

Both devices passed debug instrumentation probes for:
- Actual custom MediaStore WAV URI playback.
- Invalid URI fallback reaching a started platform player.
- Transient focus loss of a temporary in-app media player and focus restoration.
- Native deferred 30-minute notification callback (alarm set 32 minutes ahead,
  notification observed after 1–2 minutes without changing time).
- Actual notification action PendingIntent, persisted occurrence dismissal and
  preserved future repeat.
- Real short-snooze AlarmManager callback, one-shot disarming and foreground ringer.
Each probe creates/removes only its allocated alarm and app-owned test media file.

Evidence: /tmp/expressive-v015-deferred-probes/ and
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-alarm-probe-{vw962kmy,t68hqqly}/
Later OPPO probes additionally passed packaged raw-resource fallback and reran actual
playback after asynchronous descriptor handling:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-alarm-probe-8pn8xzes/

OPPO actual OpenDocument selection, UI preview/stop, draft cancellation and playback
of the exact persisted document after process restart passed. Read grant released
and uploaded WAV removed. System alarm_alert URI checked unchanged after system
picker exploration; no default-handler checkbox selected.

OPPO app smoke captured Alarms, World Clock, Timers, Stopwatch and unsaved alarm
keyboard entry/cancellation. Representative captures inspected: weekdays separate
from toggles, keypad/presets separate, controls above dock and name above keyboard.
Evidence: expressive-app-smoke-dtmbygk9 under the temp evidence directory above.
A running stopwatch observed from zero through >1 minute retained exactly the same
text bounds across different digits; stopped/reset to its original zero state.
Running capture: /tmp/expressive-stopwatch-running.png.

Saved own silent/non-vibrating ADBConfirm20261117 alarm through UI. Typed label was
preserved exactly and confirmation “Alarm in 6 h 39 min” captured above dock:
/tmp/expressive-confirmation.xml and /tmp/expressive-confirmation.png.
Cleanup confirmation is recorded in the final report after removing this fixture.

Pixel later became securely locked. Further Pixel UI/SAF checks skipped; no unlock
attempt. Pixel app smoke records locked/unavailable rather than visual success.
No previous widget font matrix, lock/unlock or physical fold/rotation tests repeated.

### Reusable verification added

- app/src/debug/.../AlarmProbe.kt with debug-only instrumentation declaration.
- scripts/adb_alarm_probe.py --serial SERIAL [--deferred], skips secure lock.
- scripts/adb_app_smoke.py --serial SERIAL [--editor], never saves drafts.
- scripts/generate_alarm_fallback.py reproduces the original synthesized WAV.
- SoundPlaybackTest, UpcomingAlarmTest and AlarmEditsTest behavioural suites.

### Limits / pending manual checks

No speaker audibility measurement, DND-policy changes, physical fold/rotation or
reboot requiring owner authentication. Credential-protected alarms are re-armed
at BOOT_COMPLETED after first unlock; ringing before first post-boot unlock is not
implemented and must not be claimed. Pixel SAF UI validation remains owner-blocked.
Samsung, Android 12–15, accessibility extremes and all OEM background restrictions
are not established by two-device results. Owner listening/longer field testing is
still appropriate before release. No polling was introduced.

### Exact next action if interrupted

First verify/remove only the ADBConfirm20261117 fixture if it still exists (never
alter Gym/Work/Standup or other user alarms). Finish final build/unit/Python checks,
OPPO World Clock contrast capture and updated audio probe if needed. Install final
APK on both with explicit serial, copy to a new Downloads filename, restore original
power/brightness settings from /tmp/expressive-overnight-device-settings.json, and
record restoration results. All original untracked files including app/release
remain untouched. No Git writes are authorised.


## Final checkpoint

See V015_IMPLEMENTATION_REPORT.md for the authoritative final result and limits,
and V015_TESTING.md for reproducible commands. All requested implementation areas
are covered. Final build and 24 Kotlin / 12 Python checks passed. Both phones have
the final APK installed; both were securely locked at the last on-device rerun.
The last on-device attempt was SKIP, not PASS. Original power/brightness settings
restored and read back exactly on both devices. ADBConfirm20261117 removed through
UI; original Gym/Work/Standup retained. Stopwatch returned to its observed zero
state. Own timer/media/document fixtures and grants cleaned up.

Later service refinements include serialized valid starts, consumed-occurrence
validation, stale-start protection, scoped playback wake lock and timer-expiry
guards. Final OPPO native probe p_7j3fja passed custom/default/packaged audio,
focus handoff, exact alarm and native timer expiry, skip persistence and real
notification action. The last added live-format listener and stale-start negative
probe case could not be rerun after lock; build/unit checks passed. No polling added.

Exact next action: when the owner has unlocked a phone, install the linked final
APK if needed and run adb_alarm_probe.py --serial SERIAL [--deferred], then
adb_app_smoke.py --serial SERIAL --editor. Validate live 12/24 changes and listen
to chosen/fallback sounds. Do not repeat completed widget/fold/lock matrices.
Review the local diff before any release. No Git/publication action is authorised.

Final APK: /Users/sambeckman/Downloads/ExpressiveClock-v015-20261117-review-debug.apk
SHA-256: 6113e84e5410d0fb6a6313e58f0d0c01783bfc3cf1ee147ccd1529b69889dabb

Final additional lint check: lintDebug fails on four pre-existing errors in
unchanged WidgetColours.kt (two NewApi findings), values/themes.xml (NewApi),
and LaunchTileServices.kt (deprecated tile launch). 400 warnings and six hints
remain. Findings in touched composition/locale/notification code were resolved;
assembleDebug and all regression tests still pass. No baseline suppression or
unrelated widget changes were introduced. The review APK above contains these
last fixes, is byte-identical to app/build's APK, and was installed on both phones.
