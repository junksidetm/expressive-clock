# v0.1.5 World Clock grid and animation polish

Completed locally on `main`. This report covers the original animation/editor pass and the two grid refinements supplied while that work was active. The interim tall single-column cards were superseded by the final compact grid.

## Final implementation

- Shared responsive two-column World Clock grid in portrait and the existing right pane. Falls back to one column when width/font scale cannot comfortably support two. Eight dp gaps; an odd final card is centred at the normal card width. Saved cities, contextual Plus, selected city and mechanical hand rendering are retained. No map, extra page or horizontal grid scrolling was introduced.
- Final cards are 108dp at normal font scale, about 11.5% shorter than the interim 122dp grid. Larger font settings increase card height. City/date/time hierarchy uses tighter spacing, medium-weight tabular time digits, soft theme-aware tonal gradients, a faint border and restrained 2dp shadow. The selected card has a theme gradient/outline and an upper-left check badge that does not reflow its content.
- Gold suns and blue-grey moons sit in translucent circles. Existing IANA representative-city coordinates and a NOAA solar-position approximation determine sunrise/sunset, including seasonal and polar conditions. Calculation is cached per minute and uses UTC, so civil timezone/DST changes do not move the calculated sun. Unknown coordinates retain the previous estimated day/night cue. Representative coordinates are not GPS; horizon, weather and atmospheric conditions are not modelled. Method: [NOAA solar equations](https://gml.noaa.gov/grad/solcalc/solareqns.PDF).
- Header remains directly scroll-driven while dragging, with the immediate coordinated crossfade retained. Release settles to expanded/collapsed at 50%, or in the direction of a strong flick. Settling is cancellable by the next drag. Fully collapsed headers pass scrolling/flinging through to the grid. Idle restoration also settles partial saved progress.
- Dock icons and selected circle share an additional 8dp horizontal inset; first/last highlight breathing room is symmetrical without increasing the pill width. World uses a lightweight projected globe: continents pass the visible horizon through a 600ms rotation rather than mirroring a flat vector. Repeated taps retarget from the current rotation and disabled animations are respected.
- New/Edit Alarm header is pinned outside the internal scroller. Material's animated keyboard insets are used once, with an inset-aware height cap and minimal focused-field scroll, retaining the rounded sheet and top margin. The editor's entered state stays alive through focus/keyboard changes.
- Alarm Sound is now a full-screen page with Back/Apply, grouped default/Silent/custom/system choices and per-row preview/stop buttons. Back discards the page's draft; Apply updates the underlying editor. There is no permanent bottom preview panel. Audio focus, fallback and persisted custom-file access remain shared with the established playback implementation. The only audio-class change is an optional natural-preview-completion callback after the existing stop/cleanup path.

## Files changed in this pass

Modified:

- `app/src/main/java/app/expressiveclock/ui/ClockApp.kt`
- `app/src/main/java/app/expressiveclock/ui/ClockDock.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPresentation.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/NewAlarmPickerSheet.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmEditSheet.kt`
- `app/src/main/java/app/expressiveclock/alarm/AlarmAudio.kt` — optional preview-completion notification; ringing behavior unchanged.

Created:

- `app/src/main/java/app/expressiveclock/ui/RotatingGlobe.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockSnap.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/CityDaylight.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmEditorDragHandle.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AlarmSoundPage.kt`
- `app/src/test/java/app/expressiveclock/ui/screens/WorldClockSnapTest.kt`
- `app/src/test/java/app/expressiveclock/ui/screens/CityDaylightTest.kt`
- This report.

Deleted:

- The previously untracked `app/src/main/java/app/expressiveclock/ui/sheets/AlarmSoundSheet.kt`, superseded by the full-screen page. Its title resolution, URI permission and preview/fallback handling were preserved.

## Verified

- Final `./gradlew assembleDebug testDebugUnitTest`: **BUILD SUCCESSFUL; 38 tests passed**, no failures. Includes threshold/flick/end-state snapping tests, seasonal/polar daylight checks and existing hand-continuity, timezone/DST, audio, notification and widget regressions.
- `git diff --check` passed. Final APK copy hash matches the generated debug APK.
- OPPO Find N6: selected a different city and confirmed clock/date update, one checked city card, below-threshold return to analogue and above-threshold completion to digital. The final tonal grid also collapses correctly even when all cards fit without grid scrolling. Captured final expanded, selected, collapsed, dark and unfolded two-pane grid states.
- OPPO: New Alarm name editing retained the typed draft; drag handle and Cancel/New Alarm/Save stayed visible above the keyboard, with rounded top corners and background still visible. Dismissing the keyboard restored the exact original header bounds. Full-screen sound page, inline preview/stop, Back without applying and Apply to the editor were checked; entered name remained intact. Unsaved test drafts were cancelled.
- OPPO dark grid and sound page were visually inspected. Temporary night mode, device-state override and USB stay-awake setting were restored to their original values (`custom_schedule`, no override, and `0`). No saved cities were added or deleted by the UI checks.
- Debug APK installed on both phones. Pixel grid appearance was inspected through its app hierarchy; its screenshots/input were unreliable because of stale Recents surfaces, so keyboard visual checks relied on the OPPO after the owner unlocked it.
- Existing `scripts/adb_alarm_probe.py` passed on both Pixel 11 Pro Fold and OPPO Find N6: custom WAV content URI, invalid-source and bundled fallback, audio focus restoration, scheduled alarm/ringer, stale-start protection, native timer expiry, upcoming notification action and stored occurrence skip. First Pixel attempt reported a missing upcoming notification; a retry passed. This intermittent result is retained in the evidence, not presented as an unqualified all-attempts pass. The probe cleans up its own test alarm/audio state; speaker audibility is not asserted.
- All unrelated pre-existing source/docs were preserved. Alarm scheduling/services/notifications, DataStore, timer/stopwatch, hybrid widgets, generated palettes, app versions and dependencies were not modified. The analogue renderer and mechanical hand-motion implementation are byte-for-byte unchanged from this pass's starting snapshot.

## Remaining verification limits

No frame-by-frame performance profile or reliable motion recording was obtained, so repeated reversal/globe animation feel still merits hands-on review. Pixel keyboard capture was not reliable. Exhaustive large-font, very narrow viewport, pure-black mode, locale and rotation combinations were not device-tested; responsive sizing and theme tokens are implemented. Solar cues are approximate representative-city conditions, not surveyed local sunrise/sunset measurements. The current full-screen OpenDocument flow retains the previously tested permission code; this pass exercised custom content-URI playback through the regression probe rather than repeating the document-picker restart test.

APK: `/Users/sambeckman/Downloads/ExpressiveClock-debug.apk`

Updated portrait screenshot: `/Users/sambeckman/Downloads/ExpressiveClock-grid-polish-review-20261117-114457/oppo-tonal-grid-selected.png`

Screenshots, hierarchies, build and probe logs: `/Users/sambeckman/Downloads/ExpressiveClock-grid-polish-review-20261117-114457`.

Final branch: `main`. Working tree remains dirty with existing modified/untracked v0.1.5 work plus this pass. Nothing staged, committed, pushed, merged, tagged, released or published.
