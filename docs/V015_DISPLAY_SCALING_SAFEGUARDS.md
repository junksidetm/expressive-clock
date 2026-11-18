# Display scaling safeguards

Implemented targeted responsive safeguards instead of disabling Android accessibility scaling. Default-size dock geometry and normal World Clock/Stopwatch face layouts are retained when content fits.

- Narrow dock fits the Plus button and selection highlight within the available width.
- World Clock reserves measured title/subtitle height, reduces its dial when needed, and constrains face numerals to the dial. Scrolled city cards now use the existing progressive blur/fade at their upper boundary as well as the bottom.
- Timer setup scroll edges use the existing progressive mask. Presets wrap into balanced rows when measured labels cannot fit. Wide setup/control panes scroll if necessary; the running timer face fits its pane.
- Compact Stopwatch reserves space for controls above the dock at enlarged settings.
- Onboarding title/status headers stack when tight rather than squeezing titles into letter-wide columns. Its scroll viewport gains progressive falloffs.

## Verification

`./gradlew assembleDebug testDebugUnitTest`: succeeded; 61 tests, zero failures. `git diff --check`: passed.

Pixel folded outer-display checks: baseline density 390/font 1.0, enlarged density 520/font 1.3. Inspected default Timer/World Clock, enlarged World Clock expanded/collapsed, Timer scrolling/preset wrapping and Stopwatch controls. Compatible debug update installed without uninstalling or clearing data. Restored density to physical 390 (no override), font scale 1.0. Some early screenshots captured launcher/transition frames and were excluded from conclusions.

Remaining validation: exhaustive maximum accessibility font sizes, every sheet, unfolded/split-window combinations and other manufacturers were not device-tested in this pass. Onboarding fix built successfully; the initial failure was reproduced, but its final stacked state was not re-entered after setup completion. No global font/density overrides were introduced.

## Files

Modified this pass:
- `app/src/main/java/app/expressiveclock/ui/ClockDock.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockHeader.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/TimersScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/StopwatchScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/OnboardingScreen.kt`

Created: `app/src/main/java/app/expressiveclock/ui/responsive/ScrollPane.kt` and this report. No files deleted.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-display-scaling-20261118-092157.apk`

Branch: main. Existing extensive modified/untracked v0.1.5 and installer work remains local and unstaged. No commits, pushes, branch changes or publishing.
