# Production widget architecture review

Investigation only. No production architecture, typography source, preview or
available-style changes were implemented for this review.

## Required production contract

Timekeeping must be independent of font availability. Always provide native time
views, including a minimal initial/fallback RemoteViews layout. Never gate time
rendering on a font fingerprint, and never replace the clock with the debug
prototype's unavailable message in production. Font checks select presentation,
not whether a clock exists. Keep previous valid native views when a decorative
redraw fails; they continue ticking in the host.

Persist the requested style independently of the effective device font. Resolve
an explicit, measurable effective profile at configuration/update time:

- Condensed: compatible Google Sans Flex with wdth=25/opsz=144/selected weight;
  otherwise the system sans-serif-condensed family, ultimately sans-serif.
- Flex: compatible Google Sans Flex with wdth=100/opsz=144/selected weight;
  otherwise system sans-serif. Roboto Flex may be a named optional profile when
  verified; it is not the same font and must not be presented as exact parity.
- Serif and Monospace: system serif and monospace families, with supported or
  nearest native weights. Vendor glyphs and synthetic weight behavior can vary.

The final generic-family step is a defined fallback, not a claim of exact design.
Explain it in configuration and show a separately labeled device-rendering preview
if needed; preserve the original-design reference. Do not show a warning in place
of time. Keep all requested-style choices and retain user preference so an exact
profile can be used on a compatible device. Revalidate on relevant configuration/
font/OS changes; do not poll. Measure actual effective glyphs and worst-case time
bounds before sizing; include locales, 12/24-hour mode and seconds in validation.

## Two viable implementations

### A. Native-first typography/layout profiles

Use native TextClock for all time/date-dependent content with the effective font
policy above. Prefer a single TextClock for each complete time block where feasible:
TextClock accepts styled Spanned format patterns, so supported foreground-color
spans can preserve hour/colon/minute colors without baking the current time into
text. A stacked pattern can include a newline. Verify span behavior across hosts
before replacing the existing split-clock implementation. Keep date live too.

Use native layouts and drawable backgrounds for designs expressible in XML.
This minimizes app-rendered graphics and presentation complexity. Native ticking
runs in the host without app minute jobs; seconds cost additional host redraws
only when enabled. Native-family capability/weight matching adds moderate work.

Fidelity: exact tested Google Sans glyph geometry where a compatible family is
available; explicit device-font approximations elsewhere. Serif/Monospace can
match their same-device preview families. Complex expressive artwork may need
simplification or additional drawable resources. Exact bundled time typography
is not universally preserved.

Compatibility: strongest common foundation for Android 12–16, Pixel/ColorOS/
Samsung and third-party launchers, subject to physical validation. No cross-OEM
font names or layout sizes should be assumed from brand alone.

### B. Hybrid static artwork + native live time (recommended)

Use the same native time/date engine and effective-font policy as A. Preserve
custom-rendered backgrounds, outlines, static labels and other non-time-dependent
artwork in ImageViews. For analog widgets, a bundled-font dial and native
AnalogClock rotating custom hand images already follow this principle.

Rasterize static layers on configuration, resize, theme/locale events as needed,
not every minute. Keep date/day-of-week and every displayed time native; never
put stale rasterized digits in front of a hidden live clock. Reuse existing
renderers where practical and keep a native clock if artwork creation fails.

Fidelity: retains expressive static design and bundled static-label typography.
Changing time digits have exactly the same font limitation as A. This hybrid
cannot make unavailable Google Sans Flex digits appear exactly on the OPPO.
It preserves more of the existing app design without a custom ticking service.

Compatibility: standard RemoteViews images plus supported native clocks; test
bitmap/binder limits, scaling, clipping, resizing, rotation, foldable host size
lists and third-party launcher behavior. Battery: no periodic app wakeups; extra
work only for static redraws plus ordinary host clock ticks. Complexity: medium
for this repository (it already uses this composition), with more size/layer QA
than a simple all-native XML design. Native-font profile resolution is shared.

Battery assessments above are architectural expectations, not measured power
benchmarks. Neither approach can make a defective launcher execute native views;
that needs host-specific reproduction and testing, not hidden polling.

## Why no third reliable exact-custom-digits option is recommended

