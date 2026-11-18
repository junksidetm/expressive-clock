# v0.1.5 World Clock typography and Alarm Sound refinement

## Result

World Clock uses explicit bundled Google Sans Flex cuts: city names 18sp/560/wdth94, dates 13sp/400/wdth98, card times 32sp/430/wdth82 with tabular numerals. Actual bundled variable axes were inspected (opsz, wdth, wght, GRAD, ROND, slnt). No font dependencies or system-font assumptions were added. Existing gradients, selected badges and solar day/night indicators remain.

The collapsed digital clock now uses the Timer setup's exact Numerals renderer and shared digit/colon/spacing calculations, including rounded glyphs. Its width is 48% of the original full-width readout: 20% larger than the requested compact 40% version. Changing locations sequentially rolls the entire display downward, including unchanged digits and separators. Normal second updates animate only changing digits. Transitioning glyphs gain progressive blur and feathered vertical edges. All positions remain fixed; system animation disabling bypasses motion. Hour/minute analogue rendering and hand-motion code were preserved.

Selected analogue city text sits 20dp lower, nearer its subtitle, with per-character sequential mechanical reels. Digital subtitle spacing and collapse range are coordinated with the compact readout. City-name spaces use explicit glyph advances, avoiding collapsed word spacing.

Alarm Sound now has a compact selected-sound preview card, grouped library, refined animated selection rows, custom-file action and real pause/resume. The waveform is a decorative playback-state animation. Apply updates the editor draft; Back discards changes. The page has horizontal/depth transitions and interactive predictive back with cancellation restoration. Playback pause relinquishes audio focus and safely reacquires it on resume; stale focus callbacks cannot revive playback. Custom URI permission/fallback logic remains intact.

## Files

Modified relative to the task-start snapshot (including previously untracked source):
- alarm/AlarmAudio.kt
- alarm/SoundPlayback.kt
- ui/sheets/AlarmSoundPage.kt
- ui/screens/WorldClockHeader.kt
- ui/screens/WorldClockPanel.kt
- ui/screens/TimersScreen.kt
- test/alarm/SoundPlaybackTest.kt

Created under app/src/main/java/app/expressiveclock/:
- ui/theme/WorldClockType.kt
- ui/MechanicalText.kt
- ui/MechanicalViewport.kt
- ui/TimerReadoutGeometry.kt
- ui/WorldClockDigitalReadout.kt

Also created app/src/test/java/app/expressiveclock/ui/MechanicalTextTest.kt and this report. No files deleted. Timer changes extract identical existing readout geometry and feathered viewport helpers for shared use; timer behavior is preserved.

## Verification

- `./gradlew assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL; 45 tests, zero failures/errors.
- `git diff --check`: passed.
- Compared task-start SHA256 snapshot: only the seven intended existing files changed; no snapshot files missing.
- Installed debug APK on OPPO Find N6 and Pixel 11 Pro Fold, including final size/direction build.
- Existing ADB alarm regression probe passed on both phones: custom audio and fallback, focus loss/restore, exact alarm callback/ringer, timer expiry and upcoming notification actions. Probes preceded the last UI-only readout sizing/direction refinement.
- OPPO UI checks covered sound selection, preview/play/pause, Apply, Back cancellation, system Back, predictive back progress and cancellation restoration. No alarm was saved by these editor-draft checks.
- OPPO screenshots cover refined cards/header, final compact digital readout, sound library/play/pause and return behavior. `oppo-final-digital.png` shows the expanded analogue state despite its filename; `oppo-final-collapsed.png` shows the final 48% digital readout.
- Temporary OPPO night/stay-awake settings restored to custom_schedule / 0.

## Limits

Pixel UI automation/window snapshots were unreliable; do not treat its screenshots as World Clock visual verification. Both phones passed alarm regression probes. No instrumented animation frame-rate measurement, exhaustive font-scale/non-Latin clipping sweep, pure-black visual run or full fold/unfold UI sweep was completed. Predictive back cancellation was verified before a final minor opacity refinement; its gesture geometry/handler remained unchanged. Static screenshots cannot establish motion smoothness.

## Delivery and repository safety

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-typography-sound-20261117-130455.apk
Screenshots/build/probe evidence: /Users/sambeckman/Downloads/ExpressiveClock-typography-sound-review-20261117-130455

Branch: main. Working tree remains dirty with all pre-existing staged/unstaged/untracked v0.1.5 work preserved. No commits, staging, branch changes, pushes, merges, tags, releases or publication. Existing Downloads/ExpressiveClock-debug.apk was preserved; the new APK uses a unique filename.
