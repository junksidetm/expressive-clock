# Smooth new-city scroll: repeated unfolded device testing

Read AGENTS.md and inspected main's existing uncommitted changes. Modified only `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`; created this report. No files deleted.

## Changes

- Centre-padding changes now animate over 420ms rather than moving existing rows instantly when a row is inserted.
- Wait for measured added item and settled padding; scroll by the exact last-row bottom reveal distance using measured viewport/end padding, a single FastOutSlowIn animation (420–750ms), and no lazy-grid multi-phase item seek/snap.
- Reveal the added card alongside movement rather than starting its entrance only after scrolling stops.
- Suppress competing item-placement springs while the addition is pending, retaining existing deletion spring behavior.
- Keep the pending addition alive until both entrance and scroll coordination complete. Interrupted scrolls release the reveal safely; superseded additions cannot strand invisible cards. Completion resets for repeated additions of the same zone.

## Actual Pixel tests

Connected Pixel 11 Pro Fold 65261FDDJ002GB, unfolded, physical 2076×2152, density 390. Compatible debug updates installed without uninstalling/clearing data. Five successful real Add Location runs were recorded and inspected as native video frame sequences:

1. Accra from list top, opening a new row.
2. ACT at list end, filling the existing row: no unnecessary list motion.
3. Adak from mid-list, creating another row.
4. Addis Ababa from list top across the longer list.
5. Final-build Accra repeat after cleanup and reinstall.

All reached a fully visible added card. Inspected frame sequences show continuous travel and no final position snap or separate delayed card pop. One capture accidentally opened New Alarm and was excluded from both success count and delivered performance results; cancelled without saving.

Android gfxinfo full-process results while screen recording (includes sheet dismissal, clock frames and recording overhead; not an isolated scroll benchmark):

| Run | Frames | Janky frames |
| --- | --- | --- |
| 01-accra | 386 | 16 (4.15%) |
| 02-same-row | 501 | 12 (2.40%) |
| 03-new-row-middle | 485 | 14 (2.89%) |
| 04-long-scroll | 478 | 17 (3.56%) |
| 06-final-verified | 384 | 18 (4.69%) |

These debug captures still report 2.4–4.69% janky frames; zero dropped frames is not claimed. No before/after hardware benchmark or exhaustive fold/font/theme matrix. Centred short-list padding path was source reviewed, not separately exercised with a shortened user dataset.

All temporary Accra/ACT/Adak/Addis Ababa entries were removed through the normal delete confirmation. Existing locations and ordering retained; restored selected Sydney/list top. No alarms/timers modified or saved. Display/font/posture/animation settings unchanged.

`./gradlew assembleDebug testDebugUnitTest`: successful, 61 tests passed. `git diff --check`: passed.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-smooth-city-scroll-20261118-131334.apk`
Videos, frame sheets, screenshots and raw gfxinfo: `/Users/sambeckman/Downloads/ExpressiveClock-smooth-city-scroll-tests-20261118-131334`

Branch main; existing extensive modified/untracked v0.1.5 work preserved, local and uncommitted. Nothing staged, pushed or published.
