# Location-name ticker fix

Modified `app/src/main/java/app/expressiveclock/ui/MechanicalText.kt`: newly composed character reels now initialise from the previous paragraph rather than the target paragraph. Extra slots in longer names start empty and enter only when their staggered update runs. Existing reels retain their displayed state, including interrupted transitions. Blur, shaped-paragraph rendering, positioning and ticker timing remain unchanged. Initial presentation still shows the complete title immediately.

Verification: `./gradlew assembleDebug testDebugUnitTest` succeeded; 55 unit tests, zero failures/errors. `git diff --check` passed. Installed on folded Pixel and captured an 8x-slowed Alarm → World Clock entry followed by Sydney → San Francisco selection. Inspected early transition and settled frames: no prematurely visible trailing letters. Animation scale restored to original unset/default. OPPO, unfolded and exhaustive rapid-selection checks not repeated this pass.

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-location-ticker-20261117-184936.apk
Screenshots: /tmp/location-title-qa/name-00.png through name-06.png.

Files modified: MechanicalText.kt. Created: this report. Deleted: none. Branch: main, existing modified/untracked v0.1.5 work preserved. Nothing staged, committed, pushed or published.
