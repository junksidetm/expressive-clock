# Minimal UI refinement

Extends existing uncommitted work without changing persistence keys, scheduling,
audio, widgets, timers or stopwatch behavior.

Changed:
- ui/AlarmWorldPages.kt: removes glass indicator container entirely. Two 5–6.5dp
  dots use fixed neutral grey / white based on surface luminance, with swipe-progress
  interpolation. Compact 24dp-wide by 40dp-high tap areas preserve navigation.
  Existing visibility timer and fade remain unchanged.
- ui/screens/WorldClockPanel.kt: optional flatPortrait presentation removes outer
  glass, inset, border and shadow. Shared map height follows width/aspect ratio
  (capped at 45% of available height on short windows), above the scrollable saved
  city list and retained Add Location button. Existing framed mode is default.
- ui/ClockApp.kt: enables flat mode only in compact portrait. Existing landscape
  and expanded pane presentation retained; content reserves indicator/dock clearance.
- ui/screens/SettingsScreen.kt: moves indicator toggle below Start week on; standard
  Alarm card time format navigation row opens existing ChoicePickerDialog with
  two radio choices. Saved preference and default remain unchanged.

Verification: assembleDebug testDebugUnitTest SUCCESS (24 Kotlin tests), Python
regression suite 12 PASS, git diff --check clean. Installed via adb -s
66231FDDJ003QS install -r preserving data. Pixel live hierarchy confirmed compact
indicator targets and original 12h cards. Further UI interactions unexpectedly
returned launcher hierarchy, so Settings dialog, 24h selection persistence, both
map layouts and light/dark visual quality are NOT claimed as device-verified in
this pass. No unreliable screenshot is counted as a visual pass. OPPO disconnected.
These visual checks remain for a reliable device session. Existing time-format
and visibility persistence keys are unchanged; no user preferences changed by tests.

APK: ~/Downloads/ExpressiveClock-v015-minimal-ui-debug.apk
No files deleted. New document: this report. Branch main; all work remains local,
unstaged/untracked. No commit, push, merge or publication; app/release preserved.
