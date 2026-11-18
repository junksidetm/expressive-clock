# City autoscroll and glow continuity

Read AGENTS.md, inspected existing main changes, and extended the shared grid in place.

Modified: `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`. Created: this report. No files deleted.

- Removed the fixed-delay and hand-estimated row/scroll-range calculation for additions. Waits for the added location to reach the measured grid, then uses LazyGrid animateScrollToItem with clear-edge offset and native padding/end clamping. Responds to column count changes. Interrupted scrolling releases the pending entrance in finally; cards whose pending flag is superseded reset visible, preventing a stranded invisible card after rapid additions/navigation.
- Selected/deletion-selection halo light is now painted by the grid viewport rather than by lazy item nodes. Position is derived from the measured visible-row anchor and shared row geometry, including horizontal page entrance translation/opacity. The halo remains drawable as its owning card is culled, and fades through the fixed viewport blur. Existing selection tint, card surface, shadow and badge animations remain. No global background animation or raster assets.

## Verification

`./gradlew assembleDebug testDebugUnitTest`: successful, 61 tests passed. `git diff --check`: passed. Installed compatible debug update on unfolded Pixel. Added temporary Singapore from Suggested Cities, confirmed autoscroll/reveal at list end, reversed scrolling into the fixed lower falloff, inspected glow, and deleted only the temporary location through the normal confirmation. Restored original selected Sydney and list top. Existing saved locations retained; display/font/posture settings unchanged. Folded mode and rapid repeated additions were source/build reviewed but not separately device-exercised this pass.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-city-scroll-glow-20261118-112620.apk`
Screenshots: `/Users/sambeckman/Downloads/ExpressiveClock-city-scroll-glow-20261118-112620`

Branch main; all existing extensive modified/untracked v0.1.5 work preserved, local and uncommitted. Nothing staged, pushed or published.
