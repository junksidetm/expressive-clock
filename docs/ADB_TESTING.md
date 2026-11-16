# Pixel widget testing

Requirements: Python 3, Android SDK platform-tools, USB debugging enabled, unlocked and authorized Pixel. Keep the widget home page visible. The script reads local.properties for the SDK before using PATH; override with ADB or select ANDROID_SERIAL / --serial.

Run from the repository root:

    python3 scripts/adb_widget_test.py check
    python3 scripts/adb_widget_test.py run --install --duration 185 --interval 30
    python3 scripts/adb_widget_test.py capture --label monospace-horizontal

The check command verifies connectivity, reads model/OS/display/fold state, captures a PNG and UI XML. The run command with --install builds assembleDebug and testDebugUnitTest, then installs with adb install -r. It never uninstalls or clears app data. Without --install, run observes the installed version. It sends HOME once and samples for three minutes. Each run gets a unique temporary output directory printed to the console. Artifacts include report.json, screenshots, UI XML, device metadata and (for run) Logcat. No logs are cleared. Captures may contain personal content; review before sharing.

The digital assertion compares visible widget text against device wall time before/after each screenshot/UI capture. PASS requires all samples to match over at least three distinct minutes. NOT_VISIBLE is not a pass. It tolerates minute rollover races. It does not establish second-level precision or date correctness. Analog descriptions and screenshot movement are evidence for review, not a numeric hand-angle assertion. Log matches are triage evidence, not automatic crash diagnoses.

The script never sets time, rotation, animation scale, screen timeout, fold state, or persistent device settings. It does not wake/unlock the phone or bypass its lock. If it sleeps or switches screens, the run will be inconclusive. Unlock it and rerun; do not change persistent timeout settings just for this test. Temporary files created by this run under /data/local/tmp are removed after each capture.

## Visual matrix and assisted checkpoints

For Condensed, Flex, Serif, Monospace (plus Cursive), test every supported weight in Horizontal, Stacked and Compact, with/without date and seconds. Open the existing widget configuration from the launcher, capture a labeled preview, save the selection, return home and capture a corresponding host checkpoint. Restore the original configuration at the end. Native font loading in RemoteViews is restricted, so a correctly updating clock can still fail typography comparison.

Inspect complete digits across minute/hour rollovers, overhangs, colon spacing, date separation, row bounds and analog pivots. Include 11:00:00 if it occurs naturally; do not set system time. Native AnalogClock includes elapsed seconds in minute angles.

Resizing and physical rotation/folding are assisted tests: use the launcher's resize handles, rotate the device, and fold/unfold it physically, then run capture with labels such as resize-small, landscape, folded and unfolded. Do not use settings put, wm size overrides, or cmd device_state state to force persistent overrides. The script reads wm size and cmd device_state print-state only. Launchers may restrict home rotation. UI dump bounds and captures document actual layouts; fold or rotation support is not inferred from the device model alone.

Repeat timing runs on each layout/launcher/device. Pixel Android 17 results cannot establish Android 12–16 compatibility. Third-party launchers need their own runs.

## Setup failures

- No device: use a data-capable USB cable and enable Developer options > USB debugging.
- unauthorized: unlock the phone and accept its RSA debugging prompt.
- offline: reconnect USB and check the phone; avoid restarting a shared ADB server.
- Multiple devices: use --serial as printed by adb devices -l.
- Signature mismatch on install: stop and resolve signing; never uninstall to get a pass.
- Screenshot black/locked: unlock and open the widget home page, then rerun.
- UI dump timeout: preserve the failure evidence and retry when animations settle.

## Automated font checkpoints on the current Pixel launcher

    python3 scripts/adb_widget_fonts.py --serial SERIAL --widget-id ID --restore-font Flex --restore-weight Regular

This opt-in scenario temporarily edits the named widget's font through the actual
configuration UI, captures paired preview/host checkpoints for Condensed, Flex,
Serif and Monospace, and restores the original font and weight in a finally block.
Pass the actual original values. Inspect the final restored-host checkpoint. UI
changes or a device disconnect can prevent restoration; do not assume a failed
run restored configuration. Other configuration fields and system settings are
not intentionally changed. This script searches by text and scrolls the configuration panel when controls are
below the fold. It stops if its text locators remain unavailable.
It collects visual evidence; it does not automatically declare font matching.

## Production native-font matrix

The production font profiles are documented in WIDGET_ARCHITECTURE.md. The font script
accepts --weights and --layouts for a full matrix; pass --restore-layout to restore the original layout too. Each
case has uniquely labeled preview/host evidence. Configuration scroll position
is normalized before each case. Screenshot capture waits for a UI hierarchy
dump before taking the PNG, reducing home-transition capture races.

When testing a widget hosted by a non-default launcher, use --home-component in
both adb_widget_fonts.py and adb_widget_test.py run. Example:

    --home-component com.google.android.apps.nexuslauncher/.NexusLauncherActivity

This launches the test host explicitly and does not change Android's default
launcher setting. A missing widget on another home screen is NOT_VISIBLE, not
a successful ticking test or proof of a renderer failure.

If foldable display metadata points to a stale/offscreen panel, compare explicit
SurfaceFlinger display captures against the UI hierarchy. After visually confirming
the correct panel, pass --display-id PHYSICAL_ID to adb_widget_test.py. This changes
only screenshot selection, never fold state or persistent display settings. Never
claim visual success from a native-text PASS when the captured panel disagrees.