A bitmap time overlay plus a hidden TextClock is not a fallback: if app updates
stop, the visible bitmap is stale. A small native clock next to conflicting stale
digits is also not acceptable. Pre-rendered animations/ViewFlipper frames are not
wall-clock-synchronized after sleep, visibility changes or time-zone changes.

Bundled resource fonts are restricted in cross-process RemoteViews; TypefaceSpan
with a custom Typeface does not transfer that Typeface across processes. Supported
native spans can style a format, not install an arbitrary bundled font in a host.
App-driven bitmap refreshes require ongoing scheduling. Widget updatePeriodMillis
has a 30-minute minimum, and repeating background work is power-restricted. Exact
alarms/services introduce access, lifecycle, battery and notification costs and
still do not satisfy this request's native-first, low-impact goals.

Therefore neither viable option promises universal exact Condensed/Flex glyphs.
A cooperating custom launcher could load custom code/fonts, but would lose broad
launcher compatibility and is not a universal app-widget architecture.

## Evidence and remaining validation

Pixel 11 Pro Fold, Android 17 / Pixel Launcher: compatible named Google Sans Flex;
24 combinations captured; seven settings-free samples over 185 seconds passed.
Folded/unfolded and physical landscape snapshots were inspected in earlier runs.
Date proportions still differ from fixed preview; this is not whole-widget pixel
parity. These results do not establish Android 12–16 behavior on Pixel hardware.

OPPO Find N6, Android 16 / ColorOS Launcher: named-font prototype rejected its
font profile, explicitly demonstrating a device-font limitation. For this review,
Serif/Regular/Stacked was explicitly selected using the existing UI as a native
clock control. The clock is left in that working selection, not the warning
state; no font substitution policy was implemented. Lifecycle results follow.

Samsung / One UI, Android 12–15 and third-party launchers: not field-tested in
this review. Do not infer brand-wide compatibility or failure from these two
phones. Font availability is primarily device/OS-dependent; widget sizing,
visibility handling, cached-view reinflation and restart behavior are host concerns.

Sources:
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/TextClock.java
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/TextView.java
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/RemoteViews.java
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/graphics/java/android/graphics/LeakyTypefaceStorage.java
- https://developer.android.com/develop/ui/views/appwidgets/advanced

## Completed OPPO lifecycle results

- Serif/Regular/Stacked selected explicitly using existing configuration UI.
- Seven settings-free samples from 14:07:54 to 14:10:59 all matched device time
  (185 seconds). No relevant widget crash/inflation exception observed. A broad
  failed-to-apply log match was reviewed and was an unrelated system cgroup warning.
- Sent KEYCODE_SLEEP at 14:12:21; power service reported Dozing. Woke the phone;
  secure keyguard required the owner to authenticate. The owner unlocked normally.
  No PIN/pattern was requested in chat and no authentication was bypassed.
- First verified home capture after unlock at 14:16:31 matched device time. The
  earlier secure-lock capture was NOT_VISIBLE and is not counted as a clock pass.
- Force-stopped only com.android.launcher, sent HOME, and verified launcher PID
  changed from 5549 to 2737. No launcher data was cleared.
- Home clock after restart showed correct time at 14:16:35; subsequent capture at
  14:18:02 also matched, demonstrating continued minute advancement after restart.
- Final screenshot inspected. OPPO left showing the working Serif clock; no
  clock-warning state was restored. Lifecycle Logcat tail had no fatal exception,
  InflateException or ActionException indicator.

Evidence directories:

- Baseline timed run: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-140754-028510
- Post-unlock: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-141630-838590
- Launcher restart: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-141635-382562
- Post-restart minute advancement: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-141802-446852

No ColorOS-specific native time-update failure was reproduced. The observed font
capability failure is not proof of a launcher bug. Samsung, other launcher brands,
Android 12–15 and OPPO analog widgets remain untested in this review. Pixel lock/
unlock and process-restart coverage was not established by the earlier Pixel run;
only its actual documented timing and layout checks should be compared.

Changed in this investigation: created this report; appended the lifecycle result
to OPPO_NATIVE_FONT_TEST.md. No application source changes or final architecture
implementation. No new APK/build required for this investigation; existing debug
prototype used. No files deleted. Branch main; existing unstaged/untracked work
preserved; no commits, pushes, merges or publication.
