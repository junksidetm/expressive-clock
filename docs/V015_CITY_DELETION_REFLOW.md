# World Clock deletion reflow — 2026-11-18

## Changes
- Modified `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt` in place, preserving its existing uncommitted work.
- Put lazy-grid placement animation on the outer item modifier, matching Alarm cards. Reuse stiffness 380 and damping ratio 0.68.
- Limit placement suppression to an active addition with moving centred padding. Release the pending addition coordinator before deletion so it cannot suppress reflow.
- Capture a single immutable city-list snapshot for the lazy item provider and its entrance/glow state maps. Device testing exposed a missing-map-key crash during addition; this prevents lazy measurement from observing a newer list with older animation maps.
- Existing exit fade/scale, stable zone keys, deletion confirmation and selection behaviour remain in use.
- Created this report. No files deleted; no versions or dependencies changed.

## Verification
- `./gradlew assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL; 61 tests, zero failures/errors.
- `git diff --check`: passed.
- Compatible debug update installed on unfolded Pixel serial 65261FDDJ002GB, without uninstalling or clearing data.
- Recorded and inspected native video frame sequences for two temporary-city deletions: Tokyo removal moved Paris right-to-left and Singapore into the preceding row; Paris removal moved Singapore right-to-left. Both show intermediate positions and spring settling rather than an instantaneous jump. Row-count reduction also animates centred content padding.
- Added Singapore after the map snapshot fix and completed three deletions without a repeat crash. Removed all temporary Tokyo/Paris/Singapore test cities. Final list Sydney, San Francisco, London, New York; Sydney selected. Original locations preserved.
- Folded, OPPO, dark-mode and long-list/offscreen deletion cases were not separately device-tested in this pass. The shared grid implementation serves both widths.

## Delivery
- APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-city-deletion-spring-20261118-143926.apk`
- Recordings, frame sheets and final screenshot: `/Users/sambeckman/Downloads/ExpressiveClock-city-deletion-checks-20261118-143926`
- Final branch: `main`. Existing extensive tracked/untracked v0.1.5 and helper work remains uncommitted; nothing staged, committed, pushed or published.
