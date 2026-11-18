# World Clock card-glow viewport fix

The remaining vertical edge came from the city grid viewport clipping selected-card light outside its 8dp content gutter. The earlier ambient-background fix remains unchanged.

Modified only app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt: the lazy grid now has a centred horizontal drawing overscan of at least 32dp per side, scaling with card width for larger panes. Equal extra content padding compensates for the wider viewport, preserving the existing card widths, positions, column count, vertical spacing, scrolling calculations and bottom progressive fade. The selection badge also has more horizontal drawing room. No glow intensity, clock, navigation, animation or persistence changes.

Files created: docs/V015_WORLD_CARD_GLOW_VIEWPORT_FIX.md. No files deleted.

Verification:
- ./gradlew assembleDebug testDebugUnitTest: BUILD SUCCESSFUL, 55 tests passed with no failures/errors.
- git diff --check passed. Task-start app/src/docs SHA256 comparison: only WorldClockPanel.kt changed; no files missing.
- Installed on OPPO Find N6 and inspected expanded and collapsed left-column selection, then a collapsed right-column selection. Card positions and content remain unchanged; the glow now continues smoothly across the former viewport boundary.
- Checked raw screenshot pixel values at the old left boundary. At x43→44, y1750 in the expanded capture, the old red channel stepped 250→241; the new capture stays 241→241. Nearby rows likewise lack the old hard jump. Full sample values are included in pixel-edge-comparison.txt.
- Temporary OPPO stay-awake setting restored to its original value 0.

Limits: Pixel, dark and unfolded device checks were not repeated for this drawing-bounds-only change. The same shared grid implementation is used by phone and wide layouts.

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-card-glow-gutter-20261117-151143.apk
Screenshots/build/pixel evidence: /Users/sambeckman/Downloads/ExpressiveClock-card-glow-review-20261117-151143

Branch: main. Working tree remains dirty with all existing uncommitted v0.1.5 work preserved. Nothing staged, committed, pushed or published. Existing APKs were not overwritten.
