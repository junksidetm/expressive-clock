> Historical prototype, now superseded by the production native font profiles. Its debug flag, warning layout and debug overrides have been retired; the prototype rollback instructions below do not apply to the current implementation.

# Native Google Sans Flex debug prototype

This is a debug-only experiment; configuration previews are unchanged. Native
TextClock still owns time/date updates in the launcher. No polling, time bitmap
refresh, alarm, service or periodic work was added.

## Isolation and rollback

The only named-family layouts are debug source-set overlays:

- app/src/debug/res/layout/widget_live_digital_flex.xml
- app/src/debug/res/layout/widget_live_digital_stacked_flex.xml

They use google-sans-flex and default numeral features, matching the preview.
The renderer already sets opsz=144, wdth=25/100 and the selected wght. In this
prototype, date labels carry the selected variable weight too, and time components
retain natural wrap-content widths rather than fixed widest-digit slots. Worst-case
width fitting remains active to avoid clipping on later times.

The main resource native_font_prototype_enabled is false. Debug overrides it to
true. To remove the experiment without undoing other work, remove the three files
under app/src/debug/res added for this prototype (two layouts plus the flag file),
then rebuild/install the debug APK with install -r. Do not git reset/restore the
renderer: it contains earlier live-update and clipping fixes. Release builds do
not use these debug overlays or the enabled branch.

## Compatibility safeguard

NativeDigitalFontPrototype rejects a missing named family or a mismatched native
outline/advance fingerprint against the bundled font at the chosen width/weight.
The tested character repertoire is Latin clock/date text, not all languages. The
check is cached per width/weight in the app process and runs during widget rendering,
not on a timer. It logs NativeFontPrototype compatibility results.

A failed check shows an explicit prototype-unavailable status on affected digital
widgets. No different clock font is selected and no style choices are removed.
This is a test safeguard, not a final production fallback or migration policy.
System family/version availability varies by device and OS. Other locales, font
accessibility adjustments and other launchers still need validation. The negative
path has not yet been tested on a device actually lacking the named family.

## Repeatable device matrix

    python3 scripts/adb_widget_fonts.py --serial SERIAL --widget-id ID --restore-font Flex --restore-weight Regular --restore-layout Horizontal --fonts Condensed Flex --weights Thin Regular Medium Bold --layouts Horizontal Stacked Compact

The script captures preview/host pairs for 24 cases and restores the specified
original font/weight/layout. It normalizes remembered scroll position, locates
controls from UI XML, and stops on missing widgets/time mismatches. Do not touch
the phone during this UI run. Inspect scenario.json to confirm restoration.

Then run without opening configuration:

    python3 scripts/adb_widget_test.py run --serial SERIAL --duration 185 --interval 30

Native family compatibility is not a full whole-widget pixel-parity claim. Preview
uses fixed sample time/date while the actual widget uses real local time/date;
widget dimensions and date localization also differ. Review weight, glyph shapes,
width, spacing, baseline placement and bounds, not a raw whole-screen image diff.

## Validation record

Pixel 11 Pro Fold / Android 17 / Pixel Launcher: final matrix completed all 24
cases (Condensed + Flex × Thin/Regular/Medium/Bold × Horizontal/Stacked/Compact).
All 24 host snapshots matched device time. Reviewed all six comparison sheets;
intended narrow/normal glyphs and weight progression are visible. No missing or
clipped digits were observed at the tested widget size, and all captured time/date
TextView rectangles were inside the widget's content bounds. This does not certify
all sizes, all possible times, seconds-enabled layouts or languages.

Font matching is substantially improved; whole-widget pixel parity is not claimed.
The host retains the previous height-based size/baseline formulas, whereas the
preview uses fixed sp sizes and reserved regions. Relative date size/gaps therefore
still differ, particularly in Compact and Horizontal. Preview samples 08:30 / OCT 27
also differ from actual clock/date strings. Source previews remain untouched.

The original Flex/Regular/Horizontal selection was restored and its preview and
home rendering captured. The final settings-free timing run is recorded separately
below when complete.

Evidence (including scenario.json, 48 preview/host captures, restored checkpoints,
qa-summary.json and six qa-*.png comparison sheets):
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-134202-052223

Earlier exploratory matrices were superseded by this final-build matrix. One was
interrupted intentionally to refine date weight; another stopped on remembered
scroll position. The final runner fixes that scroll normalization issue.

Validation: assembleDebug and testDebugUnitTest succeeded; 11 Python regression
checks passed, including debug isolation. The existing configuration preview source
has no Git diff. No unsupported-device field test or Android 12–16 test was performed.

## Changed files for this prototype

Modified: DigitalClockWidgetRenderer.kt; scripts/adb_widget_fonts.py;
scripts/adb_widget_test.py; docs/ADB_TESTING.md.

Created: NativeDigitalFontPrototype.kt; main res/values/native_font_prototype.xml;
main res/layout/widget_native_font_unavailable.xml; two debug layout overlays and
debug res/values/native_font_prototype.xml; tests/test_native_font_prototype.py;
this document. Deleted: none. Earlier uncommitted widget fixes are preserved.

APK: ~/Downloads/ExpressiveClock-native-font-prototype-debug.apk
SHA-256: 909f8f8fa42a0151fa18bc15fc3e433e05b486aa678d987f177915dc741bdcf7

## Completed settings-free live test

Final restored Flex/Regular/Horizontal observed from 13:51:32 to 13:54:37, seven
samples across 185 seconds. All seven matched device time. No configuration
activity was opened during this run. The final home screenshot was inspected.
No FATAL EXCEPTION, InflateException, ActionException or widget apply-failure
indicator was found in the captured Logcat window.

Evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-135132-488046

Final recommendation: retain this as a debug prototype for review. Named-family
font matching is successful on the tested Pixel, but date/layout proportions,
seconds-enabled/narrower widget cases, unsupported-device behavior and Android
12–16/other-launcher coverage remain open before a production rollout. No system
time, persistent device settings or app data were cleared/changed by test setup;
widget font/weight/layout were temporarily changed for the matrix and restored.
Final branch main; changes remain unstaged/untracked. No commits, pushes or publication.

## OPPO Android 16 follow-up

The OPPO Find N6 / CPH2765 was connected and the same debug APK installed. Its
ColorOS Launcher displays the prototype-unavailable guard for the selected Google
Sans style. This field-tests the negative path that was previously untested.
No compatible named family was found in the inspected system font configuration;
Roboto Flex is present but was not substituted. See OPPO_NATIVE_FONT_TEST.md.
The status message is not a native-clock timing pass; native clock ticking on
ColorOS remains a separate test. No font/style/preview code was changed.
