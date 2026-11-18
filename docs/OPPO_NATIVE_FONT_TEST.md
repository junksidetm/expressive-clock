# OPPO native-font prototype check — 16 November 2026

Device: OPPO Find N6 (ADB model CPH2765, device OP6271L1), Android 16 / API 36.
Launcher: com.android.launcher/.Launcher (ColorOS). Serial: 3B161P00QM400000.

ADB connection was authorized. Installed the existing debug prototype APK using
adb install -r; installation returned Success. No source changes or new build
were required. No app data clear, uninstall, system time change or permission
settings change was performed. The accessibility screen was left without enabling
the service; accessibility is not needed for widget testing.

A digital widget (ID 41) was added to ColorOS Launcher. Its actual home-screen
rendering displays the explicit message:

“Native font prototype unavailable: this device’s Google Sans Flex could not be
verified. No substitute clock font has been selected.”

This verifies the prototype's incompatible-font guard renders visibly on Android
16 / ColorOS. It is not a crash or a frozen-clock result: this path deliberately
renders a status message rather than a clock. The normal digital-live checker
therefore reports NOT_VISIBLE, not PASS. No native TextClock update test for this
selected font can be claimed from this status screen.

Read /system/etc/fonts.xml over ADB. It declares Roboto Flex and a condensed
system family; no google-sans-flex declaration was found there. The Pixel's
/product/etc/fonts_customization.xml path does not exist on this OPPO. These
observations support a missing compatible family, but the guard's generic message
alone does not distinguish a missing family from a different/incompatible one.
Roboto Flex is not treated as an exact replacement for Google Sans Flex.

Captured screenshot/UI evidence:
/var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-140107-818104

Outcome: the named Google Sans Flex prototype cannot be treated as a portable
Android 12–16 solution. The successful Pixel result is device-specific. No silent
fallback, preview alteration, style removal or replacement font was implemented.
The widget selection was left unchanged after the guard screenshot.

Existing Serif, Monospace or Cursive options can be tested separately to assess
native clock ticking on ColorOS; they are not substitutes for the requested
Condensed/Flex designs. Such a timing test remains outstanding on this device.

Only this document and an appended validation note in NATIVE_FONT_PROTOTYPE.md
were changed for this investigation. No files deleted. Existing work preserved;
branch main, unstaged/untracked changes retained; no commit, push or publication.

## Follow-up production-architecture investigation

The previously outstanding native-clock control test has now passed for
Serif/Regular/Stacked on ColorOS: seven settings-free samples over 185 seconds,
correct time after owner-authenticated lock/unlock, and correct time plus further
minute advancement after a verified launcher-process restart. The widget is left
on this explicitly chosen working Serif test style, not the earlier unavailable
message. No automatic fallback architecture or typography source changes were
implemented. Detailed results and two production options are documented in
PRODUCTION_WIDGET_ARCHITECTURE_REVIEW.md. Analog on this OPPO remains untested.
