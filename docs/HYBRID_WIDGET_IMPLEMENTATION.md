# Production hybrid native widget implementation

## Implementation

Time and date remain native TextClock views. AnalogClock hand rendering/stepping
code is unchanged. Canvas/WEBP artwork remains static background/dial imagery;
there is no scheduled bitmap time refresh, polling, service or new dependency.

NativeWidgetFonts and WidgetFontPolicy resolve an effective device profile without
rewriting WidgetConfig's saved font preference. Condensed prefers verified native
Google Sans Flex, then sans-serif-condensed, then sans-serif. Flex prefers verified
Google Sans Flex, then sans-serif. Serif/Monospace/Cursive use their native families.
The Google profile uses opsz=144, wdth=25/100 and the selected weight. Verification
includes native glyph outlines/advance and localized clock/date repertoire; results
are cached by width, weight and resource configuration. A verification failure
chooses a native profile; it never suppresses time.

The digital preview and digital font-choice samples use the same restricted-context
native layout inflation and setters as widget measurement. The loaded native Typeface
is wrapped for Compose, with font synthesis disabled in the large preview. A fallback
explanation appears in configuration only. Saved style enums and available choices
are unchanged. Analog font-choice previews retain their prior behavior.

The prototype warning, debug-only layouts/flag and prototype resolver were retired.
The production path is shared by debug and release source sets. Digital initial
layout is a native clock while configuration is pending. A logged render failure
also falls back to a minimal working native clock with the usual app-open action,
not an unavailable message. Cancellation is propagated normally.

Existing height/baseline/layout/background formulas are preserved. Native clocks
retain natural advance widths, with symmetric ink padding. Worst-time width fitting
handles seconds and narrow sizes; date labels have independent width fitting using
a full year of localized samples, so date width does not unnecessarily shrink time.
Day-of-week colour spans are retained in both native date format modes. Native
12/24-hour and zone behavior and existing provider boot/time/locale/resize hooks
remain active. No analog hand-stepping changes were made.

## Builds and automated checks

- ./gradlew assembleDebug testDebugUnitTest — passed.
- Kotlin: four font fallback-policy tests plus four existing sizing tests passed.
- Python: 11 ADB assertion/resource-contract checks passed.
- git diff --check — passed.
- No Gradle/Kotlin/SDK versions or application version fields changed.

## Device validation

### OPPO Find N6 — Android 16 / ColorOS

24 Condensed/Flex × four weights × three layouts preview/host pairs captured.
All 24 host snapshots matched device time, with no text rectangles outside widget
bounds in the screenshot XML checks. Representative comparison sheets inspected;
preview and host use the same disclosed native fallback family. The selected
Condensed/Flex preference remains selected in configuration. No warning replaces
the clock. The original Serif/Regular/Stacked selection is restored after testing.

Matrix evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-143224-960222

Lifecycle and Pixel results are recorded below.

## Source scope

Modified: DigitalClockWidgetRenderer.kt, WidgetConfigScreen.kt, digital widget info;
36 live digital layouts (20 existing native layouts updated, 16 fallback variants
added); native-font resource regression checks. Existing provider fixes are retained.

Created: NativeWidgetFonts.kt, WidgetFontPolicy.kt, native initial digital layout,
font-policy unit tests, production native-profile resource tests, this report.

Retired: NativeDigitalFontPrototype.kt; unavailable-message layout/values; the two
debug font layout overrides and debug flag; prototype-only resource test. No unrelated
files, app/release artifacts, generated palettes or user data were removed.

## Limits

Only tested device/OS/launcher combinations should be claimed. System families can
vary visually by OEM and user font settings. Effective native profiles are deliberate
fallbacks, not claims that OPPO typography is the original bundled Google Sans Flex.
Font rendering is matched on-device; fixed-size preview proportions and real widget
sizes/date strings can differ. Every size/locale/accessibility setting and Samsung/
Android 12–15 still need broader coverage. Native framework behavior is retained;
there is no battery-costly workaround for a launcher that fails to execute native views.

### OPPO production live run

Current final-code APK installed without clearing data. With both widgets visible
and a narrower digital size beside the newly added analog widget, seven samples
from 14:47:52 to 14:50:57 matched digital device time. Analog descriptions advanced
from 14:47 through 14:50, and native hand rendering was inspected in screenshots.
No widget settings were opened during this 185-second observation.

Evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-144751-818435

OPPO was locked at 14:51:34 and woken at 14:53:36. After normal owner unlock,
both clocks showed current 14:54 time without opening settings. Evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-145423-798463

A narrow Monospace/Bold/Horizontal case also displayed complete digits. Original
Serif/Regular/Stacked was then restored and captured at 14:55:02 with both clocks
visible and digital time passing. Evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-145427-483647

No FATAL EXCEPTION, InflateException, ActionException or Error inflating appeared
in the inspected OPPO production lifecycle logs.


### Pixel production font matrix — original device 65261FDDJ002GB

