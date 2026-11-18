# World Clock transition lighting fix

## Changes

Modified two existing components: `WorldClockPanel.kt` and `WideClockPages.kt`. No source files deleted, dependencies added or versions changed.

Fractional-alpha layers rasterised the city cards into bounds that omitted their exterior shadows and selection badges. Added native Compose `LayerOutsets` to the card entrance and containing pane fade layers, preserving overflow without changing layout or the existing opacity compositing strategy. The API is available in the project's existing Compose version.

Exterior card lighting now fades in over 320ms after the pane and individual card entrances settle, and fades out over 120ms when leaving. Readiness follows animation state rather than a fixed delay. Lighting remains attached to each card. This avoids the sudden shadow/glow appearance when partial-opacity rendering returns to direct rendering. New-location entrances use the same readiness check. Existing stagger, clock movement, card styling and selection remain intact; the phone's stationary grid starts fully lit.

## Verification

- `./gradlew assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL; 55 unit tests, zero failures/errors.
- `git diff --check`: passed.
- Source snapshot comparison: only the two components above changed during this task.
- Installed and checked on Pixel 11 Pro Fold and OPPO Find N6 in wide layouts. Inspected 4x-slowed transition captures at partial card opacity and during light fade-in. Selection badges remained circular and overflow lighting had no rectangular cuts in inspected frames.
- Rapid navigation reversals exercised on Pixel during the clipping checks. Final lighting revision inspected through slowed transitions on both devices.
- Temporary animation scales restored (Pixel unset/default, OPPO 1.0); OPPO stay-awake restored to 0 and temporary opened-posture override removed.
- Dark/pure-black, rotation and exhaustive lifecycle tests were not repeated in this targeted pass. Screenshot sampling is visual QA, not frame-rate profiling.

## Delivery

- APK: [ExpressiveClock-debug-transition-lighting-20261117-183359.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-transition-lighting-20261117-183359.apk) (copied without overwriting an existing APK; SHA-256 matches build output).
- Screenshots and build log: [ExpressiveClock-transition-lighting-20261117-183359](/Users/sambeckman/Downloads/ExpressiveClock-transition-lighting-20261117-183359).
- Report created: `docs/V015_WORLD_TRANSITION_LIGHTING_FIX.md`.
- Final branch: `main`. Existing modified and untracked v0.1.5 work preserved; working tree remains dirty. Nothing staged, committed, pushed or published.

## Follow-up: lighting travels with cards

Removed the post-entrance readiness gating and separate 320ms lighting fade from WorldClockPanel.kt and WideClockPages.kt. Glow and shadow are present inside the moving card from the beginning and share the existing card/pane opacity. Retained LayerOutsets and viewport gutters to preserve overflow. No other animation timings or card styling changed. No files created or deleted in this follow-up; this report updated.

`./gradlew assembleDebug testDebugUnitTest` passed: 55 tests, zero failures/errors. `git diff --check` passed. Installed on Pixel; unfolded screenshot validation was unavailable because it is now folded (inactive inner-display capture was black). Temporary animation scale restored to unset/default. OPPO not retested this follow-up.

APK: /Users/sambeckman/Downloads/ExpressiveClock-debug-glow-with-cards-20261117-184416.apk

Branch remains main, with existing modified/untracked work preserved. Nothing staged, committed or published.
