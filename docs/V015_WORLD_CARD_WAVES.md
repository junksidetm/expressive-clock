# v0.1.5 approved World Clock card visual implementation

## Implementation

- Added native, size-cached cubic Bézier wave paths concentrated at the lower-right. Three low-opacity gradient layers overlap, with theme tint strongest on selected cards and fading toward the text. No screenshots, textures or raster assets are used.
- Near-white inactive surfaces, continuous softly tinted selected surfaces, upper reflected-light gradients, 0.8dp outlines, diffuse native shadows, and a little local radial ambient light. Dark/pure-black colour calculations retain dark surfaces and reduce lighting/shadow strength. No global background/theme changes.
- 26dp corners, 10dp grid gaps, 14dp horizontal/12dp vertical content padding, 124dp base height (scaled with accessibility font size). Time is anchored at the bottom so two-line city names do not displace its baseline. Grid remains responsive and uses one normal cell for an odd final item rather than spanning/centring the final card. Updated existing reveal-scroll dimensions for the new spacing.
- Refined 32dp solar day/night badges: warm gold sun, cool blue-grey moon, translucent tinted radial background, fine highlight and restrained glow. Existing solar-coordinate/daylight calculations remain unchanged.
- Selection animates tint/waves, border and badge over 280ms. The 20dp checkmark overlaps the upper-left edge with surrounding grid padding; it does not consume text space. Card dimensions remain unchanged during selection.

## Typography audit

The only bundled Latin family is Google Sans Flex; the other bundled font is Noto Sans Arabic Digits, unsuitable as a replacement Latin city-name family. Inspected the actual font fvar table: opsz 6–144, wdth 25–151, wght 1–1000, GRAD 0–100, ROND 0–100 and slnt -10–0. Compared the prior 82-width/430 time cut with the broader 92-width/480 cut in actual device renders. Explicit card cuts now use:

- City: 19sp, weight 580, wdth 96, ROND 35.
- Date: 13.5sp, weight 400, wdth 100.
- Time: 34sp fitted to available width, weight 480, wdth 92, ROND 25, tnum/lnum enabled.

Font synthesis is disabled. Header typography and Timer/digital numeral styles are preserved. Long city names can wrap to two lines; their times remain aligned. The reference does not provide an actual font asset, so the bundled font is a visual approximation, not a claim of exact typeface identity.

## Selected-city header follow-up

Added requested progressive blur to the mechanical location-name reels. Blur increases smoothly with reel travel, with unbounded blur and stationary top/bottom feathering in an overscan viewport. Settled text stays crisp; layout height is unchanged. System reduced-motion settings bypass the effect.

Fixed the malformed San Francisco header text. The old renderer measured and drew isolated glyphs, causing uneven bearings/spacing. The reels now use full-paragraph caret advances and draw clipped strips of the fully shaped paragraph. This preserves actual word spaces, kerning and narrow-letter bearings while retaining sequential reel movement. Cell bounds retain the widest measured width to avoid slot shrinkage during rapid selection. Accessibility continues to announce the complete selected name. The mistakenly attempted card-title autofit change was removed following the user's clarification.

## Files

Modified existing task-start files:
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt
- app/src/main/java/app/expressiveclock/ui/theme/WorldClockType.kt
- app/src/main/java/app/expressiveclock/ui/MechanicalText.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockHeader.kt (only enables the optional name blur)

Created:
- app/src/main/java/app/expressiveclock/ui/screens/WorldCitySurface.kt
- docs/V015_WORLD_CARD_WAVES.md

No files deleted. No changes to the analogue face/hands, digital readout, header collapse geometry/visibility/snapping, bottom navigation, startup persistence, alarm audio, scheduling, timers, stopwatch or widgets.

## Verification and visual comparison

- ./gradlew assembleDebug testDebugUnitTest: BUILD SUCCESSFUL; 48 tests, zero failures/errors.
- git diff --check passed. Task-start app/src/docs SHA256 comparison: only the four intended existing files changed; no files missing.
- OPPO Find N6: inspected expanded and collapsed clocks, Sydney and another selected city, long-name card wrapping/aligned times, actual San Francisco header after shaping fix, rapid Tokyo/London/San Francisco selection, day/night badges, light/dark surfaces, and combined unfolded view. Selection survived the responsive-layout transition. Display-state override removed; night setting restored to custom_schedule and stay-awake to 0.
- Compared captures with the approved mockup. A second refinement gave selection a continuous tint, strengthened soft elevation, increased curve bend and anchored time baselines. The final cards retain the reference's pale surface hierarchy, lower-right waves and warm/cool badges; their colour follows the user's selected theme rather than imposing the mockup's cyan.
- Screenshot 01 shows final card styling with Sydney before the later header-only shaping correction (Sydney appearance is unchanged). Screenshot 02 shows the final corrected San Francisco header. Screenshot 03 shows the collapsed clock, 04 the unfolded interface. Screenshot 05 verifies final card styling in dark mode before the later header-only shaping correction. Screenshot 06 is a sampled in-motion name transition, not a frame-rate assessment.

## Remaining differences / limits

Native Android shadow softness differs from the reference's rendered lighting; the analogue face remains intentionally unchanged and ambient light is much more restrained. Longer names wrap in compact cards, unlike the short city names in the mockup, and 124dp base height provides room for them. Pixel installation/render sampling was attempted, but Recents prevented reliable full-screen visual QA; the dependable captures are OPPO. Returning to folded mode relocked the OPPO; pure-black visual checks were not completed. No exhaustive maximum-font-size/non-Latin sweep, odd-count device test or frame-rate profiling was performed. The unchanged solar/timezone/snapping regression tests passed.

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-world-waves-20261117-140510.apk
Screenshots/build log: /Users/sambeckman/Downloads/ExpressiveClock-world-waves-review-20261117-140510

Final branch: main. Working tree remains dirty with all existing uncommitted v0.1.5 changes preserved. Nothing staged, committed, pushed, merged, released or published. Existing Downloads APKs were not overwritten.