Pixel Launcher, Android 17/API 37: all 36 supported Condensed/Flex/Serif/Monospace
weight/layout combinations have passing host-time captures. Condensed and Flex each
cover four weights and three layouts; Serif and Monospace cover Regular/Bold and
three layouts. Comparison sheets inspected for all font/layout groups. No clipped
digits or overlaps were observed; UI text rectangles remain within widget bounds.
Native verification logs confirm compatible Google Sans Flex on this Pixel.
Font shapes/weights match previews; preview sample date/time and fixed container
proportions differ from the real current date/time and actual widget dimensions.

Evidence folders under /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/:
- 20261116-150201-400858 — Condensed/Horizontal four weights.
- 20261116-150516-441725 — Condensed/Stacked/Compact and Flex/Stacked.
- 20261116-151040-485167 — Flex/Horizontal/Compact, including reruns.
- 20261116-151402-712859 — Serif/Monospace all supported weights/layouts,
  plus verified Flex/Regular/Horizontal restoration.

Two earlier host captures were interrupted by Quick Share opening without user
interaction. Those NOT_VISIBLE results are excluded and were rerun successfully.
The launch source remains unknown: the retained activity buffer did not include the
original launch, and subsequent streaming logs recorded no recurrence. No data was
sent. This should not be represented as a font failure or a resolved device issue.

The matrix exercises widget updates after saving configuration; it does NOT replace
a settings-free multi-minute observation. Pixel final-build settings-free live,
lock/unlock, physical fold/rotation and resizing lifecycle checks remain pending.

### Replacement Pixel — unattended checkpoint

User connected replacement serial 66231FDDJ003QS, described as the old Pixel Fold.
ADB reports Pixel_11_Pro_Fold / yogi_beta, Android 17. Keep this serial separate from
the original Pixel results. Final APK installed with install -r successfully without
clearing data. Native font log reports width=25 weight=400 compatible=true.
The device is securely locked (deviceLocked=1), and capture reports NOT_VISIBLE,
so no home-screen font/live-update claims can be made on this device. No attempt
was made to bypass authentication or change persistent settings.

Evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-183239-717210

Historical crash buffer contains a 15:29:26 app.expressiveclock.debug crash:
NullPointerException at AlarmScheduler.showPendingIntent(AlarmScheduler.kt:88),
called by BootReceiver.reschedule. The code force-unwraps getLaunchIntentForPackage.
It predates this 18:32 installation, and its trigger/build association was not
established. Alarm code was not modified in this widget task. Separate follow-up
is required; do not describe all device logs as crash-free.
Log evidence: /tmp/expressive-replacement-pixel-logcat.txt.

## Delivered APK and repository state

Final APK: /Users/sambeckman/Downloads/ExpressiveClock-hybrid-native-final-debug.apk
SHA-256: bf0dfabac3a8e1ba3de692f8d4f5ebe5481b807faf9bee9eef9b4fbdbb821e56
Existing Downloads APKs were preserved. This APK was installed on OPPO and the
original Pixel earlier, and on the replacement Pixel during unattended resumption.

Final branch: main. Working tree remains intentionally dirty with existing modified
and untracked widget work, tests, scripts and documentation. app/release is preserved.
No staging, commits, pushes, branches, merges, app data clears, time changes or
persistent device-setting changes were performed. Remaining device lifecycle tests
are explicitly pending rather than passed.


### Replacement Pixel after owner unlock

Owner subsequently unlocked the device. No settings/configuration was opened by
automation during the live observations. Native TextClock hierarchy values matched
device time across minute changes, and AnalogClock descriptions advanced.
Default-display screenshots /tmp/expressive-default.png (18:35) and
/tmp/expressive-default2.png (18:38) were visually inspected: both clocks show current
time, analog hands moved, Condensed/Regular/Stacked digits are complete, and the
existing colours/background remain intact. This replacement's saved style was not
changed. Unlock recovery is observed; a controlled lock-across-minutes cycle remains
pending because the owner is away.

Capture limitation: explicit physical-display captures disagree with the logical UI:
the metadata-selected panel shows stale recents, and the other explicit ID can be
black. Default-display capture shows the live home screen. Therefore native-text
PASS is not treated as an automatic screenshot/visual PASS. Added --display-id to
the reusable capture script for devices where a physical panel can be verified;
this replacement still needs investigation of explicit capture behavior.
Native run evidence: expressive-clock-adb/20261116-183349-552568 and
20261116-183609-897505 under the temporary evidence directory above. Both contain
separate native hierarchy and PNG evidence, with the stated PNG limitations.
The earlier 'securely locked' checkpoint remains historical, not the final state.

Final replacement native-time run completed: seven samples from 18:36:08 through
18:39:13 all PASS; analog descriptions advanced 18:36 → 18:39. No FATAL EXCEPTION,
InflateException or ActionException in this live-run log. PNG limitations above remain.
Final verification: assembleDebug/testDebugUnitTest successful (8 Kotlin tests),
11 Python checks successful, git diff --check clean.
