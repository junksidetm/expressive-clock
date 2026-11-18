# Timer preset long-press editing — 2026-11-18

## Implementation
- `TimersScreen.kt`: passes an indexed edit callback through phone and expanded layouts, uses the existing luminousElasticClickable long-press callback, and opens SinglePresetEditDialog for the pressed index. Index mapping supports multiple rows and duplicate durations. Normal taps retain immediate preset start. Combined-clickable exposes the long-click accessibility action.
- `SettingsScreen.kt`: makes the existing SinglePresetEditDialog internal for reuse; its appearance, quick options and duration validation remain unchanged.
- `ClockApp.kt`: persists only the selected preset index via the existing ViewModel updateSettings transform against current settings. Set commits immediately from the direct editor; Cancel/back leaves persisted settings unchanged.
- Created this report. No files deleted, versions/dependencies changed, or unrelated edits overwritten.

## Verification
- `./gradlew assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL, 61 tests pass.
- `git diff --check`: passed.
- Folded Pixel 65261FDDJ002GB compatible debug install. Long-pressed Preset 2: correct title and 2 min value appeared with timer still inactive. Selected 30s then Cancel: reopening retained 2 min. Selected 30s then Set: button and reopened editor showed 30s. Restored original 2 min. Normal-tapped first preset: one-minute timer started; cancelled the test timer and confirmed original four presets (1/2/5/10 min) remain.
- Expanded layout callback wiring compiled; unfolded/OPPO/dark and app-process restart persistence not separately device-tested. Uses the same dialog and persistence path as Settings.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-preset-long-press-20261118-145122.apk`
Screenshots: `/Users/sambeckman/Downloads/ExpressiveClock-preset-edit-checks-20261118-145122`

Final branch main. Existing extensive tracked/untracked work remains local and uncommitted. Nothing staged, committed, pushed or published.
