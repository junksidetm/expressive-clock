# Project Version History: Expressive Clock

## Libraries & Tools
| Library / Tool | Current Version | Description |
| :--- | :--- | :--- |
| Android Gradle Plugin (AGP) | 9.2.1 | Android build tools |
| Kotlin | 2.4.0 | Language runtime & compiler |
| Jetpack Compose BOM (Alpha) | 2026.07.01 | Compose library compatibility manager |
| Material 3 (Compose) | 1.5.0-alpha25 | Material Design 3 Expressive UI components & tokens |
| Compose UI Text / Preview | BOM-managed | Compose UI foundation |
| Material Icons Extended | 1.7.8 | Extended Material icon set |
| Activity Compose | 1.13.0 | Activity entrypoint & edge-to-edge support |
| Lifecycle Runtime Compose | 2.11.0 | Lifecycle-aware StateFlow collection |
| Core KTX | 1.19.0 | Android KTX core extensions |
| Core SplashScreen | 1.0.1 | Android 12+ splash screen API compat |
| DataStore Preferences | 1.2.1 | Asynchronous transactional persistent storage |
| Graphics Shapes | 1.0.1 | Morphing polygons & expressive rounded shapes |
| Play In-App Update | 2.1.0 | Google Play in-app update API |
| JUnit | 4.13.2 | Unit testing framework |
| AndroidX Test Core | 1.7.0 | AndroidX testing primitives |

---

## Changelog

### Entry 001: Fork Initialization & Workspace Governance Setup
- **Timestamp:** 2026-11-15 19:43:30 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Forked upstream `samuelbeckman/expressive-clock` (derived from `rbouaf/material-clock`) to `junksidetm/expressive-clock` and established workspace governance.
- **Files Created:**
  - `GEMINI.md` (v1.0.0): Foundational workspace and coding mandates, low-memory precautions, commit verification requirements.
  - `Version.md` (v1.0.0): Immutable version history and audit log.
- **Files Modified:**
  - `D:/code/GEMINI.md`: Registered repository fork lineage for `expressive-clock`.
- **Notes:** Repository prepared for deep animation and architectural study and surgical enhancements. Local heavy build commands disabled per 4GB RAM mandate.


### Entry 002: Alignment to Workspace Single-GEMINI.md Architecture
- **Timestamp:** 2026-11-15 19:47:15 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Removed local `GEMINI.md` in repository root to strictly adhere to the workspace standard (one master `D:\code\GEMINI.md` governing all repos, with individual `Version.md` tracking files per project).
- **Files Deleted:**
  - `expressive-clock/GEMINI.md`
- **Notes:** Workspace governance verified. Proceeding with in-depth animation analysis of the repository.


### Entry 003: Layout, Scaling, Overlap & Motion Refinements
- **Timestamp:** 2026-11-15 20:17:45 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Fixed multi-screen layout overlaps, scaling issues, button label truncation, stopwatch face sizing, and alarm picker styling.
- **Files Modified:**
  - `app/src/main/java/app/expressiveclock/ui/screens/TimersScreen.kt`:
    - Added vertical scroll resilience to timer body and added spacer separation before controls so presets never crowd or overlap the Start button.
    - Optimized keypad key height from 76dp to 62dp and preset buttons from 54dp to 48dp for balanced phone proportions.
    - Fixed `RunningTimerBody` circle bounds from overflowing (`maxWidth + 24.dp`) to `minOf(maxWidth - 20.dp, 330.dp)`.
    - Proportionalized readout digit dimensions (`digitWidth`, `colonWidth`, `capHeight`) to mathematically guarantee that `00:00:00` fits cleanly inside the undulating wave ring.
    - Removed redundant horizontal padding on `TimerControls` to expand available row width for the 3-button control set.
  - `app/src/main/java/app/expressiveclock/ui/screens/StackedDigits.kt`:
    - Refined `WidePill` compact mode metrics (horizontal padding 6dp, icon 18dp, text typography `labelLargeEmphasized`), preventing truncation of "Cancel" and "Resume" into "Can" and "Res".
  - `app/src/main/java/app/expressiveclock/ui/screens/StopwatchScreen.kt`:
    - Replaced hardcoded `height(540.dp)` on the stopwatch face with a responsive `BoxWithConstraints` capped at `minOf(maxWidth - 32.dp, 330.dp)` and wrapped content height, preventing the circular dial from stretching into an oval.
    - Enabled `LazyColumn` user scrolling and added bottom padding to ensure controls ("Lap", "Reset", "Start/Stop") never fall under the floating navigation bar.
    - Proportionally tuned central readout numerals to 38sp.
  - `app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt`:
    - Scaled the alarm card toggle `Switch` (0.82x on phone) and provided responsive `DayLetters` block width (`62.dp..82.dp`) to completely eliminate toggle overlap with the week day letters.
  - `app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt`:
    - Increased spacing between the clock setting wheel frame and AM/PM buttons (10dp) so it sits a little below.
    - Increased spacing between the AM/PM selector and the week day chips (14dp).
    - Styled Sunday ("S") chip in vibrant red (`Color(0xFFE53935)`) across unselected and selected states.


