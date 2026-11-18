# Unfolded viewport and glow fixes

Read AGENTS.md and inspected existing main changes; extended the existing shared implementations.

World Clock previously animated its grid viewport height based on city count, centring that short viewport in the right pane. Its blur therefore moved with the grid and blurred final rows even when they fitted. The right grid now occupies the full pane. Short lists centre using content padding, with blur disabled. When rows exceed the available clear viewport (including 40dp end gutters), fixed top/bottom bands activate. Their positions depend only on pane bounds, not row count or scroll position. Phone layout retains its previous arrangement.

Stopwatch glow was clipped by TwoPaneLayout's explicit clipToBounds on the left pane. Added a default-on clipLeftPane parameter; only ExpandedStopwatch opts out. Its static radial illumination now fades naturally beyond the pane, while other pages retain existing clipping. Face dimensions, controls, timing and gestures unchanged.

## Files

Modified:
- `app/src/main/java/app/expressiveclock/ui/screens/WideClockPages.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/StopwatchScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/responsive/TwoPaneLayout.kt`

Created: this report. No files deleted or dependencies/version changes.

## Verification

`./gradlew assembleDebug testDebugUnitTest`: successful; 61 tests passed. `git diff --check`: passed. Installed compatible debug update on unfolded Pixel without clearing/uninstalling. Inspected short-list centring with clear final rows and Stopwatch glow without vertical clipping boundaries. A reduced-height overflow attempt encountered the device lock screen and is not counted as validation. Size override was restored with wm size reset; original density/font and device posture unchanged. Large overflowing city grids and dark mode were not separately device-verified this pass.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-unfolded-edges-20261118-111721.apk`
Screenshots: `/Users/sambeckman/Downloads/ExpressiveClock-unfolded-edges-20261118-111721`

Branch main; existing extensive modified/untracked work preserved, local and uncommitted. Nothing staged, pushed or published.
