# Alarm / World navigation refinement

Local changes extend the existing uncommitted v0.1.5 work; no alarm scheduling,
audio, widgets or stored alarm times changed in this refinement.

- AlarmSettings.cards24Hour defaults false and affects only large AlarmTile
  rendering through a composition local. The original meridiem layout remains.
- Independent DataStore keys persist card format, always-visible indicators and
  the versioned worldNavigationDemoV015 completion marker.
- Original dock restored (Alarms, Timers, Stopwatch, detached Add action).
- A saveable native HorizontalPager owns the two Alarm-section pages. Native
  directional touch slop protects vertical lists; editing/selection disables paging.
- World page reuses WorldClockPanel, including the dotted map, selection, city
  list and Add Location. Expanded Alarm split-pane remains intact. Both use the
  same ViewModel city stream and saved selection, with system time format.
- Glass pill has two accessible 44dp tap targets and swipe-progress orb size/colour.
  When always-visible is off, scrolling shows it until two seconds after settling.
- Discovery runs only in resumed, loaded, suitable Alarm content; editing/settings/
  explicit startTab navigation suppress it. Touch cancels, animations-disabled
  skips it, completion/cancellation during the demonstration is persisted. The
  completion key is versioned to this update, not reset on ordinary app updates.

## Verification

assembleDebug testDebugUnitTest passed (24 Kotlin tests); Python suite 12 passed.
git diff --check passed. No new test dependency or production polling added.

Pixel 66231FDDJ003QS was available/unlocked. Installed debug APK without data clear.
Live UI hierarchy confirmed the discovery World page followed by return to Alarms,
horizontal swipe to World, orb tap back, shared city list/Add Location, and card
format change 1:40 PM -> 13:40 / 5:30 PM -> 17:30. Restored card preference to 12h.
With indicators disabled, the idle hierarchy omitted both orb controls. DataStore
contained all three new keys and the completed demo marker. Restored always-show
preference to ON after testing. No user alarms/cities were changed.

Screenshot outputs were stale recents surfaces on both physical display captures,
so they are not visual verification of the current foreground UI. Smooth animation,
clipping at accessibility sizes, landscape/unfolded presentation and full process-
restart demo behavior need further device verification. Existing split-pane code
was preserved, not physically re-tested. OPPO was disconnected; no OPPO pass claimed.
No security, rotation, system time or power settings changed.

APK: ~/Downloads/ExpressiveClock-v015-alarm-world-debug.apk
Modified for this refinement: data/Settings.kt, data/ClockStore.kt,
ui/ClockApp.kt, ui/ClockDock.kt (restored to baseline), screens/AlarmsScreen.kt,
screens/SettingsScreen.kt. Created ui/AlarmWorldPages.kt and this report.
No files deleted. main remains dirty/unstaged; no commit/push/release performed.