### Entry 004: GitHub Actions Remote Debug Build Workflow
- **Timestamp:** 2026-11-15 20:22:30 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Configured automated GitHub Actions workflow to build and package debug APK artifacts for phone testing, complying with the zero local build mandate for low-memory environments.
- **Files Created:**
  - `.github/workflows/debug-build.yml` (v1.0.0): Builds `:app:assembleDebug` and uploads `ExpressiveClock-debug-apk` artifact on push, PR, and manual dispatch.


### Entry 005: Decouple Release Signing from Debug / Non-Release Builds
- **Timestamp:** 2026-11-15 20:25:50 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Made release signing configuration conditional on the presence of `clockStoreFile` in `app/build.gradle.kts`. Prevents configuration failures during debug builds and remote CI verification when release credentials are not present.
- **Files Modified:**
  - `app/build.gradle.kts`


### Entry 006: Successful Remote GitHub Actions Build & APK Delivery
- **Timestamp:** 2026-11-15 20:33:00 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Verified complete remote CI build on GitHub Actions (Run ID: [43727084729](https://github.com/junksidetm/expressive-clock/actions/runs/43727084729)). Built in 3m54s. Staged debug APK at `C:\Users\Abhi\Downloads\ExpressiveClock-debug.apk` for device installation.


### Entry 007: Upstream PR #5 Alignment & Defensive Keystore Decoupling
- **Timestamp:** 2026-11-19 11:14:00 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Rebased upstream PR #5 on latest upstream main (`v0.1.5`) dropping superseded UI changes. Decoupled release signing with defensive keystore check and automatic fallback to debug signing in `app/build.gradle.kts` per maintainer (`@samuelbeckman`) and contributor (`@rileyhawk1417`) request. Cryptographically signed commit (`a650901`) with SSH key and pushed to `junksidetm:fix/ui-layout-and-scaling-refinements`, updating PR #5 with 1-file diff and 0 conflicts.
- **Files Modified:**
  - `app/build.gradle.kts`


### Entry 008: Specific Date Alarms & Permanent Countdown Indicator
- **Timestamp:** 2026-11-19 11:23:00 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Implemented Specific Calendar Date Alarms and Permanent Live Countdown Header on Alarms screen per creator (@samuelbeckman) roadmap and modern Samsung Clock design paradigms.
- **Files Created:**
  - `.github/workflows/debug-build.yml`: Automated CI debug APK build workflow.
- **Files Modified:**
  - `app/src/main/java/app/expressiveclock/core/Model.kt`: Added `specificDate: LocalDate?` to `Alarm`, updated `isOneShot` evaluation, exact `nextFire()` calculation, and expressive `repeatLabel()` formatting.
  - `app/src/main/java/app/expressiveclock/core/AlarmEdits.kt`: Preserved dismissal only when `specificDate` is identical.
  - `app/src/main/java/app/expressiveclock/data/ClockStore.kt`: Fully backwards-compatible JSON serialization and deserialization of `specificDate` in Preferences DataStore.
  - `app/src/main/java/app/expressiveclock/ui/sheets/AlarmPickerUi.kt`: Added `SpecificDateScheduleRow` and integrated Material 3 Compose `DatePickerDialog` allowing future date scheduling with seamless mutual exclusivity against weekly day chips.
  - `app/src/main/java/app/expressiveclock/ui/sheets/NewAlarmPickerSheet.kt`: Wired `specificDate` state handling and saving.
  - `app/src/main/java/app/expressiveclock/ui/sheets/AlarmEditSheet.kt`: Wired `specificDate` state handling and saving.
  - `app/src/main/java/app/expressiveclock/ui/screens/AlarmsScreen.kt`: Added `NextAlarmCountdownCard` as a persistent header item spanning the alarm grid with battery-efficient `rememberMinuteTicker()` and added `SpecificDateBadge` to alarm tiles.
  - `app/src/test/java/app/expressiveclock/alarm/UpcomingAlarmTest.kt`: Added 4 unit test cases verifying future date scheduling, past date rejection, skip handling, and label generation.


### Entry 009: Upstream Pull Request #13 Submission & Remote Verification
- **Timestamp:** 2026-11-19 11:26:00 +05:30
- **Author:** Elite Android Engineer & Antigravity Assistant
- **Status:** Complete (100%)
- **Target:** Submitted official upstream [Pull Request #13](https://github.com/samuelbeckman/expressive-clock/pull/13) to `samuelbeckman/expressive-clock:main` containing the complete implementation for Specific Calendar Date Alarms and Permanent Countdown Indicator. Remote GitHub Actions debug APK build verified with 100% success ([Run 44468247472](https://github.com/junksidetm/expressive-clock/actions/runs/44468247472)).
- **PR URL:** https://github.com/samuelbeckman/expressive-clock/pull/13
