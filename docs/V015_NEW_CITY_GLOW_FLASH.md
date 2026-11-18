# Stable new-city glow opacity

Read AGENTS.md and inspected main's existing uncommitted work. Modified only `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`; created this report. No files deleted.

New-card entrance used an overshooting spring for scale and alpha. The halo used a separate shorter tween. Spring alpha could reach/clamp at 1, fall below 1 while settling, and return to 1; finishing the pending entrance also changed that alpha path. Added a retained per-city 220ms monotonic arrival opacity state and shared the exact State between card and viewport halo. Scale retains its existing spring. Clearing the pending flag leaves opacity at 1, avoiding a second fade/reset. Existing autoscroll and shared page entrance phases retained.

## Verification

`./gradlew assembleDebug testDebugUnitTest`: successful, 61 tests passed. `git diff --check`: passed. Compatible updated APK installed on unfolded Pixel 65261FDDJ002GB. Recorded and inspected native frame sequences for Alarm→World navigation plus rapid reversal, Dubai addition filling the last row, and Abidjan addition creating a row with autoscroll. No opacity flash observed in inspected sequences. Removed only temporary Dubai/Abidjan entries via normal delete confirmation; restored San Francisco and original list top. Existing locations retained. System/display/font/posture settings unchanged. Folded, dark-mode and exhaustive interruption combinations were not separately device-tested this pass.

Files: source above modified; this report created; none deleted. Prior WorldClockPanel/WideClockPages page-transition changes remain preserved.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-stable-card-glow-20261118-142650.apk`
Recordings/frame sheets: `/Users/sambeckman/Downloads/ExpressiveClock-stable-card-glow-tests-20261118-142650`

Branch main; existing extensive modified/untracked work remains local and uncommitted. Nothing staged, pushed or published.
