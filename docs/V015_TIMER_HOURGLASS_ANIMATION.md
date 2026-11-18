# Timer navigation hourglass animation

Replaced Timer's whole-icon flip with a stationary existing Material Outlined.HourglassEmpty outline and theme-coloured sand drawing. A tap runs a finite 900ms animation: the top chamber drains, a fine stream/three moving grains fall, and a small lower pile grows. Sand fades at the start/end, leaving the original empty outline at rest. Rapid taps continue current progress rather than resetting visible sand. Reduced motion skips the sand movement. Draw-time progress avoids per-frame recomposition. No continuous idle animation or dependency added.

Modified: app/src/main/java/app/expressiveclock/ui/ClockDock.kt. Created: app/src/main/java/app/expressiveclock/ui/HourglassDockIcon.kt and this report. Deleted: none. Existing icon sizing, selected highlight/scale, touch targets, semantics, navigation and timer behaviour preserved. Prior uncommitted stopwatch/World Clock/installer work retained.

Verification: `./gradlew assembleDebug testDebugUnitTest` succeeded; 55 tests, 0 failures and 0 errors. `git diff --check` passed. Reviewed clipping geometry against the bundled Material icon source. No connected-device tests performed, as requested; visual timing remains for owner review.

APK: [ExpressiveClock-debug-hourglass-20261117-204522.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-hourglass-20261117-204522.apk), SHA-256 matches build output; no existing APK overwritten. Installer Helper delivery is not activated, so no upload/notification attempted.

Final branch main, working tree remains dirty with all existing user changes. Nothing staged, committed, pushed or published.

## Follow-up: flip first, then sand

Restored Timer's original 180-degree / 320ms outline rotation, followed by the 900ms sand-flow animation. Sand is hidden during the flip. The empty symmetric outline normalises to upright before sand starts. Rapid taps finish/fade any active sand over 120ms before continuing the flip from its displayed angle; reduced motion skips both effects. Only ClockDock.kt changed in this follow-up, plus this report; no files created/deleted.

`./gradlew assembleDebug testDebugUnitTest`: successful, 55 tests passed. `git diff --check`: passed. No connected-device tests run, per user preference. APK: [ExpressiveClock-debug-hourglass-flip-20261117-204809.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-hourglass-flip-20261117-204809.apk), hash matches build output, no existing APK overwritten. Main branch, existing working-tree changes preserved; nothing staged, committed, pushed or published.

## Final refinement: remove sand

Removed the sand state, sequencing and custom sand renderer. Timer again uses its existing Material outline with the 180-degree / 320ms flip. Preserved rapid-tap continuity and reduced-motion handling. Stopwatch hand animation, glow, timezone fade and other uncommitted features unchanged.

Modified: ClockDock.kt and this report. Deleted: HourglassDockIcon.kt, the now-unused sand-only renderer created in this task sequence. Created: no source files. `./gradlew assembleDebug testDebugUnitTest` succeeded; all 55 tests passed. `git diff --check` passed. No connected-device tests run per user request.

APK: [ExpressiveClock-debug-hourglass-outline-20261117-205034.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-hourglass-outline-20261117-205034.apk), hash matches build output; no existing APK overwritten. Main branch, existing working-tree work preserved apart from the explicitly requested removal. Nothing staged, committed, pushed or published.
