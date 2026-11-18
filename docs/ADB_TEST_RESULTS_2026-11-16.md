# Pixel ADB test results — 16 November 2026

Device: Pixel 11 Pro Fold, serial 65261FDDJ002GB, reporting Android 17. Results do not establish Android 12–16 compatibility.

| Check | Result | Evidence / limits |
| --- | --- | --- |
| ADB / authorization | Reconnected and authorized | SDK platform-tools ADB 34.0.5; USB connection restored at 13:19 |
| Screenshot / UI automation | Passed | Native screenshots pulled as PNG; UI XML exposes widget text and analog description |
| Build and install | Passed | assembleDebug, testDebugUnitTest; adb install -r returned Success; no data clear/uninstall |
| Digital live updates | Passed | Seven samples from 11:08:59 to 11:12:04 matched device time without opening configuration |
| Analog live updates | Passed for sampled period | Descriptions advanced; inspected screenshot sequence shows minute-hand movement; no exact-angle assertion |
| Logcat | No relevant crash observed | No FATAL EXCEPTION, InflateException or RemoteViews apply failure found in observed window |
| Condensed / Flex typography | Failed visual matching | Paired preview/host captures confirm fallback font discrepancy; no approximation implemented |
| Serif | Basic visual checkpoint passed | Complete digits, no obvious clipping in sampled horizontal/Regular layout; not all weights/layouts covered |
| Monospace | Unfolded checkpoint passed | Preview and host captured at 13:20; complete digits, no obvious clipping in horizontal/Regular; remaining combinations unverified |
| Folded layout | Captured | Existing horizontal widget captured during timing run |
| Unfolded layout | Captured and inspected | Inner display 2076×2152, fold state 2; correct digital time, analog visible, no obvious clipping/overlap in sampled layout |
| Resizing | Inconclusive | Launcher resize gesture left the original bounds unchanged; not counted as a successful resize test |
| Rotation | Unfolded landscape checkpoint passed | UI rotation=1; digital time matches device time; analog visible; no obvious clipping/overlap; no setting override |
| Exact 11:00 alignment | Pending | No system time changes; native fractional-minute behavior documented separately |

The initial font matrix restored Flex. An interrupted retry left restoration
unverified, but this has now been resolved: the latest run selected Flex and Regular,
saved configuration, and captured restored-preview and restored-host. Screenshots
were inspected; the home widget shows the correct time. The unfolded form requires
scrolling to the weight row; the reusable script now handles that case.

The attempted digital resize did not change content bounds: before and after both
show [57,933][819,1648]. The layout remains at its original bounds. No resize pass
is inferred from a successful ADB gesture command.

Evidence directories from this session:

- Timing: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-110859-432497
- Initial font matrix: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-111306-188550
- Interrupted Monospace retry: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-111512-724056

Reusable commands and assisted matrix are in ADB_TESTING.md. Rendering restrictions
and alternatives are in WIDGET_RENDERING_FINDINGS.md. No polling, system setting
changes, time changes, commits, pushes or publication were performed.

Latest evidence:

- Unfolded before restoration: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-131938-421844
- Monospace unfolded preview/host (restoration failed in this run): /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-131958-107589
- Successful Flex/Regular restoration: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-fonts/20261116-132111-507930
- Unchanged bounds after resize attempt: /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-132234-878640

- Unfolded landscape (UI rotation=1): /var/folders/tk/0y2v9rnn19sbr8wbfq9jsv4c0000gn/T/expressive-clock-adb/20261116-132455-131613
