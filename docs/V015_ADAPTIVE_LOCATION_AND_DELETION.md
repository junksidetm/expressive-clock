# Adaptive location sheet, unfolded modals, city deletion and clean globe

## Implemented

Add Location now uses the same modal presentation on phones and wide windows. It opens around 44% of window height (capped and adjusted for short windows), with drag handle, Cancel/centred title, a softly filled rounded search field and real offline tzdb suggestions. Saved zones and the local-time card's zone are excluded. Suggestions are preferred IDs resolved against the existing platform database; no fabricated results or network source. There is no recent-search history in the existing app, so none is invented or tracked.

Focus or an upward handle drag expands the search sheet. Only the expansion fraction is animated (260ms); platform IME inset frames directly determine the remaining available content height, avoiding a second lagging keyboard tween. Material owns drag settling, scrim, entrance/exit and predictive back. The header/search stay outside the scrollable results. Clear search, matching by city/region/zone, lightweight result cards and an empty-results state are included. A closing guard hides the keyboard and completes the sheet hide before adding/dismissing. Cancellation releases the guard without committing; the New Alarm and shared deletion-confirmation callbacks use the same cancellation-safe sequencing. Existing new-city reveal/scroll/entrance logic is retained. Duplicate exclusion is also enforced atomically by DataStore.

Unfolded Plus actions no longer replace panes inline: both New Alarm and Add Location use the existing modal path, giving the same bottom-up entrance, downward dismissal and coordinated scrim as phones. Existing inline editing of saved alarms is preserved. New Alarm Save/Cancel callbacks now wait for a completed hide; cancellation or repeated taps cannot invoke a premature duplicate callback. Existing alarm/time/audio content remains intact.

World Clock cards use long press to enter selection with LONG_PRESS haptics, the same top SelectionToolbar as Alarms, and the same confirmation/cancel sheet (parameterised for locations). Normal taps select cities; in selection mode they toggle deletion selection, matching Alarms. The accessibility Delete action opens the same confirmation rather than deleting immediately. Local time is not deletable. Horizontal drag deletion and the old swipe tutorial were removed from this UI. Persisted legacy preference fields are retained for compatibility.

Deleting the active saved location chooses the first remaining saved location, or local time if none remain. DataStore city mutations use an atomic transform and retain stable zone-ID identities. Exiting cards remain in their slots for 360ms, with Alarm's 260ms fade and .90 scale / .78 damping / 500 stiffness exit. Remaining cards reposition with exactly Alarm's 380 stiffness / .68 damping spring. Wide-pane height contraction waits for the exit, and existing glow layer outsets/viewport gutters remain intact. Selection state is hoisted in the existing shared WorldClockPresentation, surviving responsive layout changes without duplicating components.

Globe: removed explicitly drawn low-opacity meridians, the source of the faint lines. Projected land polygons are unioned and drawn once to avoid overlapping antialias seams. Retained Australia-facing rest position, circle silhouette, continent geometry, theme tint and 3D rotation.

## Verification

- `./gradlew assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL; 61 tests, 0 failures, 0 errors.
- Four new offline search/suggestion tests cover saved-zone exclusion, real-data membership, city/region matching, empty results and multi-word regional labels. Existing clock/timezone/DST, globe periodicity and other regression tests passed.
- `git diff --check`: passed. Reviewed scoped source changes and Material's bundled modal/inset APIs.
- Optional full `lintDebug`: blocked by 13 pre-existing project errors (widget API guards, theme API attribute, deprecated quick-settings API, existing Alarm indentation and existing Locale.getDefault usage). No unrelated lint fixes or baseline/version changes made. Build/tests were rerun successfully separately after final edits.
- No connected-device tests, screenshots or live keyboard/gesture/rendering validation performed, following the user's earlier request. Sheet height, IME coordination, deletion bounce and globe antialiasing remain for owner visual review. Source review and JVM checks do not establish physical-device animation quality.

## Files and repository safety

Modified existing files:
- `app/src/main/java/app/expressiveclock/ui/RotatingGlobe.kt`
- `app/src/main/java/app/expressiveclock/ui/ClockApp.kt`
- `app/src/main/java/app/expressiveclock/ui/ClockViewModel.kt`
- `app/src/main/java/app/expressiveclock/data/ClockStore.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/AddCitySheet.kt`
- `app/src/main/java/app/expressiveclock/ui/sheets/NewAlarmPickerSheet.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WideClockPages.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPresentation.kt`

Created: `app/src/test/java/app/expressiveclock/ui/sheets/LocationChoicesTest.kt` and this report. Deleted: none. All changes are confined to the requested UI/interaction paths and supporting atomic city persistence. Other pre-existing v0.1.5 work, Installer Helper, production signing, app versions, dependencies and manifest are preserved.

APK: [ExpressiveClock-debug-adaptive-world-20261117-214740.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-adaptive-world-20261117-214740.apk). SHA-256 `4876a95781d9a1b08b3b54b1053d6ced428589fe3180002258f790176c57ce8d` matches build output; no existing APK overwritten.

Build/test/lint evidence: [ExpressiveClock-adaptive-world-checks-20261117-214643](/Users/sambeckman/Downloads/ExpressiveClock-adaptive-world-checks-20261117-214643).

Final branch **main**; working tree remains dirty with existing modified/untracked work plus these local changes. No staging, commits, pushes, merges, tags, releases, uploads or publication.

## Search capitalization and deletion-only ticks

AddCitySheet.kt requests KeyboardCapitalization.Words so supported keyboards automatically capitalize the first letter of city-name words. Search matching remains case-insensitive; typed/pasted names are not forcibly rewritten. WorldClockPanel.kt now gives the deletion-selection badge its own animated visibility, independent of the active-location tint. Normal active locations keep the theme outline/gradient/glow and accessibility selected state, with no visible tick. Ticks appear only for batch-selected saved cities; local time has no deletion badge. This avoids an old active card briefly flashing a tick when another card enters deletion mode.

Only these two source files and this report modified in this follow-up. No files created/deleted. `./gradlew assembleDebug testDebugUnitTest` succeeded: 61 tests, 0 failures, 0 errors. `git diff --check` passed. No connected-device tests run per user preference.

APK: [ExpressiveClock-debug-city-search-selection-20261117-215554.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-city-search-selection-20261117-215554.apk), SHA-256 matches build output; no existing APK overwritten. Main branch, existing modified/untracked work preserved. Nothing staged, committed, pushed or published.
