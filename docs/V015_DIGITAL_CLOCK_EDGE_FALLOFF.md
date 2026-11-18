# Digital World Clock edge falloff refinement

- Modified only app/src/main/java/app/expressiveclock/ui/WorldClockDigitalReadout.kt.
- Moved the top/bottom alpha falloff from the moving glyph to a stationary viewport, with 12dp edge fades and extra drawing space. Removed the animation size clip. Header layout size, digit size, staggering and selection behavior remain unchanged.
- Blur now increases smoothly with distance from the settled position, using the same spring/easing as the reel movement. Unbounded blur retains the halo until the fixed viewport fades it out. Settled numerals remain crisp; reduced-motion behavior is preserved.
- Created this report; no source files created or deleted. Existing shared Timer viewport helper is unchanged.

Verification: ./gradlew assembleDebug testDebugUnitTest succeeded; 45 tests, zero failures/errors. git diff --check passed. Installed and exercised the collapsed digital clock on OPPO Find N6; captured a live ticking frame. Screenshot checks confirm layout and soft moving-digit appearance, but do not constitute frame-rate profiling. Pixel and dark/pure-black visual checks were not repeated for this targeted change.

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-digit-falloff-20261117-131224.apk
Screenshot and build log: /Users/sambeckman/Downloads/ExpressiveClock-debug-digit-falloff-20261117-131224

Final branch: main. Working tree remains dirty with existing v0.1.5 changes preserved. Nothing staged, committed, pushed or published. Existing Downloads APKs were preserved.
