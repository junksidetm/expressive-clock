# World Clock analogue/digital implementation — 17 November 2026

Implemented locally on `main`. Existing staged/unstaged/untracked work was inspected before editing; no commits, branches, pushes, releases or publishing operations were performed.

## Changes in this task

Modified existing files (including the already untracked pager file):

- `app/src/main/java/app/expressiveclock/ui/AlarmWorldPages.kt`: bypass pager and indicators entirely in expanded/two-pane mode; retain portrait pager and refined indicators.
- `app/src/main/java/app/expressiveclock/ui/ClockApp.kt`: hoist shared World Clock viewport state; retain saveable city selection and portrait page state; use the alarm Add action in two-pane mode and its existing inline location picker.
- `app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt`: remove unused placeholder map composables; leave alarm grid, cards, controls and inline editor behavior intact.
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`: replace dotted map with shared collapsing header, retain city swipe/delete/entrance flow and selected-row styling, place local city and Add Location in the scrollable list, add selection and Remove accessibility semantics.

Created:

- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockHeader.kt`: shared analogue/digital rendering, live timezone/DST calculations, shortest-path 680ms hand correction, theme-coloured seconds/pivot glow, system 12/24-hour digital display with tabular numerals, city/date transitions.
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPresentation.kt`: hoisted list/collapse state across responsive layout branches.
- `app/src/test/java/app/expressiveclock/ui/screens/WorldClockTimeTest.kt`: shortest hand path, Nepal fractional offset and date rollover, New York DST transition tests.
- This report.

No files deleted. Dotted-map rendering was removed from the active UI; the map data asset and pre-existing unused WorldClockScreen file were retained to avoid deleting unrelated user work.

## Decisions

A single WorldClockPanel serves both layouts. Nested scroll consumes movement to shrink the header before the list scrolls; downward movement restores the header when the list reaches its top. The digital header remains outside the scrolling list. Header collapse follows drag position directly without a delayed state switch.

The analogue frame ticker is lifecycle-aware and observed in Canvas drawing, avoiding frame-rate recomposition of city rows. Text uses the existing 1Hz wall ticker. Real ZoneId rules determine every target hand position; transient angle corrections decay toward the continuously advancing target, with at most half a revolution per hand. System-disabled animations skip mechanical motion and use second-boundary redraws. Material surface/onSurface/primary tokens support light, dark and pure-black themes without hardcoded light backgrounds.

Persistence, alarm/timer/stopwatch behavior, widget code, custom sounds, notifications, app versions and dependencies were not changed.

## Verification

- Final command: `./gradlew assembleDebug testDebugUnitTest` — successful.
- 27 unit tests passed, including the three new clock tests and existing alarm/audio/upcoming-notification/widget tests.
- `git diff --check` — passed.
- Pixel 11 Pro Fold: installed debug APK; inspected portrait analogue header and city selection, previous-day San Francisco date/time, partial collapse, full digital collapse, dark-mode display, system 12-hour and 24-hour formats, two-pane layout with no page indicators, unchanged alarm grid bounds while the right pane collapses, all city rows and Add Location access.
- Used a temporary OPENED device-state override for two-pane checks. Folding back retained San Francisco and the collapsed viewport; portrait indicators returned. Opened and dismissed Add Location without adding/removing saved cities.
- Original device-state override (none), time format (24), and night-mode setting (custom_schedule) restored.
- A usable full-screen dark collapsed screenshot was captured. Expanded/partial captures were attempted but can show stale Recents surfaces; their visual evidence is limited. UI hierarchy files record the two-pane and fold-back results.

## Remaining verification limits

OPPO Find N6 was not connected. No reliable frame recording or frame-time profiling was obtained, so mechanical settling and continuous sweep quality still merit hands-on review. Full-screen light, pure-black, and every intermediate scroll state were not exhaustively visually verified. No claim is made of comprehensive device regression testing or actual physical folding; the layout test used Android's device-state override. The final downward pre-scroll safeguard was build/unit-tested; a reliable post-change device reverse-scroll capture was not obtained.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug-world-clock-20261117.apk` (existing default APK preserved).

Capture evidence: `/Users/sambeckman/Downloads/ExpressiveClock-world-clock-review-20261117-100733`.

Final branch: `main`. Working tree remains dirty with all pre-existing modified/untracked files retained, plus the local World Clock changes listed above. Nothing staged or committed by this task.
