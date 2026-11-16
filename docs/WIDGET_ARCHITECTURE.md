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

## Verification

Run `./gradlew assembleDebug testDebugUnitTest` and
`python3 -m unittest discover -s tests`. See ADB_TESTING.md for device testing.

Pixel native Google Sans Flex and OPPO native fallback profiles were compared
against configuration previews. Native digital and analog updates were observed
across minute changes; OPPO lock/unlock recovery was checked. These results do not
establish coverage of every Android version, launcher, locale or widget size.
OEM system fonts can vary. Physical fold/rotation and broader lifecycle coverage
remain separate device checks.
