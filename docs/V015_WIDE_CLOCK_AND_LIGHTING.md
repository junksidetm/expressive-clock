# v0.1.5 World Clock lighting and separate wide-screen destinations

## Final implementation

Alarm and World Clock remain separate bottom-navigation destinations at every size. Wide Alarm uses TWO columns of the existing alarm cards in its left pane, following the user's refinement; the right pane has the local analogue clock, live digital local time and date. Wide World has the selected analogue clock/name/timezone/date on the left and the existing two-column city grid on the right. No location cards appear on Alarm; no alarm cards appear on World.

A stable shared host contains one mounted analogue-clock renderer. A critically damped spring (stiffness 300) moves it between right/left positions with no rotation or dramatic scaling. Both destinations use the same bounded dial size. Functional panes fade/slide around it; city cards use 350ms entrance animations staggered by 40ms (bounded for long lists), and supporting text uses 160–250ms fade/slide. Rapid navigation retargets the current Animatable rather than launching competing movements. Hidden panes reject touches and hide their accessibility descendants. The existing inline alarm editor and city picker use the companion pane; an alarm draft remains mounted when switching between wide pages.

Hand animation state is hoisted into saved Compose presentation state and retains target zone, motion start, duration and corrections across configuration changes. The shared renderer keeps its live second-hand frame source across wide Alarm/World switches. It mechanically retargets between actual local and selected-zone time using the existing timezone/DST calculations and shortest-path hand movement. No hand geometry, marker positions, numeral proportions or second-hand styling was changed.

The wide layout uses actual available window dimensions: at least 720dp width / 520dp height, with a larger minimum width at increased font scales so the city pane can retain two usable columns. Narrow/short windows use the existing phone composition and phone tab slide/fade. Short wide city grids are vertically centred; longer grids scroll within available height. The existing hoisted city grid and collapse state are reused across responsive arrangements.

## Lighting

The shared clock now has layered, cached radial illumination, a broad faint ambient shadow, a white luminous rim and predominantly neutral face. Location cards retain their exact typography, corners, icon layout and lower-right wave artwork, with stronger diffuse native elevation, an upper white edge highlight, lower-right selected-card lighting and a broad static theme-coloured glow below the selected card. Ambient background lighting remains local to these clock pages. Dark effects are restrained. No bitmap assets, dependencies, full-screen blur pass or continuous glow animation were added.

## Configuration / direct navigation fix

Actual resize testing exposed an existing issue: the Activity's old launch tab intent was reapplied after recreation, overriding the user's newer tab selection. MainActivity now numbers navigation requests and saves that identity; ClockApp saves the handled identity. Restoring an Activity does not replay its consumed request. New intents, including repeated intents to the same destination, still override the current tab. Launcher/unknown intents preserve current selection, and default startup preferences still hydrate only after settings load.

## Files

Modified existing task-start files:
- app/src/main/java/app/expressiveclock/MainActivity.kt
- app/src/main/java/app/expressiveclock/ui/ClockApp.kt
- app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockHeader.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt
- app/src/main/java/app/expressiveclock/ui/screens/WorldCitySurface.kt

Created:
- app/src/main/java/app/expressiveclock/ui/screens/WideClockPages.kt
- app/src/main/java/app/expressiveclock/ui/StartupNavigation.kt
- app/src/test/java/app/expressiveclock/ui/screens/WideClockPagesTest.kt
- app/src/test/java/app/expressiveclock/ui/StartupNavigationTest.kt
- docs/V015_WIDE_CLOCK_AND_LIGHTING.md

No files deleted. Removed the obsolete combined ExpandedAlarmsScreen function while retaining the actual alarm tile/editor components. No changes to ClockStore, ClockViewModel, alarm scheduling/audio, widgets, Timer/Stopwatch source, palettes, versions or dependencies.

## Verification

- ./gradlew assembleDebug testDebugUnitTest: BUILD SUCCESSFUL; 55 tests, zero failures/errors.
- Added coverage for compact/landscape/resized/font-scale layout boundaries, saved in-flight hand motion, startup hydration, restored navigation versus consumed intent, and new direct intents for all four destinations. Existing timezone, daylight, hand-retargeting, snapping, non-overlap, text and sound-playback regressions passed.
- git diff --check passed. Compared task-start app/src/docs SHA256 snapshot: only the six intended existing source files changed; no snapshot files missing.
- Installed and inspected actual OPPO Find N6 renders: wide Alarm with two card columns, wide World with two city columns, selected San Francisco and correct local/selected times, light/dark, wide portrait/landscape, narrow/phone fallback, expanded and collapsed phone World Clock, and original phone Alarm.
- Exercised rapid tab reversals, selected-city retention across narrow/wide configuration changes, rotation and theme changes, animations disabled, repeated explicit Alarm intents, inline New Alarm and contextual Add Location access. An unsaved name draft survived World/Alarm switching and was cancelled. No alarm was saved by these UI checks.
- Inspected a slowed in-flight transition capture with one clock between panes and continuing hand positions; screenshot 06 was captured with animation scale 8 and should not be used to judge normal timing. OPPO rejected screenrecord file creation, so no video/frame-rate claim is made.
- Temporary OPPO resolution, display-state, rotation, night and animation-scale overrides were removed/restored; stay-awake returned to 0. No startup-page preference was changed.

## Limits and reference differences

Lighting follows the selected theme rather than forcing a particular blue; native Android elevation differs from the mockup's rendered light. Pixel device checks were not repeated. Pure-black appearance, exhaustive maximum-font/non-Latin layouts, actual split-screen app interactions and sustained frame-rate profiling were not device-verified. Window resizing and unit boundaries cover responsive fallback, but platform display-switch snapshots are outside the shared navigation animation; no claim of frame-by-frame fold-animation verification is made. Alarm ringing/audio and widget device probes were not repeated for this UI-only pass; their source remains unchanged and applicable unit regressions passed.

## Delivery and repository safety

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-wide-clock-glow-20261117-145015.apk
Screenshots and build log: /Users/sambeckman/Downloads/ExpressiveClock-wide-clock-review-20261117-145015

Final branch: main. Working tree remains dirty with existing uncommitted v0.1.5 work preserved. Nothing staged, committed, pushed, merged, tagged, released or published. Existing Downloads APKs were not overwritten.
