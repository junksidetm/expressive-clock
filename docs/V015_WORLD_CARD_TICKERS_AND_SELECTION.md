# World Clock card tickers and selection emphasis

Modified existing source files:
- `app/src/main/java/app/expressiveclock/ui/WorldClockDigitalReadout.kt`: extracted the header's existing reel motion, changing-digit staging/stagger, blur and reduced-motion handling into a private shared component. Added WorldClockCardTime using the same motion with the card's existing explicit Google Sans Flex typography and measured tabular character advances. The header retains its timer-style glyphs, geometry, colours, timing and location-change direction. Card times retain hours/minutes (no seconds added), and tick only when displayed characters change. Newly introduced character slots begin blank rather than flashing their target glyphs. Accessibility presents the whole time rather than individual reels.
- `app/src/main/java/app/expressiveclock/ui/screens/WorldClockPanel.kt`: replaced static card time labels with that shared reel presentation, including the local-time card. During deletion selection mode, unselected cards (including local time) animate to 68% opacity; selected cards spring to 1.025 scale, lift 2dp and gain 3dp shadow elevation. Grid cell sizes/keys remain unchanged. Extended layer bounds retain the glow, shadow and checkmark during the fade/pop. Existing tick badges remain deletion-only; normal active-location styling is unchanged.

Created: this report. No files deleted, dependencies or versions changed. Existing World Clock selection/deletion, hand/city-name animations, timezone calculations, bottom navigation, alarms, timers, stopwatch, widgets and Installer Helper preserved.

Verification: `./gradlew assembleDebug testDebugUnitTest` succeeded: 61 tests, 0 failures, 0 errors. `git diff --check` passed. Reviewed the extracted header code against a pre-change copy and verified that per-reel identity excludes the rest of the time string, so unchanged digits do not restart. No connected-device or rendered motion tests run, per user preference; typography, pop intensity and animation quality remain for owner visual review.

APK: [ExpressiveClock-debug-card-tickers-20261117-220607.apk](/Users/sambeckman/Downloads/ExpressiveClock-debug-card-tickers-20261117-220607.apk); SHA-256 matches build output, no existing APK overwritten. Installer delivery remains inactive; no upload or notification attempted.

Final branch main, working tree remains dirty with existing modified/untracked work preserved. Nothing staged, committed, pushed or published.
