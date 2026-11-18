# Digital native typography investigation — 16 November 2026

Investigation only. No renderer, layout, font, configuration-preview or widget
configuration changes were made in this investigation. Existing work is preserved.

## Newly verified Pixel-specific route

The connected Pixel 11 Pro Fold (Android 17) declares a named google-sans-flex
family in /product/etc/fonts_customization.xml backed by
/product/fonts/GoogleSansFlex-Regular.ttf. It also declares google-sans,
google-sans-clock and google-sans-flex-clock. These are device families, not
portable Android guarantees. Unknown family names can silently resolve to a
fallback; naming a family alone is not a capability test.

The bundled preview font is Google Sans Flex revision 4.0050048828125. The Pixel
font is revision 4.001007080078125. SHA-256 differs:

- Bundled: 04f60452e4f5bc5c7bc45075324435a6d0a05dae7f2413869053f13ffc93cc47
- Pixel: 83a85228956fcdfab605fc72f421eb0a416563d57d4eb0944396c02d874fe2a6

Both have 2000 units/em and identical axes/ranges: opsz 6–144, wdth 25–151,
wght 1–1000, GRAD 0–100, ROND 0–100, slnt -10–0.

Using FontTools variable glyph sets and decomposed outline recordings, compared
both fonts at opsz=144, wdth=25 and 100, wght=100,400,600,900. For each of these
eight combinations, default digits 0–9, colon, uppercase A–Z, comma, space,
slash, period and hyphen have identical outlines and advance widths. The digits
after lnum/tnum substitutions also match at all eight combinations (feature
lookup indices/glyph names can differ, so substitutions were followed separately).

This supports exact glyph geometry for the tested repertoire on this Pixel.
It does NOT prove identical whole-widget pixels: native family resolution,
shaping options, weight selection, spacing, baseline metrics, text bounds and
launcher RemoteViews application still require a candidate-layout device test.
It does not prove other locales, other font revisions, other devices or Android
12–16. No candidate layout was installed or applied to the production widget.

## Supported mechanisms and their limits

- A named system font can be selected through XML fontFamily without loading an
  app resource font in the launcher's restricted context. google-sans-flex is
  the strongest candidate on this device; use the same family for Condensed and
  Flex, with wdth=25 versus 100, opsz=144 and the actual selected wght.
- TextView.setFontVariationSettings is a RemoteViews-supported setter. Axes only
  affect axes implemented by the resolved font. They cannot turn Roboto into
  Google Sans Flex. A successful setter call is not proof that a family exists
  or that each requested axis affected rendering.
- setTextScaleX and setLetterSpacing are supported RemoteViews setters. They can
  adjust width/tracking, but scaling compresses strokes, curves and counters;
  it is not equivalent to designed width-axis interpolation. Font-size changes
  scale height too. These cannot guarantee matching glyphs across weights.
- sans-serif-condensed on this Pixel uses Roboto with wdth=75. roboto-flex also
  exists. Both are alternatives, not exact substitutes for the bundled family.
- A Pixel-specific google-sans or clock family must not be assumed equivalent
  from its name. It may have a different design, axes, numeral forms or coverage.
- Serif and Monospace already use the corresponding system families in preview
  and widget; Cursive offers Regular only. Retain these families and validate
  the actual selected system weight rather than applying Google Sans axes to
  them. Identical system family names do not promise identical cross-device art.

## Screenshot evidence

Reviewed existing paired ADB Condensed and Flex preview/host captures from:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-111306-188550

Condensed preview is visibly much narrower than the host. Flex preview and host
also differ in glyph appearance; host date weight/spacing is visibly different.
The compared screenshots have different sample times and text sizes, so they
establish the visible mismatch, not a pixel-difference score.

Captured current unchanged host again with ADB:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-132842-698513
Digital time matched device time at 13:28:42. No widget settings were reopened.
Candidate named-family rendering has NOT yet been screenshot-validated in a
real launcher. That is the next experiment, not a claimed result.

## Recommendation before implementation

Keep native TextClock. First run an isolated, explicitly identified named-family
prototype on the Pixel using google-sans-flex and the preview's exact axes. Test
all four Google Sans weights, both widths, all layouts and date/seconds options;
compare like-for-like sample text, digit shaping, baselines and bounds. Keep the
original preview unchanged. The current geometry evidence justifies this test;
it does not yet justify shipping a claimed universal fix.

If confirmed, a capability-validated device-specific path could retain native
updates without periodic app work. On devices lacking a compatible system font,
exact bundled Condensed/Flex parity is not generally available through supported
native RemoteViews font loading. Product choices would then be explicit:

1. Exact-compatible devices only: preserve the original design where validated;
   lose Condensed/Flex availability elsewhere (existing widgets need a migration
   decision, not silent substitution).
2. Explicit opt-in system-font alternatives: keep live updates and broad reach;
   lose the original glyph design and potentially width/weight consistency.
3. Horizontal scaling of a disclosed alternative: closer overall proportions;
   lose designed stroke/counter proportions and reliable matching across weights.

No option identified guarantees exact original typography on every Android
12–16 device while retaining the stated native-only restrictions. Do not add
polling, rasterized time refreshes, change previews to hide differences, or
silently activate fallback families.

## Primary references

- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/TextView.java
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/widget/RemoteViews.java
- https://developer.android.com/reference/android/widget/TextView
- https://developer.android.com/reference/android/graphics/Typeface

No implementation build or replacement APK was needed for this investigation.
Only this findings document was added to the repository; device font files and
analysis tooling were kept in temporary directories. Branch remains main; all
existing staged/unstaged/untracked work was preserved. No commits or publication.
