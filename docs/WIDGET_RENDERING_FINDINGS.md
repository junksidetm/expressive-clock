# Native widget typography and hand investigation

The configuration preview loads bundled Google Sans Flex inside the app process.
Both native digital variants currently reference that same resource in RemoteViews.
Android inflates RemoteViews using a restricted resource context. TextView skips
resource font loading in that context and falls back to a named/system font. Changing
variation axes cannot turn a fallback font into Google Sans Flex. Replacing the font
with an XML font-family wrapper or a downloadable font provider does not bypass the
restricted-context check. setTypeface is not a supported RemoteViews setter.

TypefaceSpan(Typeface) is not a cross-process solution: TypefaceSpan parcels via
LeakyTypefaceStorage, which rejects the typeface when the receiving PID differs.
A span naming a system family can work but cannot install the bundled font.

## Alternatives (investigation only)

- Keep native TextClock and choose system families: reliable host ticking and no app
  wakeups, but exact Condensed/Flex preview matching would be lost. User declined
  approximations; no replacement family or changed preview has been implemented.
- Rasterize the exact bundled font and update through the app: precise typography,
  but widget periodic updates have a 30-minute minimum, WorkManager cannot deliver
  minute ticks, and inexact alarms can be delayed. Exact alarms require special
  access and do not guarantee economical visible-only rendering. A service would
  add process lifetime/battery/notification costs. No polling/service was added.
- Pre-render timed animations or ViewFlipper frames: host-side animation has no
  reliable wall-clock resynchronization after invisibility, sleep or time-zone
  changes. This would reintroduce stale/wrong clocks; not implemented.
- A cooperating/custom launcher can render app-owned fonts/code, but is not a
  portable home-screen widget solution across supported third-party launchers.

There is no supported portable native TextClock route identified for the exact
bundled font on Android 12–16. Further experiments should be isolated and validated
across a real launcher process, not an in-app RemoteViews preview.

## Analog minute positioning

Android AnalogClock computes minutes = minute + seconds / 60, even with a hidden
second hand. Its minute rotation is minutes / 60 * 360. At 11:00:20 this is 2 degrees;
a screenshot labeled 11:00 does not establish that its seconds were exactly zero.
The supplied successive screenshots are consistent with this platform behavior.
The raster hand renderer draws a vertical hand about its bitmap centre.

No native RemoteViews setter was found for disabling fractional-minute positioning.
Static pre-rotation would only correct one instant and introduce errors later.
Minute-stepped bitmap updates have the same scheduling costs as rasterized digital
time. A custom launcher-owned view could step minutes without app polling but is
not portable. The native analog behavior and hand artwork are left unchanged while
these alternatives are evaluated, as requested. Exact :00 alignment still needs
naturally timed device captures; the device clock must not be changed for testing.

## Sources

- Android 12 TextView: https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android12-release/core/java/android/widget/TextView.java
- Android 16 RemoteViews (restricted resource context): https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/RemoteViews.java
- Android 16 TypefaceSpan: https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/text/style/TypefaceSpan.java
- Android 16 LeakyTypefaceStorage: https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/graphics/java/android/graphics/LeakyTypefaceStorage.java
- Android 16 AnalogClock: https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/AnalogClock.java
