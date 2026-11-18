# World Clock ambient glow edge fix

The ambient background used a radial gradient with a radius derived from the panel's longest dimension, then painted it into a rectangle. On a tall phone viewport the gradient was still nontransparent at the top and side drawing boundaries, producing the visible rectangular cutoff in both expanded and collapsed states.

Changed only app/src/main/java/app/expressiveclock/ui/screens/WorldCitySurface.kt: worldClockAmbient now draws a cached radial brush in normalized elliptical space. Its perimeter is fully transparent and sits inside the panel bounds, with softened outer colour stops. The horizontal/vertical radii adapt independently to window dimensions, so the atmospheric light fades away before its container edges. This shared helper fixes phone and wide page backgrounds without moving content or modifying the separate clock/card lighting, navigation or animations. No blur passes, dependencies or continuous animations added.

Files modified: WorldCitySurface.kt. Files created: this report. No files deleted.

Verification: ./gradlew assembleDebug testDebugUnitTest succeeded; all 55 tests passed. git diff --check passed. Task-start app/src/docs SHA256 comparison confirms only this existing source file changed, with no missing files. Installed the same built APK on the unlocked OPPO Find N6 and inspected expanded and fully collapsed portrait screenshots. Both states now fade into the surrounding background without the previous rectangular top/side boundary. Screenshots: /Users/sambeckman/Downloads/ExpressiveClock-debug-glow-edge-20261117-145814. Temporary stay-awake setting restored to its original value 0. No additional source edits or rebuild were needed for this verification.

Debug APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-glow-edge-20261117-145814.apk

Branch: main. Working tree remains dirty with all existing uncommitted v0.1.5 work preserved. Nothing staged, committed, pushed or published. Existing APKs were not overwritten; a unique filename was used.
